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
import {
    AbstractControl,
    ControlValueAccessor,
    FormsModule,
    NG_VALIDATORS,
    NG_VALUE_ACCESSOR,
    ValidationErrors,
    Validator
} from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

/**
 * Configuration interface for the generated string field
 * Defines the structure and metadata required for field rendering and functionality
 */
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

/**
 * A custom form control component that provides a text input with an integrated
 * string generation feature. The component implements ControlValueAccessor to work
 * seamlessly with Angular reactive and template-driven forms.
 *
 * Features:
 * - Text input with real-time value binding
 * - Generate button that calls a backend API to create new string values
 * - Confirmation dialog when replacing existing values
 * - Full integration with Angular Forms (validation, disabled state, etc.)
 * - Support for hints, warnings, and accessibility features
 *
 */
@Component({
    selector: 'dot-apps-configuration-detail-generated-string-field',
    imports: [
        FormsModule,
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
        },
        {
            provide: NG_VALIDATORS,
            useExisting: forwardRef(() => DotAppsConfigurationDetailGeneratedStringFieldComponent),
            multi: true
        }
    ]
})
export class DotAppsConfigurationDetailGeneratedStringFieldComponent
    implements ControlValueAccessor, Validator
{
    readonly #confirmationService = inject(ConfirmationService);
    readonly #http = inject(HttpClient);
    readonly #dotMessageService = inject(DotMessageService);

    /**
     * Required input containing the field configuration and metadata
     * Defines all aspects of the field including labels, endpoints, and validation rules
     */
    $field = input.required<GeneratedStringField>({ alias: 'field' });

    /**
     * Model signal for bidirectional binding with form controls
     * Maintains the current value of the input field and syncs with Angular Forms
     */
    readonly $value = model<string>('');

    /**
     * Signal tracking the disabled state of the component
     * Updated automatically by Angular Forms when the control is enabled/disabled
     */
    readonly $isDisabled = signal<boolean>(false);

    /**
     * Signal tracking the loading state during string generation
     * True when a generation request is in progress, false otherwise
     */
    readonly $isLoading = signal<boolean>(false);

    /**
     * Signal tracking validation errors for visual feedback
     * Contains validation errors when the field is invalid
     */
    readonly $validationErrors = signal<ValidationErrors | null>(null);

    // ControlValueAccessor callback functions

    /**
     * Callback function triggered when the component value changes
     * Registered by Angular Forms to receive value updates
     */
    private onChange = (_value: string) => {
        // Implementation provided by registerOnChange
    };

    /**
     * Callback function triggered when the component is touched (loses focus)
     * Registered by Angular Forms to track interaction state
     */
    private onTouched = () => {
        // Implementation provided by registerOnTouched
    };

    /**
     * Initializes the component and sets up the effect for synchronizing
     * model signal changes with the ControlValueAccessor onChange callback.
     * This ensures that form controls are notified when the value changes.
     */
    constructor() {
        // Sync model signal changes with ControlValueAccessor
        effect(() => {
            const currentValue = this.$value();
            this.onChange(currentValue);

            // Update validation state
            this.updateValidationState();
        });
    }

    /**
     * Updates the validation state based on current value
     * @private
     */
    private updateValidationState(): void {
        const mockControl = { value: this.$value() } as AbstractControl;
        const errors = this.validate(mockControl);
        this.$validationErrors.set(errors);
    }

    /**
     * Writes a new value to the component
     * Called by Angular forms when the form control value changes
     *
     * @param value - The new value to set, or null/undefined for empty
     */
    writeValue(value: string): void {
        this.$value.set(value || '');
    }

    /**
     * Registers a callback function that should be called when the control's value changes
     *
     * @param fn - Callback function that receives the new value
     */
    registerOnChange(fn: (value: string) => void): void {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that should be called when the control is touched
     *
     * @param fn - Callback function called when the control loses focus
     */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /**
     * Called when the form control is disabled/enabled
     * Updates the component's disabled state signal
     *
     * @param isDisabled - True if the control should be disabled, false otherwise
     */
    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }

    /**
     * Initiates the string generation process with user confirmation when needed.
     * If the current input is empty, generates immediately. If there's existing content,
     * shows a confirmation dialog to prevent accidental data loss.
     *
     * @param event - The click event from the generate button, used for positioning the confirmation dialog
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

    /**
     * Makes an HTTP request to the configured endpoint to generate a new string value.
     * Updates the component value with the response and marks the control as touched.
     * Handles errors by logging them to the console.
     *
     * @private
     */
    private generateFromBackend(): void {
        const endpoint = this.$field().buttonEndpoint;

        this.$isLoading.set(true);

        this.#http.get(endpoint, { responseType: 'text' }).subscribe({
            next: (response: string) => {
                this.$value.set(response);
                this.onTouched();
                this.$isLoading.set(false);
            },
            error: (error) => {
                console.error('Error generating string:', error);
                this.$isLoading.set(false);
            }
        });
    }

    /**
     * Custom validator to prevent spaces in the input value
     * @param control - The form control to validate
     * @returns ValidationErrors if spaces are found, null otherwise
     */
    validate(control: AbstractControl): ValidationErrors | null {
        const value = control.value;
        if (value && typeof value === 'string' && value.includes(' ')) {
            return { noSpaces: { message: 'Spaces are not allowed' } };
        }

        return null;
    }
}
