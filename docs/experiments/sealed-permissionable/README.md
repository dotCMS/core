# Can the `Permissionable` hierarchy be sealed?

Groundwork for the Java 25 talk (#34154). **Nothing here is production code and nothing here is
built** — these sources live outside every Maven source root on purpose, because two of the three
experiments are supposed to fail to compile. The failures are the result.

```bash
./run.sh          # needs only a JDK 22+ — no Maven, no dotCMS classpath, no network
```

Everything quoted below is that script's output.

## Why the question comes up

`PermissionBitFactoryImpl.resolvePermissionType` dispatches over `Permissionable` with a pattern
`switch` that ends in a `default`. A `default` is a catch-all: the day someone adds an asset type,
the code keeps compiling and the new type quietly takes the default path.

Sealing the hierarchy is what would let that `default` go away, and with it the silence — a sealed
type tells the compiler the complete list of subtypes, so it can check that a switch covers them all.

The hierarchy is not large: **14 direct implementors of `Permissionable`, 7 direct subclasses of
`Inode`.** Perfectly listable. So the question is a fair one.

## The setup

`src/` mirrors the real package layout with a handful of the real types, because the package layout
is the whole point:

| Type | Package | Role |
|---|---|---|
| `Permissionable` | `com.dotmarketing.business` | the sealed root |
| `Contentlet` | `com.dotmarketing.portlets.contentlet.model` | `sealed`, seals further down |
| `Host` | `com.dotmarketing.beans` | `final` — a leaf |
| `Folder` | `com.dotmarketing.portlets.folders.model` | `final` |
| `Identifier` | `com.dotmarketing.beans` | `final` |
| `Inode` | `com.dotmarketing.beans` | `non-sealed` — gives up and reopens |
| `PermissionResolver` | `com.dotmarketing.business` | the switch, with no `default` |

`Inode` being `non-sealed` does **not** break exhaustiveness downstream: every subclass of `Inode` is
still an `Inode`, so one `case Inode` covers all of them. Sealing reasons about permitted subtypes,
not about leaves.

## Experiment 1 — seal it where it lives

Compile those sources **without** `module-info.java`, which is the situation dotCMS is in today:

```
src/com/dotmarketing/portlets/contentlet/model/Contentlet.java:7: error: class Contentlet in unnamed module cannot extend a sealed class in a different package
public sealed class Contentlet implements Permissionable permits Host {
                                                                 ^
src/com/dotmarketing/business/Permissionable.java:12: error: class Permissionable in unnamed module cannot extend a sealed class in a different package
public sealed interface Permissionable permits Contentlet, Folder, Identifier, Inode {
                                               ^
... 5 errors
```

One error per permitted subtype. The rule: **a sealed type in the unnamed module requires every
permitted subtype to live in the same package.** dotCMS has no `module-info.java`, so everything is
in the unnamed module.

Running the same attempt against the *real* `Permissionable`, with all 14 implementors in the
`permits` clause, gives **14 errors — one per type, no exceptions**: none of the fourteen lives in
`com.dotmarketing.business`; they are spread across nine packages.

```bash
# for the record, against the real type (needs the dotcms-core classpath)
javac --release 25 -cp dotCMS/target/classes:<deps> Permissionable.java
```

### Why "just move them into one package" is not the answer

Their canonical names are **persisted data**. `permission_reference.permission_type` stores strings
like `com.dotmarketing.portlets.folders.model.Folder`, and those literals appear hardcoded in
`PermissionBitFactoryImpl`'s own SQL (lines 303 and 462). Moving `Folder` to another package is a
data migration on the permissions table, not an import refactor.

## Experiment 2 — the same sources, inside a named module

Add six lines and change nothing else:

```java
module dotcms.permissions {
    exports com.dotmarketing.business;
}
```

```
exit: 0
```

It compiles. Sealed across packages, `Contentlet` sealing down to `Host`, `Inode` reopening its
branch with `non-sealed` — and the resolver carries no `default`:

```java
return switch (permissionable) {
    case Host _       -> "Host";
    case Contentlet _ -> "Contentlet";
    case Folder _     -> "Folder";
    case Identifier _ -> "Identifier";
    case Inode _      -> "Inode";
};
```

(`Host` precedes `Contentlet` because it extends it — the other order is rejected with *"this case
label is dominated by a preceding case label"*.)

## Experiment 3 — add an asset type, touch nothing else

A fifteenth permitted type joins the `permits` clause. The resolver is left exactly as it was:

```
src/com/dotmarketing/business/PermissionResolver.java:25: error: the switch expression does not cover all possible input values
        return switch (permissionable) {
               ^
1 error
```

That is the entire payoff, and it only exists because there is no `default`. **Put a `default` back
and this compiles in silence** — sealing does not give you the check; removing `default` gives you
the check, and sealing is what makes removing it possible. Any total pattern (`case Object o`,
`case Permissionable p`) silences it just the same.

## What this measures

> Sealing this hierarchy is blocked by neither its design nor its size. **It costs a
> `module-info.java`** — and in exchange the compiler names every place that needs updating when an
> asset type is added.

Which reframes the modularisation discussion as a trade with a price tag instead of an abstract
preference. The price is real: `module-info` on a WAR carrying OSGi, Hibernate, reflection and split
packages is a project of its own, and JPMS and OSGi are two module systems competing for the same
job.

## The honest limit, worth knowing before anyone gets excited

Even fully sealed, this particular resolver would keep most of its shape, because **half its
branches do not dispatch on Java types at all.** From the test suite on #36982:

> `test_contentletOfHostContentType_resolvesAsHost` — *resolves as a Host, even though the object is
> not a `Host` instance*

A contentlet "of type Host" is usually a plain `Contentlet` whose content type — a row in the
database — is named `Host`. Hence the two branches for one concept:

```java
case Host _                                                  -> HOST;  // variant is a type
case Contentlet c when isOfContentType(c, HOST_VELOCITY_VAR) -> HOST;  // variant is data
```

Sealed types verify the variants that live in the type system. dotCMS's variants live in the
database. The `when` guard exists precisely because that variability escaped the type system — and it
is why that method's `default` is honesty rather than laziness.
