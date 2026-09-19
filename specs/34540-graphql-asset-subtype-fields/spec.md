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
  predictably rather than failing the whole query.
- A field pointing at an asset in a language the request did not ask for must follow the same
  language-fallback behavior the rest of content delivery uses.
- A customer asset type whose property name collides with one of the general asset properties must
  resolve to a single, documented meaning — a client must never silently receive one when it
  asked for the other.
- A customer who deletes a property that a live client query still asks for must get a clear
  error naming the missing property.
- An asset type the requesting user has no permission to read must not leak its property names or
  values.

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
- **FR-009a**: No property name may **silently** change what it returns. This requirement survives
  the decision to break: where a value differs from what a name returns today, that name MUST be
  removed rather than repurposed, so the client receives an explicit failure instead of different
  data. The worked example is the asset description — removed from the asset-pointing field rather
  than left in place answering with a new value.
- **FR-010**: The delivered behavior MUST be covered by an automated API-level test that a
  customer-defined property on an extended asset type is readable through both an Image field and
  a File field.
- **FR-011**: Reading N properties of one referenced asset MUST NOT cost N times the work of
  reading one; per-asset work MUST be performed once per asset per request.
- **FR-012**: *(Superseded — see "Decision: the flat view is replaced, not kept alongside" below.)*
  ~~Existing customer queries MUST keep working unchanged.~~ The flat asset view is **replaced**.
  Two selections stop working, and both MUST fail **visibly** rather than return a different value:
  - `description` on an asset-pointing field. It is the one property whose meaning differs between
    the flat view (the contentlet title) and the content answering it (a stored description), so it
    cannot be carried over without changing what a live query returns.
  - An asset-pointing field aimed at content that is not an asset now resolves to nothing rather
    than to the flat view.

  Every other property the flat view exposed — `fileName`, `fileAsset`, `metaData`, `showOnMenu`,
  `sortOrder` — MUST remain selectable **and** return exactly what it returns today, on every
  surface a client can narrow through.
- **FR-012a**: *(No longer applicable.)* There is no surviving surface to mark as superseded. What
  replaces it is documentation and release communication, not an in-schema deprecation.
- **FR-012b**: *(No longer applicable.)* Retirement is not deferred; it happens in this feature.
  The tracking item this requirement called for is therefore not opened.
- **FR-012c**: The break MUST be announced ahead of the release and MUST ship with migration
  guidance naming, for each removed selection, its replacement — `description` through a narrowing
  clause on the concrete content type, where it returns the stored value rather than the title.
- **FR-013**: This feature supersedes PR dotCMS/core#35363. That PR MUST be closed as superseded
  rather than merged, and the convenience it aimed at — reading an asset's binary properties
  without descending a level — MUST be re-raised as a second, separately tracked stage of issue
  #34540, delivered after this one. Issue #34540 MUST remain open after this feature ships,
  carrying that remaining scope, so it is not lost.
- **FR-014**: The existing automated check that locks the current shape of an asset-pointing field
  MUST be corrected as part of this work. It currently asserts that the asset view exposes *no
  property other than* the six it has today, while being named as though it asserts those six are
  present. It must assert what it claims.
- **FR-015**: The properties shared by every asset MUST be selectable **both** directly on an
  asset-pointing field and inside a clause that narrows to a specific type, without the client
  having to repeat itself or choose one place over the other.
- **FR-016**: A clause that narrows to a type the returned asset does not happen to be MUST NOT
  fail the request. It contributes nothing and the rest of the response is delivered normally.
  The response MUST additionally carry a non-fatal warning naming the clause that matched nothing,
  so a client can tell "this asset wasn't that type" apart from "I named the wrong type".
- **FR-016a**: This MUST hold across a whole result set, not just a single asset. When one request
  returns many assets of differing types, every asset that matches a clause MUST be returned with
  those properties populated, every asset that does not MUST still be returned without them, and
  the request as a whole MUST succeed. A single non-matching asset MUST NOT suppress the matching
  ones. Warnings MUST identify which clause matched nothing rather than being a single opaque
  flag on the response.
- **FR-017**: A clause that narrows to a type that does not exist at all MUST fail the request.
  This is a client mistake with no valid reading, and failing loudly is correct.
- **FR-018**: When more than one clause applies to the same returned asset — one narrowing to its
  base kind and another to its specific type — their properties MUST merge into a single result
  object, with no precedence rule needed and no duplication.
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
- **SC-007**: The AI tagging workflow, which cannot read an asset's tags today, completes
  end-to-end.
- **SC-008**: Of the six properties the flat asset view exposed, **five** are still selectable
  after this feature ships and return the same values they returned before. The sixth,
  `description`, fails explicitly rather than returning a different value.
- **SC-009**: Every removed selection fails visibly. Zero selections keep working while returning
  different data — measured by querying each removed name and confirming an error rather than a
  value.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: GraphQL content delivery — specifically how a field that points
  at an asset is described to clients. This is an actively used delivery surface, not a dormant
  legacy corner: customers run production front-ends against it today. The asset base types
  themselves are long-standing product surface.

- **Backward-compatibility expectations**: Customers already query the six properties the flat
  asset view exposes. **Five keep working and return the same values; one does not** (FR-012), and
  an asset field aimed at non-asset content stops resolving. Both breaks are visible.

  This spec changed position twice. It first accepted breaking, then reversed to a non-breaking
  design after consulting the accepted architecture decisions, and has now returned to breaking —
  **as a product decision, not a technical one**. See "Decision: the flat view is replaced" and
  ADR Alignment below.

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

