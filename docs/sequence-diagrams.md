# Verification Service Sequence Diagrams

This document contains sequence diagrams for the main endpoints of the Verification Service.

## 1. Verification Request Flow

This flow describes how a verification request is created and how the verification email is sent asynchronously.

```mermaid
sequenceDiagram
    participant User
    participant VC as VerificationController
    participant DB as Database
    participant RMQ as RabbitMQ
    participant VRH as VerificationRequestHandler
    participant NNL as NotifyNLService

    User->>VC: POST /request {email}
    activate VC
    VC->>DB: Create & Persist VerificationCode
    VC->>RMQ: Send Code ID to 'verification-requests'
    VC-->>User: 200 OK (Reference ID)
    deactivate VC

    Note over RMQ, VRH: Asynchronous Processing with Retry & Fallback
    RMQ->>VRH: Consume Message (Code ID)
    activate VRH
    
    loop Up to 6 attempts (1 initial + 5 retries)
        VRH->>DB: Find VerificationCode by ID
        VRH->>NNL: sendVerificationEmail(code)
        activate NNL
        NNL-->>VRH: Success / Exception
        deactivate NNL
        
        alt Success
            VRH->>DB: Update verifyEmailSentAt & Persist
            VRH-->>RMQ: Ack Message
            Note over VRH: Stop Loop
        else Exception
            Note over VRH: Exponential Backoff
        end
    end
    
    alt All retries failed
        VRH->>VRH: onMaxRetriesReached (Fallback)
        VRH-->>RMQ: Ack Message (Graceful Delete)
    end
    deactivate VRH
```

## 2. Verification Completion Flow

This flow describes how a user verifies their email using the received code.

```mermaid
sequenceDiagram
    participant User
    participant VC as VerificationController
    participant DB as Database

    User->>VC: POST /verify {referenceId, email, code}
    activate VC
    VC->>DB: Find VerificationCode by referenceId and email
    
    alt Code Not Found
        VC-->>User: 404 Not Found
    else Code Found
        alt Code Expired
            VC-->>User: 410 Gone
        else Code Already Used
            VC-->>User: 409 Conflict
        else Incorrect Code
            VC-->>User: 401 Unauthorized
        else Valid Code
            VC->>DB: Update verifiedAt & Persist
            VC-->>User: 200 OK
        end
    end
    deactivate VC
```

## 3. Admin Statistics Flow

This flow describes how an administrator retrieves statistics about verification requests.

```mermaid
sequenceDiagram
    participant Admin
    participant ASC as AdminStatisticsController
    participant DB as Database (VerificationStatistics)

    Admin->>ASC: GET /admin/statistics
    activate ASC
    ASC->>DB: List all VerificationStatistics
    ASC->>ASC: Calculate average time and unverified percentage
    ASC-->>Admin: 200 OK (AdminStatisticsResponse)
    deactivate ASC
```
