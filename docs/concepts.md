# Core Concepts — JWT Auth

## How JWT Works (Big Picture)

```
Client                          Server
  |                               |
  |-- POST /auth/register ------> |  (save user, hashed password)
  |<-- 201 Created --------------|
  |                               |
  |-- POST /auth/login ---------->|  (verify password)
  |<-- { accessToken, refresh }--|  (JWT tokens)
  |                               |
  |-- GET /api/profile ---------->|  (Authorization: Bearer <token>)
  |   JWT Filter validates token  |
  |<-- 200 { user data } --------|
```

## JWT Structure

A JWT has 3 parts separated by dots:
```
HEADER.PAYLOAD.SIGNATURE
```

**Header** (Base64):
```json
{ "alg": "HS256", "typ": "JWT" }
```

**Payload** (Base64 — NOT encrypted, anyone can read this):
```json
{
  "sub": "userId123",
  "roles": ["ROLE_USER"],
  "iat": 1720000000,
  "exp": 1720000900
}
```

**Signature** (HMAC-SHA256 of header + payload with your secret):
```
HMAC_SHA256(base64(header) + "." + base64(payload), secret)
```

Key point: JWT is **signed, not encrypted**. Never put passwords or PII in payload.

## Spring Security Filter Chain

Every HTTP request goes through a chain of filters:
```
Request → [CORS Filter] → [JWT Auth Filter] → [Security Checks] → Controller
```

Our custom `JwtAuthFilter extends OncePerRequestFilter`:
1. Extract `Authorization: Bearer <token>` header
2. Parse and validate the JWT
3. Load `UserDetails` from MongoDB
4. Set `Authentication` in `SecurityContextHolder`
5. Call `filterChain.doFilter()` to continue

## BCrypt Password Hashing

BCrypt is a slow, adaptive hashing algorithm. "Slow" is intentional — makes brute force attacks expensive.
```
BCrypt.hashpw("password123", BCrypt.gensalt(12))
→ "$2a$12$abcdefghijklmnop..."
```
The `12` is the cost factor — higher = slower = more secure.
Spring uses cost 10 by default. 12 is production-safe.

## Role-Based Access Control (RBAC)

Users have roles stored in MongoDB:
```json
{ "roles": ["ROLE_USER"] }
{ "roles": ["ROLE_USER", "ROLE_ADMIN"] }
```

Spring Security uses the `ROLE_` prefix convention.
In config: `.hasRole("ADMIN")` checks for `ROLE_ADMIN`.
In config: `.hasAuthority("ROLE_ADMIN")` checks exact string.

## Spring Data MongoDB — Repository Pattern

### MongoRepository<T, ID> generics
`MongoRepository<T, ID>` takes two type parameters:
- `T` — the document type (e.g. `User`). Tells Spring Data which collection to manage and what to map documents to.
- `ID` — the type of the `@Id` field in that document (e.g. `String`). Must match exactly.

```java
// User has: private String id;
// So: MongoRepository<User, String>

// If id was Long:
// MongoRepository<User, Long>
```

### Spring Data method naming convention
Spring Data reads method names and auto-generates the MongoDB query. You write zero query code.

| Method name | Generated query |
|---|---|
| `findByEmail(String email)` | `db.users.findOne({email: "..."})` |
| `existsByEmail(String email)` | `db.users.countDocuments({email: "..."}) > 0` |
| `findByEmailAndEnabled(String email, boolean enabled)` | `db.users.findOne({email: "...", enabled: true})` |
| `findAllByRolesContaining(String role)` | find all users whose roles array contains the value |

**Rules:**
- `findBy` → returns matching document(s)
- `existsBy` → returns boolean
- `countBy` → returns long
- `deleteBy` → deletes and returns count
- Field name after `By` must match exactly the field name in your model
- Always include the parameter — Spring Data needs the value to search for

## Refresh Token Pattern

- **Access Token**: short-lived (15 min), used on every API call
- **Refresh Token**: long-lived (7 days), used ONLY to get new access tokens

Why? If an access token is stolen, it expires in 15 min. The refresh token is stored securely (HttpOnly cookie or DB) and can be revoked.
