# Manual QA Plan — #37340 Block Editor emoji nodes

**Issue**: [#37340](https://github.com/dotCMS/core/issues/37340) · **PR**: [#37442](https://github.com/dotCMS/core/pull/37442)

**Spec**: [spec.md](./spec.md) v2.3, AC-001 – AC-028

**Every case is Manual and starts Not Run Yet.** Nothing here has been executed by writing it.

---

## Setup

1. **Run the seeding script** to get a playground with broken content already stored:
   <https://drive.google.com/file/d/1vY6VSKThkuaN1QOjWjM4rol78jrDi_GJ/view?usp=sharing>
2. **Context video**:
   <https://github.com/user-attachments/assets/850f5424-65a0-49aa-b281-fa0b4c7c2e06>
3. Enable `FEATURE_FLAG_NEW_BLOCK_EDITOR`.

> **Read the stored JSON, not the rendered page.** The editor draws a mark run as a single `<a>`
> however many text nodes span it. That disagreement between the document and the DOM hid a wrong
> design assumption through three drafts of this spec — see research.md R10. Use the content API:
> `GET /api/content/id/<identifier>` and read the Story Block field.

---

## Authoring — nothing may create an `emoji` node

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-001 | Insert an emoji from the **toolbar dialog** | Lands as a character in the text node. No `emoji` node in the JSON | AC-005 | Not Run Yet |
| TC-002 | **Copy/paste** an emoji from outside the editor | Same — character in the text node | AC-004 | Not Run Yet |
| TC-003 | Type the **inline shortcode** `:smiley:` | Resolves to the character | AC-006 | Not Run Yet |
| TC-004 | Type the **input rule** `:) ` | Resolves to 🙂 **and the space you typed survives** | AC-007 | Not Run Yet |
| TC-005 | Type an unknown shortcode `:notreal:` | Left exactly as typed | AC-006 | Not Run Yet |

## The `:` autocomplete — new in this PR

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-006 | Type `:smi` | A dropdown opens with **5 rows max**, glyph on the left, `:name:` label | AC-024, AC-027 | Not Run Yet |
| TC-007 | Type `:rocket` | `:rocket:` is the **first** row, not `:astronaut:` (which merely tags "rocket") | AC-027 | Not Run Yet |
| TC-008 | Type a long query such as `:face_with` | Labels are cut with an ellipsis. **No horizontal scrollbar** on the list | AC-028 | Not Run Yet |
| TC-009 | Select a row with Enter, and again with the mouse | Character inserted, trigger text consumed, no `emoji` node | AC-025 | Not Run Yet |
| TC-010 | Open the menu **inside linked text** and pick a row | One `text` node carrying the `link` mark — the character joins the link | AC-026 | Not Run Yet |

## Stored content — the heal

Use the seeded cases. **The heal runs on load, so open the field and Publish.**

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-011 | Open a seeded field and **Publish without typing anything** | The repair persists. This is the fix from `b2f086c326` — before it, the heal only stuck if you also edited | AC-013 | Not Run Yet |
| TC-012 | `text + emoji` — symbol at the end of linked text | Two text nodes; the symbol is **not** part of the link | AC-016 | Not Run Yet |
| TC-013 | `link + emoji + link`, **same URL** | **One** `text` node with the `link` mark. Renders as a single `<a>` | AC-015, AC-021 | Not Run Yet |
| TC-014 | `link + emoji + link`, **different URL** | Three nodes, two `<a>`, symbol outside both | AC-016 | Not Run Yet |
| TC-015 | `link + emoji + emoji` with no trailing text | Both become text, **not** part of the link — the run's right boundary is the block edge | AC-016 | Not Run Yet |
| TC-016 | `link + emoji + emoji + link`, **same URL** | Both join the link, collapsing to **one** `text` node. The `©®` payload | AC-015 | Not Run Yet |
| TC-017 | A symbol beside an **inline image** | Symbol becomes text and stays outside the link | AC-016 | Not Run Yet |
| TC-018 | A link applied **over** an existing emoji node | Keeps its own `link` mark; the run joins into one node | AC-014 | Not Run Yet |

## The control — highest risk on this page

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-019 | A field with **no emoji at all**: open it, edit text elsewhere, save | Stored JSON **unchanged** apart from your edit. Nothing else rewritten | AC-019 | Not Run Yet |
| TC-020 | A field with no emoji: open it and **close without saving** | **No unsaved-changes prompt.** The heal must not dirty a field it had no reason to touch | AC-019 | Not Run Yet |

> A heal that rewrites content unrelated to this defect is worse than the defect. TC-019 and TC-020
> are the two cases that would catch it, and neither is about emoji.

## Recursion and field configuration

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-021 | Symbols inside a **heading, list item, blockquote and table cell** | All heal identically to a paragraph | AC-017 | Not Run Yet |
| TC-022 | A field whose **Allowed Blocks** is restricted (e.g. headings only) | Emoji dialog button is present and `:)` fires. **Both used to disappear** | AC-008 | Not Run Yet |
| TC-023 | Same restricted field | An insert group now appears where it previously collapsed. Expected, and a visible change | AC-008 | Not Run Yet |
| TC-024 | **Paste emoji HTML copied from another Block Editor field** | Lands as text. Never an `emoji` node, and never a `dotImage` pointing at `cdn.jsdelivr.net` | AC-012 | Not Run Yet |

## Server-side, fixed as a side effect

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-025 | Make a Story Block field **required**. Put only an emoji in it. Save | Saves. On `main` today this fails as "empty" — `StoryBlockUtil` counts only `text` nodes | — | Not Run Yet |
| TC-026 | Check the character count on a field containing symbols | Counts the characters. Emoji nodes counted zero | — | Not Run Yet |

## Accessibility — the customer's reported symptom

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-027 | **Tab** through the rendered link on content authored after the fix, and on a healed TC-013 field | Exactly **one** focus stop in both | AC-022 | Not Run Yet |
| TC-028 | Open the **NVDA Elements List / VoiceOver rotor** | **One** entry carrying the complete accessible name, symbol included | AC-023 | Not Run Yet |
| TC-029 | Inspect the rendered HTML for newly authored content | No `<img alt="… emoji">`, no request to `cdn.jsdelivr.net` | AC-011 | Not Run Yet |

## Downstream rendering

| ID | Case | Expected | AC | Result |
|---|---|---|---|---|
| TC-030 | Render a healed field through **VTL** | The symbol appears. It is dropped entirely today | AC-013 | Not Run Yet |
| TC-031 | Render the same field through a **JS SDK** | The symbol appears inline. No unknown-block placeholder, no `<div>` inside a `<p>` | AC-013 | Not Run Yet |

---

## Not covered here

- **Legacy Block Editor behaviour** is out of scope and unchanged. Content carrying `emoji` nodes
  opened there is a pre-existing loss path.
- **Content nobody ever opens** keeps its `emoji` nodes. The heal runs on load; that is by design.
