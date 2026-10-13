# Data Model: New Edit Contentlet UI polish (#37958)

No persisted data, API payloads or store state change. Two in-code shapes change meaning:

## `DateOptions.icon` (`calendar-field.util.ts`)

| Field | Before | After |
|-------|--------|-------|
| `icon: string` | PrimeIcons class: `pi pi-calendar` (Date, Date and Time), `pi pi-clock` (Time) | Material Symbol name: `calendar_today` (Date, Date and Time), `schedule` (Time) |

Consumed only by `calendar-field.component.html`, which renders it inside `#triggericon`
instead of passing it to `[icon]`.

## `DotChipFilterComponent.icon` input (`libs/ui`)

| Field | Before | After |
|-------|--------|-------|
| `icon = input<string>('')` | CSS class rendered as `<i class="{{ icon }}">` | Material Symbol name rendered as the text of a `material-symbols-outlined` span |

Empty by default in both cases (no glyph). Sole consumer: the relationship site chip
(`icon="pi pi-folder"` → `icon="folder"`).
