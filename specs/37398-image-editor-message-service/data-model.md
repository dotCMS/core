# Phase 1: Data Model — DI provider topology

**Feature**: `37398-image-editor-message-service` · **Plan**: [plan.md](./plan.md) · **Date**: 2026-09-04

This fix introduces **no domain entities, no persisted state, and no API payloads**. Nothing is
stored, migrated, or serialized differently. The "model" that matters here is the *injector
topology* — which token is provided at which level — because that topology is precisely what is
broken. It is documented in entity-like form so the change is reviewable and so the tasks phase has
an unambiguous target state.

## Entities

**None.** No new interface, model, or type is added. `ImageEditorOpenParams`, `DotCMSTempFile` and
the `ImageEditorStore` state shape are all untouched.

## Injector topology

### Token inventory

Every token the `@dotcms/image-editor` component tree injects, and where it resolves from:

| Token | Resolves from | Level | Status |
|---|---|---|---|
| `MessageService` (PrimeNG) | *nothing in the library* | ambient host — **absent in 2 of 3 hosts** | ❌ **the defect** |
| `ImageEditorStore` | `DotImageEditorComponent.providers` | component | ✅ safe |
| `ConfirmationService` (PrimeNG) | `DotImageEditorComponent.providers` | component | ✅ safe |
| `DotImageEditorService` | `@Injectable({ providedIn: 'root' })` | root | ✅ safe |
| `DotPropertiesService` | `@Injectable({ providedIn: 'root' })` | root | ✅ safe |
| `DotMessageService` | `@Injectable({ providedIn: 'root' })` | root | ✅ safe |
| `Dispatcher`, `Events` (`@ngrx/signals/events`) | root / store-scoped | root | ✅ safe |
| `DynamicDialogConfig`, `DynamicDialogRef` | PrimeNG `DialogService` on open | dialog | ✅ safe — the host must provide `DialogService`, which is the documented host requirement |
| `Dialog` (PrimeNG) | `inject(Dialog, { optional: true })` | dialog, optional | ✅ safe by construction |
| `DOCUMENT`, `NgZone`, `DestroyRef`, `ElementRef`, `HttpClient` | Angular platform | platform/root | ✅ safe |

Derived from an audit of every `inject()` call in `core-web/libs/image-editor/src` (excluding
specs). `MessageService` is the sole outlier: the only token the library requires, does not provide,
and cannot obtain from a root-provided service. This table *is* the evidence for AC-004.

### State transition: before → after

```text
BEFORE (defective)

  Host component injector
  ├─ DialogService ................. provided by host (all 3 hosts must)
  ├─ MessageService ................ provided by the SHELL ONLY  ◄── the gap
  └─ DynamicDialog (opened via the host's DialogService)
     └─ DotImageEditorComponent
        ├─ providers: ImageEditorStore, ConfirmationService
        └─ …canvas
           └─ DotImageEditorAddressBarComponent
              └─ inject(MessageService) ─── walks UP the host chain
                                            ├─ shell host   → found    → renders
                                            └─ other hosts  → NOT FOUND → NG0201, blank dialog

AFTER (self-sufficient)

  Host component injector
  ├─ DialogService ................. provided by host (unchanged requirement)
  └─ DynamicDialog
     └─ DotImageEditorComponent
        ├─ providers: ImageEditorStore, ConfirmationService, MessageService  ◄── the fix
        ├─ template: <dot-toast position="top-center" />                     ◄── the outlet
        └─ …canvas
           └─ DotImageEditorAddressBarComponent
              └─ inject(MessageService) ─── resolves at the EDITOR, never reaching the host
                                            → found in every host → renders everywhere
```

The resolution now terminates inside the library, so host composition can no longer affect it. The
`inject(MessageService)` call site in `dot-image-editor-address-bar.component.ts:37` is **not
modified** — only what it resolves against changes.

### Validation rules

Rules the target state must satisfy, each traceable to an acceptance criterion:

| Rule | AC |
|---|---|
| `MessageService` appears in `DotImageEditorComponent.providers` | AC-001 |
| Exactly **one** `dot-toast` outlet exists inside the editor dialog | AC-001 |
| `DotImageEditorComponent` instantiates when no ancestor injector provides `MessageService` | AC-010 |
| `MessageService` is **not** added to any host's `providers` for the editor's benefit | AC-003 |
| `edit-content.shell.component.ts` keeps its own `MessageService` + `<p-toast />` | AC-003, AC-007 |
| No token in the inventory above is both library-required and unprovided | AC-004 |

## Host provider matrix

The second, quieter half of the fix. `IMAGE_EDITOR_LAUNCHER` is injected `{ optional: true }` by
`DotFileFieldComponent`, so a missing provider is legal at the type level and degrades silently.

| Host | `DialogService` | `IMAGE_EDITOR_LAUNCHER` | `ASSET_PICKER_LAUNCHER` | `MessageService` (own use) |
|---|---|---|---|---|
| `EditContentShellComponent` (full-screen route) | ✅ keep | ✅ keep | ✅ keep | ✅ **keep** — for the shell's own messages, not the editor's |
| `DotEditContentSidePanelComponent` (UVE, Content Drive, Query Tool) | ✅ keep | ✅ keep | ✅ keep | ➖ not needed |
| `DotEditContentDialogComponent` (UVE, side-panel flag off) | ➕ **add** | ➕ **add** | ✅ keep | ➖ not needed |

`MessageService` is deliberately absent from the "add" column everywhere: after this change no host
provides it *for the editor*. That is the invariant AC-003 protects, and the reason the fix does not
regress the moment a fourth host appears.

**Consequence to review, not a defect**: a component-scoped `MessageService` shadows the shell's
ambient one for the editor's subtree, so in the full-screen route editor toasts move from the
shell's `<p-toast />` to the editor's own outlet. Intended; called out because it is the only
visible change on an otherwise-unaffected host.
