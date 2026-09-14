# Contract: What a host must provide to embed `@dotcms/image-editor`

**Feature**: `37398-image-editor-message-service` · **Plan**: [../plan.md](../plan.md) · **Date**: 2026-09-04

`@dotcms/image-editor` exposes no REST endpoint, CLI, or public HTTP surface. The one contract it
*does* have is a **dependency-injection contract** with its embedding host: the set of tokens a host
must supply for `DotImageEditorComponent` to instantiate. That contract was implicit and undocumented,
which is why two of three hosts violated it without anyone noticing. This document states it
explicitly, before and after the fix, so a future host has something to conform to and a reviewer has
something to check against.

## Consumer-facing surface (unchanged)

```ts
// @dotcms/image-editor — public API, NOT modified by this fix
export class DotImageEditorComponent { /* opened via DialogService, never used as <dot-image-editor> */ }
export interface DotImageEditorLauncher {
    isAvailable(): boolean;
    open(params: ImageEditorOpenParams): Observable<DotCMSTempFile | null>;
}
export interface ImageEditorOpenParams {
    inode?: string;
    tempId?: string;
    variable: string;
    fieldName: string;
}
```

No signature, input, output, or exported type changes. A host that compiles today compiles after.

## The host DI contract

### Before (implicit, violated)

| Token | Required? | Enforced how? | Failure when missing |
|---|---|---|---|
| `DialogService` (PrimeNG) | **yes** | `AngularImageEditorLauncher` injects it | Launcher cannot be constructed |
| `MessageService` (PrimeNG) | **yes, undocumented** | nothing — resolved by injector walk at construction time | `NG0201` thrown mid-construction → **blank dialog, editor unusable** |

The second row is the whole defect. It was a requirement in fact but not in any type, docstring, or
test, so it could only be discovered by running each host by hand.

### After (explicit, satisfiable)

| Token | Required? | Provided by | Notes |
|---|---|---|---|
| `DialogService` (PrimeNG) | **yes** | the host | Unchanged. The launcher opens the editor through the host's instance, which is what makes the dialog's injector chain the host's chain. |
| `MessageService` (PrimeNG) | **no** | `DotImageEditorComponent` itself | Now terminates inside the library. A host may still provide one for its *own* messages — the editor's component-scoped instance shadows it for the editor subtree. |

**Post-fix invariant**: `DialogService` is the *only* token a host must provide to embed the image
editor. Anything else the editor needs, the editor provides.

### Conformance

A conforming host:

1. Provides `DialogService` in its component `providers`.
2. Provides `{ provide: IMAGE_EDITOR_LAUNCHER, useClass: AngularImageEditorLauncher }` if its
   Image/File/Binary fields should use the new editor rather than falling back to legacy Dojo.
3. Provides **nothing** on the editor's behalf beyond that.

Verified by the AC-012 assertions — one per host spec, checking `IMAGE_EDITOR_LAUNCHER` resolves to
an `AngularImageEditorLauncher` and is not `undefined`. Non-conformance on point 2 is *silent* by
design (`inject(IMAGE_EDITOR_LAUNCHER, { optional: true })` in `DotFileFieldComponent`), which is
exactly why it needs a test rather than trust.

## The launcher token contract (`IMAGE_EDITOR_LAUNCHER`)

```ts
// libs/edit-content/src/lib/fields/shared/image-editor-launcher/image-editor-launcher.token.ts
export const IMAGE_EDITOR_LAUNCHER = new InjectionToken<DotImageEditorLauncher>('IMAGE_EDITOR_LAUNCHER');
```

| Consumer | Injection | Behavior when unprovided |
|---|---|---|
| `DotFileFieldComponent` (`dot-file-field.component.ts:131`) | `inject(IMAGE_EDITOR_LAUNCHER, { optional: true })` | Falls back to the legacy Dojo image editor |

The optionality is **intentional and preserved**: the field also renders outside the new Edit
Content (inside the `dotcms-binary-field` web component in the legacy JSP editor), where no Angular
launcher exists and the Dojo fallback is the correct behavior. This fix does not make the token
required; it makes the three Angular hosts provide it, so the fallback fires only where it should.

## Toast contract

Established by `DotToastComponent`'s own docstring
(`libs/ui/src/lib/components/dot-toast/dot-toast.component.ts`) and implemented by
`DotAssetPickerComponent`:

> *"The `MessageService` stays with the consumer: provide it on the host so each outlet — a portlet
> shell, a dialog — keeps its own message stream instead of sharing one globally."*

| Element | Value |
|---|---|
| Provider scope | The component that owns the outlet — here `DotImageEditorComponent` |
| Outlet | `<dot-toast position="top-center" />`, one per provider |
| Precedent | `dot-asset-picker.component.ts:97` (provider) + `dot-asset-picker.component.html:129` (outlet) |
| Anti-pattern | A root/application-wide `MessageService` shared by unrelated features |

The image editor previously satisfied neither half — no provider, no outlet — and depended on some
ancestor happening to satisfy both. That is the pattern violation this fix corrects.

## Non-contracts

Explicitly *not* part of any contract, so no consumer can depend on them:

- The **position or styling** of the editor's toast. `top-center` mirrors the asset picker; a
  reviewer may change it without breaking a consumer.
- **Which `MessageService` instance** an editor toast reaches. After this fix it is always the
  editor's own; nothing outside the editor may observe those messages.
- The **`z-index`/stacking mechanism** that puts the toast above the dialog. Currently PrimeNG's
  default tiering, unaided. See [research.md R2](../research.md) for the fallback if that proves
  insufficient — swapping it is an implementation detail, not a contract change.
