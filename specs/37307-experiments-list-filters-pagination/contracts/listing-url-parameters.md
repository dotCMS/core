# Contract: listing URL parameters

The address is the one contract this feature genuinely changes, and it is public in practice: the
Universal Visual Editor deep-links into this screen, and users share filtered links. It is read by
a single function and written by a single function; both new parameters join on both sides
(FR-046).

## Existing parameters — unchanged

`filter`, `status`, `goal`, `page`, `per_page`, `orderby`, `direction`, `pageId`, `url`,
`language_id`.

Names, meanings and defaults are untouched, so every URL that works today produces the same rows.

## Added

| Parameter | Shape | Default (omitted when) |
|---|---|---|
| creator selection | repeatable, one user id per entry | empty selection |
| schedule window | single value from the closed set of four named windows | no constraint |

Final parameter names are settled during implementation under FR-049, which requires them to be
chosen so they survive #37007 moving this listing to server-side filtering without a rename.

## Rules

- **Every filter of the list is a parameter** (FR-045). View state lives in the route and nowhere
  else, so a filtered list can be linked, reloaded and stepped through with back and forward.
- **Absent means default** (FR-047). A value equal to its default is omitted, so a pristine listing
  carries no query string at all. This satisfies the rule rather than bending it — the state stays
  fully derivable from the route. The stricter "always write every parameter" reading is
  deliberately not adopted, because it would change all ten existing parameters and the deep links
  #37005 built on them.
- **Transient interface state is excluded** (FR-045a). The search box inside the Created By popover
  narrows the option list rather than the data; routing it would also rewrite the address on every
  settled keystroke while the popover is open.
- **Unrecognised values are dropped, with one exception** (FR-048). An unknown window value is
  discarded, matching the rule `status` and `goal` already follow. An unrecognised creator id is
  **retained** — it is not necessarily invalid, it simply matches no experiment.
- **Changing either filter resets to the first page** (FR-041).
- **A page size outside the offered set is still honoured** (FR-044), so an address bookmarked
  under the old 10/25/50 options is not broken.
