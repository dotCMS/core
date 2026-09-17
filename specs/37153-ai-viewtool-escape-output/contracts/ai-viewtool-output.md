# Contract: `$ai` viewtool output (template-facing)

This is the behavior a Velocity template author can rely on after #37153. It is a **template
contract change**: the return **types and shapes** are unchanged, the **text** of some values
is now HTML-escaped by default.

## Two entry points

| Call | Behavior |
|---|---|
| `$ai.<anything>` | Safe by default: the values listed under "Escaped set" are HTML-escaped. Everything else is exactly as before. |
| `$ai.unsafe.<anything>` | Exactly the pre-#37153 behavior for every method, including the ones that were never escaped (`embeddings.*`, `isAiEnabled()`, `completions.config`). |

`$ai.unsafe` is the **only** way to get unescaped provider output. There is no configuration
flag. `$ai.unsafe.unsafe` is the same tool as `$ai.unsafe`.

## Escaped set (default mode)

Exactly these four things, and nothing else:

1. **Everything the AI provider returned** (as rebuilt by the dotAI client): the whole result of
   `completions.raw(...)`, `generateText(...)` and `generateImage(...)`; the `openAiResponse`
   subtree of `completions.summarize(...)`. Every string at any depth, including ids, model names
   and URLs. The image result carries no model text, only `url`; its escaped text is the echoed
   `originalPrompt`.
2. **`query`** in `summarize` and `search.*` results.
3. **`originalPrompt`** in `generateImage` results.
4. **`dotCMSResults[].matches[].extractedText`** in `summarize` and `search.*` results.

Not escaped: `dotCMSResults[].title`, every other contentlet field under `dotCMSResults`,
numbers, booleans, nulls, and dotCMS metadata (`total`, `count`, `limit`, `offset`,
`threshold`, `operator`, `timeToEmbeddings`).

Error results (`$r.error`) are escaped in default mode. `generateText` still throws on provider
failure, as before.

## Encoding

OWASP Java Encoder `forHtml`: `& < > " '` become `&amp; &lt; &gt; &#34; &#39;`. The output is
safe to print in HTML body text and inside quoted attributes. It is **not** for JavaScript
strings, URLs fetched server-side, CSS, or equality comparisons; for those use `$ai.unsafe`
and encode for the right context with `$owasp`.

## Examples

Model reply: `Use <b>bold</b> & "quotes"`

```velocity
## default: renders the literal text  Use <b>bold</b> & "quotes"
#set($r = $ai.completions.summarize("Escaping probe markup"))
<p>$r.openAiResponse.choices.get(0).message.content</p>
## HTML source: Use &lt;b&gt;bold&lt;/b&gt; &amp; &#34;quotes&#34;

## opt-in: renders bold text, exactly as before #37153
#set($r = $ai.unsafe.completions.summarize("Escaping probe markup"))
<p>$r.openAiResponse.choices.get(0).message.content</p>

## search results: content fields are untouched, the excerpt is escaped
#set($results = $ai.search.query("beaches", "blogIndex"))
#foreach($result in $results.dotCMSResults)
  $result.title                     ## unchanged
  #foreach($m in $result.matches)
    $m.extractedText                ## escaped
  #end
#end

## value used in a non-HTML context: opt out, then encode for that context
<script>var s = "$owasp.forJavaScript($ai.unsafe.completions.raw($p).choices.get(0).message.content)";</script>
```

## Guarantees

- Same keys, same nesting, same array lengths, same numbers/booleans/nulls in default and
  unsafe results. Only string text differs, only where listed above, only if it contained
  `& < > " '`.
- Property access paths that work today keep working (`$r.openAiResponse.choices[0].message.content`,
  `$r.dotCMSResults[0].matches[0].extractedText`, `$r.error`).
- The default path never throws where the unsafe path does not.
- Calling `$ai` does not change what `/api/v1/ai/*` returns.

## Known consequences

- Templates that already escape (`$owasp.forHtml($r...)`, `$esc.html($r...)`) will
  double-encode. Remove the extra encoder or switch to `$ai.unsafe` + `$owasp`.
- Model Markdown passed to a client-side renderer contains `&gt;` for blockquotes and `&lt;`
  for inline code; use `$ai.unsafe` for that pattern.
- Provider URLs with query strings gain `&amp;`, correct inside `src="..."`, wrong if fetched
  server-side.
