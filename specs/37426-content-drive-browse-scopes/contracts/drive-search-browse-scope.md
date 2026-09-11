# Contract delta: `browseScope` on `POST /api/v1/drive/search`

**Feature**: [#37426](https://github.com/dotCMS/core/issues/37426) · **Spec**: [spec.md](../spec.md) · **Date**: 2026-09-11

This endpoint has two callers: Content Drive and the Asset Picker (`DotContentDriveService.search`). Both are in this repo, so the change is coordinated rather than published, but the Asset Picker is owned elsewhere and must not have to change. Everything below is written from that constraint.

---

## The addition

One optional field on the request body.

```jsonc
{
  "assetPath": "//demo.dotcms.com/",
  "browseScope": "ALL" | "ROOT" | "SYSTEM_HOST"   // optional
  // …every other field unchanged
}
```

There is no change to the response body, to any other request field, or to any status code other than the new 400 described below.

---

## What each value means

| `browseScope` | `assetPath` | Returns |
|---|---|---|
| *(omitted)* | `//site/` | Everything on the site, at any depth, System Host included when `includeSystemHost` is true. **Exactly today's behavior.** |
| *(omitted)* | `//site/folder/` | That folder's contents. **Exactly today's behavior.** |
| `ALL` | `//site/` | The same as omitting it at the site root. The explicit spelling. |
| `ROOT` | `//site/` | Only what sits at the site root. System Host is never included. |
| `SYSTEM_HOST` | `//site/` | System Host content only. The site portion of `assetPath` is context, not a filter, and is not read. |
| any value | `//site/folder/` | **400.** See below. |

---

## The compatibility guarantee

**A request that does not carry `browseScope` returns exactly what it returns today, byte for byte.** This is the contract's load-bearing promise, and it is what lets the Asset Picker stay untouched. Two consequences worth stating so they are not traded away later:

The field is **not** defaulted to `ALL` in a way that changes behavior. `ALL` is meaningful only at the site root, and an omitted scope inside a folder is not the same as `ALL` inside a folder. Defaulting the field so that it reads more tidily in the schema would silently turn the Asset Picker's folder requests recursive.

`includeSystemHost` keeps its current meaning and default. It is read when the scope is `ALL` or omitted, and ignored otherwise, because the other two scopes already answer the System Host question.

---

## The refusal

An explicit `browseScope` with a path that is not the site root is rejected with **400**, naming both values.

The three scopes are things you can only be in at the root: the whole site, the root itself, and System Host. A folder is addressed by its path. There is therefore no combination of a folder path and a scope that means anything, and rather than resolve one by precedence the request is refused, following `BulkUploadForm.isExactlyOneTargetGiven`: "Both is refused rather than resolved by precedence. A caller that sends a folder and a site has said two contradictory things, and picking one would silently put an author's files somewhere they did not choose."

```json
{ "message": "browseScope is only valid at the site root; got 'SYSTEM_HOST' with path '/application/'" }
```

---

## OpenAPI

`openapi.yaml` is generated at compile time, so the description lives in the Java annotation and the regenerated file is committed alongside it. The `@Schema` must state three things a generator cannot infer: that omitting the field means today's behavior, that it is only valid at the site root, and that `includeSystemHost` is read only for `ALL`.

Regenerate with `./mvnw compile -pl :dotcms-core --am -DskipTests`.

---

## What is not in this contract

`showFolders` stays the caller's decision. The endpoint honours whatever it is sent so the response always matches the request and the folder cursors never describe a query the caller did not make. The client suppresses folders in `ALL` and `SYSTEM_HOST`; the server does not do it for them.

The frontend's URL encoding of the same choice (absent, `/`, a path, or `SYSTEM_HOST`, all in one value) is a Content Drive concern and is not part of this contract. The Asset Picker has no URL and no browse scope.
