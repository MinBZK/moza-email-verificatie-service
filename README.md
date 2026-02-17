# Verificatieservice

De Verificatieservice is een op Quarkus gebaseerde microservice die verantwoordelijk is voor het genereren en verifiëren van verificatiecodes via e-mail (gesimuleerd via RabbitMQ).

## Functionaliteiten

- **Verificatieaanvraag**: Gebruikers kunnen een verificatiecode aanvragen voor een specifiek e-mailadres.
- **Verificatie**: Validatie van de verstrekte code tegen een referentie-ID en e-mailadres.
- **Automatische opschoning**: Verlopen en succesvol geverifieerde codes worden periodiek verplaatst naar statistieken en verwijderd uit de actieve tabel.
- **Beheerdersstatistieken**: Inzicht in de gemiddelde verificatietijd en het percentage niet-geverifieerde aanvragen.

## Architectuur & Dependencies

- **Framework**: [Quarkus](https://quarkus.io/) (Supersonic Subatomic Java Framework)
- **Database**: PostgreSQL (met Hibernate ORM & Panache)
- **Messaging**: RabbitMQ (via SmallRye Reactive Messaging)
- **Validatie**: Hibernate Validator (Jakarta Bean Validation)
- **API Documentatie**: OpenAPI/Swagger UI
- **Job Scheduling**: Quarkus Scheduler voor periodieke opschoning

## Lokale Setup

### Vereisten

- Java 21 of hoger
- Maven 3.9+
- Docker & Docker Compose

### 1. Infrastructuur opstarten

Start de benodigde services (PostgreSQL en RabbitMQ) met Docker Compose:

```shell script
docker-compose up -d
```

- **PostgreSQL**: Draait op poort `5432`.
- **RabbitMQ**: Draait op poort `5672` (Management UI op `http://localhost:15672`, log in met `user`/`password`).

### 2. Applicatie starten in Dev Mode

Start de Quarkus applicatie met live coding ingeschakeld:

```shell script
./mvnw quarkus:dev
```

De applicatie is beschikbaar op `http://localhost:8080`.
- **Swagger UI**: `http://localhost:8080/q/swagger-ui/`
- **Dev UI**: `http://localhost:8080/q/dev/`

## API Endpoints

### Verificatie

- `POST /request`: Vraag een nieuwe code aan. Verwacht een JSON met `email`. Retourneert een `referenceId`.
- `POST /verify`: Verifieer een code. Verwacht een JSON met `referenceId`, `email` en `code`.

### Beheer

- `GET /admin/statistics`: Haal statistieken op over de verificatieprocessen.

## Messaging Flow

1. Bij een aanvraag (`POST /request`) wordt een `VerificationCode` opgeslagen.
2. Een bericht met het database-ID wordt verstuurd naar het RabbitMQ exchange `verification-requests`.
3. De `VerificationRequestHandler` ontvangt dit bericht, zoekt de code op en simuleert het versturen van een e-mail door een logregel te schrijven en de verzendtijd bij te werken.

## Configuratie

Belangrijke parameters in `src/main/resources/application.properties`:

- `verification.code.validity-minutes`: Hoe lang een code geldig blijft (standaard 10 min).
- `verification.cleanup.schedule`: Frequentie van de opschoonjobs (standaard elke 60 seconden).
