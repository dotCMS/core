# Feature Specification: GraphQL Access to Inherited-Subtype Properties on Image and File Fields

**Feature Branch**: `34540-graphql-asset-subtype-fields`

**Created**: 2026-09-14

**Status**: Draft

**Type**: New Feature

**Issue**: dotCMS/core#34540 — "GraphQL Support for dotAssets and FileAssets in Image/File Fields"

**Input**: User description: "A client querying an Image or File field over GraphQL must be able to select the properties of the content type the field actually points at — including fields the customer defined on their own content types that extend the DOTASSET or FILEASSET base types — and must be able to tell which type it received."

## Overview

dotCMS lets a customer model their own kinds of assets by extending either of the two asset base
types. **Any** content type built that way is in scope here — there is no privileged set. The
authoring side works today: the customer defines the fields, editors fill them in, the values are
stored and versioned.

*(Throughout this document, a banner image carrying a campaign name and a PDF carrying a category
appear as illustrations only. Nothing in the requirements is specific to them; wherever an example
is named, read it as "any content type extending that base type".)*

The reading side does not. When another piece of content points at one of those assets through an
Image or a File field, a GraphQL client can only ever see a fixed, six-property view of it. The
customer's own fields are not offered by the schema at all, so they cannot be requested. Neither
can the asset's own identity — a client cannot ask an Image field for the asset's identifier or
its site. And because every asset comes back described the same way, a client cannot tell whether
it received an image-style asset or a file-style one.

For customers who have adopted GraphQL as their data-consumption API — and GraphQL is a
first-class delivery API in dotCMS, not a side channel — this makes a whole category of their own
content unreadable through the API they standardized on. There is no workaround for the custom
fields.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Read the customer's own asset properties (Priority: P1)

A customer has defined their own image-style content type carrying a campaign name and an ad size,
and their own file-style content type carrying a category and a download count. Content elsewhere
in the site points at those assets through an Image field and a File field. The customer's
front-end asks for a page of that content and expects to receive the campaign name alongside the
image, and the category alongside the document, in the same request.

**Why this priority**: This is the capability that does not exist in any form today and has no
workaround. It is what blocks the AI tagging workflow: the tags an asset carries cannot be read
back. Everything else in this spec is an improvement to something partially possible; this is the
difference between the customer's data being reachable and unreachable.

**Independent Test**: Define an asset type with a custom property, create an asset of it, point an
Image field at that asset, and request the custom property through GraphQL. Delivering only this
story already unblocks the tagging workflow and custom asset metadata.

**Acceptance Scenarios**:

1. **Given** a customer-defined image-style asset type with a campaign-name property, and content
   whose Image field points at an asset of that type, **When** a client requests the campaign name
   through that Image field, **Then** the stored campaign name is returned.
2. **Given** a customer-defined file-style asset type with a category property, and content whose
   File field points at a document of that type, **When** a client requests the category through
   that File field, **Then** the stored category is returned.
3. **Given** an asset that carries tags, **When** a client requests the tags through an Image
   field pointing at it, **Then** the tags are returned.
4. **Given** a customer adds a new property to an existing asset type, **When** a client requests
   that property through an Image or File field without any administrative action in between,
   **Then** the new property is available and returns its stored value.
5. **Given** *any* content type extending either asset base type — not only the two used as
   illustrations above — and any property the customer defined on it, **When** a client requests
   that property through a field pointing at content of that type, **Then** the stored value is
   returned. This scenario is the general case; scenarios 1-2 are instances of it and passing
   them alone does not satisfy it.
6. **Given** an Image field pointing at file-style content, or a File field pointing at
   image-style content, **When** a client narrows to the actual type of what is referenced,
   **Then** that type is offered and its properties are returned.

---

### User Story 2 - Know which kind of asset came back (Priority: P2)

A customer's front-end renders a mixed feed in which the same field may point at an image-style
asset or a file-style one, and each needs different treatment. The client needs the response to
say which kind of asset it received so it can branch on that, rather than guessing from which
properties happen to be populated.

**Why this priority**: Without it, a client that supports more than one asset type has to infer
the type from the shape of the data, which is fragile and breaks as soon as a customer adds a
type. It matters, but a customer with a single asset type per field is unblocked by Story 1 alone.

**Independent Test**: Point one Image field at an image-style asset and another at a file-style
asset, request both in one query, and confirm the response names a different, accurate type for
each.

