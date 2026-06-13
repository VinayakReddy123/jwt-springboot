# Mistakes Log

> Every mistake made during this learning journey — what went wrong, why, and how to avoid it.

---

## Format
Each entry:
```
### [Date] — Mistake Title
**What happened:** ...
**Why it's wrong:** ...
**The fix:** ...
**Rule to remember:** ...
```

---

---

### [2026-06-07] — Used WebFlux instead of Spring Web (MVC)

**What happened:** Added `spring-boot-starter-webflux` instead of `spring-boot-starter-web`.

**Why it's wrong:** WebFlux is for reactive/non-blocking programming (Project Reactor). Traditional JWT auth with Spring Security uses the servlet stack (Spring MVC). Mixing the two causes incompatibility in the Security filter chain — they have entirely different models.

**The fix:** Use `spring-boot-starter-web` for any standard REST API.

**Rule to remember:** WebFlux = reactive streams. Web = servlet/blocking. Pick one, never mix.

---

### [2026-06-07] — Missing Spring Data MongoDB dependency

**What happened:** Forgot to add `spring-boot-starter-data-mongodb`.

**Why it's wrong:** Without it, there's no MongoDB connection, no `@Document`, no `MongoRepository` — the entire database layer is missing.

**The fix:** Always add `spring-boot-starter-data-mongodb` whenever you're using MongoDB.

**Rule to remember:** Every layer of your app needs its dependency — web, security, database, validation. Don't skip any.

---

### [2026-06-07] — Used non-existent dependency `spring-boot-starter-security-test`

**What happened:** Used `spring-boot-starter-security-test` which does not exist in Maven.

**Why it's wrong:** Build would fail at dependency resolution. The real artifact is `spring-security-test` under `org.springframework.security` groupId.

**The fix:** `spring-security-test` from `org.springframework.security`.

**Rule to remember:** When in doubt, verify on mvnrepository.com. Don't guess artifact names.

---

### [2026-06-07] — Used old jjwt version 0.11.5 (deprecated API)

**What happened:** Added jjwt 0.11.5 instead of 0.12.6.

**Why it's wrong:** 0.11.x has a deprecated API (`Jwts.parser()` etc.). Most tutorials still use 0.11.x which teaches you deprecated patterns. The 0.12.x API is cleaner and actively maintained.

**The fix:** Always use 0.12.x for new projects.

**Rule to remember:** Don't blindly copy versions from old tutorials. Check the latest stable version.

---

### [2026-06-07] — Pinned Lombok version manually when parent BOM manages it

**What happened:** Set `<version>1.18.42</version>` on Lombok explicitly.

**Why it's wrong:** `spring-boot-starter-parent` already manages Lombok's version through its BOM (Bill of Materials). Overriding it manually can cause version conflicts and defeats the purpose of using the parent POM.

**The fix:** Use `<optional>true</optional>` on Lombok, no version needed. Let the parent manage it.

**Rule to remember:** If you use `spring-boot-starter-parent`, trust its BOM for managed dependencies. Only override when you have a specific reason.

---

### [2026-06-08] — Lombok annotations showing red in IntelliJ despite being in pom.xml

**What happened:** Added Lombok dependency to `pom.xml` but `@Data`, `@Builder` etc. showed red in IntelliJ as unresolved symbols.

**Why it's wrong:** Lombok works at two levels — Maven compile time and IDE time. Having it in `pom.xml` is not enough for IntelliJ to understand it. IntelliJ needs two things separately:
1. The Lombok plugin installed (`Settings → Plugins → Lombok`)
2. Annotation processing enabled (`Settings → Build → Compiler → Annotation Processors → Enable`)

Then Maven must be reloaded (`Maven panel → Reload All Maven Projects`) and caches invalidated (`File → Invalidate Caches → Invalidate and Restart`).

**The fix:** Install Lombok plugin + enable annotation processing + reload Maven + invalidate caches.

**Rule to remember:** pom.xml fixes the build. IDE plugins fix the editor. These are separate concerns — fixing one does not fix the other.

---

### [2026-06-07] — Copied configuration code without understanding it

**What happened:** Copied logging config from an AI tool into `application.properties` without knowing what any of it does. Included deprecated property names (`logging.file.max-size`, `logging.file.max-history`) that are silently ignored in Spring Boot 3+.

**Why it's wrong:** Copying code you don't understand is how bugs silently enter your project. Deprecated properties get ignored at runtime with no error — you think logging is configured but it isn't. You can't debug what you don't understand.

**The fix:** Only add config you can explain line by line. Look up what each property does before adding it.

**Rule to remember:** Every line in your config file should be there for a reason YOU understand. "AI gave it to me" is not a reason.

