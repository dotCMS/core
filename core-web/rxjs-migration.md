---
name: rxjs-migration
---

# RxJS 6 to RxJS 7 Migration Guide

## System Role & Objective

You are an **Expert Senior Angular/RxJS Developer** specialized in refactoring and updating legacy codebases.

Your specific task is to migrate TypeScript code from **RxJS 6 to RxJS 7**, strictly adhering to the breaking changes and deprecations documented in the RxJS 7 changelog.

## Critical Migration Rules

Apply the following transformations when you detect RxJS 6 patterns. **Do not hallucinate APIs that do not exist.**

---

### 1. Promise Conversion (Breaking Change)

**Issue:** `Observable.prototype.toPromise()` is deprecated and the return type changed to `Promise<T | undefined>`.

**Action:** Replace `.toPromise()` with `lastValueFrom(source$)` (preferred equivalent) or `firstValueFrom(source$)`.

**Import:** Ensure `lastValueFrom` or `firstValueFrom` is imported from `'rxjs'`.

**Logic:**
- Use `lastValueFrom(source$)` if the logic expects the stream to complete and take the final value (closest behavior to old `toPromise`).
- Use `firstValueFrom(source$)` if the logic only needs the first emission.

**Warning:** If the source might not emit, handle the `EmptyError` or provide a default value: `lastValueFrom(source$, { defaultValue: x })`.

**Example:**

```typescript
// ❌ RxJS 6
import { Observable } from 'rxjs';

const data$: Observable<string> = getData();
const result = await data$.toPromise();

// ✅ RxJS 7
import { Observable, lastValueFrom } from 'rxjs';

const data$: Observable<string> = getData();
const result = await lastValueFrom(data$);

// ✅ RxJS 7 (with default value)
const result = await lastValueFrom(data$, { defaultValue: 'default' });
```

---

### 2. Error Handling (Breaking Change)

**Issue:** `throwError` no longer accepts an error instance directly; it requires a factory function.

**Action:** Wrap the error argument in a callback function.

**Example:**

```typescript
// ❌ RxJS 6
import { throwError } from 'rxjs';

const error$ = throwError(new Error('Something went wrong'));

// ✅ RxJS 7
import { throwError } from 'rxjs';

const error$ = throwError(() => new Error('Something went wrong'));
```

---

### 3. Subscription Chaining (Breaking Change)

**Issue:** `Subscription.prototype.add` no longer returns the `Subscription` reference (it returns `void`), so chaining is impossible.

**Action:** Break chained `.add()` calls into multiple lines.

**Example:**

```typescript
// ❌ RxJS 6
import { Subscription } from 'rxjs';

class MyComponent {
  private subs = new Subscription();

  ngOnInit() {
    this.subs.add(obs1$).add(obs2$).add(obs3$);
  }
}

// ✅ RxJS 7
import { Subscription } from 'rxjs';

class MyComponent {
  private subs = new Subscription();

  ngOnInit() {
    this.subs.add(obs1$);
    this.subs.add(obs2$);
    this.subs.add(obs3$);
  }
}
```

---

### 4. Operator Renaming & Deprecations

**Issue:** RxJS 7 renames operators used within `.pipe()` that collide with creation functions to allow cleaner imports.

**Action:** If you see these operators inside a `.pipe()`, rename them:
- `zip` operator → `zipWith`
- `merge` operator → `mergeWith`
- `concat` operator → `concatWith`
- `race` operator → `raceWith`

**Import:** Update imports from `'rxjs/operators'`.

**Example:**

```typescript
// ❌ RxJS 6
import { zip, merge, concat, race } from 'rxjs/operators';

source$.pipe(
  zip(other$),
  merge(another$),
  concat(more$),
  race(competing$)
);

// ✅ RxJS 7
import { zipWith, mergeWith, concatWith, raceWith } from 'rxjs/operators';

source$.pipe(
  zipWith(other$),
  mergeWith(another$),
  concatWith(more$),
  raceWith(competing$)
);
```

---

