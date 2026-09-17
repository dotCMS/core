# Contract: `dot-card-field-label`

The only public contract this work changes. Internal to `libs/edit-content`, but it has **17 consumers**, so it is versioned here like an API.

No REST contract, no `@Schema`, no serialized state is affected.

## Before

```ts
@Component({ selector: 'dot-card-field-label', imports: [TooltipModule], … })
export class DotCardFieldLabelComponent {
    $variableName = input.required<string>({ alias: 'variableName' });
    $isRequired   = input.required<boolean>({ alias: 'isRequired' });
    $hint         = input<string>(null, { alias: 'hint' });   // rendered as a pTooltip on a pi-info-circle
}
```

```html
<!-- template -->
<label [attr.data-testid]="'label-' + $variableName()"
       [class.p-label-input-required]="$isRequired()"
       [for]="$variableName()">
    <ng-content />
</label>
@if ($hint()) { <i class="pi pi-info-circle text-xs" [pTooltip]="$hint()" …></i> }
```

```html
<!-- consumer -->
<dot-card-field-label [variableName]="field.variable" [isRequired]="isRequired">
    {{ field.name }}
</dot-card-field-label>
```

## After

```ts
@Component({ selector: 'label[dotCardFieldLabel]', imports: [DotFieldRequiredDirective], … })
export class DotCardFieldLabelComponent {
    $variableName = input.required<string>({ alias: 'variableName' });
    $isRequired   = input.required<boolean>({ alias: 'isRequired' });
    // `hint` is REMOVED — hints render as text in the field footer (AC-204)
}
```

```html
<!-- template collapses to projection; for/testid/required move to host bindings -->
<ng-content />
```

```html
<!-- consumer -->
<label dotCardFieldLabel [variableName]="field.variable" [isRequired]="isRequired">
    {{ field.name }}
</label>
```

## Why the selector changes

`.form .field > label` is a **direct-child** selector. With the old component selector the rendered DOM is `.field > dot-card-field-label > label`, and the rule never matches. `display: contents` does not fix it — `display` governs box generation, selectors match on the DOM tree. Making the component's host *be* the label is what makes the global rule apply. Full reasoning: [`../research.md#r1`](../research.md).

## Breaking changes and migration

| # | Change | Phase | Migration |
|---|---|---|---|
| 1 | Element selector → attribute selector on `<label>` | PR 1 | 17 templates: `<dot-card-field-label …>` → `<label dotCardFieldLabel …>`. `dot-card-field`'s projection becomes `<ng-content select="label[dotCardFieldLabel]" />`. |
| 2 | `hint` input removed | PR 2 | One caller: `dot-edit-content-block-editor.component.html:10` drops `[hint]="field.hint"`; the hint moves to the field footer like every other field. |
| 3 | `p-label-input-required` no longer hand-written | PR 1 | Replaced by `dotFieldRequired` in **bare mode**, applied conditionally on `$isRequired()`. **Not** `checkIsRequiredControl` — that mode reads `Validators.required` off the `FormGroup`, and a required Block Editor uses `blockEditorRequiredValidator()` instead, so the asterisk would silently disappear. |
| 4 | `TooltipModule` import dropped | PR 2 | None — internal. |

## Invariants that must NOT change

- `data-testid="label-<variable>"` keeps its exact format — specs across the library select on it.
- `for` keeps pointing at `$variableName()`, which is the control id.
- The asterisk is decorative: it comes from `::after` content, is not in the accessible name, and the control carries `required` / `aria-required` (AC-209).
- The rendered class is `p-label-input-required` either way, which is what keeps change 3 safe in **both** bundles: `_misc.scss:56` styles it, and `dotcms-binary-field-builder` loads `_misc.scss` (it does not load `apps/dotcms-ui/src/style.css`).

## Consumers (17)

`block-editor`, `calendar-field`, `category-field`, `checkbox-field`, `custom-field`, `file-field`, `host-folder-field`, `json-field`, `key-value`, `multi-select-field`, `radio-field`, `relationship-field`, `select-field`, `tag-field`, `text-area`, `text-field`, `wysiwyg-field`.

`line-divider-field` renders no label and is unaffected.