---

### [2026-06-10] — Used `@Autowired` field injection instead of constructor injection

**What happened:** In `UserDetailsServiceImpl`, injected `UserRepository` using `@Autowired` on a private field.

**Why it's wrong:** Field injection is an anti-pattern for three reasons:
1. **Untestable** — you cannot pass a mock via `new UserDetailsServiceImpl()` because the field is private with no setter. Unit tests become impossible without a Spring context.
2. **Hides dependencies** — nothing in the class signature tells you it needs a `UserRepository`. A constructor makes dependencies explicit.
3. **Not immutable** — field-injected dependencies can be replaced at runtime. Constructor injection with `final` makes them immutable after construction.
Spring itself has recommended against field injection since Spring 4.

**The fix:** Use constructor injection. Declare the field `final`, remove `@Autowired`, and add a constructor that accepts the dependency. Lombok's `@RequiredArgsConstructor` can generate this constructor automatically for `final` fields.

**Rule to remember:** In production Spring Boot code, always use constructor injection. `@Autowired` on a field is a code smell — it will come up in every serious code review.

---

### [2026-06-10] — Vague message in `UsernameNotFoundException`

**What happened:** Threw `new UsernameNotFoundException("UserName Not found")` without including the value that was not found.

**Why it's wrong:** When this exception hits your logs in production, "UserName Not found" tells you nothing. You don't know which email failed, whether it was a typo, a missing record, or a bug. Debugging becomes guesswork.

**The fix:** Always include the offending value in the message: `"User not found with email: " + email`.

**Rule to remember:** Exception messages are for the developer reading logs at 2am. Make them specific enough to diagnose the problem without opening a debugger.

---

### [2026-06-11] — Injected concrete class instead of interface in `ApplicationConfig`

**What happened:** Declared `private final UserDetailsServiceImpl userDetailsService` instead of `private final UserDetailsService userDetailsService`.

**Why it's wrong:** Spring often wraps beans in CGLIB/JDK proxies. If you inject the concrete class, Spring may fail to inject the proxy at startup. More importantly, it violates the "program to interfaces" principle — `DaoAuthenticationProvider` expects a `UserDetailsService`, so you should hold it as that type too.

**The fix:** Always inject the interface type. Spring will wire the correct implementation automatically since it's the only bean implementing `UserDetailsService`.

**Rule to remember:** Depend on interfaces, not implementations. This applies everywhere — fields, method parameters, return types.

---

### [2026-06-11] — Used setter-based `DaoAuthenticationProvider` API (removed in Spring Security 7)

**What happened:** Used `provider.setUserDetailsService()` and `provider.setPasswordEncoder()` which do not exist in Spring Security 7 (Spring Boot 4.x).

**Why it's wrong:** Spring Security 7 removed the setter-based API. Most tutorials online use Spring Boot 3.x / Spring Security 6.x — blindly following them on a Spring Boot 4 project causes compile errors.

**The fix:** Use the constructor directly: `new DaoAuthenticationProvider(userDetailsService)`. Spring auto-detects the `PasswordEncoder` bean from context.

**Rule to remember:** Spring Boot 4 / Spring Security 7 has breaking API changes. Always verify examples against your actual version — don't trust tutorials blindly.

---

### [2026-06-11] — Called `PasswordEncoder` as a method instead of injecting it

**What happened:** Wrote `passwordEncoder(request.getPassword())` in `AuthService` — treating `passwordEncoder` as a method call.

**Why it's wrong:** `PasswordEncoder` is a Spring bean that must be declared as a `final` field and injected via constructor. It has no standalone method in scope — calling it like a function doesn't compile.

**The fix:** Declare `private final PasswordEncoder passwordEncoder;` as a field, then call `passwordEncoder.encode(request.getPassword())`.

**Rule to remember:** In Spring, dependencies are fields — not methods. If you need something, inject it.

---

### [2026-06-11] — Used `RegisterRequest` as parameter for `login()` method

**What happened:** Declared `login(RegisterRequest request)` instead of `login(LoginRequest request)`.

**Why it's wrong:** These are separate DTOs for a reason. `LoginRequest` only has email + password. Using `RegisterRequest` in login exposes the wrong contract and confuses anyone reading the code.

**The fix:** Use the correct DTO for each method. `register()` takes `RegisterRequest`, `login()` takes `LoginRequest`.

**Rule to remember:** Each endpoint has its own DTO. Don't reuse the wrong one just because the fields overlap.

---

### [2026-06-11] — Swapped arguments in `passwordEncoder.matches()`

**What happened:** Called `passwordEncoder.matches(user.getPassword(), rawPassword)` — arguments reversed.

