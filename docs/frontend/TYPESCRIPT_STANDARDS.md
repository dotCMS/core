# TypeScript Standards

TypeScript standards for the dotCMS frontend (core-web). These apply to Angular, SDK, and any TypeScript in the monorepo. For Angular-specific patterns (signals, templates, components), see [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md).

## Strict Type Checking

- **Always enable and adhere to strict type checking.** This catches errors early and improves code quality.
- In `tsconfig`: use `"strict": true` (or equivalent strict flags). Do not disable strict checks to work around type errors; fix the types instead.

## Prefer Type Inference

Allow TypeScript to infer types when they are obvious from the context. This reduces verbosity while keeping type safety.

**Bad:**
```typescript
let name: string = 'Angular';
const count: number = 42;
const items: string[] = ['a', 'b'];
```

**Good:**
```typescript
let name = 'Angular';
const count = 42;
const items = ['a', 'b'];
```

Add explicit types when inference is unclear (e.g. function parameters, public API boundaries, or when you want to narrow the type).

## Avoid `any`

Do not use the `any` type unless absolutely necessary; it bypasses type checking.

- When the type is uncertain but you need to handle it safely, use **`unknown`** and narrow (e.g. with `typeof`, type guards, or assertions after validation).

**Bad:**
```typescript
function process(data: any) {
  return data.value; // no type safety
}
```

**Good:**
```typescript
function process(data: unknown) {
  if (data && typeof data === 'object' && 'value' in data && typeof (data as { value: unknown }).value === 'string') {
    return (data as { value: string }).value;
  }
  throw new Error('Invalid data');
}
```

For well-known shapes, prefer interfaces or types instead of `unknown` + guards.

## No Enums — Use `as const` Instead

Do not use TypeScript enums. Use `as const` objects for a set of named constants.

**Bad:**
```typescript
enum Status {
  Pending = 'pending',
  Active = 'active',
  Done = 'done',
}
```

**Good:**
```typescript
const Status = {
  Pending: 'pending',
  Active: 'active',
  Done: 'done',
} as const;

type Status = (typeof Status)[keyof typeof Status]; // 'pending' | 'active' | 'done'
```

**Good (simple union):**
```typescript
const MyEnum = {
  VALUE1: 'value1',
  VALUE2: 'value2',
} as const;
```

## Private Properties: Use `#` Prefix

Use the **`#`** prefix for private class fields (ECMAScript private). Do not use the `private` keyword for instance properties.

**Bad:**
```typescript
class MyService {
  private myPrivateProperty = 'private';
  private cache = new Map<string, unknown>();
}
```

**Good:**
```typescript
class MyService {
  #myPrivateProperty = 'private';
  #cache = new Map<string, unknown>();
}
```

`#` fields are truly private (not visible in subclasses or at runtime as own properties) and align with modern JavaScript.

## Quick Reference

| Do | Don't |
|----|--------|
| Strict type checking | Disable strict or use `any` to silence errors |
| Let TS infer when obvious | Redundant annotations like `let x: string = 'a'` |
| Use `unknown` when type is uncertain | Use `any` |
| Use `as const` for constant sets | Use `enum` |
| Use `#` for private fields | Use `private` for instance properties |

## See also
- [ANGULAR_STANDARDS.md](./ANGULAR_STANDARDS.md) — Angular, signals, templates
- [TESTING_FRONTEND.md](./TESTING_FRONTEND.md) — Spectator, typing with generics (avoid `any`)
- [docs/frontend/README.md](./README.md) — Index of all frontend docs
- [TypeScript Handbook](https://www.typescriptlang.org/docs/handbook/intro.html) — Official TypeScript docs
