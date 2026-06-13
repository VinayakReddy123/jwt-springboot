 # Project Flow — JWT Auth Spring Boot + MongoDB

> Read this top to bottom. By the end you should be able to explain every file and why it exists.

---

## 1. Project Structure Map

```
com.backendauth.authentication
│
├── model/
│   └── User.java                  ← MongoDB document + UserDetails
│
├── repository/
│   └── UserRepository.java        ← DB queries (findByEmail, existsByEmail)
│
├── dto/
│   ├── RegisterRequest.java       ← Input for /register
│   ├── LoginRequest.java          ← Input for /login
│   └── AuthResponse.java          ← Output (accessToken + refreshToken)
│
├── service/
│   ├── JwtService.java            ← Generate + validate JWT tokens
│   ├── UserDetailsServiceImpl.java← Loads user from MongoDB for Spring Security
│   └── AuthService.java           ← Business logic: register + login
│
├── filter/
│   └── JwtAuthFilter.java         ← Intercepts every request, validates JWT
│
├── config/
│   └── SecurityConfig.java        ← Wires everything into Spring Security
│
└── controller/
    └── AuthController.java        ← HTTP endpoints: /register, /login
```

---

## 2. The Big Picture — How All Files Connect

```
HTTP Request
     │
     ▼
┌─────────────────────────────────────────────────┐
│           Spring Security Filter Chain           │
│                                                  │
│   ┌──────────────────────────────────────────┐  │
│   │         JwtAuthFilter                    │  │
│   │  (runs on EVERY request, once)           │  │
│   │                                          │  │
│   │  1. Read Authorization header            │  │
│   │  2. Extract token                        │  │
│   │  3. Ask JwtService → is token valid?     │  │
│   │  4. Ask UserDetailsServiceImpl → load    │  │
│   │     user from MongoDB                    │  │
│   │  5. Set Authentication in               │  │
│   │     SecurityContextHolder                │  │
│   └──────────────────────────────────────────┘  │
│                    │                             │
│                    ▼                             │
│   ┌──────────────────────────────────────────┐  │
│   │     Security Rules (SecurityConfig)       │  │
│   │                                          │  │
│   │  /api/auth/**  → permitAll (public)      │  │
│   │  anything else → must be authenticated   │  │
│   └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
     │
     ▼
┌──────────────────┐
│  AuthController  │  ← HTTP layer only, no logic
└──────────────────┘
     │
     ▼
┌──────────────────┐
│   AuthService    │  ← All business logic lives here
└──────────────────┘
     │         │
     ▼         ▼
┌──────────┐ ┌────────────┐
│   User   │ │ JwtService │
│Repository│ │            │
└──────────┘ └────────────┘
     │
     ▼
┌──────────┐
│ MongoDB  │
└──────────┘
```

---

## 3. Flow A — User Registration (`POST /api/auth/register`)

```
Client sends:
{
  "email": "vinay@gmail.com",
  "password": "mypassword123"
}

        │
        ▼
┌───────────────────┐
│  JwtAuthFilter    │  No "Authorization" header → skip token logic
│                   │  → pass request through to controller
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  AuthController   │  @PostMapping("/register")
│                   │  @Valid validates: email format, password min 8 chars
│                   │  If invalid → 400 Bad Request (Spring auto-handles)
└───────────────────┘
        │
        ▼
┌───────────────────────────────────────────────┐
│                AuthService                    │
│                                               │
│  1. Check if email exists in MongoDB          │
│     userRepository.existsByEmail(email)       │
│     → YES: throw RuntimeException (409)       │
│     → NO: continue                            │
│                                               │
│  2. Hash the password                         │
│     passwordEncoder.encode("mypassword123")   │
│     → "$2a$10$abc123..."  (BCrypt hash)        │
│                                               │
│  3. Build User object                         │
│     User.builder()                            │
│       .email("vinay@gmail.com")               │
│       .password("$2a$10$abc123...")           │
│       .roles(["ROLE_USER"])   ← default       │
│       .build()                                │
│                                               │
│  4. Save to MongoDB                           │
│     userRepository.save(user)                 │
│                                               │
│  5. Generate tokens                           │
│     jwtService.generateAccessToken(user)      │
│     jwtService.generateRefreshToken(user)     │
└───────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────┐
│                JwtService                     │
│                                               │
│  buildToken():                                │
│    subject  = user.getUsername() (email)      │
│    issuedAt = now                             │
│    expiry   = now + 15 minutes (access)       │
│             = now + 7 days    (refresh)       │
│    sign with HMAC-SHA256 + secret key         │
│                                               │
│  Returns: "eyJhbGci....abc123"                │
└───────────────────────────────────────────────┘
        │
        ▼
Client receives:
{
  "accessToken":  "eyJhbGci....",
  "refreshToken": "eyJhbGci....",
  "tokenType":    "Bearer"
}
HTTP Status: 201 Created
```

