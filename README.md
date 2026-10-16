# Helpdesk

A multi-tenant support ticketing system built with Spring Boot.

The idea is simple: several organizations share one application, but each one only ever sees its own users and tickets. Customers raise tickets, agents work on them, and admins manage their team. Agents can leave internal notes on a ticket that the customer never sees.

Stack: Java 17, Spring Boot 3.5, Spring Security with JWT, Spring Data JPA, PostgreSQL 16, JUnit 5 and Mockito. There's also a small web UI so you can click through everything without Postman.

## Running it

The easiest way is Docker. From the project root:

```bash

docker compose up --build

```

That starts Postgres, builds the app, creates the tables and loads some demo data. Once you see `Started HelpdeskApplication` in the logs, open:

- http://localhost:8080 for the UI

- http://localhost:8080/swagger-ui.html for the API docs

- http://localhost:8080/api-docs for the raw OpenAPI JSON

To stop it, press `Ctrl+C`. If you want a clean database next time, run `docker compose down -v`.

If you'd rather run the app from your IDE or terminal (needs JDK 17+), start only the database in Docker and run the app with the Maven wrapper:

```bash

docker compose up -d postgres

./mvnw spring-boot:run

```

On Windows use `mvnw.cmd` instead of `./mvnw`. You don't need Maven installed, the wrapper downloads it.

To run the tests:

```bash

./mvnw test

```

They're plain unit tests, so no database is needed.

## Logging in

The seed data creates two organizations, Acme and Globex, so you can see the isolation working. Every account uses the password `Password@123`. On the login screen you enter the organization ID, your email and your password.

```

acme     admin@acme.com        admin

acme     agent1@acme.com       agent

acme     agent2@acme.com       agent

acme     customer1@acme.com    customer

acme     customer2@acme.com    customer

globex   admin@globex.com      admin

globex   agent@globex.com      agent

globex   customer@globex.com   customer

```

The login page also has buttons that fill these in for you.


The schema lives in `schema.sql` and the demo data in `data.sql`. Both run on startup and are safe to run repeatedly: tables are only created if missing, and seed rows are skipped if they already exist.


 
