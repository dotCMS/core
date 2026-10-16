# Feature Specification: Run @dotcms/events on Traditional Pages as an Opt-In Script

**Feature Branch**: `issue-37798-events-traditional-script`

**Created**: 2026-10-14

**Status**: Draft

**Type**: New Feature

**Epic**: [#36763 — Experiments: A/B Testing v2](https://github.com/dotCMS/core/issues/36763)

**Work item**: [dotCMS/core#37798 — Run @dotcms/events on traditional pages as a new opt-in script, /ext/events/events.min.js](https://github.com/dotCMS/core/issues/37798)

**Input**: User description: "Bring what `@dotcms/events` does on headless sites (pageviews, conversions, content impressions and clicks, experiments) to traditional (VTL) pages through one new opt-in script, `/ext/events/events.min.js`, without changing anything for sites that keep `/ext/analytics/ca.min.js`. All six parts of the issue are in scope, in one spec and one PR. A page opts in through the VTL tool alone." — taken from issue #37798, with the user's decisions of 2026-10-06.

## Clarifications

### Session 2026-10-14

- Q: When a site runs an experiment, on which LIVE pages should dotCMS wrap the contentlets so the events script can run it? → A: Only the page the running experiment tests (in any variant), and only when that page prints the tool; every other page keeps today's rule. Revised by the third answer below.
- Q: Should pages that print the events tool get #37910's HTML comment markers instead of the `div.dotcms-contentlet` wrapper? → A: Yes. On pages that print the tool, every contentlet dotCMS delimits gets comment markers and the events script reads them; pages without the tool keep the `div` wrapper for `ca.min.js`, unchanged. #37910 then covers only pages still on `ca.min.js`.
- Q: How should a page under test that is served from the page cache pick up an experiment that just started or ended? → A: With `experiments` on, every page that prints the tool gets markers whether or not an experiment runs, and whether the page is under test (hide it until the variant is decided) is added on every request after rendering, so cached pages follow at once with no cache cleared. The page cache stays a consideration when turning experiments on for traditional pages: what lives in the cached HTML changes only when the page's Cache TTL expires.
- Q (dissect-idea): How does the step after rendering tell that a page uses the events script, so it skips the old injections and decides whether the page is under test, when the page comes from the page cache? → A: From the page's own HTML: the tag the tool prints carries an attribute of its own, and that step looks for it, cache hit or not. This replaces the request flag decided on 2026-10-06 as the mechanism; the opt-in stays per page, through the tool alone.
- Q (dissect-idea): What does dotCMS add, on every request, to keep the page under test hidden until the variant is decided? → A: A CSS rule right after `<head>` that hides the whole page, lifts itself after 3 s, and carries the experiment id; the events script recognizes it, decides and reveals. It is the pattern the SDK already uses on headless pages.
- Q (dissect-idea): If a template prints `$dotAnalytics.code()` or `$dotExperiment.code()` as well as `$dotEvents.code()`, what should happen? → A: Nothing changes in those tools. The legacy experiments infrastructure (Jitsu's `/s/lib.js`) no longer works, so the only real effect is analytics events counted twice. The legacy tools and automatic injections will be deprecated in separate work.
- Q (dissect-idea): How should `$dotEvents.code()` load `events.min.js`? → A: Deferred: it downloads in parallel and runs once the HTML is parsed, so no page waits for it; the page under test stays hidden by the CSS rule until the decision.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A developer moves a page to the events script (Priority: P1)

A site developer whose site has the Content Analytics app configured adds one line to a page's template, the new VTL tool (for example `$dotEvents.code()`) in the `<head>`. From then on that page loads `/ext/events/events.min.js` with the site's Content Analytics settings, and sends its pageviews, content impressions and content clicks through `@dotcms/events`. dotCMS adds neither `ca.min.js` nor the legacy experiments scripts to that page, so nothing is counted twice. Its contentlets are delimited by HTML comments instead of extra `div` elements, so the page's own scripts and styles see the same structure they see with Content Analytics off. Other pages of the same site are untouched until their templates print the tool too.

**Why this priority**: It is the whole point of the work: traditional pages get the one SDK headless sites already use, page by page, with no site-wide switch to flip.

**Independent Test**: On a site with a Site Auth, with analytics auto-injection and experiments auto-injection turned on and content impressions on, print the tool in one template and load a LIVE page that uses it and a LIVE page that does not. The first carries only the events script tag and comment markers; the second carries `ca.min.js` and `div` wrappers as before.

**Acceptance Scenarios**:

1. **Given** a site with a Site Auth and a template that prints the tool, **When** a visitor loads a LIVE page built on that template, **Then** the page carries one `<script src="/ext/events/events.min.js" …>` tag with the site's settings in the `data-analytics-*` attributes, and the browser sends its pageview once.
2. **Given** analytics auto-injection is on for the server, **When** a page whose template prints the tool is served LIVE, **Then** the page carries no `ca.min.js` tag.
3. **Given** experiments auto-injection is on and the site runs an experiment, **When** a page whose template prints the tool is served LIVE, **Then** the page carries neither the legacy experiments head code nor its init script, and the browser requests no `/s/lib.js`.
4. **Given** the page is cached (its Cache TTL is above zero) and served from the cache, **When** a visitor loads it, **Then** scenarios 2 and 3 still hold.
5. **Given** a site with no Site Auth, **When** a template that prints the tool renders, **Then** the tool prints nothing.
6. **Given** a page whose template prints the tool, where a Text box and a Form are siblings in the same column, **When** the page renders LIVE with content impressions and clicks on, **Then** it contains no `div.dotcms-contentlet` element, each contentlet sits between `<!--dot:contentlet {…}-->` and `<!--/dot:contentlet-->`, the Form's `parentElement` is the column, and its impressions and clicks are reported.

---

### User Story 2 - A marketer runs an experiment on a page that uses the events script (Priority: P1)

A marketer starts an experiment on a traditional page whose template prints the tool. The events script decides the experiment from the contentlet markers dotCMS prints: the experiment comes from the `isExperimentPage` rule that `isUserIncluded` returns, and the variant dotCMS rendered from the markers' `variant`. A visitor is assigned once, sees one variant, and is redirected to it at most once. The site keeps content impressions and clicks off, and the experiment still runs, from the first request after it starts even when the page is cached.

**Why this priority**: Experiments are what traditional pages cannot get from `ca.min.js`, and the events script cannot run one without knowing where each contentlet is and which variant dotCMS rendered, which today it only learns when impressions or clicks are on.

**Independent Test**: On a site with a Site Auth, `experiments` on, content impressions and clicks off, start an experiment on a page whose template prints the tool and whose Cache TTL is above zero, and load it LIVE as a new visitor and again as a returning one.

**Acceptance Scenarios**:

1. **Given** a site with a Site Auth, `experiments` on, impressions and clicks off, **When** a page whose template prints the tool is rendered LIVE in any variant, **Then** every contentlet on it is delimited by comment markers carrying its identifier and variant, whether or not an experiment runs.
2. **Given** a running experiment on that page, **When** a new visitor loads it, **Then** nothing of the page shows until the variant is decided (3 s at most), the visitor gets exactly one variant and at most one redirect to it, and their events carry that experiment and variant.
3. **Given** the page was cached before the experiment started, **When** a new visitor loads it after the start, **Then** scenario 2 holds without waiting for the Cache TTL to expire.
4. **Given** the same site, **When** another page that prints the tool but is not under test is loaded, **Then** nothing of it is ever hidden while the events script asks `isUserIncluded`.
5. **Given** the same site with `experiments` off and impressions and clicks off, **When** a page that prints the tool is rendered LIVE, **Then** no contentlet is delimited, as today.

---

### User Story 3 - A site admin turns experiments off for the events script (Priority: P2)

A site admin who wants the events script for analytics only turns off the new `experiments` setting in the "Server-Rendered Pages Configuration" section of the Content Analytics app. The events script then runs no experiment on that site's pages. A site whose app was saved before the setting existed keeps experiments on, without being re-saved.

**Why this priority**: The setting gives admins the same control over experiments they have over pageviews, impressions and clicks; the default keeps experiments working for everyone who does nothing.

**Independent Test**: Toggle the setting on one site and read the tag the tool prints; read the tag on a site whose stored app config has no `experiments` value.

**Acceptance Scenarios**:

1. **Given** the Content Analytics app, **When** an admin opens it, **Then** an `experiments` on/off setting appears in the "Server-Rendered Pages Configuration" section next to auto page views, content impressions, content clicks and debug, and is on for a new configuration.
2. **Given** `experiments` off, **When** the tool prints the tag, **Then** the tag carries `data-analytics-experiments="false"` and the events script neither asks `isUserIncluded` nor hides content.
3. **Given** an app configuration saved before the setting existed, **When** the tool prints the tag, **Then** the events script runs experiments.

---

### User Story 4 - A site that stays on `ca.min.js` upgrades with no change (Priority: P2)

A site developer whose templates never print the new tool upgrades dotCMS. Their pages keep getting `ca.min.js`, the `div` wrappers it reads and the legacy experiments scripts exactly as their server flags and app say, and VTL code that calls `window.dotAnalytics` keeps working. On a site that uses both, only the pages that print the tool change.

**Why this priority**: The events script is opt-in; a site that has not opted in must not be able to tell the release happened.

**Independent Test**: Compare the LIVE HTML and the script requests of pages that do not print the tool, before and after the upgrade, on a site with and without a running experiment.

**Acceptance Scenarios**:

1. **Given** a page that does not print the tool, with analytics auto-injection on, **When** it is served LIVE, **Then** it carries the `ca.min.js` tag as today, and `/ext/analytics/ca.min.js` serves the `@dotcms/analytics` build.
2. **Given** a page that does not print the tool, on a site with a running experiment and experiments auto-injection on, **When** it is served LIVE, **Then** it carries the legacy experiments head code and init script, as today.
3. **Given** a page that does not print the tool, with content impressions or clicks on, **When** it is rendered LIVE, **Then** its contentlets keep the `div.dotcms-contentlet` wrapper, as today.
4. **Given** a running experiment whose page does not print the tool, with `experiments` on and impressions and clicks off, **When** that page is rendered LIVE, **Then** no contentlet is delimited, as today.

---

### Edge Cases

- **Cached pages and the scripts.** A page with a Cache TTL above zero is served from the page cache on GET, and its template does not run on a cache hit. The tool's tag is in the cached HTML, but anything the tool did to the request is not; the scripts dotCMS adds after rendering are added on every request, cache hit or not. FR-013 covers it.
- **Cached pages and the settings.** The tag's settings and the markers live in the cached HTML. Turning `experiments`, impressions or clicks on or off, or adding the tool to a template, reaches a cached page when its Cache TTL expires; only whether the page is under test follows at once (FR-018).
- **Tool printed twice** (in the template and again in an included file): the page still carries one events script tag (FR-007).
- **Legacy tools on the same page.** A template that prints `$dotAnalytics.code()` or `$dotExperiment.code()` next to the new tool gets what it printed: those tools stay as they are. With `$dotAnalytics.code()` the page's events are counted twice; the legacy experiments script no longer works (see Assumptions).
- **Experiment starts or ends** (manual start, scheduled start, manual or scheduled end, cancel, push publishing to a receiver): the next request for its page follows the change, from the page cache too, because whether the page is under test is decided on every request (FR-018).
- **A container used by pages of two sites, or by pages that print the tool and pages that do not**: each page gets its own decision (markers, wrapper or nothing), which the cached container code could not give (FR-017).
- **Tool printed after the containers** (for example in the footer instead of the `<head>`): on that render the containers do not know yet that the page uses the events script. Contentlets covered by impressions or clicks get the `div` wrapper, which the events script still tracks (FR-022); with impressions and clicks off they are not delimited, and an experiment on the page runs without knowing the rendered variant from the page.
- **Comments removed on the way to the browser.** dotCMS's own HTML minification must keep the markers (FR-021). A minifier or CDN optimization outside dotCMS that strips comments removes them, and the events script then tracks no contentlet on that page and runs its experiment without the markers.
- **Unbalanced or broken markers** (a contentlet that opens a tag another closes, a start marker with no end, JSON that does not parse, a marker pair with no element between them): FR-023.
- **Page scripts that walk `childNodes` or `firstChild`** see the comment nodes. This is the known cost of markers; `parentElement`, `children`, CSS selectors and layout are unaffected.
- **Experiments turned off server-wide** (`FEATURE_FLAG_EXPERIMENTS` off): no experiment counts as running, so no page is under test; `isUserIncluded` answers 403 and the events script stops asking for a while, as it does today.

## Requirements *(mandatory)*

### Functional Requirements

**The SDK**

- **FR-001**: The standalone (IIFE) build of `@dotcms/events` MUST be named `events.min.js`.
- **FR-002**: The events script MUST run no experiment when its tag carries `data-analytics-experiments="false"`, and MUST run experiments when that attribute is absent, empty or has any other value.

**Build and serving**

- **FR-003**: dotCMS MUST serve the events script at `/ext/events/events.min.js`, built from `@dotcms/events` by the regular dotCMS build.
- **FR-004**: `/ext/analytics/ca.min.js` MUST keep serving the `@dotcms/analytics` build; nothing in this work may change what that path serves.

**The Content Analytics app**

- **FR-005**: The Content Analytics app MUST offer an `experiments` on/off setting in its "Server-Rendered Pages Configuration" section, on by default.
- **FR-006**: A site whose stored app configuration has no `experiments` value MUST be treated as having experiments on, in the tag the tool prints, in which contentlets are delimited (FR-015) and in whether a page is under test (FR-018).

**Putting the script on a page**

- **FR-007**: A new VTL tool MUST print, in LIVE mode, the events script tag with the site's Content Analytics settings: Site Auth, debug, auto page views, content impressions, content clicks and advanced configuration under the attribute names `ca/html/analytics_head.html` uses today (`data-analytics-auth`, `-debug`, `-auto-page-view`, `-impressions`, `-clicks`, `-config`), plus `data-analytics-experiments`, and an attribute of its own that marks the page as using the events script (FR-013). A page carries the tag once, however many times its templates call the tool. The tag MUST NOT hold up the page: the page renders while the script downloads, and the script runs once the HTML is parsed.
- **FR-008**: The tool MUST print nothing when the site has no Site Auth (the app's System Host fallback applies, as today), and nothing outside LIVE mode.
- **FR-009**: The tool MUST escape the values a user typed into the app (Site Auth, advanced configuration) the way the `ca.min.js` tag escapes them today.
- **FR-010**: Printing the tool MUST be the only way a page opts in to the events script. There is no site-wide or server-wide switch.

**Never both scripts on one page**

- **FR-011**: A LIVE page that carries the events script tag printed by the tool MUST NOT receive the automatically injected `ca.min.js` tag, whatever the analytics auto-injection flag says.
- **FR-012**: That page MUST receive nothing from the automatic legacy experiments injection: no `experiment/html/experiment_head.html` (so no `/s/lib.js` request), no `experiment/js/init_script.js`, and not the snippet it prints when no experiment is running.
- **FR-013**: FR-011 and FR-012 MUST hold when the page is served from the page cache. dotCMS MUST tell that a page uses the events script from the page's own HTML (the tag the tool prints and its attribute), never from something set while the template ran, which a page-cache hit does not repeat.
- **FR-014**: On the same site, a page that does not print the tool MUST keep receiving both automatic injections under the same conditions as today.

**Which contentlets are delimited, and the page under test**

- **FR-015**: In LIVE mode, on a page that prints the tool, every contentlet MUST be delimited when the site has a Site Auth and `experiments` on, whether or not an experiment runs and even with content impressions and clicks off.
- **FR-016**: On a page that does not print the tool, and on a page that prints it with `experiments` off, which contentlets are delimited MUST follow today's rule (Site Auth and content impressions or clicks on). A site with `experiments`, impressions and clicks all off delimits nothing in LIVE mode. Edit mode keeps its `div` wrappers unchanged.
- **FR-017**: Whether and how a page's contentlets are delimited MUST be decided while the page renders, for the page and site being rendered, never stored in the cached container code, which is shared by every page and site that uses the container. It MUST NOT depend on whether an experiment is running.
- **FR-018**: Whether the page being served is the page a running experiment tests (in any variant), on a site with `experiments` on and for a page that uses the events script (told as in FR-013), MUST be decided on every request after the page is rendered, including when the page comes from the page cache, so an experiment that starts or ends is reflected on the next request for its page without clearing any cache.

**Comment markers on pages that print the tool**

- **FR-019**: On a LIVE page that prints the tool, every delimited contentlet MUST be preceded by `<!--dot:contentlet {json}-->` and followed by `<!--/dot:contentlet-->`, with no `div.dotcms-contentlet` element. Pages that do not print the tool keep the `div` wrapper (FR-016).
- **FR-020**: The marker JSON MUST carry the values the LIVE wrapper carries today for the trackers: identifier, inode, content type, base type, title, language, variant, content type id and on-number-of-pages. It MUST parse with `JSON.parse`, and a title containing `--`, `>`, quotes or non-ASCII characters MUST still produce a valid comment whose JSON decodes back to the original title.
- **FR-021**: The markers MUST survive dotCMS's own HTML minification (`FEATURE_FLAG_MINIFY_HTML`), which today drops every comment except conditional comments.
- **FR-022**: The events script MUST track marker-delimited contentlets the way it tracks wrapped ones: one impression when at least the configured visible ratio of the combined area of the elements between a contentlet's markers stays in the viewport for the configured dwell time; a click on a link or button between a contentlet's markers, with that contentlet's identifier, inode and content type; contentlets inserted after load with their markers tracked, and removed ones no longer observed; the rendered variant read from the markers' `variant`. Contentlets rendered as `.dotcms-contentlet` elements (the headless SDKs, or a traditional page that still prints wrappers) MUST keep being tracked.
- **FR-023**: The events script MUST ignore a marker pair with no element between them, a start marker without its end marker, and marker JSON that does not parse, keep tracking the rest of the page, and report the broken marker in the console only with debug on.
- **FR-024**: On the page under test (FR-018), a new visitor MUST see nothing of the page until the variant is decided, and the page MUST show after 3 s at the latest. dotCMS MUST hide the page itself, on every request, with what it adds right after `<head>`: a rule that hides the whole page before the events script loads, lifts itself after 3 s even if JavaScript never runs, and tells the events script which experiment the page runs; the events script reveals the page once it decides. Pages that are not under test MUST never be hidden.

**Tests**

- **FR-025**: Automated tests MUST cover the new setting and its default for configurations saved before it existed, the tag the tool prints (with and without a Site Auth), the two scripts never sharing a page (including a page served from the page cache), which contentlets are delimited and how (markers on pages that print the tool, the `div` wrapper elsewhere), the page-under-test decision on a page served from the page cache, the marker JSON and its escaping, markers surviving minification, the events script tracking and deciding from markers, and `data-analytics-experiments` in the SDK.

### Key Entities

- **Content Analytics app configuration**: per site, with System Host fallback. Today it holds Site Auth, auto page views, content impressions, content clicks, advanced configuration and debug for server-rendered pages; it gains `experiments` (on/off, on when absent).
- **Events script tag**: the `<script src="/ext/events/events.min.js">` tag the tool prints, with the `data-analytics-*` attributes the events script reads its configuration from.
- **Contentlet markers**: the pair of HTML comments `<!--dot:contentlet {json}-->` … `<!--/dot:contentlet-->` around a contentlet on a LIVE page that prints the tool; the JSON carries the contentlet's identifier, variant and the other values the trackers read. The events script finds contentlets and the rendered variant through them.
- **Contentlet wrapper**: the `div.dotcms-contentlet` with `data-dot-*` attributes that dotCMS keeps printing in edit mode and on LIVE pages that do not print the tool; `ca.min.js` reads it, and the headless SDKs render their own.
- **Running experiment and the page under test**: an experiment in the RUNNING state, and the page it tests. Only the page under test is hidden until a new visitor's variant is decided, and whether a page is under test is decided per request, never cached.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer moves a page to the events script by adding one line to its template, with no change to server flags or other templates.
- **SC-002**: On pages that use the events script, every pageview, content impression and content click is recorded exactly once: zero duplicates in a manual check of 20 page loads with every auto-injection flag on.
- **SC-003**: On an experiment page that uses the events script, with impressions and clicks off, 20 new visitors each see one variant, nothing of another variant before it, and at most one redirect, and every one of their events carries that variant.
- **SC-004**: For a site whose templates never print the tool and that runs no experiment, the LIVE HTML and the script requests of its pages are the same before and after the upgrade.
- **SC-005**: On a page that prints the tool, with content impressions and clicks on, a page script that finds a sibling contentlet through `parentElement` (the Form success callback reported in #37910) runs without error, as it does with Content Analytics off.
- **SC-006**: When an experiment starts on a page with a Cache TTL of one hour that is already cached, the first new visitor after the start runs the experiment, hidden until decided, with no cache cleared and no wait for the TTL.

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: the Content Analytics app (Apps), the scripts dotCMS adds to LIVE pages after rendering them (Content Analytics and the legacy experiments code, both behind server flags), container rendering and its cached Velocity code (the contentlet wrapper), the static page cache, dotCMS's HTML minification, the VTL toolbox, and static files served under `/ext/`. The legacy experiments code and the container code are among the older parts of the rendering pipeline. The page cache is part of running experiments on traditional pages: the settings and markers a page carries change when its Cache TTL expires, so turning experiments on for a site whose pages are cached takes effect page by page.
- **Backward-compatibility expectations**: `/ext/analytics/ca.min.js`, `@dotcms/analytics`, `window.dotAnalytics` and the `div` wrapper they read do not change. `$dotAnalytics.code()` and `$dotExperiment.code()` keep their output. App configurations saved before this work keep working and get `experiments` on. This work deprecates nothing: `$dotAnalytics.code()`, `$dotExperiment.code()` and the automatic injections will be deprecated in separate work. Pages that move to the events script call `window.dotEvents` (`init`, `conversion`, `pageView`), which has no `track` because dotCMS rejects custom events.
- **Known related decisions**: the contentlet wrapper came with #34400 (content clicks on traditional pages) and #34401 (content impressions on traditional pages), shipped in #35832 and #35881. `@dotcms/events` was built in #37799 for #37683 (one SDK for analytics and experiments); its rules stand: no cookies (variant stickiness lives in localStorage or the URL), changes stay inside `libs/sdk/events`, and traditional pages run it in `contentlets` mode. This work brings the comment markers of #37910 (open, replace the LIVE wrapper with HTML comment markers) to the pages that print the tool, with its marker format, JSON contents and whole-page anti-flicker; #37910 then covers only the pages still on `ca.min.js`, and its body needs updating to say so. No license gate applies. The plan consults `dotCMS/platform-adrs`.

## Assumptions

- **The issue names the wrong place for the `ca.min.js` injection.** `AnalyticsTrackWebInterceptor` does not add the tag to pages: it only sets the content type and CORS headers when `/ext/analytics/ca.min.js` itself is requested. The tag is added after the page renders, in `HTMLPageAssetRenderedAPIImpl.getPageHtml`, next to the legacy experiments code. FR-011 applies there.
- **Both automatic injections are off unless a server flag turns them on.** `ca.min.js` is injected only with `FEATURE_FLAG_CONTENT_ANALYTICS_AUTO_INJECT` on and app secrets for the site; the legacy experiments code only with `ENABLE_EXPERIMENTS_AUTO_JS_INJECTION` on. Both default to off (the experiments `docker-compose` example turns both on). Without the flags, the legacy scripts reach a page only through `$dotAnalytics.code()` and `$dotExperiment.code()`.
- **The opt-in is read from the page, not from the request.** On 2026-10-06 the user decided that the tool tells the request, so both injections skip the page. On a page-cache hit the template does not run and the request is never told, so on 2026-10-14 the mechanism became the page's own HTML (FR-013); the opt-in itself is unchanged.
- **The legacy VTL tools stay as they are.** Their Javadoc already makes the template author responsible for not printing a script twice; the never-both rule covers what dotCMS adds automatically. A template that prints `$dotAnalytics.code()` or `$dotExperiment.code()` next to the new tool is the author's choice. Per the user, the legacy experiments infrastructure (Jitsu's `/s/lib.js`) no longer works, so the only real effect is analytics events counted twice.
- **The tool belongs in the `<head>`.** The containers learn that the page uses the events script only if the tool has run before them, so they can print markers instead of the `div` wrapper; the docs say to print it in the `<head>`, and say how the page cache affects running experiments on traditional pages. Making the order irrelevant would mean rewriting the HTML after every render, which this work does not do.
- **Today's rule keeps its known staleness.** For pages that do not print the tool, the wrapper decision stays baked into the cached container code, as today: saving the Content Analytics app does not refresh it, and a container shared by two sites carries the decision of whichever site wrote it first. Fixing that is separate work.
- **The markers and the page-under-test decision need a Site Auth**, as the impressions and clicks condition does: without one the events script does not start.
- **The marker JSON leaves out what only edit mode reads** (edit permission, page-language version, style properties), as #37910 lists it.
- **`experiments` is read from its attribute only.** The advanced configuration JSON does not gain an `experiments` key.
- **The SDK's own docs say otherwise.** `core-web/libs/sdk/events/CLAUDE.md` describes #37798 as the backend changes that make the events SDK the only script, switched on by pointing `core-web/pom.xml` at its build. The issue supersedes it: `ca.min.js` stays, and the events script ships at a new path. The SDK's docs and comments that name `ca.min.js` get updated with the implementation, not in this spec.