**Acceptance Scenarios**:

1. **Given** content whose Image field points at an image-style asset and whose File field points
   at a file-style asset, **When** a client requests type information for both in one query,
   **Then** each reports the name of the specific content type it is, and the two differ.
2. **Given** an asset of a customer-defined type, **When** a client requests type information,
   **Then** the reported name is the customer's own type, not a generic asset label.

---

### User Story 3 - Reach the referenced asset's own identity (Priority: P3)

A customer's front-end needs to build a link to an asset, cache it by identity, or check whether
it is published — so it needs the asset's identifier, its site, its URL mapping and its published
state, requested through the same field that points at it.

**Why this priority**: Real value, and it removes a class of follow-up requests, but a client can
work around it today by issuing a second query against the asset's own content type. Story 1 has
no such escape.

**Independent Test**: Request the asset's identifier and published state through an Image field
and confirm they match the asset's own record.

**Acceptance Scenarios**:

1. **Given** content whose Image field points at a published asset, **When** a client requests the
   asset's identifier and published state through that field, **Then** both are returned and match
   the asset's own record.
2. **Given** an asset that lives on a specific site, **When** a client requests the site through
   the pointing field, **Then** the correct site is reported.

---

### Edge Cases

- An Image or File field that is empty must return an explicit empty result, not an error.
- A client narrowing to a type the asset is not must receive the rest of its data plus a warning,
  never a failed request (FR-016) — whereas naming a type that does not exist must fail (FR-017).
- A client narrowing to both the base kind and the specific type in one request must receive one
  merged object, not two partial ones (FR-018).
- A field that points at an asset that has since been archived or deleted must degrade
  predictably rather than failing the whole query (FR-019).
- A field pointing at an asset in a language the request did not ask for must follow the same
  language-fallback behavior the rest of content delivery uses (FR-019).
- A customer asset type whose property name collides with one of the general asset properties must
  resolve to a single, documented meaning — a client must never silently receive one when it
  asked for the other (FR-021).
- A customer who deletes a property that a live client query still asks for must get a clear
  error naming the missing property (FR-020).
- An asset type the requesting user has no permission to read must not leak its property names or
  values (FR-022).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A client MUST be able to request, through an Image or File field, any property the
  customer defined on the content type of the asset that field points at.
- **FR-001a**: Every content type extending either asset base type MUST be reachable through an
  asset-pointing field — the whole open-ended set, including types a customer creates later. No
  subset may be privileged, and the capability MUST NOT be satisfiable by enumerating known types.
- **FR-001b**: An asset-pointing field MUST be able to return, and a client MUST be able to narrow
  to, content of **either** base type regardless of which kind of field is doing the pointing.
  Verified against a running instance: an Image field accepts and resolves a reference to
  file-style content (a plain-text file was returned through one), so a client narrowing on that
  field MUST be offered file-style types as well as image-style ones. Typing the field by the
  field's own kind would silently drop content the field can already hold today.
- **FR-002**: A client MUST be able to request the tags carried by an asset through the Image or
  File field that points at it.
- **FR-003**: The response MUST identify the specific content type of the asset that was returned,
  distinguishably for two different asset types.
- **FR-004**: A client MUST be able to request the referenced asset's own identity and state —
  its identifier, its internal version reference, its site, its URL mapping, its published state
  and its title — through the field that points at it.
- **FR-005**: Properties a customer adds to, or removes from, an asset type MUST become available
  or unavailable to clients without any administrative step, restart, or manual registration.
- **FR-006**: The API MUST remain self-documenting: every property a client can request MUST be
  discoverable by inspecting the API's own published description of itself, with no reliance on an
  untyped escape hatch.
- **FR-007**: An empty Image or File field MUST return an explicit empty result rather than an
  error, and MUST NOT fail the rest of the query.
- **FR-008**: Asset properties MUST be subject to the same read-permission rules as the asset
  itself; a user who cannot read an asset MUST NOT obtain its property values through a pointing
  field.
- **FR-009**: Where a property name could refer either to the referenced asset or to the binary
  file it carries, the API MUST give that name exactly one documented meaning, and MUST offer an
  unambiguous way to reach the other.