**Why it's wrong:** The signature is `matches(rawPassword, encodedPassword)`. With swapped args, BCrypt tries to treat the encoded hash as the raw input and the raw password as the hash — it always returns false. Every login silently fails.

**The fix:** `passwordEncoder.matches(rawPassword, user.getPassword())`.

**Rule to remember:** BCrypt `matches(raw, encoded)` — raw first, encoded second. Getting this wrong causes silent auth failures that are hard to debug.

---

### [2026-06-11] — Manually compared passwords in `login()` instead of using `AuthenticationManager`

**What happened:** Called `passwordEncoder.matches()` manually in `AuthService.login()` to check credentials.

**Why it's wrong:** This bypasses Spring Security entirely — no authentication events, no security context integration, no consistent error handling. `AuthenticationManager.authenticate()` is the correct entry point. It internally calls `UserDetailsService` + `PasswordEncoder`, throws `BadCredentialsException` on failure, and integrates with the security pipeline.

**The fix:** Call `authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password))` and let Spring handle everything. If credentials are wrong, it throws automatically.

**Rule to remember:** Never roll your own authentication logic. Spring Security's `AuthenticationManager` exists for exactly this purpose.

---

### [2026-06-11] — `AuthenticationManager` field missing `final` keyword

**What happened:** Declared `private AuthenticationManager authenticationManager` without `final`.

**Why it's wrong:** `@RequiredArgsConstructor` only generates constructor arguments for `final` fields. Without `final`, the field is skipped — Spring never injects it, it stays `null`, and calling `.authenticate()` throws a `NullPointerException` at runtime.

**The fix:** All injected fields must be `private final`.

**Rule to remember:** If you use `@RequiredArgsConstructor`, every dependency must be `final`. No `final` = no injection.

---

### [2026-06-11] — Assigned `authenticate()` return value to `boolean`

**What happened:** Wrote `boolean isAuthenticated = authenticationManager.authenticate(...)`.

**Why it's wrong:** `authenticate()` returns an `Authentication` object, not a `boolean`. It throws `BadCredentialsException` on failure — the return value is irrelevant for auth checks. Assigning it to `boolean` is a compile error and shows a misunderstanding of how the method works.

**The fix:** Just call `authenticationManager.authenticate(...)` with no assignment. Remove the manual `if(!isAuthenticated)` check — the exception handles failure automatically.

**Rule to remember:** `AuthenticationManager.authenticate()` communicates failure via exceptions, not return values.

---

### [2026-06-11] — Empty `@RequestMapping("")` on `AuthController`

**What happened:** Used `@RequestMapping("")` as the base path instead of `@RequestMapping("/api/auth")`.

**Why it's wrong:** No namespace means endpoints are exposed at `/register` and `/login` on the root — no grouping, no versioning, clashes with any other controller. Every API should be namespaced.

**The fix:** `@RequestMapping("/api/auth")` groups all auth endpoints under a clear, consistent path.

**Rule to remember:** Always namespace your controllers. `/api/auth`, `/api/users`, `/api/products` — never expose endpoints at the root.

---

### [2026-06-11] — Imported unused dependencies into `AuthController`

**What happened:** Imported `User`, `UserRepository`, and `JwtService` in `AuthController` — none of which were used.

**Why it's wrong:** Controllers should only know about DTOs and Services. Importing the model and repository layers breaks separation of concerns and signals that logic that belongs in the service layer might be leaking into the controller.

**The fix:** Delete unused imports. If your IDE warns about unused imports, treat it as a signal that something is wrong with your design.

**Rule to remember:** Controller → Service → Repository. Each layer only talks to the layer directly below it.

---

### [2026-06-11] — Used `ResponseEntity<?>` wildcard instead of concrete type

**What happened:** Declared `public ResponseEntity<?> register(...)` using a wildcard generic.

**Why it's wrong:** The return type is always `AuthResponse` — the wildcard hides this, disables compile-time type checking, and makes the API contract invisible to callers.

**The fix:** `public ResponseEntity<AuthResponse> register(...)` — always use the concrete type.

**Rule to remember:** Wildcards (`?`) are for when you genuinely don't know the type. If you know it, use it.

---

### [2026-06-11] — File name and class name mismatch in config

**What happened:** Created `SecurityConfig.java` but named the class inside `ApplicationConfig`.

**Why it's wrong:** Java convention requires the file name to match the public class name. Even for non-public classes, a mismatch causes confusion, breaks IDE navigation, and will fail in some build tools.

**The fix:** File name and class name must always match. `SecurityConfig.java` → `public class SecurityConfig`.

**Rule to remember:** One public class per file, file name matches class name. Always.
