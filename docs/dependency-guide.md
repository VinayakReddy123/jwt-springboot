# Spring Boot Dependency Selection Guide

> How to pick, version, and verify dependencies like a pro — for every project you build.

---

## The Golden Rule

> **If Spring Boot has a starter for it, use the starter. Never add the raw library directly.**

```xml
<!-- WRONG — raw library, you manage the version, transitive deps, config -->
<dependency>
    <groupId>org.mongodb</groupId>
    <artifactId>mongodb-driver-sync</artifactId>
    <version>5.1.0</version>
</dependency>

<!-- CORRECT — starter wraps the driver + auto-configuration + Spring integration -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

Starters bundle: the library + auto-configuration + sensible defaults. You get everything wired up.

---

## Step 1 — Start at Spring Initializr, Not Google

**URL:** https://start.spring.io

This is the ONLY place to start a new Spring Boot project. Here's why:
- It only shows dependencies that are **compatible with your chosen Spring Boot version**
- It generates a `pom.xml` with correct versions already set
- It won't let you add incompatible combinations

**Workflow:**
1. Go to start.spring.io
2. Set your Spring Boot version FIRST
3. Add your dependencies by searching
4. Download → extract → open in IDE

**Common mistake:** Creating the project on Initializr but then manually adding more dependencies from mvnrepository without checking compatibility. Don't do this blindly.

---

## Step 2 — Understand the Spring Boot BOM

When you use `spring-boot-starter-parent` as your parent POM, you inherit a **BOM (Bill of Materials)** — a pre-tested list of hundreds of library versions that are guaranteed to work together.

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.6</version>
</parent>
```

**What this means for you:**
- For any Spring Boot-managed dependency, **do NOT add a `<version>` tag**
- The parent picks a version it has tested to be compatible
- When you upgrade Spring Boot version, all managed deps upgrade together safely

**Managed deps (no version needed):**
```xml
<!-- Spring Data MongoDB — parent manages version -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>

<!-- Lombok — parent manages version -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**External deps that need explicit versions (not managed by parent):**
```xml
<!-- jjwt is NOT a Spring project, so you must specify version -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
```

**Rule:** If the groupId starts with `org.springframework.boot` → no version needed. Otherwise → check if the parent manages it before adding a version.

---

## Step 3 — How to Pick the Right Spring Boot Version

**Decision tree:**
```
Is this a new project?
├── YES → Use the latest STABLE release (no M, no RC in version)
└── NO (existing project) → Don't upgrade unless you have a reason
```

**Version types:**
| Version | Meaning | Use in production? |
|---|---|---|
| `3.4.6` | Stable GA release | YES |
| `3.4.0-RC1` | Release Candidate | NO — still being tested |
| `3.4.0-M3` | Milestone 3 | NO — pre-release, APIs can change |
| `3.4.0-SNAPSHOT` | Nightly build | NEVER |

**How to check what's latest stable:**
- Go to start.spring.io — the default selected version is always the latest stable
- Or check: https://spring.io/projects/spring-boot (look for the green "CURRENT" badge)

---

## Step 4 — How to Pick External Library Versions (Not Managed by Parent)

For libraries the parent doesn't manage (like jjwt, MapStruct, etc.):

**Where to look:**
1. **GitHub releases page** of the library — most reliable, shows changelogs
2. **mvnrepository.com** — but READ carefully (see below)

**On mvnrepository.com — watch out for:**
```
4.1.0-M3   ← Milestone — pre-release, DO NOT use
4.1.0-RC1  ← Release Candidate — DO NOT use
4.1.0      ← Stable — USE THIS
3.9.2      ← Older stable — use if 4.x has known issues
```

The site sorts by "newest" which often shows pre-release at the top. Always scroll to find the latest **stable** (no suffix).

**Rule:** Never use a version with `-M`, `-RC`, or `-SNAPSHOT` in production or learning projects.

---

## Step 5 — Understand Scopes

Every dependency has a scope that controls when it's available:

| Scope | Available when | Example use |
|---|---|---|
| `compile` (default) | Always — compile + runtime + test | Spring Web, Security, MongoDB |
| `runtime` | Only at runtime, not compile time | jjwt-impl, jjwt-jackson, DB drivers |
| `test` | Only during test execution | spring-boot-starter-test, spring-security-test |
| `optional` | Compile only, not passed to dependents | Lombok, annotation processors |

**Why jjwt-impl is `runtime` and jjwt-api is `compile`:**
- `jjwt-api` — you write code against this (interfaces/classes you use directly)
- `jjwt-impl` — the actual implementation, loaded at runtime via ServiceLoader. You never import from it directly.
- This is the "program to interface, not implementation" principle.

**`<scope>compile</scope>` is the default — never write it explicitly.** It's noise.

---

## Step 6 — The Core Dependency Checklist

For every Spring Boot project, ask yourself:

```
[ ] Am I building a REST API?          → spring-boot-starter-web
[ ] Do I need a database?
    MongoDB                            → spring-boot-starter-data-mongodb
    PostgreSQL/MySQL                   → spring-boot-starter-data-jpa + driver
    Redis                              → spring-boot-starter-data-redis
