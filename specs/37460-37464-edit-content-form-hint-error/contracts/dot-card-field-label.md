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
@Component({
    selector: 'label[dotCardFieldLabel]',
    // NOT `imports`. A component cannot apply a directive to its own host from its own
    // template, and the host IS the label — so the directive is attached as a host
    // directive, with `isRequired` forwarded to it in the directive's boolean mode.
    hostDirectives: [
        { directive: DotFieldRequiredDirective, inputs: ['dotFieldRequired: isRequired'] }
    ],
    host: {
        '[attr.for]': '$variableName()',
        '[attr.id]': '$testId()',        // composite widgets point aria-labelledby here
        '[attr.data-testid]': '$testId()'
    },
    …
})
export class DotCardFieldLabelComponent {
    $variableName = input.required<string>({ alias: 'variableName' });
    // `isRequired` is NOT declared here — it is the host directive's input, aliased above.
    // `hint` is REMOVED — hints render as text in the field footer (AC-204)
    protected $testId = computed(() => `label-${this.$variableName()}`);
}
```

```html
<!-- template collapses to projection; for/id/testid/required all move to host bindings -->
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
| 3 | `p-label-input-required` no longer hand-written | PR 1 | Replaced by `dotFieldRequired` in **boolean mode** — a mode the directive gained for this (`input<Field<unknown> \| boolean \| ''>`), because the consumer passes a plain `isRequired` boolean and there is no conditional template to apply a bare attribute in: the directive is on the host. **Not** `checkIsRequiredControl` — that mode reads `Validators.required` off the `FormGroup`, and a required Block Editor uses `blockEditorRequiredValidator()` instead, so the asterisk would silently disappear. |
| 4 | `TooltipModule` import dropped | PR 2 | None — internal. |

## Invariants that must NOT change

- `data-testid="label-<variable>"` keeps its exact format — specs across the library select on it.
- `for` keeps pointing at `$variableName()`, which is the control id.
- **`id="label-<variable>"` is load-bearing, not decoration.** `<label for>` only associates with labelable elements, so every composite widget — radio group, checkbox group, key-value, category, relationship, file, iframe — names itself with `aria-labelledby="label-<variable>"` pointing back at this id. Removing or renaming it silently strips the accessible name off seven field types; nothing throws.
- The asterisk is decorative: it comes from `::after` content and is deliberately kept out of the accessible name, so a screen reader does not announce "star". That leaves nothing conveying "required" unless the control itself says so — which is why `dot-card-field` sets `aria-required` on whatever element this label's `for` names (AC-209). Only where ARIA defines the attribute: a plain `role="group"` is not one of those, so a checkbox group carries the name but not the required state.
- The rendered class is `p-label-input-required` either way, which is what keeps change 3 safe in **both** bundles: `_misc.scss:56` styles it, and `dotcms-binary-field-builder` loads `_misc.scss` (it does not load `apps/dotcms-ui/src/style.css`).

## Consumers (17)

`block-editor`, `calendar-field`, `category-field`, `checkbox-field`, `custom-field`, `file-field`, `host-folder-field`, `json-field`, `key-value`, `multi-select-field`, `radio-field`, `relationship-field`, `select-field`, `tag-field`, `text-area`, `text-field`, `wysiwyg-field`.

`line-divider-field` renders no label and is unaffected.
