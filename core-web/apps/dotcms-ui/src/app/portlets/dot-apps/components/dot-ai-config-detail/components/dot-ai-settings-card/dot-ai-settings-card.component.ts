import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    OnInit,
    inject,
    input,
    output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { CheckboxModule } from 'primeng/checkbox';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { DotMessagePipe } from '@dotcms/ui';

import {
    DotAiAdditionalPropertiesComponent,
    DotAiAdditionalPropertyGroup
} from '../dot-ai-additional-properties/dot-ai-additional-properties.component';
import {
    IMAGE_SIZE_OPTIONS,
    SETTINGS_ADVANCED_FIELDS,
    SETTINGS_COMMON_FIELDS
} from '../../dot-ai-config.constants';

export type DotAiSettingsValue = Record<string, unknown>;

/**
 * Shared prompt/behavior settings applied across every capability (role prompt, text/image
 * prompts, image size, and the embeddings/indexing advanced knobs from `com.dotcms.ai.app.AppKeys`).
 */
@Component({
    selector: 'dot-ai-settings-card',
    templateUrl: './dot-ai-settings-card.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ReactiveFormsModule,
        InputTextModule,
        InputNumberModule,
        TextareaModule,
        SelectModule,
        CheckboxModule,
        PanelModule,
        DotMessagePipe,
        DotAiAdditionalPropertiesComponent
    ]
})
export class DotAiSettingsCardComponent implements OnInit {
    private readonly destroyRef = inject(DestroyRef);

    readonly initialValue = input<DotAiSettingsValue | null>(null);
    readonly changed = output<void>();

    readonly commonFields = SETTINGS_COMMON_FIELDS;
    readonly advancedFields = SETTINGS_ADVANCED_FIELDS;
    readonly imageSizeOptions = IMAGE_SIZE_OPTIONS;

    readonly form = new FormGroup({
        rolePrompt: new FormControl<string | null>(null),
        textPrompt: new FormControl<string | null>(null),
        imagePrompt: new FormControl<string | null>(null),
        imageSize: new FormControl<string | null>(null)
    });

    readonly advancedForm = new FormGroup({});
    readonly additionalProperties = new FormArray<DotAiAdditionalPropertyGroup>([]);

    ngOnInit(): void {
        this.advancedFields.forEach((field) => {
            this.advancedForm.addControl(
                field.key,
                new FormControl(field.type === 'checkbox' ? (field.defaultValue ?? false) : null)
            );
        });

        const initial = this.initialValue();
        if (initial) {
            this.form.patchValue(initial, { emitEvent: false });
            this.advancedForm.patchValue(initial, { emitEvent: false });

            const knownKeys = new Set([
                ...this.commonFields.map((f) => f.key),
                ...this.advancedFields.map((f) => f.key)
            ]);
            Object.entries(initial).forEach(([key, value]) => {
                if (knownKeys.has(key)) {
                    return;
                }
                this.additionalProperties.push(
                    new FormGroup({
                        key: new FormControl(key, { nonNullable: true }),
                        value: new FormControl(
                            typeof value === 'string' ? value : JSON.stringify(value),
                            { nonNullable: true }
                        )
                    })
                );
            });
        }

        this.form.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.changed.emit());
        this.advancedForm.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.changed.emit());
        this.additionalProperties.valueChanges
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => this.changed.emit());
    }

    buildPayloadSection(): DotAiSettingsValue {
        const section: DotAiSettingsValue = {};

        Object.entries(this.form.value).forEach(([key, value]) => {
            if (value !== null && value !== undefined && value !== '') {
                section[key] = value;
            }
        });

        Object.entries(this.advancedForm.value as Record<string, unknown>).forEach(
            ([key, value]) => {
                if (value !== null && value !== undefined && value !== '') {
                    section[key] = value;
                }
            }
        );

        this.additionalProperties.controls.forEach((group) => {
            const key = group.value.key?.trim();
            if (key) {
                section[key] = parseIfJson(group.value.value ?? '');
            }
        });

        return section;
    }
}

/**
 * Additional-property values round-trip through a plain text input, but some preserved settings
 * (e.g. `listenerIndexer`) are objects in the stored JSON. Parses back to an object when the
 * text looks like JSON, otherwise keeps the raw string.
 */
function parseIfJson(value: string): unknown {
    const trimmed = value.trim();
    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
        return value;
    }
    try {
        return JSON.parse(trimmed);
    } catch {
        return value;
    }
}
