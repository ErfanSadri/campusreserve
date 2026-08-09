# CampusReserve Engineering Guidelines

## Project

CampusReserve is a university-focused event discovery and registration platform.

The project prioritizes backend and distributed-systems engineering while keeping product scope intentionally small.

## Current Architecture

- Java 21
- Spring Boot
- Maven
- `apps/reservation-api` is the primary HTTP application.

Additional infrastructure and services must only be introduced as required by the current milestone.

## Engineering Rules

- Keep changes scoped to the requested milestone.
- Do not add dependencies without a concrete need.
- Do not redesign unrelated code.
- Prefer clear, maintainable code over unnecessary abstractions.
- Keep product behavior understandable without relying on technical jargon.
- Add or update tests for behavior changes.
- Run relevant tests after modifications.
- Never add real credentials or secrets to the repository.
- Do not invent benchmark or performance numbers.
- Do not implement future milestones early.

## Validation

For reservation API changes, run:

```bash
cd apps/reservation-api
./mvnw test