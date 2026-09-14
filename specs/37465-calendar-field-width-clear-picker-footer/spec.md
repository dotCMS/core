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
opens when the user picks a value carries a footer that was never designed for this product: it
offers a *Clear* action the content author does not need there, and hides the one piece of
information they do need — which timezone the value is being interpreted in.

This feature brings all three into line: **full-column width**, **a clear control on the field
itself**, and **a picker footer that states the timezone and offers a single *Today* / *Now*
action**.

### Verified current behaviour

Confirmed by reading the code on `origin/main` (not quoted from the issue):

| Aspect | What happens today |
|---|---|
| Width | The picker control sizes to its content, so it stops short of the column edge while Text, Select and the other field types stretch to fill it. |
| Clearing | A clear (X) control appears **only** on the field designated as the content type's *expire date*. Every other Date/Time/Date-and-time field, once given a value, can never be emptied through the UI. |
| Picker footer | The stock picker footer renders **Today on the left and Clear on the right**, both as plain text buttons. |
| Timezone | Rendered **below the input**, as muted text in the field's footer row — and only when the field *also* carries a hint, because that footer row is drawn only when there is a hint or a required error to show. On such fields the hint is displaced into the label as a tooltip. On a field with a timezone but **no** hint, the timezone is never shown at all. |
| Today action | Sets the date from the **browser's clock**, not the server's, so on a server in another timezone it can set the wrong day. |

The last two rows matter beyond cosmetics: the timezone — the single piece of context that makes a
stored timestamp interpretable — is today shown only by accident of the field having a hint, and the
*Today* shortcut disagrees with the timezone the rest of the field is built around.

All three field types are rendered by the **same** field component, so one change covers all three.

### Relationship to #37464

[#37464](https://github.com/dotCMS/core/issues/37464) (*Standardize field hint and required-error
presentation*) is **open and unstarted** — it has no spec and no branch. It contains one criterion
saying that when a Date/Time field has both a timezone and a hint, only the hint is shown. This
feature **removes the timezone from the field footer entirely**, so that collision cannot occur and
that criterion becomes moot.

This spec assumes **#37465 lands first**. On merge, the now-moot criterion must be withdrawn from
#37464 — see [FR-016](#fr-016). If #37464 lands first instead, this spec's FR-008/FR-009 still
apply unchanged; only the withdrawal direction reverses.

### Divergence from the issue's acceptance criteria

The issue specifies a picker footer holding **one** button — *Today* / *Now* — with the value
applied the moment the author selects it. During spec review the developer added a second action:
an **Accept** button that **confirms** the selection on Date-and-time and Time-only fields. On
those two types the picker no longer writes to the field as the author clicks; it holds a pending
selection that reaches the field only on *Accept*.

That is a deliberate scope addition, not a reading of the issue, and it changes behaviour the issue
described as settled. Its shape was decided with the developer:

| Question | Decision |
|---|---|
| Which types get the confirm gate? | **Date-and-time and Time-only only.** Date-only keeps today's one-click flow — selecting a day applies the value and closes — so the most common case gains no extra click and shows no *Accept* button. |
| Dismissing without *Accept* (Escape, click outside)? | **The pending selection is discarded** and the field keeps the value it had before the picker opened. |

