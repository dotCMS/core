# Search API Migration Guide

This guide is intended for **dotCMS plugin and integration developers** who use the
`ContentletAPI`, `ESSeachAPI`, or the `$ESContent` Velocity tool in their extensions.

The changes described here are part of the ongoing ES → OpenSearch migration. The
deprecated methods listed below **will be removed** when dotCMS completes the cutover
to OpenSearch. Migrate before that happens to avoid compilation failures in your plugins.

---

## 1. `ContentletAPI` — deprecated search methods

### What was deprecated

| Method | Return type | Status |
|--------|-------------|--------|
| `esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles)` | `ESSearchResults` | `@Deprecated(forRemoval = true)` |
| `esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles)` | `SearchResponse` | `@Deprecated(forRemoval = true)` |

Both methods delegate to `ESSeachAPI` directly and return Elasticsearch-specific types
(`ESSearchResults`, `org.elasticsearch.action.search.SearchResponse`). They will be
removed at OS cutover.

### Replacements

| Old method | New method | New return type |
|------------|------------|-----------------|
| `esSearch(...)` | `search(String query, boolean live, User user, boolean respectFrontendRoles)` | `ContentSearchResults<Contentlet>` |
| `esSearchRaw(...)` | `searchRaw(String query, boolean live, User user, boolean respectFrontendRoles)` | `ContentSearchResponse` |

The new methods route through the phase-aware `SearchAPI` router (ES in phases 0–1,
OpenSearch in phases 2–3) and return vendor-neutral DTOs.

### Migration example

```java
// Before
ContentletAPI contentletAPI = APILocator.getContentletAPI();

ESSearchResults results = contentletAPI.esSearch(query, false, user, false);
for (Object obj : results) {
    Contentlet c = (Contentlet) obj;
    // ...
}

SearchResponse raw = contentletAPI.esSearchRaw(query, false, user, false);
SearchHit[] hits = raw.getHits().getHits();

// After
ContentSearchResults<Contentlet> results = contentletAPI.search(query, false, user, false);
for (Contentlet c : results) {
    // no cast needed
}

ContentSearchResponse raw = contentletAPI.searchRaw(query, false, user, false);
List<SearchHit> hits = raw.hits().hits(); // neutral SearchHit DTO
```

---

## 2. `ContentletAPIPreHook` / `ContentletAPIPostHook` — deprecated hook methods

If your OSGi plugin implements either hook interface to intercept search calls, the
following methods are deprecated and will be removed:

### `ContentletAPIPreHook`

| Deprecated | Replacement |
|-----------|-------------|
| `boolean esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles)` | `boolean search(String query, boolean live, User user, boolean respectFrontendRoles)` |
| `boolean esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles)` | `boolean searchRaw(String query, boolean live, User user, boolean respectFrontendRoles)` |

### `ContentletAPIPostHook`

| Deprecated | Replacement |
|-----------|-------------|
| `void esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles)` | `void search(String query, boolean live, User user, boolean respectFrontendRoles)` |
| `void esSearchRaw(String esQuery, boolean live, User user, boolean respectFrontendRoles)` | `void searchRaw(String query, boolean live, User user, boolean respectFrontendRoles)` |

Both replacement methods have default no-op implementations in the interface, so you
only need to override them if you need to intercept those calls.

### Migration example

```java
// Before
public class MyPreHook implements ContentletAPIPreHook {
    @Override
    public boolean esSearch(String esQuery, boolean live, User user, boolean respectFrontendRoles) {
        Logger.info(this, "intercepted esSearch: " + esQuery);
        return true;
    }
}

// After
public class MyPreHook implements ContentletAPIPreHook {
    @Override
    public boolean search(String query, boolean live, User user, boolean respectFrontendRoles) {
        Logger.info(this, "intercepted search: " + query);
        return true;
    }
}
```

---

## 3. Velocity / VTL — `$ESContent` viewtool

The `$ESContent` viewtool (`ESContentTool`) is available in Velocity templates. Two of
its methods changed return types in this release.

### `$ESContent.search(query)`

