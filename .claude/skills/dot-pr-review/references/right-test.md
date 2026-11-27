# The right test

Shared by `dotcms-pr-test-reviewer` (frontend specs) and `dotcms-pr-java-test-reviewer` (Java and API tests). Each agent applies these rules to every new or changed test before its own stack rules: syntax is easy to fix, while a test that passes whatever the code does reads as coverage, adds maintenance and catches nothing.

## 1. Is it the right test?

- **The revert test.** Picture the production change in this PR reverted. Would this test fail? If it would still pass, it doesn't test the change: **Important**, or **Critical** when it is the only test for a bug fix or a changed behavior. Read the production code the test covers to answer this; the test alone cannot tell you.
- **It targets what the PR is about**: the behavior the issue or bug describes, not something adjacent that was easier to set up. Name the behavior that is left untested.
- **It asserts an observable outcome.** Frontend: the DOM the user sees (`byTestId`), the store state a selector exposes, the value an `output()` emits, the HTTP request sent. Java: the returned value, the persisted row, the response status and body, the thrown exception. When the outcome was observable, a test whose only assertion is that an internal method or mock was called (`toHaveBeenCalled`, `verify(mock).method()`) is **Important**; such assertions next to real ones are **Quality**.
- **The right level.** The risk decides the level, and a test that mocks away the very thing at risk proves nothing: **Important**.
  - Frontend: component logic gets a component test that renders and interacts; a pure function, mapper or util gets a plain unit test without TestBed; a store gets a store test against its public API.
  - Java: SQL, transactions, permission filtering, cache/DB consistency and REST wiring are only proven by an **integration** test against the real database and APIs (or an API test); a unit test that mocks `DotConnect`, `PermissionAPI` or the factory to "test" them is **Important**. Pure logic (parsing, mapping, validation) belongs in a fast unit test; an integration test for it is **Quality** (slow, not wrong).

## 2. Is it tautological? Reduce or eliminate it

A tautological test can't fail, because its expected value comes from the same place as its actual value. Report it, and say plainly that **deleting it is an acceptable fix** when no real assertion can replace it: fewer, sharper tests beat more tests.

- **The mock echoes itself**: the test stubs a call to return `X` and asserts the result is `X`, with no transformation by the code under test in between.
- **The call checks itself**: `expect(spy).toHaveBeenCalled()` or `verify(x).foo()` right after the test itself called that same spy or method.
- **The expected value comes from the code under test**: the test calls the function (or copies its algorithm) to build `expected`.
- **The framework or the language is the thing tested**: `setInput('x', 1)` then `expect(component.x()).toBe(1)`, a signal set and read back, getter/setter round-trips, a record's accessors, Jackson serializing a plain field, a builder returning what was set, a constant compared with itself.
- **Over-mocking**: mocking the very class under test, or so deeply that only the mocks are exercised.
- **An assertion that can't fail, or none**: `toBeTruthy()` / `toBeDefined()` / `assertNotNull` on something just created, `expect(true).toBe(true)`, `assertTrue(true)`, a test that only exercises a path.
- **A smoke test is the only test** (`should create`, or only constructing the object) for a new component, store, service or class that has logic: **Important**, the logic is untested.

**Severity.** A tautological test that is the **only** coverage of changed behavior is **Critical**: it is coverage theater, and the change has no real test. A tautological test next to real tests is **Important**: delete it, or replace it with the assertion it was trying to make.