- **FR-009a**: No property name may **silently** change what it returns. Where a name would
  otherwise answer differently, it MUST be made to answer as it does today; only where that is
  impossible may it be removed instead, so the client gets an explicit failure rather than
  different data. The worked example is the asset description: two meanings have always shared
  that name — an asset-pointing field answered with the contentlet title, the content type's own
  field holds what an editor typed — and **both are preserved**, selected by how the asset was
  reached. Nothing is removed and no client rewrites a query.
- **FR-010**: The delivered behavior MUST be covered by an automated API-level test that a
  customer-defined property on an extended asset type is readable through both an Image field and
  a File field.
- **FR-011**: Reading N properties of one referenced asset MUST NOT cost N times the work of
  reading one; per-asset work MUST be performed once per asset per request.
- **FR-012**: Existing customer queries MUST keep working unchanged. **All six** properties the
  flat asset view exposed — `fileName`, `description`, `fileAsset`, `metaData`, `showOnMenu`,
  `sortOrder` — MUST remain selectable **and** return exactly what they return today, on every
  surface a client can narrow through. No selection that validates today may stop validating.

  Two consequences of the change are not additive and MUST be documented rather than prevented:
  - **`__typename`** on an asset-pointing field answered with the constant `DotFileasset` and now
    answers with the concrete content type. This is inseparable from the capability: the same
    resolution that makes `... on BannerImages` work is what `__typename` reports. Suppressing it
    would mean reporting a type the value does not have.
  - An asset-pointing field aimed at content that is **not** an asset resolves to nothing rather
    than to the flat view. Forced, not chosen — see the decision section below.
- **FR-012a**: *(No longer applicable.)* Nothing is superseded, so there is no surface to mark as
  such. What ships instead is documentation and release communication.
- **FR-012b**: *(No longer applicable.)* Nothing is retired, so nothing is deferred. The tracking
  item this requirement called for is therefore not opened.
- **FR-012c**: The non-additive consequences MUST be announced ahead of the release and MUST
  ship with migration guidance: what `__typename` returns now and who depends on it (notably
  normalized client caches keyed on it); that a type-kind change from object to interface
  requires clients generating types from the schema to regenerate; and that an asset-pointing
  field aimed at content that is not an asset now answers `null` where it used to answer the flat
  view — the one case where a query keeps validating while returning different data (SC-009).
- **FR-013**: This feature supersedes PR dotCMS/core#35363, which MUST be closed as superseded
  rather than merged. The convenience it aimed at — reading an asset's binary properties without
  descending a level — MUST be **delivered here rather than deferred**: one PR, both limbs of
  issue #34540, so the issue closes with this work.
- **FR-013a**: The binary's properties MUST be selectable directly on an asset-pointing field,
  without descending into the binary. Any whose name already means something else on a contentlet
  MUST be excluded rather than redefined, and MUST stay reachable through the binary itself. The
  two that collide are the file's title and its modification date: on a contentlet those names
  already mean the contentlet's own, and the modification date is not even the same type — a date
  on the content, a numeric timestamp on the file — so one name could not describe both.
- **FR-013b**: Deriving the binary's properties MUST cost the same whether one or all of them are
  requested. #35363 derived them once per property, so selecting ten ran the full transformer ten
  times per asset, per row of a result set.
- **FR-014**: The existing automated check that locks the current shape of an asset-pointing field
  MUST be corrected as part of this work. It currently asserts that the asset view exposes *no
  property other than* the six it has today, while being named as though it asserts those six are
  present. It must assert what it claims.
- **FR-015**: The properties shared by every asset MUST be selectable **both** directly on an
  asset-pointing field and inside a clause that narrows to a specific type, without the client
  having to repeat itself or choose one place over the other.
- **FR-016**: A clause that narrows to a type the returned asset does not happen to be MUST NOT
  fail the request. It contributes nothing and the rest of the response is delivered normally —
  this is standard GraphQL behavior and stays so.

  On top of that, the response MUST carry a non-fatal warning for a clause that matched **nothing
  in the whole response**, so a client can tell "no asset here was that type" apart from "I named
  the wrong type". Standard GraphQL has no such warning; it is a dotCMS addition and is therefore
  confined to the place the GraphQL response format reserves for implementation-specific data:

  - **Where**: `extensions.warnings`, an array. `data` and `errors` are never touched, so a client
    that ignores `extensions` sees exactly a standard response.
  - **Shape**: one entry per unmatched clause, `{ "path": "BannerCollection.image",
    "typeCondition": "BannerImages", "message": "No content at this path was of type
    BannerImages." }`. The path is the field path the clause was written under, not a row index.
  - **Deduplicated per request, not per row**: a clause is reported only if no content anywhere in
    the response was of that type. In a mixed collection where some rows match, it produces **no**
    warning; a clause is never reported once per non-matching row.
  - **Scope**: inline fragments (`... on X { }`) only. A named fragment spread is not followed, so
    a warning is never attributed to a path the client did not write.
  - **Not switchable**: there is no setting to turn it off. It is absent from any response whose
    clauses all matched or that has no clauses, costs nothing in the latter case, and is inert for
    a client that does not read `extensions` — so an off-switch would buy nothing.
