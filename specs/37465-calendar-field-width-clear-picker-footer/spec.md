# Feature Specification: Date, Time and Date-and-Time field width, clearing, and picker footer

**Feature Branch**: `issue-37465-calendar-field-width-clear-picker-footer`

**Created**: 2026-09-14

**Status**: Draft

**Type**: New Feature (presentation/interaction enhancement to an existing field)

**Input**: GitHub issue [#37465](https://github.com/dotCMS/core/issues/37465) — *Improve Date, Time, and Date-and-Time field width, clearing, and picker footer in the new Edit Contentlet*. Labels: `Team : Falcon`, `Type : Task`, `dotCMS: New Edit Contentlet`.

---

## Context

In the new Edit Contentlet, the three temporal field types — **Date**, **Time** and **Date and time** —
are the only fields that look and behave differently from their neighbours. They sit narrower than
the column that contains them, they cannot be emptied once a value is set, and the overlay that
opens when the user picks a value carries PrimeNG's stock footer, with a *Clear* action that is
redundant once the field has its own clear control.

This feature brings all three into line: **full-column width**, **a clear control on the field
itself**, and **a picker footer that shows the timezone label and a single *Today* / *Now*
action**.

### Verified current behaviour

Read from the code on `origin/main` (not quoted from the issue):

| Aspect | What happens today |
|---|---|
| Width | The picker control sizes to its content, so it stops short of the column edge while Text, Select and the other field types stretch to fill it. |
| Clearing | A clear (X) control appears **only** on the field designated as the content type's *expire date*. Every other Date/Time/Date-and-time field, once given a value, can never be emptied through the UI. |
| Picker footer | The stock picker footer renders **Today on the left and Clear on the right**, both as plain text buttons. |
| Timezone | Rendered **below the input**, as muted text in the field's footer row — and only when the field *also* carries a hint, because that footer row is drawn only when there is a hint or a required error to show. On such fields the hint is displaced into the label as a tooltip. On a field with a timezone but **no** hint, the timezone is never shown at all. |
| Today action | Sets the date from the **browser's clock**, not the server's, so on a server in another timezone it can set the wrong day. |

Conversion between the server timezone and UTC already works and is not changed by this feature.
The timezone change is a relocation of an existing label: it moves from under the input into the
picker footer. The *Today* shortcut is the one timezone-related defect — it reads the browser's
clock instead of the server's.

All three field types are rendered by the **same** field component, so one change covers all three.

### Out of scope

- Standardizing hint and required-error presentation across all field types — owned by #37464. **This feature owns the hint's placement on the three calendar field types only**, because FR-009 moves it: the regression test for that placement belongs to this issue, not to #37464, which keeps the cross-field-type consolidation and the styling.
- Moving the timezone to a secondary line under the **label**.
- Any change to how values are **stored** or **converted** between the server timezone and UTC.
- The legacy (non-new) Edit Contentlet screen.
- The picker's header, day grid and time spinner.
- Date/time fields rendered anywhere outside the new Edit Contentlet form.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Empty a date I no longer want (Priority: P1)

A content author has set a *Posting Date* on a piece of content and then decides the content should
not carry one. Today their only options are to leave a wrong date in place or to abandon the entry
and start over. They need to clear the field in place.

**Why this priority**: This is the only item of the three that is a *capability gap* rather than a
presentation one — there is currently no path at all to the outcome. It is also independently
valuable: shipping only this already removes a dead end.

**Independent Test**: Open any content with a Date, Time or Date-and-time field that holds a value,
clear it from the field, save, and reopen — the field is empty.

**Acceptance Scenarios**:

1. **Given** a *Date and time* field holding `09/04/2026 09:33`, **When** the author activates the clear control on the field, **Then** the field displays no value and the content is considered edited (unsaved-changes tracking reacts).
2. **Given** a *Date* field with no value, **When** the author looks at the field, **Then** no clear control is offered.
3. **Given** a **required** *Time* field holding a value, **When** the author clears it, **Then** the field is empty and flagged as invalid — clearing is not blocked because the field is required.
4. **Given** a field that is **disabled**, **When** the author looks at it, **Then** no clear control is offered regardless of whether it holds a value.
5. **Given** the content type's **expire-date** field holding a value, **When** the author clears it, **Then** the field shows its *Never Expires* placeholder and offers exactly **one** clear control, not two.
6. **Given** the author is navigating with the keyboard only, **When** they tab through a field that holds a value, **Then** they reach the clear control and it announces a meaningful name (not an unlabelled icon).
7. **Given** a field configured with a `defaultValue` of `now` or a fixed date, **When** the author clears it, saves the content, and reopens it, **Then** the field is still empty — the default is not re-applied over a value the author deliberately removed.

---

### User Story 2 - Timezone label moves into the picker (Priority: P2)

The timezone label already exists under the input of Date-and-time and Time fields, but it only
renders when the field also has a hint, and on those fields it pushes the hint into the label
tooltip. This story moves the label into the picker footer and returns the hint to the field footer,
where every other field type shows it.

**Why this priority**: A presentation change to an existing label; the author is not blocked without
it. Independently shippable.

**Independent Test**: Open the picker on a Date-and-time field, confirm the timezone reads in the
footer; close it and confirm nothing renders under the input; confirm the field's hint now renders
under the input as it does for a Text field.

**Acceptance Scenarios**:

1. **Given** a *Date and time* field, **When** the author opens the picker, **Then** the timezone is shown as muted, non-interactive text at the **left** of the picker footer.
2. **Given** a *Time* field, **When** the author opens the picker, **Then** the timezone is shown in the same place, in the same style.
3. **Given** a *Date* field, **When** the author opens the picker, **Then** **no** timezone is shown — the footer carries the action button alone, with no empty slot and no shift in the button's position.
4. **Given** any of the three field types, **When** the author looks at the field itself, **Then** no timezone line is rendered under the input.
5. **Given** a *Date and time* field that carries a hint, **When** the author looks at the field, **Then** the hint renders under the input in the field's footer — the same place a Text field puts it — and is **not** displaced into the label.

---

### User Story 3 - Jump to today or now in one click (Priority: P2)

An author filling a *Posting Date* usually means "now". The picker already offers a shortcut, but it
reads from the browser's clock, which disagrees with the server timezone the field stores against —
so on a server in another timezone it can set the wrong day. It also sits beside a *Clear* button
that duplicates what the field's own clear control now does.

**Why this priority**: The shortcut already exists; this corrects it and cleans up beside it. Real
value, but the author is not blocked without it.

**Independent Test**: Set the server to a timezone whose current date differs from the browser's,
click the footer button on each of the three field types, and confirm the value matches the
**server's** current date/time.

**Acceptance Scenarios**:

1. **Given** a *Date and time* field, **When** the author opens the picker, **Then** a single button sits at the **right** of the footer, reading **Today**, styled as a secondary outlined button.
2. **Given** a *Time* field, **When** the author opens the picker, **Then** that button reads **Now**.
3. **Given** a *Date* field, **When** the author opens the picker, **Then** that button reads **Today**.
4. **Given** a *Date and time* field, **When** the author clicks **Today**, **Then** the field takes the server's current date **and** the server's current time.
5. **Given** a *Date* field, **When** the author clicks **Today**, **Then** the field takes the server's current date.
6. **Given** a *Time* field, **When** the author clicks **Now**, **Then** the field takes the server's current time.
7. **Given** the browser and the server are in timezones where it is a different calendar day, **When** the author clicks the button, **Then** the value set is the **server's** day, not the browser's.
8. **Given** the author clicks the button, **When** the picker settles, **Then** the field shows the new value and the content is considered edited.
9. **Given** any of the three field types, **When** the author clicks **Today** / **Now**, **Then** the picker stays **open**, the same as when a day or time is selected.
10. **Given** any of the three field types, **When** the author opens the picker, **Then** there is **no** *Clear* button anywhere in the footer.

---

### User Story 4 - Fields line up with their neighbours (Priority: P3)

An author scanning a form sees the Date field stopping short of the column edge while every field
above and below it reaches it, which reads as a rendering defect.

**Why this priority**: Purely visual, blocks nothing, and is the smallest of the four. Last.

**Independent Test**: Place a Date field next to a Text field in the same column and compare their
right edges, in a single-column and in a multi-column layout.

**Acceptance Scenarios**:

1. **Given** a form column containing a Text field and a Date field, **When** the author views it, **Then** both reach the same right edge.
2. **Given** a Date, Time or Date-and-time field at full width, **When** the author views it, **Then** the input and its calendar/clock trigger still read as one joined control, with the trigger flush against the right edge.
3. **Given** the author focuses the field, **When** the focus ring appears, **Then** it encloses input **and** trigger as a single unit, as it does today.
4. **Given** a multi-column layout, **When** the author views the form, **Then** each field fills 100% of its own column and never overflows it.

---

### Edge Cases

- **Clearing a field that carries a default value.** A field configured with a default (`now` or a fixed date) is populated on open; clearing it must leave it empty rather than re-applying the default.
- **Clearing the expire-date field.** This field has its own placeholder semantics (*Never Expires*) and already had a clear control; it must end up with exactly one, behaving identically to the others.
- **Typing a value directly** into the input rather than picking it: the clear control must appear once the input holds a value, and disappear when it is emptied by hand.
- **The action button on a field that already holds a value**: it overwrites both date and time on a Date-and-time field, not just the missing half.
- **A required field cleared and left empty on save**: validation must block the save exactly as it would for a required field never filled.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Width

- **FR-001**: Date, Time and Date-and-time fields MUST occupy the full width of the column they are rendered in, matching the other field types in the same form.
- **FR-002**: At full width the input and its calendar/clock trigger MUST remain visually joined as one control, with the trigger flush against the field's right edge.
- **FR-003**: The focus ring MUST continue to enclose input and trigger as a single unit, and the invalid-state border MUST continue to render on both.
- **FR-004**: FR-001 through FR-003 MUST hold in single-column and multi-column form layouts: the field is always 100% of the width of its column.

#### Clearing on the field

- **FR-005**: All three field types MUST offer a clear control positioned inside the field, between the value and the calendar/clock trigger.
- **FR-006**: The clear control MUST be rendered **only** when the field holds a value, and MUST NOT be rendered when the field is empty or disabled. *(The issue's acceptance criterion also names a read-only state. These fields have no read-only state, so that part of the criterion is not valid.)*
- **FR-007**: Activating the clear control MUST empty the displayed value, set the underlying value to none, and mark the field as touched and dirty so validation and unsaved-change tracking react. Clearing MUST NOT be suppressed on required fields — a cleared required field is empty and invalid.
- **FR-007a**: The clear control MUST be reachable by keyboard and MUST carry an accessible name.
- **FR-007b**: The expire-date field MUST continue to show its *Never Expires* placeholder when cleared, and MUST offer exactly one clear control.

#### Picker footer — timezone

- **FR-008**: Opening the picker on a **Date and time** or **Time** field MUST show the system timezone as muted, non-interactive text at the left of the picker footer. It MUST be the same timezone value shown under the input today.
- **FR-008a**: Opening the picker on a **Date** field MUST NOT show timezone text.
- **FR-008b**: On the field types that show it, the timezone MUST be conveyed to assistive technology while the author is choosing a value — it MUST NOT be reachable only by a user who happens to navigate to the picker's footer. The mechanism is left to implementation; what is required is that a screen-reader user learns which timezone the value is read in without hunting for it. *(The label carries `role="status"` and `aria-live="polite"` under the input today; moving it into an overlay changes when and how it should be announced, so those attributes are not simply carried over.)*
- **FR-009**: The timezone line under the input MUST be removed from the field types that render one today — Date-and-time and Time-only; a Date-only field has none to remove — and the field's hint MUST render in the field footer under the input — the same placement the other field types use — rather than being displaced into the label.
- **FR-009a**: When a required field is invalid **and** carries a hint, the footer MUST show **both**, required error first and hint below it — not one in place of the other. The hint says what to enter, which is precisely what the author needs while the field is in error, so the error MUST NOT evict it. *(The footer renders one or the other today; this is the only presentation rule this feature settles, and only for these three field types — see Out of scope.)*

#### Picker footer — Today / Now

- **FR-010**: The picker footer MUST hold exactly one button, at its right, styled as a secondary **outlined** button using the shared button styling rather than bespoke CSS.
- **FR-011**: That button MUST read **Today** on Date and Date-and-time fields, and **Now** on Time-only fields.
- **FR-012**: Activating it MUST set: today's date and the current time on a Date-and-time field; today's date on a Date-only field; the current time on a Time-only field. "The current time" is the server's time at the moment of activation, resolved to **second** precision — the same precision the field already applies to a `now` default.
- **FR-013**: The value it sets MUST be interpreted in the **server's timezone**, through the field's existing `getCurrentServerTime` / `convertServerTimeToUtc` path — not in the browser's. To be precise about what this does and does not fix: the *instant* still comes from the browser's clock (`getCurrentServerTime` starts at `new Date()`), and this feature does not change that — correcting clock **skew** between browser and server would need a server time source that does not exist today and is out of scope. What FR-013 corrects is the **zone** that instant is read in, which is what makes *Today* land on the server's calendar day rather than the browser's.
- **FR-014**: After activation the field MUST show the new value, the underlying value MUST update, and the field MUST be marked touched and dirty.
- **FR-014a**: Activating it MUST leave the picker open on all three field types, the same as selecting a day or a time does. The picker closes only when the author clicks outside it.
- **FR-015**: The stock **Clear** button MUST NOT appear in the picker footer for any of the three field types.
- **FR-015a**: The button label and any accessible name introduced by this feature MUST come from the localized message bundle under the existing calendar-field namespace, not from hardcoded strings.
- **FR-015b**: The footer action MUST be reachable by keyboard and MUST carry an accessible name, matching the reachability FR-007a requires of the clear control.

#### Preserved behaviour

- **FR-016**: Stored values MUST be unchanged by this feature: Date-and-time values continue to store as UTC timestamps, Date-only as UTC midnight, and Time-only against a consistent date base.
- **FR-017**: Default-value handling (`now` and fixed dates) MUST continue to work when content is **created**, and MUST NOT be re-applied to a field the author has explicitly cleared — neither within the editing session nor after the content is saved empty and reopened. A default belongs to new content: once a contentlet exists, a field stored empty MUST render empty. An empty value on existing content and an unset value on new content are different states and MUST be distinguished, even though both reach the field as "no value".
- **FR-018**: The picker MUST continue not to close on time selection, and MUST continue to render detached from the form so it is not clipped by the field's container.
- **FR-019**: Clearing, the footer timezone and the *Today* / *Now* button MUST be unconditional per field type — no new content-type or field-level setting turns them on or off.

### Key Entities

- **Temporal field**: one of Date, Time or Date-and-time on a content type. Carries a name, an optional hint, a required flag, an optional default value, and — for exactly one field per content type — the expire-date role.
- **System timezone**: the server's timezone, with a human-readable label. It determines how *Today*/*Now* resolves and what the picker footer states.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A content author can empty a Date, Time or Date-and-time field in a single action, on **100%** of such fields — up from only the expire-date field today.
- **SC-001a**: A cleared field is still empty after the content is saved and reopened, in **100%** of cases — including fields configured with a `defaultValue`, which today would repopulate on reload.
- **SC-002**: In a form containing a Date, Time or Date-and-time field beside any other field type in the same column, **0** fields stop short of the column edge, in single-column and multi-column layouts.
- **SC-003**: The timezone governing a Date-and-time or Time value is visible to the author whenever they are choosing that value, on **100%** of such fields — up from only those fields that happen to carry a hint today.
- **SC-003a**: That timezone reaches a screen-reader user too, not only a sighted one — on **100%** of Date-and-time and Time fields, without the user having to navigate to the picker's footer to find it.
- **SC-004**: A field's hint is shown to the author on **100%** of Date, Time and Date-and-time fields that define one, in the same position as every other field type.
- **SC-005**: With the server and the browser on calendar days that differ, the *Today* / *Now* shortcut resolves against the **server's** zone in **100%** of attempts across all three field types — the day on Date-only, the time on Time-only, both on Date-and-time.
- **SC-005a**: On **Date-and-time** and **Time** fields, the value the shortcut sets is resolved to the **second** in **100%** of attempts — not rounded to the minute, and not drifting from the precision a `now` default already produces. Date-only is out of its scope: FR-016 stores it as UTC midnight, so it carries no time to resolve.
- **SC-006**: The picker footer offers exactly **one** action on all three field types; the redundant *Clear* action appears **0** times.
- **SC-006a**: After using the shortcut, the picker is still open on **100%** of attempts across all three field types.
- **SC-006b**: The footer action is operable from the keyboard alone — reachable by Tab, activatable by Enter or Space — on **100%** of attempts, matching the clear control's reachability.
- **SC-007**: Every string this feature adds to the interface is translatable — **0** hardcoded user-visible strings.
- **SC-008**: Values saved before the change and read after it are identical in **100%** of cases across the three field types, including Date-only, Time-only and default-valued fields.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Date, Time and Date-and-time field presentation in the **new** Edit Contentlet — a modern surface, not legacy. The legacy Edit Contentlet screen has its own date/time rendering and is explicitly untouched.
- **Backward-compatibility expectations**: No stored data, API or content-type configuration changes. Existing content must open, display and save identically (SC-008). The only externally observable additions are new keys in the localized message bundle, which is additive. Clearing now produces empty values in fields that previously could never be emptied once set — an intended capability, and already reachable today for the expire-date field, so no consumer can be assuming these fields are non-empty.
- **Known related decisions**:
  - The server-timezone semantics of these fields (values entered and displayed in the *server's* timezone, stored as UTC) is long-standing behavior this feature must respect rather than revisit — FR-013 and FR-016 hold it fixed.
  - The joined input-plus-trigger focus ring is a deliberate existing behavior and a constraint on the width fix (FR-003), not incidental styling.
  - ADR consultation (`dotCMS/platform-adrs`): ADR-0013 is the only relevant record, and there is no conflict with it.
