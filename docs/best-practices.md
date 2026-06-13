# Production Best Practices — JWT Auth in Spring Boot + MongoDB

## Security
- BCrypt for all passwords (never MD5, SHA1, or plain text)
- JWT secret >= 256 bits, loaded from env or secrets manager (never hardcoded)
- Short-lived access tokens (15 min), long-lived refresh tokens (7 days)
- HTTPS only in production (Spring Security config)
- CORS configured properly — never wildcard `*` in production

## Architecture (Clean Layering)
```
Controller  →  Service  →  Repository  →  MongoDB
                ↑
           Security Filter Chain
```
- Controllers: only HTTP in/out, delegate to Service
- Services: business logic, JWT generation/validation
- Repositories: data access only, extend MongoRepository
- Filters: stateless JWT validation, no business logic

## Spring Security 6 Rules
- Use `SecurityFilterChain` bean, NOT extending `WebSecurityConfigurerAdapter` (deprecated)
- `OncePerRequestFilter` for JWT filter — runs exactly once per request
- `AuthenticationProvider` pattern for clean separation
- `UserDetailsService` implementation loads user from MongoDB

## JWT Rules
- Always validate: signature, expiry, issuer, not-before
- Never store sensitive data in JWT payload (it is Base64, not encrypted)
- Use `sub` claim for user ID, not username (safer)
- Blacklist on logout — store invalidated tokens in Redis or MongoDB

## MongoDB Rules
- Index on `email` field — always (unique + query speed)
- Store `roles` as a List<String> in the User document
- Never return the password field from queries to the client

## API Response Standards
- `200 OK` — success
- `201 Created` — resource created (registration)
- `400 Bad Request` — validation error (wrong input)
- `401 Unauthorized` — no token or invalid token
- `403 Forbidden` — valid token but insufficient role
- `409 Conflict` — user already exists
- `500 Internal Server Error` — never expose stack traces
