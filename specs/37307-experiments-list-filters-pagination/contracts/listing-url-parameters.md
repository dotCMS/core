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
| `created_by` | repeatable, one user id per entry | empty selection |
| schedule window | single value from the closed set of four named windows | no constraint |

`created_by` takes the name #36823 defines and #37007 will consume, so the eventual move to
server-side filtering changes where the value is applied and not what it is called (FR-049).

The schedule parameter deliberately does **not** adopt `running_from`/`running_to` (FR-049a).
Those carry absolute ISO dates; this filter offers five relative windows and no custom range, so an
absolute date in the address could not say which window was chosen — a link shared one week and
opened the next would match no option. The address therefore carries the window itself, and the
conversion to an absolute lower bound happens when the request is built. That conversion is
#37007's to make; the address does not change when it lands.

Note that the server-side contract does not exist yet: the endpoint still accepts only `pageId`,
`name` and `status`. These names are adopted from a specification, not from a built API.

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
