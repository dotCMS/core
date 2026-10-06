import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

export type DotAiAdditionalPropertyGroup = FormGroup<{
    key: FormControl<string>;
    value: FormControl<string>;
}>;

/**
 * A free-form key/value escape hatch for provider-specific settings the dynamic form doesn't
 * model. The backend may not read a given key today — this only builds the payload; nothing
 * here validates it against the provider's actual API.
 */
@Component({
    selector: 'dot-ai-additional-properties',
    templateUrl: './dot-ai-additional-properties.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ReactiveFormsModule, ButtonModule, InputTextModule, DotMessagePipe]
})
export class DotAiAdditionalPropertiesComponent {
    readonly properties = input.required<FormArray<DotAiAdditionalPropertyGroup>>();

    addProperty(): void {
        this.properties().push(
            new FormGroup({
                key: new FormControl('', { nonNullable: true }),
                value: new FormControl('', { nonNullable: true })
            })
        );
    }

    removeProperty(index: number): void {
        this.properties().removeAt(index);
    }
}