---

## 4. Flow B — User Login (`POST /api/auth/login`)

```
Client sends:
{
  "email": "vinay@gmail.com",
  "password": "mypassword123"
}

        │
        ▼
┌───────────────────┐
│  JwtAuthFilter    │  No token → pass through
└───────────────────┘
        │
        ▼
┌───────────────────┐
│  AuthController   │  @Valid validates input
└───────────────────┘
        │
        ▼
┌───────────────────────────────────────────────┐
│                AuthService                    │
│                                               │
│  1. authenticationManager.authenticate(       │
│       email, rawPassword                      │
│     )                                         │
│                                               │
│     → internally calls:                       │
│       UserDetailsServiceImpl.loadUserByUsername(email)
│       → finds User in MongoDB                 │
│       DaoAuthenticationProvider               │
│       → BCrypt.verify(rawPassword, storedHash)│
│       → MATCH: authentication succeeds        │
│       → NO MATCH: throws BadCredentialsException
│                                               │
│  2. Load user from MongoDB again              │
│     (to get full User object for JWT)         │
│                                               │
│  3. Generate access + refresh tokens          │
└───────────────────────────────────────────────┘
        │
        ▼
Client receives:
{
  "accessToken":  "eyJhbGci....",
  "refreshToken": "eyJhbGci....",
  "tokenType":    "Bearer"
}
HTTP Status: 200 OK
```

---

## 5. Flow C — Accessing a Protected Endpoint

```
Client sends:
GET /api/some-protected-route
Authorization: Bearer eyJhbGci....

        │
        ▼
┌───────────────────────────────────────────────┐
│               JwtAuthFilter                   │
│                                               │
│  1. Read header: "Authorization"              │
│     → found: "Bearer eyJhbGci...."           │
│                                               │
│  2. Extract token: substring after "Bearer "  │
│     → "eyJhbGci...."                         │
│                                               │
│  3. jwtService.extractUsername(token)         │
│     → parse JWT, verify signature             │
│     → extract "sub" claim → "vinay@gmail.com" │
│                                               │
│  4. Is SecurityContext empty?                 │
│     → YES (no auth yet for this request)      │
│                                               │
│  5. userDetailsService.loadUserByUsername(    │
│       "vinay@gmail.com"                       │
│     )                                         │
│     → MongoDB query → returns User object     │
│                                               │
│  6. jwtService.isTokenValid(token, user)      │
│     → email matches? ✓                        │
│     → not expired?  ✓                         │
│     → VALID                                   │
│                                               │
│  7. Create Authentication object              │
│     → set in SecurityContextHolder           │
│     (Spring now knows WHO this request is)    │
│                                               │
│  8. filterChain.doFilter() → continue        │
└───────────────────────────────────────────────┘
        │
        ▼
┌───────────────────┐
│  Security Rules   │  Request is authenticated → allow through
└───────────────────┘
        │
        ▼
┌───────────────────┐
│   Your Controller │  Handles the request normally
└───────────────────┘
        │
        ▼
HTTP Status: 200 OK + response data
```

---

## 6. Flow D — Invalid or Expired Token