| | Before | After |
|--|--------|-------|
| Return type | `ESSearchResults` (extends raw `List`) | `ContentSearchResults<ContentMap>` (implements `List<ContentMap>`) |
| Element type | `ContentMap` | `ContentMap` (unchanged) |

**Impact:** Templates that iterate over the result with `#foreach` are unaffected —
the elements are still `ContentMap` objects with the same properties.

```velocity
## This continues to work unchanged
#foreach($content in $ESContent.search($query))
  $content.title
#end
```

Templates that access the result as `ESSearchResults` through a Java helper or cast
will fail at runtime. Replace with `ContentSearchResults`.

### `$ESContent.raw(query)`

| | Before | After |
|--|--------|-------|
| Return type | `org.elasticsearch.action.search.SearchResponse` | `ContentSearchResponse` |

**Impact:** Templates that call `.toString()` on the raw response to obtain ES
wire-format JSON (e.g. to parse it manually) will receive a different string. The new
`ContentSearchResponse.toString()` is a Java object representation, not JSON.

```velocity
## HIGH RISK — if your template does this, it will stop receiving valid JSON
#set($json = $ESContent.raw($query).toString())

## Use the structured accessors instead
#set($raw = $ESContent.raw($query))
#set($hits = $raw.hits().hits())
#foreach($hit in $hits)
  $hit.id()
#end
```

Useful accessors on `ContentSearchResponse`:

| Method | Description |
|--------|-------------|
| `hits()` | Returns `SearchHits` — iterable collection of `SearchHit` |
| `hits().hits()` | `List<SearchHit>` |
| `hits().totalHits().value()` | Total number of matching documents |
| `scrollId()` | Scroll ID for paginated requests, or `null` |
| `tookMillis()` | Query execution time in milliseconds |
| `aggregations()` | `Map<String, List<AggregationBucket>>` — terms aggregations |

---

## 4. Return type change: `ESSearchResults` → `ContentSearchResults<T>`

`ESSearchResults` (package `com.dotcms.content.elasticsearch.business`) is not yet
removed but is no longer returned by the new API methods. If your plugin declares
variables of type `ESSearchResults`, update them to `ContentSearchResults<Contentlet>`.

```java
// Before
ESSearchResults results = (ESSearchResults) contentletAPI.esSearch(query, live, user, roles);

// After
ContentSearchResults<Contentlet> results = contentletAPI.search(query, live, user, roles);
```

The key structural difference:

| | `ESSearchResults` | `ContentSearchResults<T>` |
|--|-------------------|-----------------------------|
| Implements | `List` (raw) | `List<T>` (typed) |
| Element access | Requires `(Contentlet)` cast | Type-safe, no cast needed |
| Response metadata | `getResponse()` → `SearchResponse` (ES) | `getResponse()` → `ContentSearchResponse` (neutral) |
| Total results | `getResponse().getHits().getTotalHits().value` | `getTotalResults()` |
| Scroll ID | `getResponse().getScrollId()` | `getScrollId()` |

---

## 5. Summary of new neutral DTOs

These classes replace the Elasticsearch-specific types in the public API:

| Old type (ES-specific) | New type (neutral) | Package |
|------------------------|--------------------|---------|
| `org.elasticsearch.action.search.SearchResponse` | `ContentSearchResponse` | `com.dotcms.content.index.domain` |
| `com.dotcms.content.elasticsearch.business.ESSearchResults` | `ContentSearchResults<T>` | `com.dotcms.content.index.domain` |
| `org.elasticsearch.search.SearchHits` | `SearchHits` | `com.dotcms.content.index.domain` |
| `org.elasticsearch.search.SearchHit` | `SearchHit` | `com.dotcms.content.index.domain` |
| `org.elasticsearch.search.TotalHits` | `TotalHits` | `com.dotcms.content.index.domain` |

---

## 6. Timeline

| Phase | Action |
|-------|--------|
| Now (this release) | `esSearch` / `esSearchRaw` marked `@Deprecated(forRemoval = true)`. New `search` / `searchRaw` methods available. |
| OpenSearch cutover | `esSearch`, `esSearchRaw`, and ES-specific return types removed. Plugins that have not migrated will **fail to compile**. |

Migrate as soon as possible to get the full migration window.
