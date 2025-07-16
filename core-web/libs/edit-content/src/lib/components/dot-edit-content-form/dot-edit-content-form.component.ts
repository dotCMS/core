import { animate, style, transition, trigger } from '@angular/animations';
import { NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    OnInit,
    output
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormGroup,
    FormsModule,
    ReactiveFormsModule,
    ValidatorFn,
    Validators
} from '@angular/forms';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputSwitchChangeEvent, InputSwitchModule } from 'primeng/inputswitch';
import { MessagesModule } from 'primeng/messages';
import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';

import { take } from 'rxjs/operators';

import {
    DotMessageService,
    DotWizardService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import { DotcmsConfigService, SystemTimezone } from '@dotcms/dotcms-js';
import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSWorkflowAction,
    DotWorkflowPayload
} from '@dotcms/dotcms-models';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

import { resolutionValue } from './dot-edit-content-form-resolutions';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import {
    CALENDAR_FIELD_TYPES,
    CONTENT_SEARCH_ROUTE,
    FLATTENED_FIELD_TYPES
} from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotWorkflowActionParams } from '../../models/dot-edit-content.model';
import { DotEditContentStore } from '../../store/edit-content.store';
import {
    generatePreviewUrl,
    getFinalCastedValue,
    isFilteredType
} from '../../utils/functions.util';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

/**
 * DotEditContentFormComponent
 *
 * A standalone component responsible for rendering and managing the form for editing content in DotCMS.
 * This component uses a signal-based store approach for state management and provides a dynamic form
 * based on the content type structure.
 *
 * Features:
 * - Dynamic form generation based on content type fields
 * - Real-time form value updates with signal-based reactivity
 * - Custom field type handling (calendar fields, flattened fields)
 * - Workflow action integration with push publish support
 * - Form validation (required fields, regex patterns)
 * - Content locking mechanism
 * - Preview functionality for content types
 * - Tab-based field organization
 *
 * @example
 * ```typescript
 * <dot-edit-content-form></dot-edit-content-form>
 * ```
 */
@Component({
    selector: 'dot-edit-content-form',
    standalone: true,
    templateUrl: './dot-edit-content-form.component.html',
    styleUrls: ['./dot-edit-content-form.component.scss'],
    imports: [
        ReactiveFormsModule,
        DotEditContentFieldComponent,
        ButtonModule,
        TabViewModule,
        DotWorkflowActionsComponent,
        TabViewInsertDirective,
        NgTemplateOutlet,
        DotMessagePipe,
        InputSwitchModule,
        FormsModule,
        MessagesModule
    ],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('fadeIn', [
            transition(':enter', [
                style({ opacity: 0 }),
                animate('250ms ease-in', style({ opacity: 1 }))
            ])
        ])
    ]
})
export class DotEditContentFormComponent implements OnInit {
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
    readonly #router = inject(Router);
    readonly #destroyRef = inject(DestroyRef);
    readonly #fb = inject(FormBuilder);
    readonly #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    readonly #dotWizardService = inject(DotWizardService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #dotcmsConfigService = inject(DotcmsConfigService);
    #globalSystemConfig = toSignal(this.#dotcmsConfigService.getConfig());

    /**
     * Output event emitter that informs when the form has changed.
     * Emits an object of type Record<string, string> containing the updated form values.
     *
     * @memberof DotEditContentFormComponent
     */
    changeValue = output<FormValues>();

    /**
     * Computed property that retrieves the filtered fields from the store.
     *
     * @type {ComputedSignal<DotCMSContentTypeField[]>}
     * @memberof DotEditContentFormComponent
     */
    $filteredFields = computed(
        () => this.$store.contentType()?.fields?.filter(isFilteredType) ?? []
    );

    $formFields = computed(
        () => this.$store.contentType()?.fields?.filter((field) => !isFilteredType(field)) ?? []
    );