- **FR-016a**: This MUST hold across a whole result set, not just a single asset. When one request
  returns many assets of differing types, every asset that matches a clause MUST be returned with
  those properties populated, every asset that does not MUST still be returned without them, and
  the request as a whole MUST succeed. A single non-matching asset MUST NOT suppress the matching
  ones. Warnings MUST identify which clause matched nothing rather than being a single opaque
  flag on the response, and follow the shape and deduplication rule in FR-016.
- **FR-017**: A clause that narrows to a type that does not exist at all MUST fail the request.
  This is a client mistake with no valid reading, and failing loudly is correct.
- **FR-018**: When more than one clause applies to the same returned asset — one narrowing to its
  base kind and another to its specific type — their properties MUST merge into a single result
  object, with no precedence rule needed and no duplication.
- **FR-019**: *Which* asset a field resolves to MUST be decided exactly as it is today — same
  live/working selection, same language fallback, same outcome for an archived or deleted target —
  because the lookup that resolves the reference is unchanged; this feature changes only what can
  be selected on the result. A target that cannot be resolved (deleted, archived and not visible
  to the request, or not readable by the caller) MUST answer `null` for that field and MUST NOT
  fail the rest of the query.
- **FR-020**: Selecting a property that no longer exists on the type — because the customer
  deleted it — MUST fail the request with the standard GraphQL validation error, which names both
  the property and the type (`Field 'campaignName' in type 'BannerImages' is undefined`). It MUST
  NOT quietly answer `null`, which would be indistinguishable from an empty value.
- **FR-021**: When a customer asset type defines a property whose name is also one of the general
  asset properties the asset field offers directly (the flat view's and the binary's), the
  **customer's property wins on that name**. It existed before this feature and changing what it
  returns would be the silent change FR-009a forbids. The general meaning stays reachable,
  unambiguously, through the binary (`asset { }` / `fileAsset { }`). `description` is the one
  exception: FR-009a already fixes its meaning by how the asset was reached.

  A collision MUST NOT be able to invalidate the schema. A GraphQL interface and every type
  implementing it must agree on each shared property's type, so a customer `size` stored as text
  against the general `size` as a number would otherwise reject the **whole** schema and take every
  GraphQL query on the instance down with it. Therefore:
  - A general property whose name any asset type on the instance defines with an incompatible type
    is left off the asset field's shared description for that instance. Selecting it directly on
    the asset field then fails validation with an error naming it — loud, never different data —
    while the customer's own property keeps working through its type and the general value stays
    reachable through the binary.
  - Creating a **new** asset-type property whose name and type would collide incompatibly MUST be
    refused when the property is saved, as dotCMS already refuses properties that collide with the
    general content properties. The build-time rule above exists for data that predates this
    feature, which cannot be refused retroactively.
- **FR-022**: Permission-restricted content MUST NOT leak through this feature:
  - Property values, and the concrete type reported by `__typename`, are only ever returned for an
    asset the caller can read (FR-008); an unreadable asset answers `null` (FR-019).
  - Warnings (FR-016) name only type conditions the client itself wrote. They never carry asset
    content or the type of content the caller could not read.
  - Which content types and property names the schema describes is unchanged: the schema is
    built once for all callers, not per user, and already describes every content type today.
    This feature adds interface membership to those existing types, not new visibility.
### Key Entities

- **Asset base types**: the two built-in kinds of asset content in dotCMS — one image-oriented,
  one file-oriented. Customers extend these to define their own asset types.
- **Customer-defined asset type**: a content type a customer created by extending one of the asset
  base types, carrying both the inherited asset properties and the customer's own.
