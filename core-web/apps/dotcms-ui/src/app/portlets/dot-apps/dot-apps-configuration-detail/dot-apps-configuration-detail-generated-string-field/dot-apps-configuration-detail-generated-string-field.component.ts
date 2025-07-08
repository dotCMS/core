import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import {
    ChangeDetectionStrategy,
    Component,
    effect,
    forwardRef,
    inject,
    input,
    model,
    signal
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

// Define the field interface based on the usage pattern
interface GeneratedStringField {
    name: string;
    label: string;
    hint?: string;
    warnings?: string[];
    required: boolean;
    type: string;
    value?: string;
    buttonLabel: string;
    buttonEndpoint: string;
}

@Component({
    selector: 'dot-apps-configuration-detail-generated-string-field',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        DotIconModule,
        DotMessagePipe,
        ButtonModule,
        InputTextModule,
        TooltipModule,
        ConfirmPopupModule
    ],
    templateUrl: './dot-apps-configuration-detail-generated-string-field.component.html',
    styleUrl: './dot-apps-configuration-detail-generated-string-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotAppsConfigurationDetailGeneratedStringFieldComponent),
            multi: true
        }
    ]
})
export class DotAppsConfigurationDetailGeneratedStringFieldComponent
    implements ControlValueAccessor
{
    // Dependency injection
    readonly #confirmationService = inject(ConfirmationService);
    readonly #http = inject(HttpClient);
    readonly #dotMessageService = inject(DotMessageService);

    // Input for field configuration
    $field = input.required<GeneratedStringField>({ alias: 'field' });

    // Model signal for bidirectional binding
    protected readonly $value = model<string>('');
    protected readonly $isDisabled = signal<boolean>(false);

    // ControlValueAccessor methods

    private onChange = (value: string) => {
        // empty
    };
    private onTouched = () => {
        // empty
    };

    constructor() {
        // Sync model signal changes with ControlValueAccessor
        effect(() => {
            const currentValue = this.$value();
            this.onChange(currentValue);
        });
    }

    /**
     * Writes a new value to the component
     * Called by Angular forms when the form control value changes
     */
    writeValue(value: string): void {
        this.$value.set(value || '');
    }

    /**
     * Registers a callback function that should be called when the control's value changes
     */
    registerOnChange(fn: (value: string) => void): void {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that should be called when the control is touched
     */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /**
     * Called when the form control is disabled/enabled
     */
    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    /**
     * Handles the blur event and marks the control as touched
     */
    protected handleBlur(): void {
        this.onTouched();
    }

    /**
     * Makes HTTP request to generate string from backend
     */
    private generateFromBackend(): void {
        const endpoint = this.$field().buttonEndpoint;
        this.#http.get(endpoint, { responseType: 'text' }).subscribe({
            next: (response: string) => {
                this.$value.set(response);
                this.onTouched();
            },
            error: (error) => {
                console.error('Error generating string:', error);
            }
        });
    }

    /**
     * Generates a new string value with confirmation when input has value
     */
    protected generateString(event: Event): void {
        const currentValue = this.$value();

        const isEmpty = !currentValue || currentValue.trim().length === 0;

        if (isEmpty) {
            this.generateFromBackend();
        } else {
            this.#confirmationService.confirm({
                target: event.target as EventTarget,
                message: this.#dotMessageService.get(
                    'apps.content-analytics.generated.string.confirm.replace.message',
                    currentValue
                ),
                header: this.#dotMessageService.get(
                    'apps.content-analytics.generated.string.confirm.replace.header'
                ),
                icon: 'pi pi-exclamation-triangle',
                acceptLabel: this.#dotMessageService.get('dot.common.dialog.accept'),
                rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
                accept: () => {
                    this.generateFromBackend();
                }
            });
        }
    }
}
