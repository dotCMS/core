# Index Field Emission — how a contentlet becomes an index document

How `ESMappingAPIImpl` turns a contentlet's fields into the document written to Elasticsearch and
OpenSearch, and the one invariant in that path that is easy to break and impossible to see from
any single file.

Applies to both engines: the document is rendered **once** and shared.

## The two keys every field emits

`loadFields` writes each field under two names (`ESMappingAPIImpl.java`, in the per-field loop):

| Local variable | Key written | What happens to it |
|---|---|---|
| `keyName` | `<contenttype>.<field>` | survives into the final document |
| `keyNameText` | `<contenttype>.<field>_text` | **renamed** to `_dotraw`, then dropped |

The rename is not in `loadFields` — it is in `toMap`, in the post-processing loop that runs
*after* `loadFields` returns. That loop:

1. lowercases every key,
2. renames `_text` → `_dotraw`,
3. drops the `_text` key unless `CREATE_TEXT_INDEX_FIELD_FOR_NON_TEXT_FIELDS=true` (default false).

**So `_dotraw` is the only survivor of the pair**, and a test that calls `loadFields` in isolation
cannot observe it at all.

## The invariant: `_dotraw` on a numeric field is zero-padded, or absent

`*_dotraw` is mapped `keyword` (`es-content-mapping.json`, `template_1`), so it sorts
**lexicographically**. And every sort in dotCMS resolves through it — `addBuilderSort` appends
`_dotraw` to whatever field name the caller passes (`ContentFactoryIndexOperationsES`, and the
OpenSearch counterpart).

That is why numeric fields are padded to a fixed width by the `DecimalFormat` in `loadFields`:

```
"0000000000000000000.000000000000000000"     19 integer digits, 18 decimals
54  →  "0000000000000000054.000000000000000000"
```

Fixed-width zero-padding is the **only** reason ordering by a numeric field is numeric rather than
alphabetical. Break it and results silently reorder — no error, no exception, nothing in the log:

- `"N/A"` sorts after every padded digit string, because `'N'` (0x4E) > `'9'` (0x39).
- an unpadded `54` sorts before `9`.

**Rule: for a field on a numeric storage column, `<field>_dotraw` is either the padded form or the
key is not written at all. Never raw text, never an unpadded number.**

### The trap when omitting a field

`toMap`'s derivation loop has a fallback: when `<field>_text` is missing it synthesizes
`<field>_dotraw` **from the bare key's value, unpadded**. So omitting only `_text` while still
writing the numeric key produces exactly the malformed `_dotraw` this invariant forbids.

**Omitting a numeric field means omitting both keys together.** They are one atomic change.

## Storage column decides the branch, not the field type

`loadFields` selects its serialization branch from `field.getFieldContentlet()` — the storage
column — not from the declared field type. That matters because dotCMS allows a `TextField` to be
backed by a numeric column, and its own built-in types do it:
`htmlpageasset.sortOrder` is an `ImmutableTextField` with `DataTypes.INTEGER`
(`PageContentType`), and `FieldFactoryImpl` accepts it because `TextField.acceptedDataTypes()`
includes `INTEGER` and `FLOAT`.

Consequence: **the value in a numeric-column field may be a `Number` or a `String`.** Handing a
`String` straight to `DecimalFormat.format` throws `IllegalArgumentException`, and since the
per-field catch rethrows, the whole contentlet is lost from the index — from *both* engines at
once, in every migration phase, because the mapping is computed before the provider fan-out
(`ContentletIndexAPIImpl.mapContentletForProcessor`: *"Compute mapping once; reuse across all
providers"*). This was issue #37272.

`loadNumericField` handles it: a `Number` takes the identical pre-existing path, a `String` is
converted best-effort against the column's own type (`NumberUtil.toLongOrEmpty` /
`toFloatOrEmpty`), and a value that cannot be represented numerically omits both keys and logs a
WARN naming the field and content type.

### Generated mapping by column

| `field_contentlet` | Mapping | Emitted class |
|---|---|---|
| `integer%` | `long` | `Long` |
| `float%` | `double` | **`Float`** |
| `bool%` | `boolean` | `Boolean` |
| `*_dotraw` | `keyword`, `ignore_above: 8191` | `String` |

Mapping type and emitted class differ by design (`double` vs `Float`). It is pre-existing and
harmless — do not "normalize" it, since changing an emitted class changes the document for every
correctly-stored value in every installation.

## Testing this path

- **Assert against `toMap`, not `loadFields`.** `_dotraw` does not exist yet when `loadFields`
  returns, so the assertions that matter are unreachable from there.
- **Assert the key is absent**, not that it differs from the raw text. `assertNotEquals("n/a", …)`
  passes against the unpadded-number bug described above.
- **Field defaults bite.** `FieldDataGen` defaults `defaultValue` to `"testDefaultValue<millis>"`,
  a non-numeric String; combined with the generator's `IndexPolicy.FORCE`, the save indexes
  synchronously and a numeric-column field will fail during test setup. Give it a numeric default.
- **A `unique` field is implicitly required** and must carry a value at save time; a field-level
  default is not enough.
- **`CheckboxField` does not accept `DataTypes.BOOL`** — only Hidden, Radio and Select do.
- Working example: `dotcms-integration/src/test/java/com/dotcms/content/elasticsearch/business/ESMappingAPINumericFieldTest.java`
