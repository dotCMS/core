# Feature Specification: GraphQL Access to Inherited-Subtype Properties on Image and File Fields

**Feature Branch**: `34540-graphql-asset-subtype-fields`

**Created**: 2026-09-14

**Status**: Draft

**Type**: New Feature

**Issue**: dotCMS/core#34540 — "GraphQL Support for dotAssets and FileAssets in Image/File Fields"

**Input**: User description: "A client querying an Image or File field over GraphQL must be able to select the properties of the content type the field actually points at — including fields the customer defined on their own content types that extend the DOTASSET or FILEASSET base types — and must be able to tell which type it received."

## Overview

dotCMS lets a customer model their own kinds of assets by extending the two asset base types: a
banner image that also carries a campaign name and an ad size, a PDF that also carries a category
and a download count. The authoring side of that works today — the customer defines the fields,
editors fill them in, the values are stored and versioned.

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
- **FR-009a**: No property name may silently change what it returns. Where this feature changes
  the meaning of an existing name, that name MUST be changed too, so the client receives an
  explicit failure rather than a different value. A break a client can see is required; a break a
  client cannot see is not acceptable.
- **FR-010**: The delivered behavior MUST be covered by an automated API-level test that a
  customer-defined property on an extended asset type is readable through both an Image field and
  a File field.
- **FR-011**: Reading N properties of one referenced asset MUST NOT cost N times the work of
  reading one; per-asset work MUST be performed once per asset per request.
- **FR-012**: Existing customer queries MAY break. This feature is not required to keep the
  current asset view working. The break MUST be announced ahead of the release and MUST ship with
  migration guidance naming, for each property available today, its replacement or the fact that
  it has none. See "Decision: breaking change accepted" below.
- **FR-013**: This feature supersedes PR dotCMS/core#35363. That PR MUST be closed as superseded
  rather than merged, and the convenience it aimed at — reading an asset's binary properties
  without descending a level — MUST be re-raised as a second, separately tracked stage of issue
  #34540, delivered after this one. Issue #34540 MUST remain open after this feature ships,
  carrying that remaining scope, so it is not lost.
- **FR-015**: The properties shared by every asset MUST be selectable **both** directly on an
  asset-pointing field and inside a clause that narrows to a specific type, without the client
  having to repeat itself or choose one place over the other.
- **FR-016**: A clause that narrows to a type the returned asset does not happen to be MUST NOT
  fail the request. It contributes nothing and the rest of the response is delivered normally.
  The response MUST additionally carry a non-fatal warning naming the clause that matched nothing,
  so a client can tell "this asset wasn't that type" apart from "I named the wrong type".
- **FR-017**: A clause that narrows to a type that does not exist at all MUST fail the request.
  This is a client mistake with no valid reading, and failing loudly is correct.
- **FR-018**: When more than one clause applies to the same returned asset — one narrowing to its
  base kind and another to its specific type — their properties MUST merge into a single result
  object, with no precedence rule needed and no duplication.
- **FR-014**: The existing automated check that locks the current shape of an asset-pointing field
  MUST be corrected as part of this work. It currently asserts that the asset view exposes *no
  property other than* the six it has today, while being named as though it asserts those six are
  present. It must assert what it claims.

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

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: GraphQL content delivery — specifically how a field that points
  at an asset is described to clients. This is an actively used delivery surface, not a dormant
  legacy corner: customers run production front-ends against it today. The asset base types
  themselves are long-standing product surface.

- **Backward-compatibility expectations**: Customers already query the six properties the current
  asset view exposes. Changing how an asset-pointing field is described invalidates those queries
  on deploy. **This is accepted** (FR-012): the current view is a facade that reports asset
  content using property names borrowed from the other base type, and preserving it would mean
  preserving the misdescription permanently. The break ships with announcement and migration
  guidance, and FR-009a requires every break to be visible.

### What breaks, and whether the client can see it

Measured against a running instance, for the six properties the current view exposes:

- **Five are invented for image-style assets** — `fileName`, `fileAsset`, `metaData`,
  `showOnMenu`, `sortOrder` do not exist on that content at all; the current view synthesizes
  them. Removing the facade removes them. A client asking for one gets an outright request
  failure, which is loud and immediately visible.
- **One is real but currently masked** — `description`. The current view does not return the
  asset's stored description; it returns the asset's title, which for an image is the file name.
  The asset's own description is unreachable. After the change the same name returns the stored
  value instead. Measured on a real instance: the current view returns a value for 57 of 57
  images, while only 2 of those 57 have a stored description — so 55 of 57 would go from a
  populated string to empty, **with no error of any kind**. This is the one case a client cannot
  detect, and it is why FR-009a requires renaming rather than silently repurposing it.
- **File-style assets fare better**: their properties are real and merely move from the shared
  view to the specific type, so a client recovers all of them by naming the type.

- **Known related decisions**: Issue dotCMS/core#34540 asks for two things: (a) customer-defined
  properties on extended asset types must be queryable, and (b) the ~20 general asset properties
  reachable today only through the untyped `_map` escape hatch must be properly described. **This
  spec covers (a).** Limb (b) is addressed by in-flight work described below. The plan phase will
  formally consult `dotCMS/platform-adrs`.

### Relationship to in-flight work

Pull request dotCMS/core#35363 is open and unmerged, and states that it closes #34540. It delivers
limb (b): a shared description of the binary properties an asset carries, so a client can ask for
things like size, MIME type and dimensions directly instead of descending a level. It does **not**
deliver limb (a) — customer-defined properties remain unreachable after it.

That PR is an **input to be reviewed, not a settled foundation.** The plan phase must evaluate it
on its merits before deciding how this work relates to it, and specifically must reach a position
on each of the following, all of which are visible in the PR as it stands:

1. **Scope versus claim.** It closes #34540 while delivering only limb (b). Decide whether it
   should close the issue at all.
2. **An overloaded property name.** It introduces `title` and `modDate` on the asset-pointing
   view meaning the *binary file's* title and date. Everywhere else in the delivery API those
   names mean the *content item's* title and date. A customer asking an Image field for `title`
   would get the file's. This ambiguity originates in that PR, independently of this feature, and
   is what FR-009 exists to resolve.
3. **How the returned type is chosen.** It selects which description to apply by inspecting the
   runtime shape of the value being resolved. Assess how robust that is; it is also the exact
   decision point this feature must change in order to satisfy FR-003.
4. **Cost per property.** Its resolution of each binary property re-derives the asset's binary
   metadata from scratch, with no reuse across the properties of the same asset in the same
   request. Selecting all twelve therefore repeats that derivation twelve times per asset, per row
   of a result set. Quantify it against FR-011 and decide whether reuse is required.
5. **A hardcoded assumption.** It assumes the binary-carrying property of an image-style asset is
   always named `asset`. Confirm that holds for every customer-extended image-style type.
6. **Unfinished verification.** Its own test plan leaves two items unchecked: a manual end-to-end
   check through the GraphQL console, and API-level test coverage for these queries. Treat both as
   outstanding.

**Structural tension**: that PR attaches the twelve binary properties to the single fixed asset
view. This feature replaces that fixed view with per-type descriptions, which is how FR-001 and
FR-003 get satisfied — so those twelve properties do not come along, and the queries #35363
enables would stop working unless carried over.

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
- **Its deliverable would have to be rebuilt anyway.** The twelve properties are attached to the
  view this feature removes.
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
