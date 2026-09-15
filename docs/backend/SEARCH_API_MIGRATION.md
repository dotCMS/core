# Search API Migration Guide

This guide is for two audiences:

- **Plugin and integration developers** who call `ContentletAPI` or `ESSeachAPI` from Java —
  sections 1, 2, 4 and 5.
- **Template authors** who use the `$estool` Velocity tool — section 3, which is self-contained
  and written entirely in Velocity rather than Java.

The changes described here are part of the ongoing ES → OpenSearch migration. The
deprecated methods listed below **will be removed** when dotCMS completes the cutover
to OpenSearch. Migrate before that happens: Java code will fail to compile, and templates
will fail silently.

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
List<SearchHit> hits = raw.hits().getHits(); // neutral SearchHit DTO
```

Note the accessor names on `SearchHits`: its record components are declared `getHits` and
`getTotalHits` (so that Velocity can resolve them as bean properties), which means the Java
calls are `hits().getHits()` and `hits().getTotalHits()` — not `hits().hits()`.

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

## 3. Velocity / VTL — `$estool` viewtool

The viewtool backed by `ESContentTool` is registered under the key **`estool`**
(`toolbox.xml`), so templates reach it as `$estool`. Two of its methods changed return
types in this release.

> **Velocity is not Java.** Velocity resolves `$a.b` through JavaBean getters, so the
> Java call `response.hits().getHits()` is written `$response.hits.hits` in a template —
> no parentheses, and the getter name is what has to exist. The record accessors used in
> the Java examples earlier in this guide (`hits()`, `tookMillis()`) are **not** reachable
> from VTL; the bean aliases are. Every VTL snippet below is written the way it must
> appear in a template.

### `$estool.search(query)`

| | Before | After |
|--|--------|-------|
| Return type | `ESSearchResults` (extends raw `List`) | `ContentSearchResults<ContentMap>` (implements `List<ContentMap>`) |
| Element type | `ContentMap` | `ContentMap` (unchanged) |

**Impact:** Templates that iterate over the result with `#foreach` are unaffected —
the elements are still `ContentMap` objects with the same properties.

```velocity
## This continues to work unchanged
#foreach($content in $estool.search($query))
  $content.title
#end
```

Templates that access the result as `ESSearchResults` through a Java helper or cast
will fail at runtime. Replace with `ContentSearchResults`.

### `$estool.raw(query)`

| | Before | After |
|--|--------|-------|
| Return type | `org.elasticsearch.action.search.SearchResponse` | `ContentSearchResponse` |

**Impact:** Templates that call `.toString()` on the raw response to obtain ES
wire-format JSON (e.g. to parse it manually) will receive a different string. The new
`ContentSearchResponse.toString()` is a Java object representation, not JSON — and
nothing errors, so the page renders and whatever consumed that string silently receives
garbage. See [JSON output](#json-output-replacing-tostring) below for the replacement.

```velocity
## HIGH RISK — if your template does this, it will stop receiving valid JSON
#set($json = $estool.raw($query).toString())

## Use the structured accessors instead
#set($raw = $estool.raw($query))
#foreach($hit in $raw.hits.hits)
  $hit.id
#end
```

Accessors on `ContentSearchResponse`, in both dialects:

| In a template (VTL) | From Java | Description |
|---------------------|-----------|-------------|
| `$raw.hits` | `hits()` | `SearchHits` — iterable collection of `SearchHit` |
| `$raw.hits.hits` | `hits().getHits()` | `List<SearchHit>` |
| `$raw.hits.totalHits.value` | `hits().getTotalHits().value()` | Total number of matching documents |
| `$raw.scrollId` | `scrollId()` | Scroll ID for paginated requests, or `null` |
| `$raw.tookInMillis` | `tookMillis()` | Query execution time in milliseconds |
| `$raw.aggregations` | `getAggregations()` | `Map<String, Aggregation>` — the **full** aggregation tree |
| — | `aggregations()` | `Map<String, List<AggregationBucket>>` — first-level **terms only**; nested aggregations and `top_hits` are dropped |

On each `SearchHit`: `$hit.id`, `$hit.index`, `$hit.score`, `$hit.sourceAsMap`,
`$hit.sortValues`.

> **Use `$raw.aggregations`, not the flattened `aggregations()` view.** The flattened map
> keeps only first-level terms aggregations and silently discards nested ones and
> `top_hits`. From VTL, `$raw.aggregations` resolves to `getAggregations()` and returns the
> whole tree; each value exposes `.buckets`, and each bucket exposes `.key`,
> `.keyAsString`, `.keyAsNumber`, `.docCount` and its own nested `.aggregations`.

> **Aggregation names come back lowercased.** Both `search` and `raw` fold the whole query
> to lower case before running it (`StringUtils.lowercaseStringExceptMatchingTokens`, the
> same call the deprecated methods made), so an aggregation declared as `"tagAgg"` is keyed
> `tagagg` in the response. Looking it up by the name you wrote returns `null`, and a
> `#foreach` over `null` renders nothing rather than failing.

<a id="json-output-replacing-tostring"></a>

### JSON output — what replaces `.toString()`

`.toString()` no longer produces JSON, but you have not lost JSON output. What is gone is
**Elasticsearch's own wire format**; that distinction decides whether you can swap one call
or have to restructure.

**If the consumer just needs JSON** — a `<script>` block, a fetch endpoint, anything you
control — use `$json.generate(...)`, the `JSONTool` viewtool registered under the key
`json`:

```velocity
#set($raw = $estool.raw($query))
<script>
  var data = $json.generate($raw);
</script>
```

`JSONTool.generate(Object)` builds the JSON reflectively from the object's bean getters.
This path is deliberately supported: `ContentSearchResponse.getAggregations()` is
intentionally **not** annotated `@JsonIgnore` so the reflection-based JSON builder keeps
seeing it, and templates doing `$json.generate($response).aggregations…` keep working
(issue #36435).

For a smaller payload, generate only what the consumer needs:

```velocity
#set($raw = $estool.raw($query))
#set($out = {"total": $raw.hits.totalHits.value, "items": []})
#foreach($hit in $raw.hits.hits)
  #set($ignore = $out.items.add($hit.sourceAsMap))
#end
<script>var data = $json.generate($out);</script>
```

**If the consumer needs the Elasticsearch wire format specifically** — a JavaScript library
that parses ES responses, a published contract, anything expecting `hits.hits[]._source`
and the rest of the ES envelope — there is no replacement, and there will not be one. The
neutral response has different keys by design, since the point of the migration is that the
engine's shape no longer leaks into the API. Those templates have to be restructured around
the accessors above.

The same applies server-side: `/api/es/raw` returns the neutral shape, not the ES envelope.

| What you had | What to use now |
|--------------|-----------------|
| `$estool.raw($q).toString()` consumed as JSON | `$json.generate($estool.raw($q))` — neutral shape |
| `$estool.raw($q).toString()` parsed as an ES response | No equivalent. Restructure around `$raw.hits.hits` / `$raw.aggregations` |
| A JS library that parses ES JSON | Build the payload the library needs explicitly, as above |

---

## 4. Return type change: `ESSearchResults` → `ContentSearchResults<T>`

`ESSearchResults` (package `com.dotcms.content.elasticsearch.business`) is not yet
removed but is no longer returned by the new API methods. If your plugin declares
variables of type `ESSearchResults`, update them to `ContentSearchResults<Contentlet>`.

```java
// Before
ESSearchResults results = contentletAPI.esSearch(query, live, user, roles);

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
