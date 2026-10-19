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
| `schedule_from` | a local calendar date, `YYYY-MM-DD` | no lower bound |
| `schedule_to` | a local calendar date, `YYYY-MM-DD` | no upper bound |

`created_by` takes the name #36823 defines and #37007 will consume, so the eventual move to
server-side filtering changes where the value is applied and not what it is called (FR-049).

The two schedule parameters are independent: either may appear without the other, which is what an
open bound looks like and also what the calendar produces while the second end is still being
picked. Both bounds are inclusive and cover whole local days (FR-021).

They are deliberately **not** named `running_from`/`running_to`, which #36823 defines, even though
the shape now matches (FR-049a). Those compare the **running window** — actual runs, or the
scheduled window for an experiment that never ran — while these compare the **scheduled start**.
For an experiment that has already run the two answer different questions, so sharing the names
would assert an equivalence that does not hold, and a link written by one and read by the other
would silently filter on something else. #37007 settles the comparison first (FR-052a) and renames
only if it settles in #36823's favour.

The dates are local calendar dates rather than instants, because that is what a period picked on a
calendar means: the link reopens on the days that were chosen. The consequence, accepted, is that
the same link read in another time zone selects that reader's days.

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
- **Unrecognised values are dropped, with one exception** (FR-048). A schedule bound that is not a
  date is discarded, matching the rule `status` and `goal` already follow. An unrecognised creator
  id is **retained** — it is not necessarily invalid, it simply matches no experiment.
- **An inverted range is reported, not applied** (FR-021a). Two parseable dates whose end precedes
  its start match nothing by construction, so applying them would read as a site with no
  experiments scheduled then. Only the address can produce one.
- **Clear all widens the path, not the scope** (FR-051a). The filter bar's button clears the
  search term, the chips and `url`, and leaves `pageId` and `language_id` in force. `pageId` is
  written by the listing itself on every way out and read back on the way in, so it is a scope
  the screen hands down; `url` has no writer anywhere in the application and only ever arrives
  typed or pasted, so it is a filter the user applied — and one that would otherwise be
  unremovable while it still matched rows.
- **Changing either filter resets to the first page** (FR-041).
- **A page size outside the offered set is still honoured** (FR-044), so an address bookmarked
  under the old 10/25/50 options is not broken.