[ ] Do I need authentication?          → spring-boot-starter-security
[ ] Do I need input validation?        → spring-boot-starter-validation
[ ] Do I need to reduce boilerplate?   → lombok (optional)
[ ] Do I need JWT?                     → jjwt-api, jjwt-impl, jjwt-jackson (0.12.x)
[ ] Do I need to write tests?          → spring-boot-starter-test (always add)
[ ] Am I testing security?             → spring-security-test
[ ] Do I need to send emails?          → spring-boot-starter-mail
[ ] Do I need caching?                 → spring-boot-starter-cache + cache provider
[ ] Do I need async messaging?         → spring-boot-starter-amqp (RabbitMQ) or kafka
```

---

## Step 7 — Compatibility Rules

### Spring Boot version → Java version
| Spring Boot | Minimum Java |
|---|---|
| 3.x | Java 17 |
| 2.x | Java 8 |

Never mix Spring Boot 3.x with Java 11 or lower — it won't compile.

### Spring Boot → Spring Framework (automatic)
| Spring Boot | Spring Framework |
|---|---|
| 3.x | 6.x |
| 2.x | 5.x |

You never add Spring Framework directly — Spring Boot pulls the right version.

### Spring Security version is managed by Spring Boot
Never add `spring-security-core` or `spring-security-web` directly. Always use `spring-boot-starter-security` and let the parent manage the Spring Security version.

### How to check if a library is compatible with your Spring Boot version
1. Search the library name at mvnrepository.com
2. Click on the version you want
3. Look at its `pom.xml` tab — check what Spring version it requires
4. Compare with what your Spring Boot version pulls in

---

## Step 8 — Common Mistakes Reference

| Mistake | Why Wrong | Correct |
|---|---|---|
| Added `spring-boot-starter-webflux` | Reactive stack, incompatible with servlet security | `spring-boot-starter-web` |
| Pinned version on BOM-managed dep | Can cause version conflicts on Boot upgrade | Remove `<version>` tag |
| Used `-M3` or `-RC1` version | Pre-release, unstable | Find latest stable (no suffix) |
| Wrote `<scope>compile</scope>` | Redundant, it's the default | Remove it |
| Added raw driver instead of starter | No auto-configuration | Use the starter |
| Pinned Lombok version manually | Parent BOM manages it | Remove `<version>` |
| Used `spring-boot-starter-security-test` | Doesn't exist | `spring-security-test` from `org.springframework.security` |
| Forgot `spring-boot-starter-validation` | `@NotBlank`, `@Email` do nothing at runtime | Add explicitly — it's not included by default |
| Added `spring-boot-starter-test` scope as compile | Should only be in test classpath | Always `<scope>test</scope>` |

---

## Quick Reference — How to Add Any New Dependency (Decision Flow)

```
1. Does Spring Boot have a starter for it?
   YES → Use spring-boot-starter-* (no version needed)
   NO  → Go to step 2

2. Is it on mvnrepository.com?
   Find latest STABLE version (no M, RC, SNAPSHOT)

3. Does the parent BOM manage it?
   Check: https://docs.spring.io/spring-boot/appendix/dependency-versions.html
   YES → No version tag needed
   NO  → Add explicit version

4. What scope does it need?
   You write code against it at compile time → compile (default, write nothing)
   Only needed at runtime (drivers, impls) → runtime
   Only needed in tests → test
   Annotation processor / code gen → optional

5. Verify it runs:
   mvn dependency:tree   ← see all resolved versions
   mvn spring-boot:run   ← confirm app starts without errors
```
