# Can the `Permissionable` hierarchy be sealed — all of it?

Groundwork for the Java 25 talk (#34154). **Nothing here is production code and nothing here is
built** — these sources live outside every Maven source root on purpose, because five of the seven
experiments are supposed to fail. The failures are the result.

```bash
./run.sh          # needs only a JDK 22+ — no Maven, no dotCMS classpath, no network
```

Everything quoted below is that script's output. Two compile cleanly, and one of those two is the bad
news (experiment 5).

## Why the question comes up

`PermissionBitFactoryImpl.resolvePermissionType` decides which key an asset inherits permissions
under. [PR #36982](https://github.com/dotCMS/core/pull/36982) turned it from thirteen chained
`instanceof` tests into a pattern `switch` — and it still ends in a `default`. A `default` is a
catch-all: the day someone adds an asset type, the code keeps compiling and the new type quietly takes
the default path.

Sealing is what would let that `default` go away, and with it the silence. A sealed type hands the
compiler the complete list of subtypes, so it can check that a switch covers them all.

Round 1 of this experiment sealed `Permissionable` over four representative subtypes and declared
`Inode` **`non-sealed`** — it stopped at the first level. This round does the whole thing: `Inode`
sealed over its own subtypes, `WebAsset` and `Contentlet` sealed under that, and the complete real
membership instead of a sample. **40 types across 25 packages**, one file each.

## The shape that actually exists

The hierarchy everyone describes from memory — *`Inode` permits `IHTMLPage`, `Container`, `Link`,
`Contentlet`; `Permissionable` permits `Host`, `Contentlet`, `Page`, `Identifier`* — is the conceptual
one. The Java one is a different shape, and sealing is what forces the difference into the open:

| The picture in everyone's head | What the compiler sees |
|---|---|
| `Contentlet` is an `Inode` | it is **not** — `Contentlet` implements `Permissionable` directly, and has no kinship with `Inode` at all |
| `Container` and `Link` are `Inode` subclasses | they are **`WebAsset`** subclasses; `WebAsset` is the `Inode` subclass, and `permits` only accepts *direct* subtypes |
| a page is a top-level asset | `IHTMLPage`'s only implementor is `HTMLPageAsset extends Contentlet` |
| `Host` sits next to `Contentlet` | `Host extends Contentlet` — and so do `FileAsset`, `Persona`, `Event`, `KeyValue`, `VanityUrl` |

So the real membership, verified against the tree:

| Sealed type | Permitted subtypes |
|---|---|
| `Permissionable` | **22** — 14 classes in 9 packages, plus the 8 interfaces that extend it |
| `Inode` | 6 — `WebAsset`, `Structure`, `Field`, `Category`, `FileUpload`, `UserComment` |
| `WebAsset` | 4 — `Container`, `Link`, `Template`, `WorkflowMessage` |
| `Contentlet` | 8 — `Host`, `HTMLPageAsset`, `FileAsset`, `Persona`, `Event`, `DefaultKeyValue`, `KeyValue404`, `DefaultVanityUrl` |
| `ContentType` | 9 abstract subclasses, each with an Immutables-**generated** concrete class under it |

Two details in that first row are where the cost hides.

**Fourteen classes, but only eleven distinct branches.** `Structure` and `WebAsset` already inherit
`Permissionable` through `Inode`, and `Host` through `Contentlet` — all three re-declare
`implements Permissionable` anyway. A sealed interface counts a *declaration*, not an inheritance, so
all three must be named. Delete the redundant `implements` and the clause loses three names.

**The eight sub-interfaces are not optional.** `Treeable`, `Ruleable`, `Categorizable`, `IFileAsset`,
`KeyValue`, `VanityUrl`, `IPersona` and `IHTMLPage` all name `Permissionable` in their `extends`
clause, so all eight are permitted subtypes and each must then be declared `sealed` or `non-sealed`.
Experiment 6 prices that.

## Experiment 1 — seal it where it lives

Compile the sources **without** `module-info.java`, which is the situation dotCMS is in today:

```
src/com/dotmarketing/business/Permissionable.java:44: error: class Permissionable in unnamed module cannot extend a sealed class in a different package
...one error per permitted subtype, in every permits clause — 37 in total
exit: 1
```

The rule: **a sealed type in the unnamed module requires every permitted subtype to live in the same
package.** dotCMS has no `module-info.java`, so everything is in the unnamed module.

37 is not an arbitrary number. The model's `permits` clauses name 42 subtypes in total, and exactly
five of those already sit in the same package as the type that permits them — `Treeable` and `Ruleable`
next to `Permissionable`, `WebAsset` next to `Inode`, and the two `ContentType` subclasses. Every other
one is an error: **37 of 42.**

### Why "just move them into one package" is not the answer

Their canonical names are **persisted data**. `permission_reference.permission_type` stores strings
like `com.dotmarketing.portlets.folders.model.Folder`, and those literals are built into
`PermissionBitFactoryImpl`'s own SQL (`Folder.class.getCanonicalName()` at lines 290 and 295, and again
at 956 and 1854). Moving `Folder` to another package is a data migration on the permissions table, not
an import refactor.

## Experiment 2 — the same sources, inside a named module

Add six lines and change nothing else:

```java
module dotcms.permissions {
    exports com.dotmarketing.business;
    exports com.dotmarketing.beans;
}
```

```
exit: 0
```

It compiles. Sealed two levels deep — `Permissionable` → `Inode` → `WebAsset`, and `Contentlet` sealing
its own eight — and **neither resolver carries a `default`**:

```java
case Host _                                                  -> HOST;
case Contentlet c when HOST.equals(c.contentTypeVariable())  -> HOST;
case HTMLPageAsset _                                         -> ...
case Contentlet _                                            -> ...
case Folder _                                                -> ...
case Identifier _                                            -> ...
case Inode i                                                 -> InodePermissionResolver.resolve(i);
```

Note the guard. A guarded case contributes **nothing** to exhaustiveness — the unguarded
`case Contentlet _` below it is what makes that subtree total.

## Experiment 3 — the permits clause everyone writes from memory

`Inode permits IHTMLPage, Container, Link, Contentlet`, verbatim, and nothing else changed:

```
src/com/dotmarketing/beans/Inode.java:11: error: invalid permits clause
        IHTMLPage, Container, Link, Contentlet {
        ^
  (class IHTMLPage must extend sealed class)
...
src/com/dotmarketing/beans/WebAsset.java:18: error: class is not allowed to extend sealed class: Inode (as it is not listed in its 'permits' clause)
...6 of those in total: one per real Inode subclass the clause left out
exit: 1
```

Ten errors, and every one of them is a fact about the hierarchy: four names that are not `Inode`
subtypes at all, and six that are and were left out. **This is the most useful thing sealing does
here** — long before any exhaustiveness check pays off, writing the `permits` clause is what proves
the mental model wrong.

## Experiment 4 — add an asset type, touch nothing else

`Experiment` joins the `permits` clause. `PermissionResolver` is left exactly as it was:

```
src/com/dotmarketing/business/PermissionResolver.java:52: error: the switch expression does not cover all possible input values
exit: 1
```

That is the payoff, and it only exists because there is no `default`. **Put a `default` back and this
compiles in silence** — sealing does not give you the check; removing `default` gives you the check,
and sealing is what makes removing it possible. Any total pattern (`case Object o`,
`case Permissionable p`) silences it just the same.

## Experiment 5 — the same thing one level down, and the surprise

Add a seventh `Inode` subclass, `Rating`, to `Inode`'s `permits`. Touch neither resolver:

```
exit: 0
```

**Silence.** Which is the opposite of the point, and the most interesting result here.

`Inode` is not abstract — in this model or in dotCMS (`Inode.java:40`, `public class Inode`). A bare
inode row is a real, instantiable `Inode`, so the compiler *demands* a case for the base type itself,
and `case Inode _` swallows every present and future subclass. It is a `default` in all but name.

Make `Inode` abstract, drop that last case, and add `Rating` again:

```
src/com/dotmarketing/business/InodePermissionResolver.java:31: error: the switch expression does not cover all possible input values
exit: 1
```

So the finding is sharper than "seal `Inode`":

> Sealing an **instantiable** class buys exhaustiveness nothing. The permits clause still names the new
> type at the declaration site, but the switch stays quiet. `Inode` has to become abstract for the
> compiler to have anything to say — and that is a behavioural change, not a declaration change.

Worth adding what sealing `Inode` does **not** buy either: the top-level switch covers the entire
`Inode` subtree with one `case Inode i`, sealed or not. The Inode-level seal only ever pays off inside
`InodePermissionResolver` — the switch that dispatches *within* the subtree.

## Experiment 6 — what the eight sub-interfaces cost

Exhaustiveness is recursive: a permitted subtype counts as covered if a `case` matches it, or if it is
*itself* sealed and all of its own permitted subtypes are covered. A `non-sealed` interface is covered
by neither — anything at all can still implement it. So the resolver needs one case per sub-interface,
purely to be total. Delete them, change nothing else:

```
     removed: 8 cases, out of 23
src/com/dotmarketing/business/PermissionResolver.java:52: error: the switch expression does not cover all possible input values
exit: 1
```

**Eight of the switch's 23 cases exist only to satisfy the compiler.** Not one of them can be reached
in practice — a `Treeable` always arrives as some asset matched further up. The alternative is sealing
all eight interfaces too, which cascades into `Treeable`'s 7 implementors, `Ruleable`'s 4 and
`Categorizable`'s 2.

## Experiment 7 — the wall that is not a cost you can choose to pay

Everything above prices sealing as a trade: pay for a `module-info.java`, get compiler-checked
dispatch. This experiment is different, because there is nothing to pay.

`com.dotmarketing.beans`, `com.dotmarketing.business`, `com.dotmarketing.portlets.contentlet.model`,
`com.dotmarketing.portlets.htmlpageasset.model` and `com.dotcms.contenttype.model.type` are all
published to plugin bundles in `dotCMS/src/main/resources/osgi/osgi-extra.conf` (lines 126, 237, 336,
152 and 27). Implementing `Permissionable` from a plugin is a supported thing to do today. So:

```
plugin/com/acme/plugin/AcmeAsset.java:6: error: class is not allowed to extend sealed class: Permissionable (as it is not listed in its 'permits' clause)
exit: 1
```

And it cannot be listed, because **permitted subtypes must live in the same module as the sealed
type**. A plugin is, by definition, not in that module. Compile the same plugin against the API as it
is published today — an ordinary open interface — and then drop the sealed module in its place:

```
     the plugin compiles fine against the open API (exit 0). Now swap in the sealed module:
Exception in thread "main" java.lang.IncompatibleClassChangeError: Failed same module check: subclass
com.acme.plugin.AcmeAsset is in module 'dotcms.plugin' with loader 'app', and sealed class
com.dotmarketing.business.Permissionable is in module 'dotcms.permissions' with loader 'app'
exit: 1
```

The check is enforced twice, at compile time and again at class load. Every existing customer plugin
that implements `Permissionable` fails on startup, with no source change on their side.

> Sealing `Permissionable` is not a refactor with a price tag. It is the removal of a published
> extension point.

## What this measures

| Item | Cost |
|---|---|
| `module-info.java` on a WAR carrying OSGi, Hibernate, reflection and split packages | a project of its own; JPMS and OSGi are two module systems competing for the same job |
| `permits` names to write and keep current | 22 at the root, plus 6 + 4 + 8 in the subtrees |
| sub-interfaces to mark `sealed`/`non-sealed` | 8 — and 8 unreachable switch cases if they stay non-sealed |
| redundant `implements Permissionable` to delete | 3 (`Structure`, `WebAsset`, `Host`) |
| `Inode` made abstract | required for the Inode-level seal to check anything at all |
| plugins implementing `Permissionable` | **broken, permanently** |

The first five are a trade. The last one is a decision about the product.

## The honest limit, worth knowing before anyone gets excited

Even fully sealed, this particular resolver would keep most of its shape, because **half its branches
do not dispatch on Java types at all.** From the test suite on #36982:

> `test_contentletOfHostContentType_resolvesAsHost` — *resolves as a Host, even though the object is
> not a `Host` instance*

A contentlet "of type Host" is usually a plain `Contentlet` whose content type — a row in the
database — is named `Host`. Hence two branches for one concept:

```java
case Host _                                                  -> HOST;  // the variant is a type
case Contentlet c when HOST.equals(c.contentTypeVariable())  -> HOST;  // the variant is data
```

Sealed types verify the variants that live in the type system. dotCMS's variants live in the database.
The `when` guard exists precisely because that variability escaped the type system — and it is why that
method's `default` is honesty rather than laziness.

## What this does not settle

`ContentType` is abstract, has nine abstract subclasses, and every concrete class beneath it
(`ImmutableSimpleContentType` and friends) is **generated by the Immutables annotation processor**. A
`permits` clause would have to name classes that do not exist until that processor has run, in the same
compilation. This model cannot answer whether javac accepts that, because it has no processor —
`ImmutableSimpleContentType` here is hand-written, which is exactly the part that would differ.
Answering it needs a real processor round, and it is worth answering before anyone takes the idea
further than a talk.