- **Asset-pointing field**: a field on some other content type (Image or File) whose value refers
  to one asset contentlet.
- **Published API description**: the machine-readable description of the delivery API that clients
  and tooling inspect to learn what can be requested.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the properties a customer defines on an extended asset type are readable
  through an Image or File field that points at content of that type.
- **SC-002**: A customer can retrieve an asset together with its custom properties in a **single**
  request, where today the custom properties are unobtainable at any number of requests.
- **SC-003**: A client can correctly determine which of two different asset types it received in
  100% of responses, without inspecting which properties are populated.
- **SC-004**: A property added to an asset type is readable by a client within the same session,
  with zero administrative actions performed in between.
- **SC-005**: Every property a client can request appears in the API's published self-description
  — no property is reachable only through an untyped escape hatch.
- **SC-006**: Requesting all available properties of a referenced asset performs the same amount
  of per-asset work as requesting one, measured as a constant rather than a per-property cost.
- **SC-006a**: The binary's properties are selectable directly on an asset-pointing field, with
  no intermediate level, on both DOTASSET- and FILEASSET-derived content. The two whose names
  already mean something else on a contentlet are the only ones absent, and both are still
  reachable through the binary.
- **SC-007**: The AI tagging workflow, which cannot read an asset's tags today, completes
  end-to-end.
- **SC-008**: **All six** properties the flat asset view exposed are still selectable after this
  feature ships and return the same values they returned before, measured by executing a query
  against each on both a DOTASSET-derived and a FILEASSET-derived asset. `description` is included
  and returns the title through an asset-pointing field, the stored value when queried directly —
  the same two answers it gave before.
- **SC-009**: Zero selections keep working while returning different data, with exactly two
  exceptions, both announced under FR-012c rather than prevented:
  - `__typename` reports the resolved content type rather than a constant, because holding it
    fixed would mean naming a type the value does not have.
  - An asset-pointing field aimed at content that is **not** an asset answers `null` instead of the
    flat view. It cannot keep the flat view — handing a non-asset to the asset interface fails the
    whole request — and a warning was considered and rejected: the flat view of a non-asset had no
    binary, so what it returned was already meaningless, and the misconfiguration is in stored
    data, which the client developer reading the warning cannot fix.
- **SC-010**: Each edge case has a measured outcome: an archived, deleted or unreadable target
  answers `null` without failing the query (FR-019); a deleted property fails validation with an
  error naming it (FR-020); an asset type carrying a property that collides with a general asset
  property — with the same type or a different one — leaves the schema valid and every other query
  working (FR-021); and no response to a user without read permission on an asset contains
  that asset's property values or concrete type name (FR-022).

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: GraphQL content delivery — specifically how a field that points
  at an asset is described to clients. This is an actively used delivery surface, not a dormant
  legacy corner: customers run production front-ends against it today. The asset base types
  themselves are long-standing product surface.

- **Backward-compatibility expectations**: Customers already query the six properties the flat
  asset view exposes. **All six keep working and return the same values** (FR-012). No query has to
  be rewritten.

  This spec changed position three times, and the record is kept because the reasoning is the
  useful part. It first accepted breaking, then reversed to a non-breaking design after consulting
  the accepted architecture decisions, then returned to breaking as a product decision — and has
  now arrived at a design that delivers the requested query shape **without** the break, once
  `description` was made to answer according to how the asset was reached. What remains is the
  `__typename` change, which no design can avoid. See "Decision: both meanings of `description`
  are kept" and ADR Alignment below.

### ADR Alignment

Two accepted decisions in `dotCMS/platform-adrs` govern how a published contract may change, and
both were consulted before this spec was finalized:

- **ADR-0020** (accepted) deprecated a core REST endpoint and kept it **functional**, annotated for
  removal, with existing integrations explicitly unaffected.
- **ADR-0022** (accepted) states the principle directly — *"a URL, parameter, or response field is
  a promise to callers in exactly the same way a database column is a promise to queries"* — and
  names why it bites here: dotCMS is self-hosted and upgraded by customers on their own schedule,
  so *"'nobody is calling the old endpoint right now' is not the same question as 'it's safe to
  remove.'"* It prescribes **expand → adopt → bake → retire**, gates removal on both a
  supported-version floor and a confirmed zero-use window, and requires the deprecation to be
  marked **in the schema itself**, not only in code.

