# Architecture & Flow — JWT Auth System

## The Big Picture

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────────┐
│                  FILTER CHAIN                        │
│                                                      │
│   JwtAuthFilter  ──→  reads token from header        │
│         │             validates it via JwtService    │
│         │             loads user via UserDetailsService│
│         ▼                                            │
│   SecurityContextHolder  ←── stores authenticated user│
└─────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────┐
│                  CONTROLLER LAYER                    │
│                                                      │
│   AuthController  →  /auth/register, /auth/login     │
│   UserController  →  /api/me  (protected)            │
└─────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────┐
│                  SERVICE LAYER                       │
│                                                      │
│   AuthService            →  register/login logic     │
│   JwtService             →  create/validate tokens   │
│   UserDetailsServiceImpl →  load user from MongoDB   │
└─────────────────────────────────────────────────────┘
     │
     ▼
┌─────────────────────────────────────────────────────┐
│                  DATA LAYER                          │
│                                                      │
│   UserRepository  →  MongoDB queries                 │
│   User (entity)   →  maps to users collection        │
└─────────────────────────────────────────────────────┘
```

---

## Every Class — What It Does & Why It Exists

### 1. `User.java` — Entity / Model
**Package:** `model`
**What it does:** Represents a user document stored in MongoDB.
**Why it exists:** This is your data structure. Every user in the DB maps to this object.
**Key detail:** Implements `UserDetails` — this is the contract Spring Security understands.
When Spring Security calls `getUsername()`, it gets the email. When it calls `getAuthorities()`,
it gets the roles. Spring Security never touches MongoDB directly — it only talks to `UserDetails`.

---

### 2. `UserRepository.java` — Data Access
**Package:** `repository`
**What it does:** Queries the MongoDB `users` collection.
**Why it exists:** Spring Data generates the implementation automatically from method names.
`findByEmail(email)` → `db.users.findOne({email: "..."})` — zero query code written.
**Used by:** `UserDetailsServiceImpl` (to load user by email), `AuthService` (to save new users,
check if email exists).

---

### 3. `JwtService.java` — Token Engine
**Package:** `service`
**What it does:** Creates JWT tokens and validates them.
**Why it exists:** All JWT logic lives here. No other class touches jjwt directly.
**Methods and their purpose:**

| Method | Purpose |
|---|---|
| `generateAccessToken(userDetails)` | Creates a 15-min token after login/register |
| `generateRefreshToken(userDetails)` | Creates a 7-day token after login/register |
| `isTokenValid(token, userDetails)` | Checks username match + not expired |
| `extractUsername(token)` | Pulls email out of the token's `sub` claim |
| `extractAllClaims(token)` | Parses token, throws if invalid/expired/tampered |

**Used by:** `JwtAuthFilter` (to validate incoming tokens), `AuthService` (to generate tokens
after login/register).

---

### 4. `UserDetailsServiceImpl.java` — Spring Security ↔ MongoDB Bridge
**Package:** `service`
**What it does:** Implements Spring Security's `UserDetailsService` interface.
Has one method: `loadUserByUsername(email)` — looks up user in MongoDB, returns `UserDetails`.
**Why it exists:** Spring Security doesn't know MongoDB exists. It only knows `UserDetailsService`.
This class is the bridge between them.
**Used by:** Spring Security internally during login, and by `JwtAuthFilter` when validating a token.

---

### 5. `JwtAuthFilter.java` — Request Interceptor
**Package:** `filter`
**What it does:** Runs on EVERY incoming HTTP request before it reaches your controller.
Reads the `Authorization: Bearer <token>` header, validates the token, and tells Spring Security
who this request belongs to.
**Why it exists:** HTTP is stateless. Every request is a stranger. This filter is how your server
recognizes a logged-in user on every request without a session.
**Flow inside the filter:**
```
1. Read "Authorization" header
2. No header? → pass through unauthenticated (SecurityConfig decides if that's OK)
3. Extract JWT from "Bearer <token>"
4. extractUsername(token) → get email
5. loadUserByUsername(email) → get UserDetails from MongoDB
6. isTokenValid(token, userDetails) → verify signature + expiry + username
7. Valid? → set Authentication in SecurityContextHolder
8. Pass request to the next filter / controller
```

---

### 6. `SecurityConfig.java` — Security Rules
**Package:** `config`
**What it does:** Defines the rules for your entire security setup in one place.
**Why it exists:** This is where you declare: which endpoints are public, which need a token,
what type of sessions to use, and how to authenticate users.
**Key decisions made here:**
- `SessionCreationPolicy.STATELESS` — no HTTP sessions, every request must carry a JWT
- `/auth/**` is public — register and login don't need a token
- Everything else requires authentication
- `JwtAuthFilter` runs before Spring's default login filter
- `DaoAuthenticationProvider` — tells Spring Security: use `UserDetailsServiceImpl` to load
  users and `BCryptPasswordEncoder` to verify passwords

---

### 7. `AuthService.java` — Register & Login Logic
**Package:** `service`
**What it does:** Business logic for registration and login.
**Why it exists:** Controllers should be thin — only handle HTTP in/out.
All decisions ("does this email already exist?", "hash this password", "issue tokens") live here.

**Register flow:**
```
1. Check if email already exists → if yes, throw exception (409 Conflict)
2. Hash the password with BCrypt
3. Build User object with email + hashed password
4. Save to MongoDB via UserRepository
5. Generate accessToken + refreshToken via JwtService
6. Return AuthResponse DTO
```

**Login flow:**
```
1. Call authenticationManager.authenticate(email, password)
      → DaoAuthenticationProvider loads user from MongoDB
      → BCrypt.matches(rawPassword, storedHash)
      → Wrong password? throws BadCredentialsException → 401
2. Load User from MongoDB
3. Generate accessToken + refreshToken via JwtService
4. Return AuthResponse DTO
```

---

### 8. `AuthController.java` — HTTP Endpoints
**Package:** `controller`
**What it does:** Exposes `/auth/register` and `/auth/login` endpoints.
**Why it exists:** Handles HTTP concerns only — reads request body, returns response with
correct status code. Delegates all logic to `AuthService`.

| Endpoint | Method | Input | Output | Status |
|---|---|---|---|---|
| `/auth/register` | POST | `RegisterRequest` | `AuthResponse` | 201 Created |
| `/auth/login` | POST | `LoginRequest` | `AuthResponse` | 200 OK |

---

## The Two Main Flows End-to-End

### Flow 1 — Registration
```
POST /auth/register  { email, password }
  → AuthController.register()
  → @Valid validates email format + password length
  → AuthService.register()
      → userRepository.existsByEmail() → duplicate? throw exception
      → passwordEncoder.encode(password) → BCrypt hash
      → userRepository.save(user) → stored in MongoDB
      → jwtService.generateAccessToken(user)
      → jwtService.generateRefreshToken(user)
  → return 201 { accessToken, refreshToken, tokenType }
```

### Flow 2 — Authenticated Request
```
GET /api/me  Authorization: Bearer <token>
  → JwtAuthFilter runs first
      → extract token from header
      → jwtService.extractUsername(token) → email
      → userDetailsService.loadUserByUsername(email) → User from MongoDB
      → jwtService.isTokenValid(token, user) → true/false
      → valid → SecurityContextHolder.setAuthentication(...)
  → Controller method executes
  → @AuthenticationPrincipal User user → injected from SecurityContext
  → return 200 { email, roles }
```

---

## Why This Layering?

```
Controller  →  only HTTP (request/response, status codes)
Service     →  only business logic (rules, decisions)
Repository  →  only database (queries, saves)
Filter      →  only security (token validation, authentication)
Config      →  only wiring (rules, providers, beans)
```

Each layer has one job. If a bug is in the database query — you look in the repository.
If a bug is in token generation — you look in JwtService. Nothing bleeds into anything else.