**An exception to ADR-0022 IS requested, knowingly.** This feature does not expand, adopt, bake and
retire — it retires now. The expand phase was designed, implemented and verified green (a companion
field beside each asset field, breaking nothing), and was then set aside because it could not put
the narrowing clauses in the same block as the flat properties. That ergonomic difference — one
block instead of two — was judged by product to be worth the break.

The technical constraint is not negotiable and is worth recording, because it is what makes the
compliant option unable to deliver the requested shape: a GraphQL field has exactly one type and a
resolved value has exactly one runtime type, so the flat view and the asset itself — two
descriptions of the same content — cannot occupy the same position. Keeping both means keeping them
at different positions, which is precisely what the expand-phase design did.

What the exception preserves from the ADR's intent:

- The break is **visible**, never silent. ADR-0022's concern is a self-hosted customer who upgrades
  on their own schedule and cannot tell that data changed; every removal here fails loudly
  (FR-009a).
- Five of the six properties are carried over unchanged, so the blast radius is one property plus
  one edge case, not the whole surface.
- FR-012c requires the announcement and migration guidance the ADR's process would otherwise have
  provided through the bake window.

**Sign-off needed**: @fmontes as an author of ADR-0020, and @nollymar who approved the spec in its
non-breaking form (PR #37537). Neither has agreed to this exception yet — it is recorded here as
requested, not granted.

### Decision: the flat view is replaced, not kept alongside

**This is a product decision, and it overrides what the rest of this section originally argued.**
Recorded in full because the reasoning ran both ways and a later reader will otherwise assume the
compliant option was never available.

**What was built and set aside.** A non-breaking design was implemented and verified green: a
companion field beside each asset field (`image` gaining `imageContent`), typed by the asset
interface. Nothing broke, the spec needed no change, and no ADR exception was required. It was set
aside for one reason — the narrowing clauses lived in a second block rather than beside the flat
properties:

```
imageContent { ... on Images { tags } }     # what the compliant option offered
image        { fileName  ... on Images { tags } }   # what was asked for
```

**Why the compliant option could not deliver the requested shape.** A GraphQL field has exactly one
type, and a resolved value has exactly one runtime type. The flat view and the asset itself are two
descriptions of the same content, so they cannot occupy the same position — keeping both means
keeping them at different positions. That is a property of GraphQL, not of this implementation, and
no amount of work removes it.

**What the break actually costs**, measured rather than estimated:

- **Five of the six properties survive unchanged** — `fileName`, `fileAsset`, `metaData`,
  `showOnMenu`, `sortOrder`. They are synthesized onto DOTASSET-derived types using the very same
  fetchers the flat view used, so they answer identically. Notably `fileName`, which was never a
  stored value for that content.
- **`description` does not.** It is the one property whose meaning differs: the flat view answered
  with the contentlet title, while the content answering it stores something else. On a real
  instance the flat view returned a value for 57 of 57 images while only 2 of those 57 have a
  stored description. Carrying the name over would have returned different data **without
  failing** — so it is removed instead, and fails loudly.
- **An asset field aimed at non-asset content** now resolves to nothing rather than to the flat
  view. Forced: a contentlet outside the interface cannot be handed on, and doing so fails the
  entire request with `UnresolvedTypeException` rather than just that field.

**What is preserved from the earlier position.** FR-009a survives intact and is the reason the two
breaks take the shape they do: nothing may change value silently. A removal a client can see is
acceptable; a name that keeps working and returns something else is not.

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

- **Its benefit does not survive this feature.** #35363 exists to spare the client one level of
  nesting: `image { size }` instead of `image { fileAsset { size } }`. But once an asset-pointing
  field returns the asset's real type, that type's binary property is already a fully described
  binary carrying all twelve of those properties plus a focal point — verified against a running
  instance. The client still descends exactly one level, just to the correctly named property. The
  saving the PR offers is gone.
- **Its deliverable lands on a view that is now on a retirement path.** The twelve properties
  would be attached to the flat asset view, which this feature removes — so the work would be
  spent extending a surface that no longer exists and would have to be redone on the interface.
- **It is not in a mergeable state.** Its automated checks have been failing since it was last
  updated, on a check in its own area, tripped by its own change and left unaddressed. It carries
  no review. See FR-014: the check in question is itself wrong and must be corrected regardless.
- **What to keep from it**: its recognition of the problem, and the corrected version of the check
  it trips.

**The remaining scope is not dropped.** Issue #34540 stays open after this feature ships, carrying
the second stage: exposing an asset's binary properties conveniently on the new per-type shape.
That is deliberate sequencing, not an oversight — this feature must land first because the second
stage's design depends on the shape this one establishes.

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
- Limb (b) of issue #34540 — conveniently exposing the general asset/binary properties — is out
  of scope for this stage. It is **not** tracked through PR #35363, which is superseded (FR-013);
  it stays on issue #34540, which remains open after this feature ships.
- "Single request" in SC-002 means one GraphQL query from the client's perspective; it makes no
  claim about server-side work.
- The five surviving flat properties keep their exact present behaviour, including the synthesized
  `fileName` that derives rather than stores its value. Correcting it would be a silent change to a
  live contract, which FR-009a forbids.
