# Walkthrough media

Evidence for the manual walkthrough of #37005, recorded end to end on a local instance with
`FEATURE_FLAG_EXPERIMENTS_PORTLET` on. Referenced from PR #37256 and from
[quickstart.md](../quickstart.md).

| File | What it shows |
|------|---------------|
| `breadcrumb-flow.mp4` | The whole flow, unedited: editor on `/index` → A/B → the list narrowed to that page → New → the name typed → Save Draft → the crumb back to the still-narrowed list |
| `1-uve-home.jpg` | The editor on Home. A/B in the right rail is the entry point |
| `2-list-narrowed.jpg` | The list narrowed to `?pageId=…&language_id=1`, with the page on the trail and no page-filter chip on the toolbar |
| `3-new-experiment.jpg` | New, with the Page field prefilled from the narrowing. The crumb names the screen |
| `4-named-draft.jpg` | The typed name reaches the header, not the crumb |
| `5-configure.jpg` | Save Draft: the address swaps to the experiment's own and the crumb follows it in place |
| `6-back-to-narrowed-list.jpg` | The `Experiments List` crumb returns to the list still narrowed, the Configure crumb truncated away |

Recorded with Playwright against `localhost:4360`. The instance behind it serves the previous copy
of `experiment.container.configuration.title` ("Experiments Configuration"); that key now reads
"Configure Experiment".