```
Client sends:
GET /api/some-protected-route
Authorization: Bearer expiredOrFakeToken

        │
        ▼
┌───────────────────────────────────────────────┐
│               JwtAuthFilter                   │
│                                               │
│  try {                                        │
│    jwtService.extractUsername(token)          │
│    → JwtException thrown!                     │
│      (signature mismatch OR token expired)    │
│  }                                            │
│  catch (JwtException e) {                     │
│    response.setStatus(401 UNAUTHORIZED)       │
│    return  ← stops here, no further processing│
│  }                                            │
└───────────────────────────────────────────────┘
        │
        ▼
Client receives:
HTTP Status: 401 Unauthorized
(no stack trace, no internal info leaked)
```

---

## 7. JWT Token Anatomy

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ2aW5heUBnbWFpbC5jb20iLCJpYXQiOjE3MjAwMDAwMDAsImV4cCI6MTcyMDAwMDkwMH0.SIGNATURE

│──── HEADER ────│  │────────────────── PAYLOAD ──────────────────│  │─ SIGNATURE ─│

HEADER (Base64 decoded):
{
  "alg": "HS256"
}

PAYLOAD (Base64 decoded — NOT encrypted, anyone can read this):
{
  "sub": "vinay@gmail.com",   ← who this token belongs to
  "iat": 1720000000,          ← issued at (Unix timestamp)
  "exp": 1720000900           ← expires at (iat + 15 min)
}

SIGNATURE:
HMAC_SHA256(
  base64(header) + "." + base64(payload),
  yourSecretKey
)
→ Only YOUR server can produce this signature
→ Anyone with a different key = invalid signature = rejected
```

---

## 8. Spring Security Bean Dependency Map

```
SecurityConfig
├── defines → PasswordEncoder (BCryptPasswordEncoder)
├── defines → AuthenticationProvider (DaoAuthenticationProvider)
│              ├── uses → UserDetailsService (UserDetailsServiceImpl)
│              └── uses → PasswordEncoder
├── defines → AuthenticationManager (from AuthenticationConfiguration)
└── defines → SecurityFilterChain
               ├── uses → JwtAuthFilter
               └── uses → AuthenticationProvider

AuthService
├── injects → UserRepository
├── injects → JwtService
├── injects → PasswordEncoder
└── injects → AuthenticationManager

JwtAuthFilter
├── injects → JwtService
└── injects → UserDetailsService
```

---

## 9. Why Each File Exists — One Line Each

| File | Why it exists |
|---|---|
| `User.java` | The MongoDB document AND the Spring Security identity in one class |
| `UserRepository.java` | The only file allowed to talk to MongoDB for users |
| `RegisterRequest.java` | Defines and validates what a register request must contain |
| `LoginRequest.java` | Defines and validates what a login request must contain |
| `AuthResponse.java` | Defines what we send back after successful auth |
| `JwtService.java` | Single responsibility: everything JWT (create, parse, validate) |
| `UserDetailsServiceImpl.java` | Bridge between Spring Security and your MongoDB user data |
| `AuthService.java` | Business logic only: orchestrates register and login |
| `JwtAuthFilter.java` | Guards every request — validates the token before anything else runs |
| `SecurityConfig.java` | Wires all the beans together and defines the security rules |
| `AuthController.java` | HTTP layer only — receives request, calls service, returns response |

---

## 10. Common Questions

**Q: Why does `User` implement `UserDetails`?**
Spring Security doesn't know about your `User` class. `UserDetails` is the contract Spring understands. By implementing it, your `User` speaks Spring Security's language — `getUsername()`, `getAuthorities()`, `isEnabled()` etc.

**Q: Why do we call `userRepository.findByEmail()` again in `login()` after `authenticationManager.authenticate()`?**
`authenticationManager.authenticate()` returns an `Authentication` object, not your `User`. We need the actual `User` object to call `jwtService.generateAccessToken(user)`. So we fetch it again. It's one extra DB query but keeps the code clean.

**Q: Why is the session stateless?**
JWT is stateless — the token itself contains everything needed to identify the user. There's no server-side session to store. Every request is self-contained: token in → validate → process → respond. This scales to millions of users because the server stores nothing between requests.

**Q: What's the difference between 401 and 403?**
- `401 Unauthorized` — we don't know WHO you are (no token or bad token)
- `403 Forbidden` — we know WHO you are but you don't have PERMISSION (valid token, wrong role)