    /**
     * Computed property that determines if the preview link should be shown.
     *
     * @memberof DotEditContentFormComponent
     */
    $showPreviewLink = computed(() => {
        const contentlet = this.$store.contentlet();

        return contentlet?.baseType === 'CONTENT' && !!contentlet.URL_MAP_FOR_CONTENT;
    });

    /**
     * FormGroup instance that contains the form controls for the fields in the content type
     *
     * @type {FormGroup}
     * @memberof DotEditContentFormComponent
     */
    form!: FormGroup;

    /**
     * Computed property that determines if the content type has only one tab.
     *
     * @memberof DotEditContentFormComponent
     */
    $hasSingleTab = computed(() => this.$store.tabs().length === 1);

    /**
     * Computed property that retrieves the tabs from the store.
     *
     * @memberof DotEditContentFormComponent
     */
    $tabs = this.$store.tabs;

    /**
     * The system timezone.
     */
    $systemTimezone = computed(() => this.#globalSystemConfig()?.systemTimezone);

    ngOnInit(): void {
        if (this.$store.tabs().length) {
            this.initializeForm();
            this.initializeFormListener();
        }
    }

    constructor() {
        /**
         * Effect that enables or disables the form based on the loading state.
         */
        effect(
            () => {
                const isLoading = this.$store.isLoading();

                if (isLoading) {
                    this.form.disable();
                } else {
                    this.form.enable();
                }
            },
            { allowSignalWrites: true }
        );

        /**
         * Effect that initializes the form and form listener when copying locale.
         *
         * This effect listens for changes in the `isCopyingLocale` state from the store.
         * If `isCopyingLocale` is true, it initializes the form and sets up the form listener.
         */
        effect(() => {
            const isCopyingLocale = this.$store.isCopyingLocale();

            if (isCopyingLocale) {
                this.initializeForm();
                this.initializeFormListener();
            }
        });
    }

    /**
     * Handles form value changes and emits the processed value.
     *
     * @param {Record<string, any>} value The raw form value
     * @memberof DotEditContentFormComponent
     */
    onFormChange(value: Record<string, string>) {
        const processedValue = this.processFormValue(value);
        this.changeValue.emit(processedValue);
    }

    /**
     * Initializes a listener for form value changes.
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private initializeFormListener() {
        this.form.valueChanges.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((value) => {
            this.onFormChange(value);
        });
    }

    /**
     * Handles the workflow action execution process.
     *
     * This method processes the workflow action based on its configuration:
     * 1. If no action inputs are required, executes the action directly
     * 2. If push publish is required, checks for available environments first
     * 3. For other cases, opens the workflow wizard
     *
     * @param {DotWorkflowActionParams} params - Parameters needed for the workflow action
     * @param {DotCMSWorkflowAction} params.workflow - The workflow action to execute
     * @param {string} params.inode - The content inode
     * @param {string} params.contentType - The content type
     * @param {string} params.languageId - The language ID
     * @param {string} params.identifier - The content identifier
     * @memberof DotEditContentFormComponent
     */
    fireWorkflowAction({
        workflow,
        inode,
        contentType,
        languageId,
        identifier
    }: DotWorkflowActionParams): void {
        const contentlet = {
            ...this.processFormValue(this.form.value),
            contentType,
            languageId,
            identifier
        };

        const { actionInputs = [] } = workflow;
        const isPushPublish =
            this.#dotWorkflowEventHandlerService.containsPushPublish(actionInputs);

        if (!actionInputs.length) {
            this.$store.fireWorkflowAction({
                actionId: workflow.id,
                inode,
                data: {
                    contentlet
                }
            });

            return;
        }

        if (!isPushPublish) {
            this.openWizard(workflow, inode, contentlet);

            return;
        }

        // Check if there are any publish environments available if not fire a notification.
        this.#dotWorkflowEventHandlerService
            .checkPublishEnvironments()
            .pipe(take(1))
            .subscribe((hasEnvironments: boolean) => {
                if (hasEnvironments) {
                    this.openWizard(workflow, inode, contentlet);
                }
            });
    }

    /**
     * Opens the workflow wizard for actions that require additional input.
     *
     * This method:
     * 1. Opens a wizard dialog using DotWizardService
     * 2. Processes the workflow payload with any additional inputs
     * 3. Fires the workflow action with the combined data
     *
     * @private
     * @param {DotCMSWorkflowAction} workflow - The workflow action configuration
     * @param {string} inode - The content inode
     * @param {{ [key: string]: string | object }} contentlet - The content data
     */
    private openWizard(
        workflow: DotCMSWorkflowAction,
        inode: string,
        contentlet: { [key: string]: string | object }
    ): void {
        this.#dotWizardService
            .open<DotWorkflowPayload>(
                this.#dotWorkflowEventHandlerService.setWizardInput(
                    workflow,
                    this.#dotMessageService.get('Workflow-Action')
                )
            )
            .pipe(take(1))
            .subscribe((data: DotWorkflowPayload) => {
                this.$store.fireWorkflowAction({
                    actionId: workflow.id,
                    inode,
                    data: {
                        ...this.#dotWorkflowEventHandlerService.processWorkflowPayload(
                            data,
                            workflow.actionInputs
                        ),
                        contentlet
                    }
                });
            });
    }

    /**
     * Processes the form value, applying specific transformations for different field types.
     *
     * Handles special cases:
     * - Flattened fields: Joins array values with commas
     * - Calendar fields: Formats dates with proper timezone handling
     * - Null/undefined values: Converts to empty string
     *
     * @private
     * @param {Record<string, string | string[] | Date | null | undefined>} value - The raw form value
     * @returns {FormValues} The processed form value ready for submission
     */
    private processFormValue(
        value: Record<string, string | string[] | Date | null | undefined>
    ): FormValues {
        return Object.fromEntries(
            Object.entries(value).map(([key, fieldValue]) => {
                const field = this.$formFields().find((f) => f.variable === key);

                if (!field) {
                    return [key, fieldValue];
                }

                if (
                    Array.isArray(fieldValue) &&
                    FLATTENED_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)
                ) {
                    fieldValue = fieldValue.join(',');
                } else if (
                    fieldValue instanceof Date &&
                    CALENDAR_FIELD_TYPES.includes(field.fieldType as FIELD_TYPES)
                ) {
                    const systemTimezone = this.$systemTimezone();

                    // Formatear según el tipo de campo con manejo de timezone
                    fieldValue = this.formatDateForBackend(
                        fieldValue,
                        field.fieldType as FIELD_TYPES,
                        systemTimezone
                    );
                }

                return [key, fieldValue ?? ''];
            })
        );
    }



    /**
     * Formatea fecha para envío al backend según el tipo de campo
     * @param date - Fecha a formatear
     * @param fieldType - Tipo del campo
     * @param systemTimezone - Información del timezone del sistema
     * @returns String formateado para el backend
     */
    private formatDateForBackend(
        date: Date,
        fieldType: FIELD_TYPES,
        systemTimezone: SystemTimezone | null
    ): string {
        console.log('🔄 Formatting date for backend:');
        console.log('Original date from form:', date);
        console.log('Field type:', fieldType);
        console.log('System timezone:', systemTimezone);

        switch (fieldType) {
            case FIELD_TYPES.DATE:
                // Solo fecha: yyyy-MM-dd (sin timezone)
                const dateResult = date.toISOString().split('T')[0];
                console.log('DATE result:', dateResult);
                return dateResult;

            case FIELD_TYPES.TIME:
                // Solo hora: HH:mm:ss (sin timezone)
                const timeResult = date.toTimeString().split(' ')[0];
                console.log('TIME result:', timeResult);
                return timeResult;

            case FIELD_TYPES.DATE_AND_TIME:
                // Fecha y hora con offset del servidor
                const dateTimeResult = this.formatDateTimeWithServerOffset(date, systemTimezone);
                console.log('DATE_AND_TIME result:', dateTimeResult);
                return dateTimeResult;

            default:
                const defaultResult = date.toISOString().replace(/T|\.\d{3}Z/g, (match) => (match === 'T' ? ' ' : ''));
                console.log('Default result:', defaultResult);
                return defaultResult;
        }
    }

    /**
     * Formatea fecha-hora con offset del servidor para DATE_AND_TIME
     * @param date - Fecha a formatear
     * @param systemTimezone - Información del timezone del sistema
     * @returns String formateado con offset del servidor
     */
    private formatDateTimeWithServerOffset(date: Date, systemTimezone: SystemTimezone | null): string {
        console.log('Formatting DATE_AND_TIME with server offset:');
        console.log('Input date:', date);
        console.log('System timezone:', systemTimezone);

        if (!systemTimezone) {
            // Sin timezone, usar formato ISO sin Z
            const result = date.toISOString().replace('T', ' ').replace(/\.\d{3}Z$/, '');
            console.log('No timezone, ISO format result:', result);
            return result;
        }

        // En lugar de convertir el timestamp, usar las partes UTC de la fecha directamente
        // La fecha que recibimos ya está ajustada para mostrar la hora del servidor
        // Pero necesitamos revertir esa visualización para obtener la hora real a guardar

        const serverOffsetSeconds = Number(systemTimezone.offset);
        const browserOffsetMinutes = date.getTimezoneOffset();
        const browserOffsetSeconds = browserOffsetMinutes * 60;

        // Calcular qué hora era realmente antes del ajuste de visualización
        const adjustmentSeconds = browserOffsetSeconds - serverOffsetSeconds;
        const originalTimestamp = date.getTime() - (adjustmentSeconds * 1000);

        // Crear una fecha con el timestamp original y extraer partes UTC
        const originalDate = new Date(originalTimestamp);
        console.log('Original timestamp date:', originalDate);

        // Formatear usando las partes UTC de la fecha original
        const year = originalDate.getUTCFullYear();
        const month = String(originalDate.getUTCMonth() + 1).padStart(2, '0');
        const day = String(originalDate.getUTCDate()).padStart(2, '0');
        const hours = String(originalDate.getUTCHours()).padStart(2, '0');
        const minutes = String(originalDate.getUTCMinutes()).padStart(2, '0');
        const seconds = String(originalDate.getUTCSeconds()).padStart(2, '0');

        // Calcular offset string para el servidor
        const offsetHours = Math.floor(Math.abs(serverOffsetSeconds) / 3600);
        const offsetMinutes = Math.floor((Math.abs(serverOffsetSeconds) % 3600) / 60);
        const offsetSign = serverOffsetSeconds >= 0 ? '+' : '-';
        const offsetString = `${offsetSign}${offsetHours.toString().padStart(2, '0')}${offsetMinutes.toString().padStart(2, '0')}`;

        console.log('Extracted UTC parts:', { year, month, day, hours, minutes, seconds });
        console.log('Calculated offset string:', offsetString);

        // Formato: yyyy-MM-dd HH:mm:ss +HHMM
        const result = `${year}-${month}-${day} ${hours}:${minutes}:${seconds} ${offsetString}`;

        console.log('Final formatted string:', result);
        return result;
    }

    /**
     * Creates form controls for each field in the content type.
     *
     * This method:
     * 1. Filters out non-form fields
     * 2. Creates form controls with appropriate initial values and validators
     * 3. Combines all controls into a FormGroup
     *
     * @private
     */
    private initializeForm() {
        const controls = this.$formFields().reduce(
            (acc, field) => ({
                ...acc,
                [field.variable]: this.createFormControl(field)
            }),
            {}
        );

        this.form = this.#fb.group(controls);
    }

    /**
     * Creates a form control for a specific content type field.
     *
     * This method:
     * 1. Determines the initial value based on field type
     * 2. Applies appropriate validators (required, regex)
     * 3. Sets the control's disabled state based on field configuration
     *
     * @private
     * @param {DotCMSContentTypeField} field - The field configuration
     * @returns {AbstractControl} The configured form control
     */
    private createFormControl(field: DotCMSContentTypeField) {
        const initialValue = this.getInitialFieldValue(field, this.$store.contentlet());
        const validators = this.getFieldValidators(field);

        return this.#fb.control({ value: initialValue, disabled: field.readOnly }, { validators });
    }

    /**
     * Retrieves the initial value for a field based on the contentlet data.
     *
     * This method:
     * 1. Gets the appropriate resolution function for the field type
     * 2. Applies the resolution function to extract the value
     * 3. Casts the value to the correct type
     * 4. Falls back to null if no value can be resolved
     *
     * @private
     * @param {DotCMSContentTypeField} field - The field configuration
     * @param {DotCMSContentlet | null} contentlet - The contentlet containing field values
     * @returns {unknown} The resolved and cast field value
     */
    private getInitialFieldValue(
        field: DotCMSContentTypeField,
        contentlet: DotCMSContentlet | null
    ): unknown {
        const resolutionFn = resolutionValue[field.fieldType as FIELD_TYPES];
        if (!resolutionFn) {
            console.warn(`No resolution function found for field type: ${field.fieldType}`);

            return null;
        }

        const value = resolutionFn(contentlet, field);

        return getFinalCastedValue(value, field) ?? null;
    }

    /**
     * Generates validators for a content type field.
     *
     * Applies the following validators based on field configuration:
     * - Required validator if field.required is true
     * - Pattern validator if field.regexCheck is set
     *
     * Handles invalid regex patterns gracefully by logging errors without throwing exceptions.
     *
     * @private
     * @param {DotCMSContentTypeField} field - The field to generate validators for
     * @returns {ValidatorFn[]} Array of Angular validator functions
     */
    private getFieldValidators(field: DotCMSContentTypeField): ValidatorFn[] {
        const validators: ValidatorFn[] = [];

        if (field.required) {
            validators.push(Validators.required);
        }

        if (field.regexCheck) {
            try {
                const regex = new RegExp(field.regexCheck);
                validators.push(Validators.pattern(regex));
            } catch (e) {
                console.error('Invalid regex', e);
            }
        }

        return validators;
    }

    /**
     * Navigates back to the content listing page.
     *
     * Uses the current content type as a filter parameter when navigating back
     * to ensure relevant content is displayed in the listing.
     *
     * @memberof DotEditContentFormComponent
     */
    goBack(): void {
        const contentTypeVariable = this.$store.contentType().variable;

        this.#router.navigate([CONTENT_SEARCH_ROUTE], {
            queryParams: { filter: contentTypeVariable }
        });
    }

    /**
     * Opens the content preview in a new browser tab.
     *
     * Generates a preview URL based on the current contentlet's URL_MAP_FOR_CONTENT
     * and opens it in a new tab. Logs a warning if the URL cannot be generated.
     *
     * @memberof DotEditContentFormComponent
     */
    showPreview(): void {
        const contentlet = this.$store.contentlet();
        const realUrl = generatePreviewUrl(contentlet);

        if (!realUrl) {
            console.warn(
                'Preview URL could not be generated due to missing contentlet attributes.'
            );

            return;
        }

        window.open(realUrl, '_blank');
    }

    /**
     * Updates the active tab index in the store.
     *
     * This method is triggered by the PrimeNG TabView component when the active tab changes.
     * It synchronizes the UI state with the store to maintain tab selection across renders.
     *
     * @param {TabViewChangeEvent} event - The tab change event containing the new active index
     * @memberof DotEditContentFormComponent
     */
    onActiveIndexChange({ index }: TabViewChangeEvent) {
        this.$store.setActiveTab(index);
    }

    /**
     * Handles the content lock toggle.
     *
     * This method is triggered when the user toggles the content lock switch.
     * It updates the content lock state in the store based on the switch value.
     *
     * @param {InputSwitchChangeEvent} event - The switch change event containing the new checked state
     * @memberof DotEditContentFormComponent
     */
    onContentLockChange(event: InputSwitchChangeEvent) {
        event.checked ? this.$store.lockContent() : this.$store.unlockContent();
    }
}
