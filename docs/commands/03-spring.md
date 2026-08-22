# 03 — Spring Boot

Commands executed in milestone M0.1 — Spring Boot Bootstrap.

All commands are run from the `backend/` directory.

---

## Build

Compile sources and run tests, producing the executable JAR in `target/`.

```bash
cd backend
mvn clean package
```

| Flag | Purpose |
|------|---------|
| `clean` | Delete the `target/` directory before building |
| `package` | Compile, test, and package the application into a JAR |

**Result (M0.1):** BUILD SUCCESS — context loads, 1 test passed.

---

## Run

Start the application using the Maven Spring Boot plugin. Uses the `local` profile by default.

```bash
cd backend
mvn spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## Skip tests

Build without executing tests (useful during rapid iteration).

```bash
cd backend
mvn clean package -DskipTests
```

---

## Run tests only

```bash
cd backend
mvn test
```

---

## Clean

Remove all build output.

```bash
cd backend
mvn clean
```
---

## Verify (build + test)

Compile sources, run all tests, and verify the build succeeds.

```bash
cd backend
mvn clean verify
```

**Result (M2.0):** BUILD SUCCESS — context loads, Spring beans registered, 1 test passed.

---

## Exchange Rates feature (M2.0)

The exchange rates endpoint is registered at:

```
GET http://localhost:8080/api/exchange-rates
```

Example response:

```json
{
  "tableNo": "150/A/NBP/2025",
  "effectiveDate": "2025-08-01",
  "rates": [
    {
      "code": "USD",
      "name": "dolar amerykański",
      "midRate": 3.9245
    }
  ]
}
```

The endpoint fetches live data from the Polish National Bank (NBP) public API using `RestClient`.

If the NBP API is unavailable, the endpoint returns HTTP 502 with a `ProblemDetail` body.

### Backend feature package

```
com.lease-demo.exchange/
├── client/         # NbpClient — RestClient wrapper
├── config/         # NbpClientConfig — RestClient Spring Bean
├── controller/     # ExchangeRateController — GET /api/exchange-rates
├── dto/external/   # NbpTableDto, NbpRateDto — NBP JSON DTOs
├── dto/response/   # ExchangeRateResponse, ExchangeRateDto — public API DTOs
├── exception/      # NbpClientException
├── mapper/         # ExchangeRateMapper (MapStruct)
└── service/        # ExchangeRateService
```

See `docs/development/milestones/M2.0-exchange-rates.md` for full architecture details.
