# JWT Auth Spring Boot + MongoDB — Learning Project

## Goal
Build production-grade JWT Authentication & Authorization in Spring Boot + MongoDB.
The user is learning backend development and wants to reach industry/pro level.

## Teaching Philosophy
- Teach step by step — one concept at a time
- Explain WHY, not just WHAT
- Call out common mistakes and anti-patterns explicitly
- Every code choice must be justified against industry standards

## Project Stack
- Java 17+
- Spring Boot 3.x
- Spring Security 6.x
- MongoDB (via Spring Data MongoDB)
- JWT (io.jsonwebtoken / jjwt library)
- Maven

## Docs Folder
All mistakes, lessons, and best practices go in `/docs/`:
- `mistakes-log.md` — every mistake the user makes, why it's wrong, how to fix it
- `best-practices.md` — production patterns to follow
- `concepts.md` — theory behind JWT, Spring Security, MongoDB auth

## Key Rules for This Project
1. No storing plain-text passwords — always BCrypt
2. No putting secrets in code — use application.properties / env vars
3. JWT secret must be >= 256 bits (32 bytes) for HS256
4. Always validate JWT claims: expiry, issuer, subject
5. Refresh token pattern — short-lived access tokens, long-lived refresh tokens
6. Role-based access control (RBAC) from day one
7. Proper HTTP status codes: 401 Unauthorized vs 403 Forbidden
8. Never return stack traces to the client in production