### 5. Generic Type Signatures

**Issue:** Many creation functions (`combineLatest`, `forkJoin`, `of`, `from`, `merge`, `concat`) have stricter or changed generic signatures.

**Action:** Remove explicit generic types passed to these functions and let TypeScript inference handle the types. Explicit generics often cause compilation errors in v7.

**Example:**

```typescript
// ❌ RxJS 6
import { combineLatest, forkJoin } from 'rxjs';

const result$ = combineLatest<[string, number]>([str$, num$]);
const data$ = forkJoin<{ user: User; posts: Post[] }>({
  user: user$,
  posts: posts$
});

// ✅ RxJS 7
import { combineLatest, forkJoin } from 'rxjs';

const result$ = combineLatest([str$, num$]);
const data$ = forkJoin({
  user: user$,
  posts: posts$
});
```

---

### 6. Multicasting (Refactor)

**Issue:** `multicast`, `publish`, `publishReplay`, `publishLast` are deprecated.

**Action:**
- If simple multicasting is needed, prefer `share()` or `shareReplay()`.
- For complex cases: Use the new `connectable` API.

**Example:**

```typescript
// ❌ RxJS 6
import { publish, publishReplay, publishLast } from 'rxjs/operators';

source$.pipe(publish());
source$.pipe(publishReplay(1));
source$.pipe(publishLast());

// ✅ RxJS 7
import { share, shareReplay } from 'rxjs/operators';

source$.pipe(share());
source$.pipe(shareReplay({ bufferSize: 1, refCount: true }));
```

---

### 7. Subject & Observable Internals

**Breaking Changes:**
- `lift` is no longer exposed.
- `Subscriber` constructor arguments are stricter.
- `AsyncSubject`, `BehaviorSubject`, `ReplaySubject` protected methods `_subscribe` are no longer public.

**Action:** Avoid using these internal APIs. If found, refactor to use public APIs.

---

### 8. Pluck Operator Deprecation (Breaking Change - RxJS 8)

**Issue:** `pluck` operator is deprecated and will be removed in RxJS 8.

**Action:** Replace `pluck(...properties)` with `map(x => x?.property1?.property2?.propertyN)` using optional chaining.

**Import:** Ensure `map` is imported from `'rxjs/operators'` or `'rxjs'`. Remove `pluck` from imports.

**Logic:**
- Convert `pluck('foo')` to `map(x => x?.foo)`
- Convert `pluck('foo', 'bar')` to `map(x => x?.foo?.bar)`
- Convert `pluck('foo', 'bar', 'baz')` to `map(x => x?.foo?.bar?.baz)`
- For nested properties, chain optional chaining operators (`?.`)

**Example:**

```typescript
// ❌ RxJS 6/7 (deprecated in RxJS 8)
import { fromEvent, pluck } from 'rxjs';

const clicks = fromEvent(document, 'click');
const tagNames = clicks.pipe(pluck('target', 'tagName'));

tagNames.subscribe(x => console.log(x));

// ✅ RxJS 7/8
import { fromEvent } from 'rxjs';
import { map } from 'rxjs/operators';

const clicks = fromEvent(document, 'click');
const tagNames = clicks.pipe(map(x => x?.target?.tagName));

tagNames.subscribe(x => console.log(x));
```

**More Examples:**

```typescript
// ❌ RxJS 6/7
import { pluck } from 'rxjs/operators';

source$.pipe(pluck('user', 'name'));
source$.pipe(pluck('data', 'items', 0, 'title'));

// ✅ RxJS 7/8
import { map } from 'rxjs/operators';

source$.pipe(map(x => x?.user?.name));
source$.pipe(map(x => x?.data?.items?.[0]?.title));
```

---

## Execution Strategy

When migrating code, follow this systematic approach:

1. **Analyze Imports:** Check `import { ... } from 'rxjs'` and `'rxjs/operators'`.
2. **Scan Usage:** Look for:
   - `.toPromise()` calls
   - `throwError` without factory function
   - Chained `.add()` calls
   - Renamed operators (`zip`, `merge`, `concat`, `race` inside `.pipe()`)
   - Explicit generic types on creation functions
   - Deprecated multicasting operators
   - `pluck` operator usage (deprecated in RxJS 8)
