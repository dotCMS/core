# Issue Resolution Specification: Admin UI labels show raw i18n keys on loads where translations must be downloaded

**Feature Branch**: `37993-i18n-raw-keys-on-load`

**Created**: 2026-10-16

**Status**: Draft

**Type**: Issue / Bug Resolution

**Related GitHub Issue**: [#37993](https://github.com/dotCMS/core/issues/37993)

**Input**: User description: "On loads where translations must be downloaded (new build, fresh browser, language change on login), admin UI labels show raw i18n keys — user menu, dm-pipe labels, My Account / Report an Issue validation messages. Rendering isn't gated on translations, the message map is empty during download, and consumers capture text once. Also covers appconfiguration failure and the login-language being overwritten by "default". Full analysis, repro and ACs in dotCMS/core#37993"

## Problem Statement *(mandatory)*

On some page loads, labels in the dotCMS admin UI show their raw translation keys instead of
the translated text. The confirmed case is the user menu (the avatar menu in the top-right
toolbar): it shows `my-account`, `login-as`, `report-an-issue` and `Logout` instead of
"My Account", "Sign In As", "Report an Issue" and "Sign Out". The keys stay on screen for the
rest of that page load, even after the translations finish downloading. A refresh makes them go
away, so users perceive the problem as random.

It happens on any load where the browser has to download the translations instead of using the
copy it saved earlier:

- the first load after a new dotCMS build is deployed (the build date changes, so every user's
  saved translations are discarded);
- the first login on a new browser, or after the site data is cleared;
- after changing the language on the login page.

Two related failures share the same cause:

- When the configuration request (`/api/v1/appconfiguration`) fails, the saved translations are
  never loaded and every label in that session can show as a raw key.
- A language chosen on the login page does not survive the login: the admin UI loads the
  default language right after, overwriting the user's choice, and labels captured before that
  download finishes can stay in the chosen language — so one screen can mix two languages.

**Severity / Impact**: Medium. Every admin user hits it on the first load after each deploy,
plus anyone on a new browser or changing language at login. Nothing is lost or blocked — the
problem is cosmetic — but it is very visible and makes the product look broken.

## Reproduction *(mandatory)*

**Environment**: dotCMS built from `main`; any browser and OS. It depends on network timing:
it shows up when the translations response arrives after the logged-in user's data.

**Steps to Reproduce**:

1. Log in to the admin UI.
2. In DevTools → Application → Local Storage, delete `dotMessagesKeys` and `buildDate`.
3. Make `/api/v2/languages/default/keys` slow, so it answers in about 1.5 seconds: put a proxy
   in front that delays only that path, or throttle the network in the DevTools Network panel.
   Keep the delay under 2.5 seconds — that is the longest the fixed UI waits for translations
   (AC-005); with a longer delay or a blocked request, raw keys after the fix are expected.
4. Reload the page.
5. Once the page has finished loading, open the avatar menu in the top-right toolbar.
6. Reload again.

**Expected Behavior**: In step 5 the menu shows "My Account", "Sign In As", "Report an Issue",
"Sign Out".

**Actual Behavior**: In step 5 the menu shows `my-account`, `login-as`, `report-an-issue`,
`Logout`, and keeps showing them after the translations request finishes. In step 6 the labels
are correct, because the translations are now saved in the browser.

**Reproducibility**: Always with the slow-translations setup above. Without it, intermittent:
locally the login response arrived at about 118 ms and the translations at about 154 ms, and a
slower server or network reverses that order. The language-mixing path was found by reading the
code and has not been reproduced.

## Scope of Investigation *(mandatory)*

- **Affected area**: Admin UI (Angular app in `core-web/apps/dotcms-ui`) — how translations are
  loaded at startup and on the login page, and how UI text reads them: the user menu, template
  labels translated in place, and the validation messages in the My Account and Report an Issue
  dialogs.
- **Suspected surface**: Frontend only. The translation loading lives in the shared message
  service (`core-web/libs/data-access`) and the shared translate pipe (`core-web/libs/ui`), both
  used across the admin UI and the portlet libraries. No backend (`com.dotcms.*` /
  `com.dotmarketing.*`) change is expected: the translations and configuration endpoints already
  return what the UI needs.
- **Related known decisions**: None known. The plan formally consults `dotCMS/platform-adrs`.

## Root-Cause Hypothesis

Three behaviors combine:

1. **The UI renders without waiting for translations.** The app starts loading translations only
   after the configuration request returns, and does not wait for them before showing the UI.
   Nothing at startup holds rendering until translations are available.
2. **While a download is in progress, there is no text to return.** When the saved copy is stale
   or missing, the in-memory translations stay empty until the download completes, so every
   lookup returns the key itself. Even a valid saved copy is only read after the configuration
   request returns, so the very first lookups miss it too.
3. **Several consumers read the text only once.** The user menu builds its labels a single time,
   when the logged-in user's data arrives. The template translate pipe does not re-evaluate a
   label it already rendered. The My Account and Report an Issue dialogs build their validation
   messages when they are created. Whatever they read at that moment — the key — stays.

On a cold load the translations request and the login request run in parallel, and when the
login answers first the menu is built from empty translations.

The related paths follow from the same design: when the configuration request fails, the error
handler leaves no build date and the translations are never loaded at all; and the startup load
always asks for the `default` language, so it discards the language the login page saved.

## Fix Scope & Non-Goals *(mandatory)*

**In scope**:

- Admin UI startup: translations are available before translatable text is first shown. The UI
  waits for the configuration request, which says whether the saved copy is current, then uses
  the saved copy when it is valid or downloads translations when it is not. The 2.5-second limit
  covers both requests together.
- During a download that replaces a saved copy, lookups keep returning the saved text instead of
  raw keys.
- Configuration request failure: translations still load (saved copy, or a fresh download of the
  default language).
- Translations request failure, or no translations within 2.5 seconds: the UI still loads; raw
  keys are acceptable only in this case, and the failure or timeout is logged to the browser
  console.
- Login-page language choice: it stays in effect after login and is not replaced by the default
  language.
- The consumers named in the issue — the user menu, translate-pipe template labels, and the My
  Account and Report an Issue validation messages — show translated text on a cold load.
- Unit tests for the startup and lookup behavior listed in Acceptance & Verification.

**Explicitly out of scope / non-goals**:

- Auditing or rewriting every other place in the admin UI and portlet libraries that reads
  translated text once. The fix makes translations available before the UI renders, which covers
  those consumers without touching each of them; a consumer that still misbehaves after the fix
  gets its own issue.
- Live language switching inside the admin UI (changing language without a reload).
- Changes to the translations or configuration endpoints, to how translations are cached on the
  server, or to the content of the translation files.
- The separately bootstrapped apps and libraries that load translations on their own (the binary
  field builder, the block editor) — beyond keeping them working.
- Showing a loading indicator or splash screen while translations download.

## Regression Risk *(mandatory)*

- **Blast radius**: The message service and translate pipe are shared by the admin UI and every
  portlet library (`libs/portlets`, `libs/edit-content`, `libs/ui`, the block editors, the
  template builder, the image editor). Any change to when translations load or how a lookup
  behaves reaches all of them. Every load now waits for the configuration request before the
  first render (today it does not); a cold load also waits for the translations download, both
  together capped at 2.5 seconds. On a valid saved copy no download is waited for. The login page
  and its language selector use the same service before the user is authenticated, so they must
  keep working without a session.
- **Backward compatibility**: No API, content, database or index change. The saved translations
  in the browser (`dotMessagesKeys`, `dotMessagesKeys-lang`, `buildDate` in local storage) keep
  their names and shape, so a copy saved by the current version is still read after the upgrade
  and vice versa. Callers that start translations themselves and ignore the result must keep
  working. Nothing here is rollback-unsafe.
- **Data considerations**: None. A stale or mismatched saved copy is replaced by a download, as
  today.

## Acceptance & Verification *(mandatory)*

- **AC-001**: On a load where the translations must be downloaded — no saved copy, or a saved
  copy from a different build or language — and the download completes within 2.5 seconds, no
  translatable text is shown as a raw key: the user menu, translate-pipe template labels, and the
  validation messages in My Account and Report an Issue. The reproduction steps above show the
  translated menu at step 5. (A download that fails or takes longer is covered by AC-005.)
- **AC-002**: When a valid saved copy exists — same language, and the configuration request
  reports the same build — the UI renders with it as soon as the configuration request returns,
  without requesting translations. The only request the first render waits for is the
  configuration request.
- **AC-003**: While newer translations are downloading, lookups return the saved text rather than
  raw keys.
- **AC-004**: If `/api/v1/appconfiguration` fails, the UI still shows translated labels, using the
  saved copy or a fresh download of the default language.
- **AC-005**: If the translations request itself fails, the UI still loads; raw keys are
  acceptable only in this case, and the failure is logged to the browser console. The same
  applies when translations are still not available 2.5 seconds after startup (counting the
  configuration and translations requests together): the UI stops waiting, renders with whatever
  translations it has (the saved copy, if any), and the timeout is logged to the browser console.
- **AC-006**: A language chosen on the login page is still in effect after login. It is not
  replaced by the default language, and the UI never shows a mix of the two.
- **AC-007**: Regression — the login page, its language selector, and the separately bootstrapped
  apps that load translations themselves keep showing translated text as they do today.
- **Verification method**:
  - Unit tests (Vitest + Spectator) on the message service and the admin UI startup covering: no
    saved copy; a stale saved copy (build date changed, and language changed); a valid saved copy
    used without a translations request; a failed configuration request; a failed translations request
    (logged to the console, UI still loads); configuration or translations still pending after
    2.5 seconds (UI renders, timeout logged to the console); lookups during a download returning the saved text; and the user's
    data arriving before the translations — the user menu labels are translated.
  - Unit test on the login flow: the language chosen on the login page is the one loaded after
    login.
  - Manual: the reproduction steps above, plus the same steps after changing the language on the
    login page, and with `/api/v1/appconfiguration` blocked in DevTools.

## Assumptions

- The translations endpoint answers without a session, as the login page already relies on.
- Waiting for the configuration request on every load, plus the translations download on a cold
  load (capped together at 2.5 seconds), is acceptable in exchange for never showing raw keys —
  including keys added by a new build, which a saved copy from the previous build lacks.
- "The language chosen on the login page" means the locale picked in the login page's language
  selector, saved in the browser; the user's profile language is not involved.
