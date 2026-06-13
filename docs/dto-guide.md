# DTO — Data Transfer Object

## The One-Line Mental Model

```
Entity  =  how your data looks in the DATABASE
DTO     =  how your data looks on the NETWORK
```

These are two different things. Never mix them.

---

## What is a DTO?

A DTO is a simple class whose only job is to carry data between the client and your server.
It has fields and getters/setters — nothing else. No business logic, no database annotations.

```
Client  →  [Request DTO]  →  Controller  →  Service  →  Entity  →  MongoDB
Client  ←  [Response DTO] ←  Controller  ←  Service  ←  Entity  ←  MongoDB
```

---

## Why Not Just Use the Entity (User.java) Directly?

This is the most common beginner mistake. Here is exactly what goes wrong.

---

### Problem 1 — The client receives your password hash

Imagine you write this without a DTO:

```java
@PostMapping("/register")
public ResponseEntity<User> register(@RequestBody User user) {
    userRepository.save(user);
    return ResponseEntity.ok(user); // returning the User entity directly
}
```

The return type is `ResponseEntity<User>`. You told Spring: "take this User object and send it back."

Spring uses a library called **Jackson** internally. Jackson's job is to take any Java object
and convert it to JSON. It does this by scanning every single field in the object:

```
User object
  ├── id        → "64abc123"
  ├── email     → "vinayak@example.com"
  ├── password  → "$2a$10$hashed..."    ← sensitive
  ├── roles     → ["ROLE_USER"]
  └── enabled   → true
```

Jackson doesn't know which fields are sensitive. It doesn't know what "password" means.
It just sees fields and converts ALL of them to JSON. That JSON goes over the network to the client.

**You told Spring to return the User object. Spring returned the User object. All of it.**

With a response DTO (`AuthResponse`) that only has `accessToken` and `refreshToken`,
Jackson can only serialize those two fields. The password never leaves the server.

---

### Problem 2 — The client sets fields they should never control

Your `User` entity has these fields:

```java
private String id;
private String email;
private String password;
private List<String> roles;    // ← internal, server-controlled
private boolean enabled;        // ← internal, server-controlled
```

If you accept `User` directly from the client as a `@RequestBody`, Spring maps
the incoming JSON to your User object field by field. The client can send this:

```json
{
    "email": "vinayak@example.com",
    "password": "mypassword",
    "roles": ["ROLE_ADMIN"],
    "enabled": true
}
```

Spring maps this JSON to your User object. Now `user.getRoles()` returns `["ROLE_ADMIN"]`.
You call `userRepository.save(user)`. That person is now an admin in your database.

**The model was designed to represent a user in the DB — not to represent what a
client is allowed to send. Those are two different things.**

With a `RegisterRequest` DTO that only has `email` and `password`, the fields
`roles` and `enabled` simply don't exist on the DTO. The client cannot set them.
You assign roles yourself in the service layer, where you control the logic.

---

### Problem 3 — Internal changes break your API contract (tight coupling)

This one bites you months later, not immediately.

Right now your `User` entity has:

```java
private String password;
```

Your API response (because you return the entity directly) looks like:

```json
{
    "email": "vinayak@example.com",
    "password": "$2a$10$hashed..."
}
```

Your frontend developer writes code that reads `response.password`.

Three months later you rename the field internally for clarity:

```java
private String hashedPassword; // renamed from "password"
```

Jackson serializes field names as-is. Your API response is now:

```json
{
    "email": "vinayak@example.com",
    "hashedPassword": "$2a$10$hashed..."  ← name changed
}
```

`response.password` no longer exists. Your frontend breaks. Your mobile app breaks.
Any third party consuming your API breaks. **One internal rename broke everything outside.**

---

With a DTO, the DTO is the stable contract. Your entity can change freely:

```java
// Entity changed internally
private String hashedPassword;

// But your DTO field name never changes
public class UserProfileResponse {
    private String email;
    // You control the mapping in the service:
}

// Service maps entity → DTO
UserProfileResponse dto = UserProfileResponse.builder()
        .email(user.getEmail())
        // internal name changed, but the DTO field is stable
        .build();
```

**The DTO absorbs internal changes. The outside world sees a stable contract.**

---

## Summary Table

| Problem | Without DTO | With DTO |
|---|---|---|
| Sensitive data leak | Password hash sent to client | Response DTO only exposes what you choose |
| Client controls internal fields | Client can set roles, enabled | Request DTO only accepts email + password |
| Internal changes break API | Renaming a field breaks all clients | DTO is the stable contract, entity changes freely |

---

## The 3 DTOs in This Project

### RegisterRequest — client → server
```
email     @NotBlank @Email
password  @NotBlank @Size(min=8)
```
- `@NotBlank` — rejects null, empty strings, and whitespace-only strings
- `@Email` — rejects strings that aren't valid email format
- `@Size(min=8)` — enforces minimum password length at the API boundary

### LoginRequest — client → server
```
email     @NotBlank @Email
password  @NotBlank
```
No `@Size` on password — you're checking an existing password, not creating one.

### AuthResponse — server → client
```
accessToken   String
refreshToken  String
tokenType     String  (default: "Bearer")
```
The client builds the Authorization header from this:
`Authorization: Bearer <accessToken>`

---

## How Validation Works End-to-End

```java
@PostMapping("/register")
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    ...
}
```

- `@RequestBody` — Jackson deserializes the incoming JSON into your DTO object
- `@Valid` — triggers all constraint annotations (`@NotBlank`, `@Email`, `@Size` etc.)

If validation fails, Spring automatically returns `400 Bad Request` before your
method body even runs. You write zero validation logic yourself.

**Important:** `@Valid` without `@RequestBody` does nothing. Both are required together.

---

## Rule to Remember

| Layer | Class | Purpose |
|---|---|---|
| API input | Request DTO | What the client is allowed to send |
| API output | Response DTO | What the client is allowed to see |
| Database | Entity (User) | What actually gets stored in MongoDB |

Never expose your entity directly. Always map between entity and DTO in the Service layer.
