# Phase 1 Data Model: Contentlet map key precedence (#37584)

This fix introduces no entity, no table and no index. What it *does* change is a precedence
rule inside an existing structure — the flat `Map<String, Object>` that a Contentlet carries
and that the transform pipeline hands to the REST layer. That rule is the model worth writing
down, because the defect is a direct consequence of it being unwritten.

## The structure

`Contentlet.getMap()` is a single flat namespace holding three kinds of entry:

| Kind | Origin | Example keys |
|---|---|---|
| **Stored field values** | One per field variable on the Content Type, loaded from `contentlet_as_json` / the columnar store | `hostName` (Host), `aliases`, `title`, `description` |
| **System properties** | Intrinsic to every Contentlet, persisted on the row | `identifier`, `inode`, `host`, `folder`, `languageId`, `modDate` |
| **Derived properties** | Computed by transform strategies at read time; never persisted | `hostName` (parent Site name), `url`, `urlMap`, `shortyId`, `titleImage`, `hasTitleImage`, `modUserName`, `ownerUserName` |

One namespace, three sources, no namespacing convention. Collisions are therefore possible by
construction.

## How collisions are normally prevented

`FieldFactoryImpl.RESERVED_FIELD_VARS`
(`dotCMS/src/main/java/com/dotcms/contenttype/business/FieldFactoryImpl.java:60`) rejects a
field variable that collides with a system or derived key — compared **lowercased**, via
`isFieldVariableValid`. It covers `host`, `identifier`, `inode`, `languageId`, `modDate`,
`modUserName`, `ownerUserName`, `creationDate`, `publishUser`, `publishUserName` and ~24 more.

It does **not** cover `hostName`. Nor `url`, `urlMap`, `shortyId`, `titleImage` or
`hasTitleImage`.

## The rule this fix establishes

> When a derived property's key collides with a field variable the Content Type actually
> declares, **the stored field value wins** and the derived property is not written.

Precedence, highest first:

1. Stored field value, when the Content Type declares a field with that variable
2. Derived property
3. `NOT_APPLICABLE` sentinel, when the derivation yields nothing

This is not a new invention. `DefaultTransformStrategy.addCommonProperties` already applies it
to `url` eleven lines below the defect:

```java
//We only calculate the fields if it is not already set
//However WebAssets (Pages, FileAssets) are forced to calculate it.
if (!map.containsKey(URL_FIELD)) {
```

The fix applies the same rule to `hostName`, keyed on field declaration rather than map
presence (research R2 explains why the keying differs).

## Affected keys

| Key | Constant | Reserved? | Declared as a field by | Behavior before | Behavior after |
|---|---|---|---|---|---|
| `hostName` | `Contentlet.HOST_NAME` | **No** | Host CT ("Site Key", required TextField); any custom CT may | Always overwritten with the parent Site's name → `System Host` for every Site | Stored value preserved when the CT declares the field; derived value written otherwise |
| `host` | `Contentlet.HOST_KEY` | Yes | Nothing — the factory forbids it | Derived value written | **Unchanged.** No guard needed; the reserved list makes a collision impossible |

No other key in `addCommonProperties` changes behavior.

## Entity touched at read time

**Host** (`com.dotmarketing.beans.Host`, Content Type variable `Host`) — the only shipped
Content Type that declares a colliding field.

| Field variable | Label | Type | Note |
|---|---|---|---|
| `hostName` | Site Key | TextField, required | The field this fix repairs. `Host.getHostname()` reads this same map key (`Host.java:102`), so the corruption reached any code wrapping a hydrated Contentlet in a `Host` — not only the JSON |
| `aliases` | Aliases | TextArea | |
| `isDefault` / `isSystemHost` | — | Hidden | |
| `tagStorage` | Tag Storage | Custom | Renders correctly today; unaffected |
| `hostThumbnail` | Host Thumbnail | Binary | |

Invariants, unchanged by this fix:

- Every Host has `host = "SYSTEM_HOST"` and `folder = "SYSTEM_FOLDER"`. This is *why* the
  derived value was always the constant `System Host`.
- `Host.getMap()` aliases lowercase `hostname` to `hostName`, but is `@JsonIgnore`, so the
  modern JSON path never runs it. **Out of scope — not touched.**
- A Host never has a `workflow_task` row, because workflow actions are prohibited on the
  Content Type. This is defect B's trigger, not its cause.

## State transitions

None. Both changes are on the read/transform path; nothing is written, so there is no state
machine and no migration.
