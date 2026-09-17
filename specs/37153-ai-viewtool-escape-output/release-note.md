# Release note: `$ai` viewtool output is HTML-escaped by default (#37153)

**Type**: security fix, template behaviour change. **Applies to**: every Velocity template or
`.vtl` widget that prints output of the `$ai` viewtool.

## What changed

The dotAI Velocity viewtool (`$ai`) used to return the AI provider's reply exactly as received.
A template that printed the model's answer rendered any HTML the model produced, so a model
steered by indexed content or by a crafted prompt could inject a script into the page for every
visitor.

From this release, `$ai` **HTML-escapes** these values before handing them to the template:

1. everything the AI provider returned: the whole result of `$ai.completions.raw(...)`,
   `$ai.generateText(...)` and `$ai.generateImage(...)`, and the `openAiResponse` part of
   `$ai.completions.summarize(...)`;
2. the echoed `query` in summarize and search results;
3. the echoed `originalPrompt` in image results;
4. the search excerpt `dotCMSResults[].matches[].extractedText` in summarize and search results.

Nothing else changes. `dotCMSResults[].title` and every other content field in search results
are returned exactly as stored, as `$dotcontent` returns them. Numbers, booleans and dotCMS
metadata (`total`, `count`, `limit`, `offset`, `threshold`, `operator`) are untouched. Error
results (`$r.error`) are escaped too. Result **shape and property paths are unchanged**: code such
as `$r.openAiResponse.choices.get(0).message.content` keeps working; only the text differs.

Escaping uses the OWASP Java Encoder (`& < > " '` become `&amp; &lt; &gt; &#34; &#39;`) and is safe
for HTML body text and quoted attributes.

## If your template needs the raw output

Prefix the call with `unsafe`. `$ai.unsafe` is the same tool with escaping turned off and
behaves exactly as `$ai` did before this release, for every method.

Before (rendered the model's HTML, and any injected script):

```velocity
#set($summary = $ai.completions.summarize("Where can I find the best beaches?", "blogIndex"))
$summary.openAiResponse.choices.get(0).message.content
```

After, default (renders the model's reply as text; markup is shown, not executed):

```velocity
#set($summary = $ai.completions.summarize("Where can I find the best beaches?", "blogIndex"))
$summary.openAiResponse.choices.get(0).message.content
```

After, opting in to raw HTML (your responsibility to trust the model's output):

```velocity
#set($summary = $ai.unsafe.completions.summarize("Where can I find the best beaches?", "blogIndex"))
$summary.openAiResponse.choices.get(0).message.content
```

The same prefix works everywhere: `$ai.unsafe.completions.raw(...)`, `$ai.unsafe.generateText(...)`,
`$ai.unsafe.generateImage(...)`, `$ai.unsafe.search.query(...)`, `$ai.unsafe.search.related(...)`.
There is no configuration setting to restore the old behaviour globally.

## Check your templates for these three cases

- **You already escape the output yourself** (`$owasp.forHtml($r...)`, `$esc.html($r...)`). It will
  now be encoded twice and show text like `&amp;lt;`. Remove your own encoder, or switch to
  `$ai.unsafe` and keep it.
- **You pass the model's Markdown to a client-side renderer.** Markdown uses `>` for quotes and `<`
  in inline code; the renderer will now receive entities. Use `$ai.unsafe` for that value.
- **You use a value outside HTML text**: inside a `<script>` block, a URL your template fetches
  server-side, or a string comparison. Use `$ai.unsafe` and encode for that context with the
  `$owasp` tool (`forJavaScript`, `forUriComponent`, ...). Provider URLs with query strings now
  contain `&amp;`, which is correct inside `src="..."` and wrong if fetched server-side.

Search-result templates that print `$result.title`, `$m.extractedText` or content fields see no
visible change: the excerpt is plain text already and content fields are not escaped.

## If you roll back to the previous release

Templates you edited to use `$ai.unsafe` keep that code after a rollback, but the previous
release does not know `$ai.unsafe`: those templates fail to render until you remove the prefix
again or roll forward. Plan the template edits together with the upgrade.

## Not affected

The REST endpoints under `/api/v1/ai/*` return the same bodies as before. This change is limited
to the Velocity viewtool.
