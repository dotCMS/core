# Can the `Permissionable` hierarchy be sealed?

Groundwork for the Java 25 talk (#34154). Two experiments, in this order, because they answer different
halves of the question:

| | What it does | What only it can answer |
|---|---|---|
| **[`real-tree/`](real-tree/)** | declares the **real** `Permissionable`, `Inode` and `ContentType` `sealed`, in place, and runs the real Maven compile | what the actual compiler says, with the real classpath, the real Immutables processor, and every one of dotCMS's own subclasses in scope |
| **[`src/`](src/)** | a 40-type model of the same hierarchy, outside every source root | what the sealed version would *look* like — the real tree cannot get far enough to compile one |

```bash
cd real-tree && ./run-on-real-tree.sh    # ~4 min: patches the real sources, compiles, reverts
./run.sh                                 # ~10s: the model. Needs only a JDK 22+
```

Everything quoted below is those two scripts' output. **No production file is modified by this PR** —
`run-on-real-tree.sh` applies a patch, records what came back, and reverts.

## Why the question comes up

`PermissionBitFactoryImpl.resolvePermissionType` decides which key an asset inherits permissions under.
[PR #36982](https://github.com/dotCMS/core/pull/36982) turned it from thirteen chained `instanceof`
tests into a pattern `switch` — and it still ends in a `default`. A `default` is a catch-all: the day
someone adds an asset type, the code keeps compiling and the new type quietly takes the default path.
Sealing is what would let that `default` go away, and with it the silence.

---

# Part 1 — on the real sources

## A. Seal the real `Permissionable`: one line, 42 errors

`public interface Permissionable` becomes `public sealed interface Permissionable permits …`, naming
all 22 of its declared subtypes. Nothing else in the tree is touched:

```
distinct errors: 42
   20  cannot extend a sealed class in a different package
   22  sealed, non-sealed or final modifiers expected
```

Two separate costs, and the second one is the one nobody predicts:

- **20 × package.** A sealed type in the unnamed module needs every permitted subtype in the *same
  package*. dotCMS has no `module-info.java`, so everything is in the unnamed module. Only `Treeable`
  and `Ruleable` escape — they already live in `com.dotmarketing.business`.
- **22 × modifier.** Every permitted subtype must *itself* be re-declared `final`, `sealed` or
  `non-sealed`. Sealing one interface is a 22-file change before anything else is considered.

`javac` reports each diagnostic twice, once per annotation-processing round — the raw log says 84. The
script deduplicates; 42 is the honest number.

## B. Seal the real `Inode`: the same wall, one level down

`Inode permits WebAsset, Structure, Field, Category, FileUpload, UserComment` — the six real direct
subclasses:

```
distinct errors: 11
    5  cannot extend a sealed class in a different package
    6  sealed, non-sealed or final modifiers expected
```

Five, not six, because `WebAsset` is the one subclass that shares `Inode`'s package.

### B2 — the control, so that "no other subclass exists" is a measurement

Both A and B report **zero** `class is not allowed to extend sealed class`, which is only meaningful if
javac would have said so. Leave `UserComment` out of the clause on purpose:

```
    1  class is not allowed to extend sealed class
com/dotmarketing/portlets/user/model/UserComment.java:[9,7] error: class is not allowed to extend
sealed class: Inode (as it is not listed in its 'permits' clause)
```

It does say so, by name, even with the rest of the clause already in error. So the lists in A and B are
**complete for `dotCMS/src/main/java`** — no forgotten implementor, no anonymous `new Permissionable(){}`.
(`compile` does not reach test sources; those are not covered by this claim.)

## C. `ContentType` — where packages are not the problem, and something else is

`ContentType` is the one part of the hierarchy that looks sealable **today**: it is abstract, and all
nine of its subclasses live in its own package, so the module rule has nothing to bite on. Seal it and
mark the nine `non-sealed`:

```
distinct errors: 2
    2  anonymous classes must not extend sealed classes
```

Both of them are the same pattern, `new ContentType() { … }`:

- `com/dotcms/contenttype/transform/contenttype/StructureTransformer.java:102`
- `com/dotcms/contenttype/transform/contenttype/DbContentTypeTransformer.java:60`

**An anonymous class can never extend a sealed class** — there is no `permits` entry that fixes it,
because there is no name to put there. `ContentType` is two refactors away from being sealable in place:
turn those two anonymous subclasses into named types, and the sealing costs nothing else.

That is the most actionable finding here, and it is invisible from a model of the hierarchy.

## D. Does `permits` work with generated classes? Yes — and you must name the generator's internals

Every concrete `ContentType` is produced by the **Immutables** annotation processor, so a `permits`
clause has to name classes that do not exist until the processor has run, in the same compilation. The
first attempt failed in a way that is worth keeping:

```
target/generated-sources/annotations/…/ImmutableSimpleContentType.java:[1980,15]
error: class is not allowed to extend sealed class: SimpleContentType (as it is not listed in its 'permits' clause)
```

Line 1980 is not `ImmutableSimpleContentType`. It is a *nested* class the generator emits for Jackson:

```java
static final class Json extends SimpleContentType {
```

Name both, and the real build goes green:

```java
public abstract sealed class SimpleContentType extends ContentType
        implements UrlMapable, Serializable, Expireable
        permits ImmutableSimpleContentType, ImmutableSimpleContentType.Json {
```

```
BUILD SUCCESS — it compiles.
exit: 0
```

So: **javac resolves processor-generated classes in a `permits` clause**, and sealing an
`@Value.Immutable` type means writing that generator's package-private internals into your own
declaration — which changes when the generator's version changes.

## What Part 1 establishes

| | |
|---|---|
| Seal `Permissionable` in place | **no** — 20 package errors, and it needs a `module-info.java` first |
| Seal `Inode` in place | **no** — same reason, 5 errors |
| Cost before anything works | 22 sibling declarations for `Permissionable`, 6 for `Inode` |
| Seal `ContentType` in place | **almost** — two anonymous `new ContentType(){}` to refactor, nothing else |
| Seal an Immutables type | **yes, today** — `SimpleContentType` compiles green, at the price of naming `ImmutableSimpleContentType.Json` |
| Implementor lists | verified complete against the real tree, by control |

---

# Part 2 — the model, for what the real tree cannot reach

The real tree stops at the package rule, so it can never show a *working* sealed hierarchy. `src/`
mirrors the real package layout with 40 types — the complete membership, one file each — and asks the
rest of the questions. Five of its seven experiments are meant to fail.

## The shape that actually exists

The hierarchy everyone describes from memory — *`Inode` permits `IHTMLPage`, `Container`, `Link`,
`Contentlet`; `Permissionable` permits `Host`, `Contentlet`, `Page`, `Identifier`* — is the conceptual
one. Experiment 3 compiles it verbatim and gets ten errors, each of which is a fact:

| The picture in everyone's head | What the compiler says |
|---|---|
| `Contentlet` is an `Inode` | `(class Contentlet must extend sealed class)` — it implements `Permissionable` directly, no kinship with `Inode` |
| `Container` / `Link` are `Inode` subclasses | they are **`WebAsset`** subclasses, and `permits` accepts only *direct* subtypes |
| a page is a top-level asset | `IHTMLPage`'s only implementor is `HTMLPageAsset extends Contentlet` |
| `Host` sits next to `Contentlet` | `Host extends Contentlet` — as do `FileAsset`, `Persona`, `Event`, `KeyValue`, `VanityUrl` |

Plus six more, one per real `Inode` subclass the clause left out. **This is the most useful thing
sealing does here**, and it happens before any exhaustiveness check pays off: writing the clause down is
what disproves the picture in your head.

The real membership, for the record:

| Sealed type | Permitted subtypes |
|---|---|
| `Permissionable` | **22** — 14 classes in 9 packages, plus the 8 interfaces that extend it |
| `Inode` | 6 — `WebAsset`, `Structure`, `Field`, `Category`, `FileUpload`, `UserComment` |
| `WebAsset` | 4 — `Container`, `Link`, `Template`, `WorkflowMessage` |
| `Contentlet` | 8 — `Host`, `HTMLPageAsset`, `FileAsset`, `Persona`, `Event`, `DefaultKeyValue`, `KeyValue404`, `DefaultVanityUrl` |
| `ContentType` | 9 abstract subclasses, each with Immutables-generated classes beneath it |

Of the 14 classes, only eleven are distinct branches: `Structure` and `WebAsset` inherit
`Permissionable` through `Inode`, and `Host` through `Contentlet` — all three re-declare
`implements Permissionable` anyway, and a sealed interface counts a *declaration*, not an inheritance.

## Experiments 1 and 2 — the wall, and what is on the other side of it

Compiling the model without `module-info.java` reproduces the real tree's verdict (37 of its 42
permitted references error out). Add six lines:

```java
module dotcms.permissions {
    exports com.dotmarketing.business;
    exports com.dotmarketing.beans;
}
```

```
exit: 0
```

Sealed two levels deep — `Permissionable` → `Inode` → `WebAsset`, and `Contentlet` over its own eight —
and **neither resolver carries a `default`**. Note the guard in the resolver: a guarded case contributes
nothing to exhaustiveness, so the unguarded `case Contentlet _` below it is what makes that subtree
total.

## Experiment 4 — add an asset type, touch nothing else

```
PermissionResolver.java:52: error: the switch expression does not cover all possible input values
```

That is the payoff, and it exists only because there is no `default`. **Put a `default` back and this
compiles in silence** — sealing does not give you the check; removing `default` gives you the check, and
sealing is what makes removing it possible.

## Experiment 5 — sealing an instantiable class buys nothing

Add a seventh `Inode` subclass. Touch neither resolver:

```
exit: 0
```

Silence — the opposite of the point. `Inode` is not abstract, in the model or in dotCMS
(`Inode.java:40`, `public class Inode`): a bare inode row is a real instance, so the compiler *demands*
a case for the base type, and `case Inode _` then swallows every present and future subclass. It is a
`default` in all but name. Make `Inode` abstract, drop that case, add the subclass again:

```
InodePermissionResolver.java:31: error: the switch expression does not cover all possible input values
```

> Sealing an **instantiable** class gives exhaustiveness nothing. `Inode` has to become abstract for the
> compiler to have anything to say — a behavioural change, not a declaration change.

And sealing `Inode` never helps the *top-level* switch either: one `case Inode i` covers the whole
subtree, sealed or not. It pays off only inside the Inode-level switch.

## Experiment 6 — eight of 23 cases exist only to satisfy the compiler

Exhaustiveness is recursive: a permitted subtype is covered if a `case` matches it, or if it is itself
sealed and all of its own subtypes are covered. A `non-sealed` interface is covered by neither. So each
of `Permissionable`'s eight sub-interfaces needs a case, none of them reachable in practice. Delete
them and the switch stops compiling. The alternative is sealing all eight, which cascades into
`Treeable`'s 7 implementors, `Ruleable`'s 4 and `Categorizable`'s 2.

## Experiment 7 — the part that is not a cost you can choose to pay

`com.dotmarketing.beans`, `com.dotmarketing.business`, `com.dotmarketing.portlets.contentlet.model`,
`…htmlpageasset.model` and `com.dotcms.contenttype.model.type` are all published to plugin bundles in
`dotCMS/src/main/resources/osgi/osgi-extra.conf` (lines 126, 237, 336, 152, 27). Implementing
`Permissionable` from a plugin is a supported thing to do today. Sealed, it is not — and it cannot be
opted in, because **permitted subtypes must live in the same module**. Compile a plugin against the API
as published, then drop the sealed module in its place:

```
java.lang.IncompatibleClassChangeError: Failed same module check: subclass com.acme.plugin.AcmeAsset
is in module 'dotcms.plugin' with loader 'app', and sealed class
com.dotmarketing.business.Permissionable is in module 'dotcms.permissions' with loader 'app'
```

Enforced at compile time and again at class load. Every existing plugin that implements
`Permissionable` fails on startup, with no source change on their side.

> Sealing `Permissionable` is not a refactor with a price tag. It is the removal of a published
> extension point.

---

# The ledger

| Item | Cost |
|---|---|
| `module-info.java` on a WAR carrying OSGi, Hibernate, reflection and split packages | a project of its own; JPMS and OSGi are two module systems competing for the same job |
| sibling declarations before anything compiles | 22 for `Permissionable`, 6 for `Inode` (measured on the real tree) |
| `permits` names to write and keep current | 22 at the root, plus 6 + 4 + 8 in the subtrees |
| unreachable switch cases, if the 8 sub-interfaces stay `non-sealed` | 8 of 23 |
| redundant `implements Permissionable` to delete | 3 — `Structure`, `WebAsset`, `Host` |
| anonymous subclasses to convert into named types | 2 for `ContentType`; unknown for the rest, because the compile never gets that far |
| `Inode` made abstract | required for the Inode-level seal to check anything at all |
| plugins implementing `Permissionable` | **broken, permanently** |

The first seven are a trade. The last one is a product decision.

# The honest limit, worth knowing before anyone gets excited

Even fully sealed, this resolver keeps most of its shape, because **half its branches do not dispatch on
Java types at all.** From the test suite on #36982:

> `test_contentletOfHostContentType_resolvesAsHost` — *resolves as a Host, even though the object is
> not a `Host` instance*

A contentlet "of type Host" is usually a plain `Contentlet` whose content type — a row in the database
— is named `Host`. Hence two branches for one concept:

```java
case Host _                                                  -> HOST;  // the variant is a type
case Contentlet c when HOST.equals(c.contentTypeVariable())  -> HOST;  // the variant is data
```

Sealed types verify the variants that live in the type system. dotCMS's variants live in the database.
The `when` guard exists precisely because that variability escaped the type system — and it is why that
method's `default` is honesty rather than laziness.