3. **Refactor:** Apply changes file-by-file or block-by-block.
4. **Verification:** Ensure no strict type errors are introduced (especially with `lastValueFrom` return types).

---

## Complete Example Refactor

### Input (RxJS 6):

```typescript
import { throwError, of, merge } from 'rxjs';
import { map, zip } from 'rxjs/operators';

class DataService {
  private subscription = new Subscription();

  async fetchData(): Promise<string> {
    const source$ = of('data');
    return source$.toPromise();
  }

  handleError() {
    return throwError(new Error('Operation failed'));
  }

  setupSubscriptions() {
    const obs1$ = of(1);
    const obs2$ = of(2);
    this.subscription.add(obs1$).add(obs2$);
  }

  combineStreams(source$: Observable<string>, other$: Observable<string>) {
    return source$.pipe(zip(other$));
  }
}
```

### Output (RxJS 7):

```typescript
import { throwError, of, lastValueFrom } from 'rxjs';
import { map, zipWith } from 'rxjs/operators';
import { Subscription } from 'rxjs';

class DataService {
  private subscription = new Subscription();

  async fetchData(): Promise<string> {
    const source$ = of('data');
    return lastValueFrom(source$);
  }

  handleError() {
    return throwError(() => new Error('Operation failed'));
  }

  setupSubscriptions() {
    const obs1$ = of(1);
    const obs2$ = of(2);
    this.subscription.add(obs1$);
    this.subscription.add(obs2$);
  }

  combineStreams(source$: Observable<string>, other$: Observable<string>) {
    return source$.pipe(zipWith(other$));
  }
}
```

---

## Instructions for Cursor Agent

1. **Scan the selected code/files** and identify all RxJS 6 patterns listed above.

2. **Apply the migration rules** systematically, one transformation at a time.

3. **For ambiguous cases:**
   - When choosing between `firstValueFrom` vs `lastValueFrom`, analyze the context:
     - If the observable is expected to complete and you need the final value → use `lastValueFrom`
     - If you only need the first emission → use `firstValueFrom`
   - **Default to `lastValueFrom`** for `toPromise()` replacements unless context clearly indicates otherwise.

4. **Preserve code structure:** Maintain the same logic flow and variable names when possible.

5. **Update imports:** Ensure all necessary imports are added and unused imports are removed.

6. **Verify types:** After migration, ensure TypeScript types are correct and no compilation errors are introduced.

7. **Handle edge cases:**
   - If an observable might not emit, add `defaultValue` option to `lastValueFrom`/`firstValueFrom`
   - If error handling needs adjustment, ensure factory functions are used with `throwError`

8. **Document changes:** If making significant refactoring decisions, add brief comments explaining the migration choice.



## Architecture & Structure

### Monorepo Organization

-   **apps/** - Main applications (dotcms-ui, dotcms-block-editor, dotcms-binary-field-builder, mcp-server)
-   **libs/sdk/** - External-facing SDKs (client, react, angular, analytics, experiments, uve)
-   **libs/data-access/** - Angular services for API communication
-   **libs/ui/** - Shared UI components and patterns
-   **libs/portlets/** - Feature-specific portlets (analytics, experiments, locales, etc.)
-   **libs/dotcms-models/** - TypeScript interfaces and types
-   **libs/block-editor/** - TipTap-based rich text editor
-   **libs/template-builder/** - Template construction utilities

### Technology Stack

-   **Angular 20.3.0** with standalone components
-   **Nx 20.5.1** for monorepo management
-   **PrimeNG 17.18.11** UI components
-   **TipTap 2.14.0** for rich text editing
-   **NgRx 19.2.1** for state management
-   **Jest 29.7.0** for testing
-   **Playwright** for E2E testing
-   **Node.js >=v22.15.0** requirement
