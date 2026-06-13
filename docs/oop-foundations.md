# OOP Foundations — Interfaces, Abstract Classes, extends, implements, override

> Written from real confusion encountered in this project. Every example is from our codebase.

---

## The Core Question: "Who defines the rules, and who follows them?"

There are three roles in Java OOP:

| Role | Keyword | Meaning |
|---|---|---|
| Define a contract | `interface` | "Anyone who signs this must do these things" |
| Partially implement | `abstract class` | "I've done some of it, you finish the rest" |
| Fully implement | `class` | "I do everything myself" |

---

## Interfaces — A Contract

An interface says: **"If you claim to be this thing, you must provide these methods."**

```java
public interface UserDetailsService {
    UserDetails loadUserByUsername(String username);
}
```

This is a contract. It says: "Any class that claims to be a `UserDetailsService` MUST implement `loadUserByUsername`."

**You use `implements` to sign the contract:**

```java
public class UserDetailsServiceImpl implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String email) {
        // your implementation
    }
}
```

**Rules:**
- You MUST implement every method the interface declares — no exceptions
- You use `@Override` to signal "I am fulfilling this contract method"
- A class can implement multiple interfaces

**From your project:**
- `UserDetailsServiceImpl implements UserDetailsService` — you signed Spring Security's contract
- `User implements UserDetails` — your User model signed Spring Security's user contract

---

## Abstract Classes — Partial Implementation

An abstract class says: **"I've done some of the work. You must finish the rest."**

```java
public abstract class OncePerRequestFilter {

    // Already implemented — don't touch this
    public final void doFilter(request, response, chain) {
        // checks if filter already ran this request
        // then calls doFilterInternal()
    }

    // NOT implemented — you MUST provide this
    protected abstract void doFilterInternal(request, response, chain);
}
```

`OncePerRequestFilter` did the hard infrastructure work (`doFilter`). It left `doFilterInternal` abstract — meaning: "I don't know your specific logic, you tell me."

**You use `extends` to inherit and complete it:**

```java
public class JwtAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(request, response, chain) {
        // your JWT logic here
    }
}
```

**Rules:**
- You MUST override every `abstract` method — the class won't compile otherwise
- You CAN override non-abstract methods if you want to change their behavior
- You should NOT override `final` methods — they're locked intentionally
- A class can only extend ONE class (Java has no multiple inheritance)

---

## The Critical Difference

| | Interface | Abstract Class |
|---|---|---|
| Has implemented methods? | No (before Java 8) / sometimes (default methods) | Yes |
| Has unimplemented methods? | All of them | Some of them (`abstract`) |
| Keyword to use | `implements` | `extends` |
| How many can you use? | Many | Only one |
| Purpose | Define a contract | Share partial implementation |

---

## When to Override — The Two Cases

### Case 1: You MUST override (abstract methods)
If a method is marked `abstract`, you have no choice. The class won't compile until you implement it.

```java
// abstract — you MUST override this
protected abstract void doFilterInternal(...);
```

### Case 2: You CAN override (non-abstract methods)
If a method already has an implementation but you want different behavior, you can override it.

```java
// already implemented in UserDetails — but User overrides it
@Override
public String getUsername() {
    return email; // we return email instead of a username field
}
```

Your `User` model did this — `UserDetails` has `getUsername()` with a default, but you overrode it to return `email` instead.

---

## The Template Method Pattern (what confused you)

This is a specific design pattern used by `OncePerRequestFilter`:

```
Framework says:
  "I'll handle the infrastructure (doFilter).
   You handle the business logic (doFilterInternal).
   I'll call yours from mine."
```

This is called the **Template Method Pattern** — the framework defines the skeleton of an algorithm, you fill in one specific step.

You encountered this when you tried to override `doFilter()` — Spring had already implemented it and made it `final`. Your job was always `doFilterInternal()`.

**Rule:** When extending a framework class, look for the `abstract` method — that's your hook. Don't fight the framework's existing implementation.

---

## Quick Decision Guide

```
Do I need to define a contract that multiple classes will follow?
  → Use interface

Do I have shared logic but also need subclasses to fill in specific parts?
  → Use abstract class

Am I implementing an interface?
  → implements, must override all methods

Am I extending an abstract class?
  → extends, must override all abstract methods

Am I extending a concrete class and want to change some behavior?
  → extends, can override any non-final method

Is the method marked abstract?
  → You MUST override it

Is the method marked final?
  → You CANNOT override it
```

---

## Your Project — Mapped to These Rules

| Class | Rule Applied |
|---|---|
| `User implements UserDetails` | Signed Spring Security's user contract — must implement all methods |
| `UserDetailsServiceImpl implements UserDetailsService` | Signed the service contract — must implement `loadUserByUsername` |
| `JwtAuthFilter extends OncePerRequestFilter` | Inherited filter infrastructure — must implement `doFilterInternal` |
| `User.getUsername()` returns `email` | Overrode an existing method to change its behavior |
| `User.isAccountNonExpired()` returns `true` | Overrode to provide a simple default |
