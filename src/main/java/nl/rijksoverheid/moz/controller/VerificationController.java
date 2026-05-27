package nl.rijksoverheid.moz.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.rijksoverheid.moz.dto.request.VerificationApplicationRequest;
import nl.rijksoverheid.moz.dto.request.VerificationRequest;
import nl.rijksoverheid.moz.dto.response.ErrorResponse;
import nl.rijksoverheid.moz.dto.response.VerificationFailureReason;
import nl.rijksoverheid.moz.dto.response.VerificationResponse;
import nl.rijksoverheid.moz.entity.VerificationCode;
import nl.rijksoverheid.moz.service.NotifyNLService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Path("/")
public class VerificationController {

    private static final Logger LOG = Logger.getLogger(VerificationController.class);

    @jakarta.inject.Inject
    NotifyNLService notifyNLService;

    @POST
    @Path("/request")
    @Transactional
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(
            summary = "Create a new verification request",
            description = "Creates a new verification code for the given email and sends it via the messaging system."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Verification request created, returns the reference ID",
                    content = @Content(
                            mediaType = MediaType.TEXT_PLAIN,
                            schema = @Schema(type = SchemaType.STRING)
                    )
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public Response requestVerification(@Valid VerificationApplicationRequest request) {
        VerificationCode code = new VerificationCode();
        code.persist();
        LOG.debugf("Verification code entity persisted with reference ID: %s", code.getReferenceId());

        boolean result = notifyNLService.sendVerificationEmail(
                code, 
                request.getEmail(), 
                request.getApiKey(), 
                request.getTemplateId()
        );

        if(result) {
            code.setVerifyEmailSentAt(LocalDateTime.now());
            LOG.infof("Verification request successful. Reference ID: %s", code.getReferenceId());
            return Response.ok(code.getReferenceId()).build();
        }
        
        LOG.errorf("Verification request failed for email: %s. NotifyNL service returned failure.", request.getEmail());
        throw new InternalServerErrorException();
    }

    @POST
    @Path("/verify")
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Operation(
            summary = "Verify a verification code",
            description = "Verifies the code for a given reference ID and email."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Verification attempt completed",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = VerificationResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request format",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public VerificationResponse verify(@Valid VerificationRequest request) {
        LOG.infof("Verification attempt for referenceId: %s", request.getReferenceId());

        Optional<VerificationCode> codeOpt = VerificationCode.findByReferenceId(request.getReferenceId());
        if (codeOpt.isEmpty()) {
            LOG.warnf("Verification failed: code not found for referenceId: %s", request.getReferenceId());
            return new VerificationResponse(false, VerificationFailureReason.CODE_NOT_FOUND);
        }
        VerificationCode code = codeOpt.get();

        if (code.getValidUntil().isBefore(LocalDateTime.now())) {
            LOG.warnf("Verification failed: code expired for referenceId: %s", request.getReferenceId());
            return new VerificationResponse(false, VerificationFailureReason.CODE_EXPIRED);
        }

        if (code.isUsed()) {
            LOG.warnf("Verification failed: code already used for referenceId: %s", request.getReferenceId());
            return new VerificationResponse(false, VerificationFailureReason.CODE_ALREADY_USED);
        }

        if (!Objects.equals(code.getCode(), request.getCode())) {
            LOG.warnf("Verification failed: incorrect code for referenceId: %s", request.getReferenceId());
            return new VerificationResponse(false, VerificationFailureReason.INCORRECT_CODE);
        }

        VerificationCode.update("verifiedAt = ?1 where id = ?2", LocalDateTime.now(), code.id);
        LOG.infof("Verification successful for referenceId: %s", request.getReferenceId());
        return new VerificationResponse(true);
    }
}
