# Phase 1 Data Model: Opt-In Second-Level Relationship Filter

**Feature**: `20261001-094124-filter-second-level-relationships` | **Spec**: [spec.md](./spec.md)

No database, ES/OpenSearch, or persisted-content schema changes. This feature only adds fields to
existing **in-memory, request-scoped** domain objects and one new **static YAML config file**. The
"entities" below are the structures the implementation introduces or extends.

## 1. PP Filter definition (YAML-backed, existing type: `FilterDescriptor`)

| Field | Type | Notes |
|---|---|---|
| `relationshipsSecondLevel` | `boolean` | **New.** Optional filter key, defaults to `false` when absent from a filter's YAML (so every existing filter file is unaffected without modification). When `true`, enables the second-level traversal behavior described below. |

- **Validation**: `FilterDescriptor.validate()` gains `relationshipsSecondLevel` to its
  `listOfPossibleFilters` allow-list and a `Boolean`-cast check, mirroring the existing
  `dependencies`/`relationships`/`forcePush` checks (an unrecognized key or non-boolean value
  fails YAML validation at load time, same as today).
- **State transitions**: None — a filter descriptor is loaded once per YAML file at startup/reload
  and is otherwise immutable for the lifetime of a bundle generation.

## 2. Resolved filter (existing type: `PublisherFilter` / `PublisherFilterImpl`)

| Field | Type | Notes |
|---|---|---|
| `relationshipsSecondLevel` | `boolean` | **New.** Carried from the `FilterDescriptor` into the resolved `PublisherFilter` used during bundle generation. Exposed via a new `isRelationshipsSecondLevel()` method, added as a `default` method on the `PublisherFilter` interface (returns `false`) so existing out-of-repo implementers remain binary-compatible. |

- **Relationships**: One `PublisherFilter` is constructed per bundle (`PublisherAPIImpl.createPublisherFilter`), reading the new key off the bundle's `FilterDescriptor` the same way `dependencies`/`relationships` are read today.

## 3. Relationship-hop tracking (new, transient, in-memory only)

A new field on `PushPublishigDependencyProcesor`, scoped to a single bundle-generation run:

| Field | Type | Notes |
|---|---|---|
| `relationshipHopConsumed` | `Set<String>` (`ConcurrentHashMap.newKeySet()`) | **New.** Holds the identifiers of Contentlets that have already been recursed into once via a relationship (i.e., have "consumed" the one extra hop this filter allows). Not persisted; discarded with the processor instance at the end of the bundle run. |

- **Lifecycle**: Starts empty for every new `PushPublishigDependencyProcesor` (i.e., every bundle
  generation). Populated only when `publisherFilter.isRelationshipsSecondLevel()` is `true` and a
  Contentlet not already in the set is found to have related content — that related content's
  identifier is added to the set *before* it is enqueued for its own dependency processing.
- **Invariant enforced**: a Contentlet identifier, once in the set, causes any of *its* related
  Contentlets to be added to the bundle without being enqueued for further relationship
  processing — this is the mechanism that caps traversal at exactly one extra level and makes
  circular relationship chains inherently non-recursive beyond that point (see research.md R1).
- **Concurrency**: Reads/writes happen from the dependency processor's worker threads
  (`ConcurrentDependencyProcessor`'s thread pool); a `ConcurrentHashMap`-backed set is sufficient
  since the only operations are `contains`/`add`, both already picked for similar use elsewhere in
  this class (`assetsRequestToProcess` in `ConcurrentDependencyProcessor` uses the same pattern).

## 4. New static config artifact

`dotCMS/src/main/webapp/WEB-INF/publishing-filters/SecondLevelRelationships.yml` — a new PP Filter
descriptor, structurally identical to `Intelligent.yml` ("Everything and Dependencies") except
`relationshipsSecondLevel: true` and `default: false`. Auto-discovered by the existing PP filter
loading mechanism (`PublisherAPIImpl`) the same way every other filter file is; no new loader code.
