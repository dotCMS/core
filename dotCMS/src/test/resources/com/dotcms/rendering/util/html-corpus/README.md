# HTML minification corpus

Real rendered pages used by `HtmlMinifierIntegrityTest`. Hand-written fixtures only cover cases
somebody thought of; real templates combine features in ways nobody writes deliberately, which is
what these are for.

| File | Origin | Why it is here |
|---|---|---|
| `demo-home.html` | `/index` on the demo starter site | 54KB, 630 attribute values, an 11KB inline `<style>` block, 93 comments, and a `<select>` whose `<option>` whitespace is not rendered |
| `demo-members.html` | `/members/index` on the demo starter site | Inline `<script>` whose correctness depends on automatic semicolon insertion, so it catches any attempt to minify script bodies |
| `icons-and-media.html` | **built, not captured** | 45 inline `<svg>` icons plus `iframe`, `canvas`, `video`, `audio`, `noscript` and `template`. None of these appear in either demo page, so real markup could not catch whitespace bugs around them |

The first two are output of the public dotCMS demo starter, captured with
`FEATURE_FLAG_MINIFY_HTML` off.

`icons-and-media.html` exists because the captures had a blind spot rather than a missing feature:
two content bugs shipped to review that no page here contained an example of. It is written to look
like a real template, with the `Home <svg>…</svg>` pattern that a modern icon set produces, and
`test_minify_keeps_word_to_element_separations_in_real_pages` counts 41 of those separations in it.

## Adding or refreshing a page

These are **committed to a public repository**, so treat them as published the moment they land.

1. Capture with the minify flag **off**, so the file is the un-minified input the test minifies.
2. **Scrub anything identifying.** A page rendered while authenticated can embed the logged-in
   user's name, email and privilege flags. `demo-members.html` did: its profile block has been
   replaced with `Test User` / `user@example.com`. Prefer capturing anonymously where the page
   allows it.
3. Check for secrets before committing: bearer tokens, API keys, `JSESSIONID` or other session
   identifiers, CSP nonces, gravatar hashes (they are hashes of an email address), and internal
   hostnames or IPs. Capture from a demo or local instance rather than a customer environment.
4. Register the file name in `HtmlMinifierIntegrityTest.CORPUS`.

No expected-output file is needed. The test asserts an invariant, that minified markup is
semantically identical to the input, so a new page needs no golden copy and will not need updating
when the minifier legitimately changes how much whitespace it removes.