**No exception to ADR-0022 is required.** An earlier revision of this spec requested one,
knowingly, on the grounds that the asset description had to be retired rather than carried through
expand → adopt → bake → retire. That is no longer the case: nothing is retired. Every name the flat
view published still resolves and still returns the same value, so there is no removal for the
ADR's process to govern.

The constraint that forced the earlier position is worth recording, because it is real and it is
what a later reader will expect to block this: a GraphQL field has exactly one type, and a resolved
value has exactly one runtime type, so the flat view and the asset itself — two descriptions of the
same content — cannot occupy the same position. What dissolved the conflict was noticing that only
**one** name actually collided, `description`, and that its two meanings were *already* both
shipping, selected by query path. Preserving that path-dependence costs one data fetcher and keeps
every client query valid. See "Decision: both meanings of `description` are kept".

What remains is not a field removal and is not what ADR-0022 governs:

- **`__typename`** changes value. It is a GraphQL meta-field, not a published dotCMS field, and its
  rule is unchanged — it has always reported the runtime type. What changed is that the runtime
  type is no longer a constant. It cannot be held fixed without reporting a type the value does not
  have, which would break the narrowing clauses this feature exists to deliver.
- **The kind of `DotFileasset`** changes from object to interface. Query text is unaffected;
  clients that generate types from the schema must regenerate. This, rather than `__typename`, is
  the item a supported-version floor speaks to.
- **An asset field aimed at non-asset content** answers `null` instead of the flat view. This one
  *is* a selection that keeps validating while returning different data, and is recorded as the
  explicit exception in SC-009 rather than argued away.

All three are covered by FR-012c's announcement and migration guidance.

**Sign-off**: @fmontes and @nollymar were asked to weigh an ADR-0022 exception against the earlier
breaking design. That request is withdrawn — there is no longer an exception to grant. Their review
is still wanted on the three items above.

### Decision: both meanings of `description` are kept

Recorded in full because the reasoning ran three ways, and a later reader will otherwise assume
either that the compliant option was never available or that the break was unavoidable.

**The requested shape.** Narrowing clauses had to sit in the *same* block as the flat properties:

```
imageContent { ... on Images { tags } }             # the first, non-breaking design
image        { fileName  ... on Images { tags } }   # what was asked for
```

Delivering the second means the asset field itself is the polymorphic position, typed by an
interface. A GraphQL field has exactly one type and a resolved value has exactly one runtime type,
so the flat view could not also live there — which is why this was believed to cost a break.

**Why it did not.** Of the six flat properties, five are plain values that the same fetchers answer
identically once synthesized onto the concrete types. Only `description` collided, because the flat
view derived it from the contentlet title while an asset content type's own `description` field
holds what an editor typed.

The collision turned out to be already shipping. Measured on a running instance, `image
{ description }` returned a value for 57 of 57 images, while only 2 of those 57 have a stored
description — the flat view was answering with the title, and the content type's own field was
answering with the stored value, under the same name, in the same schema. The two meanings were
never reconciled; they were separated by which query reached the content.

So the resolution is to conserve that separation rather than pick a winner: `description` is
resolved by a fetcher that answers according to whether the asset was reached through an
asset-pointing field or queried directly. Both contracts hold, and no client rewrites a query. Two
implementation details make it work and both fail silently if got wrong, so they are locked by
tests:

- The concrete type's own `description` definition must be **replaced**, not filled in around.
  Most asset types define one, and leaving it in place lets the stored value answer an
  asset-pointing field — exactly the silent change FR-009a forbids.
- The declared type must be read from the parent's **field definition**, not from its execution
  step. graphql-java rewrites the step to the concrete type before running the sub-selection, so
  by the time the fetcher runs the interface is already gone and every asset looks
  directly-queried.

**What still changes**, measured rather than estimated:

- **All six properties survive unchanged** — `fileName`, `description`, `fileAsset`, `metaData`,
  `showOnMenu`, `sortOrder`. Notably `fileName`, which was never a stored value for DOTASSET
  content, and `description`, whose path-dependence is preserved exactly.
- **`__typename`** answered with the constant `DotFileasset` and now answers with the concrete
  content type. Inseparable from the capability: the resolution that makes `... on BannerImages`
  work is what `__typename` reports. The clients this reaches are those keying a normalized cache
  on it, or asserting it in snapshots.
