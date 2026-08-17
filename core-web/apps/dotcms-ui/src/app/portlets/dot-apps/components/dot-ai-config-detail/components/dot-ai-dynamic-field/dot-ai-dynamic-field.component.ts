import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { DotAiProviderField, DotAiProviderFieldType } from '@dotcms/dotcms-models';

import { MASKED_SECRET_VALUE } from '../../dot-ai-config.constants';

/**
 * Renders a single dynamic dotAI provider field (text, number or secret) inside a parent
 * `FormGroup`, based purely on the field metadata returned by `GET /v1/ai/providers` — no
 * per-provider knowledge lives here, so a new backend provider's fields render automatically.
 *
 * A `SECRET` field renders as a masked password input with a reveal toggle when it has no saved
 * value yet. Once a value is already saved, the real secret never reaches the browser — the
 * backend sends {@link MASKED_SECRET_VALUE} instead — so it renders as a plain text input showing
 * that placeholder, with no toggle (there's nothing behind it to reveal). Typing over the
 * placeholder to set a new secret stays in plain text for the rest of this field instance's
 * lifetime — see `isMaskedSecret`.
 */
@Component({
    selector: 'dot-ai-dynamic-field',
    templateUrl: './dot-ai-dynamic-field.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, InputTextModule, InputNumberModule, PasswordModule]
})
export class DotAiDynamicFieldComponent {
    readonly field = input.required<DotAiProviderField>();
    readonly formGroup = input.required<FormGroup>();

    readonly DotAiProviderFieldType = DotAiProviderFieldType;

    readonly label = computed(() => humanizeFieldName(this.field().name));

    readonly isInvalid = computed(() => {
        const control = this.formGroup().get(this.field().name);

        return !!control && control.invalid && (control.touched || control.dirty);
    });

    /**
     * Evaluated only when `field()`/`formGroup()` change identity (field metadata change or a
     * provider switch rebuilding the FormGroup) — not on every keystroke — so replacing the
     * placeholder with a freshly-typed secret doesn't flip the input back to password-masked
     * mid-edit.
     */
    readonly isMaskedSecret = computed(() => {
        const field = this.field();
        if (field.type !== DotAiProviderFieldType.SECRET) {
            return false;
        }

        return this.formGroup().get(field.name)?.value === MASKED_SECRET_VALUE;
    });
}

/**
 * Turns a camelCase provider field name into a human-readable label, e.g.
 * `maxRetries` -> `Max retries`, `apiKey` -> `Api key`.
 */
export function humanizeFieldName(name: string): string {
    const spaced = name.replace(/([a-z0-9])([A-Z])/g, '$1 $2');

    return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
}