**#37465's acceptance criteria still describe the single-button footer and must be corrected** —
see [FR-016a](#cross-issue-bookkeeping).

### Out of scope

- Hint and required-error **styling and wording** — owned by #37464. This feature only relocates the hint back into the field footer (FR-009); how it looks there is #37464's call.
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
4. **Given** a field that is **disabled or read-only**, **When** the author looks at it, **Then** no clear control is offered regardless of whether it holds a value.
5. **Given** the content type's **expire-date** field holding a value, **When** the author clears it, **Then** the field shows its *Never Expires* placeholder and offers exactly **one** clear control, not two.
6. **Given** the author is navigating with the keyboard only, **When** they tab through a field that holds a value, **Then** they reach the clear control and it announces a meaningful name (not an unlabelled icon).

---

### User Story 2 - See which timezone my value is in, when it matters (Priority: P1)

An author entering a *Date and time* needs to know whether `09:33` means 9:33 on their own clock or
on the server's. Today that information appears under the input only when the field happens to carry
a hint — so on most fields it is simply absent — and when it does appear it costs the field its hint.
Moving it into the picker puts it exactly where the author is looking as they choose the value, and
gives the hint its normal place back.

**Why this priority**: Without it, a timestamp is ambiguous, and the current placement means the
information is missing on most fields and displacing the hint on the rest. Independently shippable.

**Independent Test**: Open the picker on a Date-and-time field, confirm the timezone reads in the
footer; close it and confirm nothing renders under the input; confirm the field's hint now renders
under the input as it does for a Text field.

**Acceptance Scenarios**:

1. **Given** a *Date and time* field, **When** the author opens the picker, **Then** the timezone is shown as muted, non-interactive text at the **left** of the picker footer.
2. **Given** a *Time* field, **When** the author opens the picker, **Then** the timezone is shown in the same place, in the same style.
3. **Given** a *Date* field, **When** the author opens the picker, **Then** **no** timezone is shown — the footer carries the action button alone, with no empty slot and no shift in the button's position.
4. **Given** the system timezone is unavailable, **When** the author opens the picker on any of the three types, **Then** the footer renders the action button alone, with no blank gap and no change in the footer's height.
5. **Given** any of the three field types, **When** the author looks at the field itself, **Then** no timezone line is rendered under the input.
6. **Given** a *Date and time* field that carries a hint, **When** the author looks at the field, **Then** the hint renders under the input in the field's footer — the same place a Text field puts it — and is **not** displaced into the label.

---

### User Story 3 - Jump to today or now, and commit when I mean to (Priority: P2)

An author filling a *Posting Date* usually means "now". The picker already offers a shortcut, but it
reads from the browser's clock, which disagrees with the server timezone the field stores against —
so on a server in another timezone it can set the wrong day. It also sits beside a *Clear* button
that duplicates what the field's own clear control now does.

Separately, on the two types that carry a time the author sets the value in two moves — pick a day,
then adjust the hour — and today each move writes straight to the field. There is no point at which
the author says "this is the one", and no way to back out of a half-made change except by retyping
the old value. An **Accept** button gives them that point.

**Why this priority**: The shortcut already exists; this corrects it and cleans up beside it. The
confirm gate makes the two-move flow deliberate. Real value, but the author is not blocked without
either.

**Independent Test**: Set the server to a timezone whose current date differs from the browser's,
use the footer button on each of the three field types, and confirm the value matches the
**server's** current date/time. Then, on a Date-and-time field, make a selection, dismiss without
*Accept*, and confirm the field kept its original value.

**Acceptance Scenarios — Today / Now**:

1. **Given** a *Date and time* field, **When** the author opens the picker, **Then** a **Today** button sits at the right of the footer, styled as a secondary outlined button.
2. **Given** a *Time* field, **When** the author opens the picker, **Then** that button reads **Now**.
3. **Given** a *Date* field, **When** the author opens the picker, **Then** that button reads **Today** and is the footer's **only** button.
4. **Given** a *Date* field, **When** the author uses **Today**, **Then** the field takes the server's current date, the content is considered edited, and the picker closes.
5. **Given** a *Date and time* field, **When** the author uses **Today**, **Then** the picker's pending selection becomes the server's current date **and** current time — the field itself does not change yet.
6. **Given** a *Time* field, **When** the author uses **Now**, **Then** the picker's pending selection becomes the server's current time — the field itself does not change yet.
7. **Given** the browser and the server are in timezones where it is a different calendar day, **When** the author uses the button, **Then** the day resolved is the **server's**, not the browser's.
8. **Given** any of the three field types, **When** the author opens the picker, **Then** there is **no** *Clear* button anywhere in the footer.

**Acceptance Scenarios — Accept**:

9. **Given** a *Date and time* or *Time* field, **When** the author opens the picker, **Then** an **Accept** button sits to the right of *Today* / *Now*, styled as the footer's primary action.
10. **Given** a *Date* field, **When** the author opens the picker, **Then** there is **no** *Accept* button — selecting a day applies the value and closes the picker, as it does today.
11. **Given** a *Date and time* field holding `09/04/2026 09:33`, **When** the author selects `15/04/2026` and sets the time to `14:00`, **Then** the field still reads `09/04/2026 09:33` until *Accept* is used.
12. **Given** that pending selection, **When** the author uses **Accept**, **Then** the field takes `15/04/2026 14:00`, the content is considered edited, and the picker closes.
13. **Given** that pending selection, **When** the author dismisses the picker with Escape or by clicking outside, **Then** the field still reads `09/04/2026 09:33` and the content is **not** considered edited.
14. **Given** a *Date and time* field, **When** the author opens the picker, changes nothing, and uses **Accept**, **Then** the picker closes and the content is **not** marked as edited.
15. **Given** the author is navigating by keyboard, **When** they reach the footer, **Then** both buttons are reachable and each announces a meaningful name.

---

### User Story 4 - Fields line up with their neighbours (Priority: P3)

An author scanning a form sees the Date field stopping short of the column edge while every field
above and below it reaches it, which reads as a rendering defect.

**Why this priority**: Purely visual, blocks nothing, and is the smallest of the four. Last.

**Independent Test**: Place a Date field next to a Text field in the same column and compare their
right edges at several viewport widths.

**Acceptance Scenarios**:

1. **Given** a form column containing a Text field and a Date field, **When** the author views it, **Then** both reach the same right edge.
2. **Given** a Date, Time or Date-and-time field at full width, **When** the author views it, **Then** the input and its calendar/clock trigger still read as one joined control, with the trigger flush against the right edge.
3. **Given** the author focuses the field, **When** the focus ring appears, **Then** it encloses input **and** trigger as a single unit, as it does today.
4. **Given** a multi-column layout and a narrow viewport, **When** the author views the form, **Then** the field fills its column in both and never overflows it.

---

### Edge Cases

- **Timezone arrives late.** The system timezone is loaded asynchronously and can be absent on first render. The footer must tolerate it appearing after the picker has already been opened, without a layout jump.
- **Clearing a field that carries a default value.** A field configured with a default (`now` or a fixed date) is populated on open; clearing it must leave it empty rather than re-applying the default.
- **Clearing the expire-date field.** This field has its own placeholder semantics (*Never Expires*) and already had a clear control; it must end up with exactly one, behaving identically to the others.
- **Typing a value directly** into the input rather than picking it: the clear control must appear once the input holds a value, and disappear when it is emptied by hand.
- **The action button on a field that already holds a value**: it overwrites both date and time on a Date-and-time field, not just the missing half.
- **Clearing the field while the picker is open** on a Date-and-time or Time-only field: the interaction between the field's X and a pending selection must be decided rather than left to chance — clearing empties the field, and reopening or accepting must not resurrect the discarded selection.
- **Accept on a field whose value was never set**: the author opens an empty Date-and-time field, picks nothing, and accepts — the field stays empty rather than defaulting to the date the picker happened to be showing.
- **Dismissing the picker by switching to another field** (tabbing away, clicking a different input) counts as a dismissal without *Accept* and discards the pending selection like Escape does.
- **Reopening after a discard**: the picker must reopen showing the field's actual value, not the selection the author abandoned.
- **A required field cleared and left empty on save**: validation must block the save exactly as it would for a required field never filled.
- **Very long timezone labels** in a narrow picker: the footer must not push the action button out of the overlay.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Width

- **FR-001**: Date, Time and Date-and-time fields MUST occupy the full width of the column they are rendered in, matching the other field types in the same form.
- **FR-002**: At full width the input and its calendar/clock trigger MUST remain visually joined as one control, with the trigger flush against the field's right edge.
- **FR-003**: The focus ring MUST continue to enclose input and trigger as a single unit, and the invalid-state border MUST continue to render on both.
- **FR-004**: FR-001 through FR-003 MUST hold in single-column and multi-column form layouts and at narrow viewport widths.

#### Clearing on the field

- **FR-005**: All three field types MUST offer a clear control positioned inside the field, between the value and the calendar/clock trigger.
- **FR-006**: The clear control MUST be rendered **only** when the field holds a value, and MUST NOT be rendered when the field is empty, disabled or read-only.
- **FR-007**: Activating the clear control MUST empty the displayed value, set the underlying value to none, and mark the field as touched and dirty so validation and unsaved-change tracking react. Clearing MUST NOT be suppressed on required fields — a cleared required field is empty and invalid.
- **FR-007a**: The clear control MUST be reachable by keyboard and MUST carry an accessible name.
- **FR-007b**: The expire-date field MUST continue to show its *Never Expires* placeholder when cleared, and MUST offer exactly one clear control.

#### Picker footer — timezone

- **FR-008**: Opening the picker on a **Date and time** or **Time** field MUST show the system timezone as muted, non-interactive text at the left of the picker footer. It MUST be the same timezone value shown under the input today.
- **FR-008a**: Opening the picker on a **Date** field MUST NOT show timezone text. When the timezone is unavailable for any type, nothing MUST be rendered in its place — no empty slot, no change in the footer's height or in the action button's position.
- **FR-009**: The timezone line under the input MUST be removed for all three field types, and the field's hint MUST render in the field footer under the input — the same placement the other field types use — rather than being displaced into the label.

#### Picker footer — actions

- **FR-010**: The picker footer's actions MUST be grouped at its right, after the timezone text, using the shared button styling rather than bespoke CSS:
  - **Date-only**: one button, **Today**, styled as secondary **outlined**.
  - **Date-and-time and Time-only**: two buttons in this order — **Today** / **Now** styled as secondary **outlined**, then **Accept** styled as the footer's primary action.
- **FR-011**: The first button MUST read **Today** on Date and Date-and-time fields, and **Now** on Time-only fields.
- **FR-012**: **Today** / **Now** MUST resolve to today's date on a Date-only field, today's date and the current time on a Date-and-time field, and the current time on a Time-only field.
- **FR-013**: The value it resolves MUST be derived from the **server** timezone through the field's existing server-time path, never from the browser's clock.
- **FR-015**: The stock **Clear** button MUST NOT appear in the picker footer for any of the three field types.
- **FR-015a**: Every button label and accessible name introduced by this feature — including **Accept** — MUST come from the localized message bundle under the existing calendar-field namespace, not from hardcoded strings.
- **FR-015b**: Both footer buttons MUST be reachable by keyboard and MUST carry an accessible name.

#### Picker footer — when the value reaches the field

- **FR-014**: On a **Date-only** field the picker MUST continue to behave as it does today: selecting a day — or using **Today** — applies the value to the field, marks it touched and dirty, and closes the picker. Date-only fields MUST NOT show an *Accept* button.
- **FR-014a**: On **Date-and-time** and **Time-only** fields the picker MUST hold the author's choices as a **pending selection**. Selecting a day, adjusting the time, or using **Today** / **Now** MUST change only that pending selection — the field's displayed value and underlying value MUST NOT change while the picker is open.
- **FR-014b**: Using **Accept** on those two types MUST apply the pending selection to the field, mark it touched and dirty, and close the picker.
- **FR-014c**: Dismissing the picker on those two types **without** using *Accept* — by Escape, by clicking outside, or by any other dismissal — MUST discard the pending selection. The field MUST keep the value it held when the picker opened, and MUST NOT be marked touched or dirty by the discarded selection.
- **FR-014d**: Using **Accept** when the author has made no selection MUST close the picker without changing the field's value and without marking it touched or dirty.
- **FR-014e**: The confirm gate MUST apply only to selections made **inside the picker**. A value the author types directly into the field's input MUST continue to reach the field as it does today, without requiring *Accept*.

#### Cross-issue bookkeeping

- **FR-016**: On merge, the criterion in #37464 governing the timezone-versus-hint collision MUST be withdrawn from that issue, since FR-009 removes the collision. *(Bookkeeping, not code — see [Relationship to #37464](#relationship-to-37464). Withdrawal from a GitHub issue requires developer approval; this requirement records the obligation, it does not authorize the edit.)*
- **FR-016a**: #37465's own acceptance criteria MUST be corrected to describe the two-button footer and the confirm gate. As written they specify a single-button footer and a value applied on selection, which FR-010 and FR-014a–FR-014e supersede. Specifically: the criteria stating that *"the footer's right slot holds a single button"*, that **Today** *"sets"* the value on Date-and-time and Time-only fields, and that *"after clicking the button the field shows the new value"* no longer hold for those two types. *(Same standing as FR-016: records the obligation, does not authorize the edit.)*

#### Preserved behaviour

- **FR-017**: Stored values MUST be unchanged by this feature: Date-and-time values continue to store as UTC timestamps, Date-only as UTC midnight, and Time-only against a consistent date base.
- **FR-018**: Default-value handling (`now` and fixed dates) MUST continue to work, and MUST NOT be re-applied to a field the author has explicitly cleared.
- **FR-019**: The picker MUST continue not to close on time selection, and MUST continue to render detached from the form so it is not clipped by the field's container.

### Key Entities

- **Temporal field**: one of Date, Time or Date-and-time on a content type. Carries a name, an optional hint, a required flag, an optional default value, and — for exactly one field per content type — the expire-date role.
- **System timezone**: the server's timezone, with a human-readable label. Loaded asynchronously and may be absent. It determines how *Today*/*Now* resolves and what the picker footer states.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A content author can empty a Date, Time or Date-and-time field in a single action, on **100%** of such fields — up from only the expire-date field today.
- **SC-002**: In a form containing a Date, Time or Date-and-time field beside any other field type in the same column, **0** fields stop short of the column edge, at every supported viewport width.
- **SC-003**: The timezone governing a Date-and-time or Time value is visible to the author whenever they are choosing that value, on **100%** of such fields — up from only those fields that happen to carry a hint today.
- **SC-004**: A field's hint is shown to the author on **100%** of Date, Time and Date-and-time fields that define one, in the same position as every other field type.
- **SC-005**: With the server and the browser on calendar days that differ, the *Today* / *Now* shortcut resolves to the **server's** day and time in **100%** of attempts across all three field types.
- **SC-006**: The picker footer offers exactly **one** action on Date-only fields and **two** on Date-and-time and Time-only fields; the redundant *Clear* action appears **0** times on all three.
- **SC-006a**: On Date-and-time and Time-only fields, a selection abandoned in the picker reaches the field **0%** of the time — in **100%** of dismissals without *Accept*, the field still holds the value it had when the picker opened and the content is not flagged as edited.
- **SC-006b**: Setting a value on a Date-only field still takes **one** interaction, unchanged by this feature — the confirm gate adds no step to the most common case.
- **SC-007**: Every string this feature adds to the interface is translatable — **0** hardcoded user-visible strings.
- **SC-008**: Values saved before the change and read after it are identical in **100%** of cases across the three field types, including Date-only, Time-only and default-valued fields.

---

## Legacy Considerations *(dotCMS-specific — mandatory)*

- **Existing behavior touched**: The Date, Time and Date-and-time field presentation in the **new** Edit Contentlet — a modern surface, not legacy. The legacy Edit Contentlet screen has its own date/time rendering and is explicitly untouched.
- **Backward-compatibility expectations**: No stored data, API or content-type configuration changes. Existing content must open, display and save identically (SC-008). The only externally observable additions are new keys in the localized message bundle, which is additive. Clearing now produces empty values in fields that previously could never be emptied once set — an intended capability, and already reachable today for the expire-date field, so no consumer can be assuming these fields are non-empty.
- **Known related decisions**:
  - The server-timezone semantics of these fields (values entered and displayed in the *server's* timezone, stored as UTC) is long-standing behavior this feature must respect rather than revisit — FR-013 and FR-017 hold it fixed.
  - The joined input-plus-trigger focus ring is a deliberate existing behavior and a constraint on the width fix (FR-003), not incidental styling.
  - Overlap with #37464 as described above.
  - `/speckit-plan` will formally consult `dotCMS/platform-adrs`.

---

## Assumptions

1. **#37465 lands first.** #37464 is open with no spec and no branch, so this spec is written as if the hint/error standardization has not happened. FR-009 puts the hint back in the field footer; #37464 later decides how it looks there.
2. **Hint returns to the field footer** rather than remaining a label tooltip — confirmed with the developer, and consistent with the issue's statement that the field footer carries hint and required-error text only.
3. **The confirm gate is scoped to Date-and-time and Time-only** — confirmed with the developer. Applying it to all three types was considered and rejected: it would turn the most common case, setting a plain date, from one interaction into two. Date-only therefore keeps today's behaviour exactly and shows no *Accept* button.
3a. **Dismissing without *Accept* discards the pending selection** — confirmed with the developer, and the normal semantics of a confirm gate. The cost is real and accepted: an author used to today's behaviour can lose a selection by clicking outside. The alternative, applying it anyway, was rejected because it would make *Accept* a close button wearing another name.
3b. ***Accept* is styled as the footer's primary action** and *Today* / *Now* stays secondary outlined, so the confirming action is the visually dominant one. The issue only specified the styling of *Today*; this is an inference from *Accept* being the commit step, and is worth a reviewer's eye.
3c. ***Accept* is always enabled**, including when nothing has been selected, in which case it simply closes the picker (FR-014d). Disabling it until a selection exists was considered and rejected as a state the author cannot explain to themselves.
4. **Clearing is a value-level action, not a validation bypass.** A cleared required field is empty and invalid, and saving is blocked exactly as for a required field never filled.
5. **The timezone in the footer is display-only.** It is not selectable, not editable, and does not change what the field stores.
6. **"Current time" means the server's current time** at the moment the button is activated, at second precision, consistent with how the field already resolves a `now` default.
7. **New message-bundle keys** are added under the existing calendar-field namespace; no existing key is reused or repurposed, and no existing key is removed.
8. **No new field-level configuration.** Clearing, the footer timezone and the *Today*/*Now* button are unconditional per field type — a content-type editor cannot turn them off.