- **An asset field aimed at non-asset content** now resolves to nothing rather than to the flat
  view. Forced: a contentlet outside the interface cannot be handed on, and handing it on raises
  `UnresolvedTypeException`, which fails the **entire request** — one mis-pointed field taking
  every other collection in the query down with it. Returning nothing for that field is the
  containable outcome, and the data was already wrong.

**What is preserved from the earlier position.** FR-009a survives intact and is what drove the
design to this shape: nothing may change value silently. The earlier spec concluded that the only
way to honour it for `description` was removal; the path-aware resolver honours it without one.

### Depth of the type hierarchy

Verified by introspection against a running instance: the hierarchy an asset-pointing field
exposes is exactly **two levels deep, always** — the asset's base kind, then its specific content
type. It cannot grow deeper, because a dotCMS content type may only extend one of the fixed base
types; it can never extend another content type. The base kind is a plain shared description that
itself sits under nothing, and a specific type names its shared descriptions side by side, not
nested.

So a client never faces a chain of narrowing clauses. It faces at most one clause for the base
kind and one for the specific type, and per FR-018 those merge. A deeper case does not arise and
the design need not account for one.

### Decision: this feature supersedes PR #35363

Recorded as FR-013. The reasoning, verified rather than assumed:

- **Its goal is kept; its implementation could not be.** #35363 exists to spare the client one
  level of nesting — `image { size }` instead of `image { fileAsset { size } }` — and that is
  delivered here (FR-013a). What could not carry over is where it hung the properties: on the flat
  asset object, which is no longer the type an asset-pointing field resolves to. The same
  properties had to be declared on the interface and synthesized onto every concrete asset type
  instead, or they would be selectable through one clause and not another.
- **Two of its twelve could not come along.** It flattened the file's `title` and modification
  date, and both names already mean the contentlet's own at that level — the modification date
  not even as the same type. Carrying them would have made one name answer differently depending
  on where it was read, which is the exact failure FR-009a forbids. Both stay reachable through
  the binary.
- **Its per-property cost could not come along either.** It derived the binary once per property
  resolved, so selecting all twelve ran the full transformer twelve times per asset, per row.
  Here the derivation is cached per asset per request (FR-013b, SC-006).
- **It is not in a mergeable state.** Its automated checks have been failing since it was last
  updated, on a check in its own area, tripped by its own change and left unaddressed. It carries
  no review. See FR-014: the check in question is itself wrong and must be corrected regardless.
- **What to keep from it**: its recognition of the problem, and the corrected version of the check
  it trips.

**The remaining scope is absorbed, not deferred.** An earlier revision of this spec sequenced it
as a second stage on a still-open issue. That was reversed by a product decision: one PR delivers
both limbs, so issue #34540 closes with this work. The convenience #35363 aimed at ships here —
ten of the binary's twelve properties are selectable directly on the asset field (FR-013a),
derived once per asset per request rather than once per property (FR-013b).

What genuinely stays out is the file's own `title` and modification date. Not sequencing — those
two names are already taken at the asset level by the contentlet's own, and for the modification
date the types differ besides. Both remain reachable through the binary. If exposing them under
different names is ever wanted, that is a new question, not leftover scope from this one.

## Assumptions

- The permission model for reading an asset through a pointing field is unchanged: it follows the
  asset's own read permissions (FR-008), not those of the content that points at it.
- Language and version resolution for the referenced asset keeps the behavior clients see today;
  this feature widens *which properties* are readable, not *which asset version* is selected.
- Write operations are untouched. This is a read/delivery capability only.
- The dynamic registration of customer content types into the delivery API already works and is
  not part of this scope — verified against a running instance, where a customer-defined asset
  type created moments earlier was already described by the API, carrying its own properties,
  with no administrative action. FR-005 therefore records an existing guarantee that must be
  preserved, not new work.
- Limb (b) of issue #34540 — conveniently exposing the general asset/binary properties — is **in**
  scope and delivered here (FR-013a), not tracked through PR #35363, which is superseded. The
  issue closes with this feature rather than staying open for a second stage.
- "Single request" in SC-002 means one GraphQL query from the client's perspective; it makes no
  claim about server-side work.
- The six surviving flat properties keep their exact present behaviour, including the synthesized
  `fileName` that derives rather than stores its value. Correcting it would be a silent change to a
  live contract, which FR-009a forbids.
