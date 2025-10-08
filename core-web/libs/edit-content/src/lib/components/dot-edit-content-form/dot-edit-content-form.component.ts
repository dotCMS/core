import { Subscription } from 'rxjs';

import { animate, style, transition, trigger } from '@angular/animations';
import { DOCUMENT, NgTemplateOutlet } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    OnInit,
    output
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import {
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSWorkflowAction,
    DotWorkflowPayload
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

import { resolutionValue } from './dot-edit-content-form-resolutions';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import { DISABLED_WYSIWYG_FIELD } from '../../models/disabledWYSIWYG.constant';
import { CONTENT_SEARCH_ROUTE } from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotWorkflowActionParams } from '../../models/dot-edit-content.model';
import { DotEditContentStore } from '../../store/edit-content.store';
import {
    generatePreviewUrl,
    getFinalCastedValue,
    isFilteredType,
    processFieldValue
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
    readonly #rootStore = inject(GlobalStore);
    readonly $store: InstanceType<typeof DotEditContentStore> = inject(DotEditContentStore);
    readonly #router = inject(Router);
    readonly #destroyRef = inject(DestroyRef);
    readonly #fb = inject(FormBuilder);
    readonly #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    readonly #dotWizardService = inject(DotWizardService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #document = inject(DOCUMENT);

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
     * Subscription for form value changes - using this to manage the listener lifecycle
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private formValueSubscription?: Subscription;

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

    changeDetectorRef = inject(ChangeDetectorRef);

    /**
     * The system timezone.
     */
    $systemTimezone = computed(() => this.#rootStore.systemTimezone());

    ngOnInit(): void {
        if (this.$store.tabs().length) {
            this.initializeForm();
            this.initializeFormListener();
        }
    }

    constructor() {
        /**
         * Effect that reinitializes the form when contentlet changes (e.g., when viewing historical versions)
         *
         * This effect listens for changes in the contentlet and reinitializes the form
         * to reflect the new content data.
         */
        effect(() => {
            const contentlet = this.$store.contentlet();
            const tabs = this.$store.tabs();

            // Only reinitialize if we have both contentlet and tabs, and form exists
            if (contentlet && tabs.length > 0 && this.form) {
                this.initializeForm();
                this.initializeFormListener();
            }
        });

        /**
         * Effect that enables or disables the form based on the loading state and historical view.
         */
        effect(() => {
            const isLoading = this.$store.isLoading();
            // const isViewingHistoricalVersion = this.$store.isViewingHistoricalVersion();
            const contentlet = this.$store.contentlet();

            // Only apply state changes if form exists
            if (this.form && contentlet) {
                // TODO: put back isViewingHistoricalVersion in the
                // condition after all fields have disabled state
                if (isLoading) {
                    this.form.disable();
                } else {
                    this.form.enable();
                }
            }
        });

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
     * Automatically handles cleanup of previous subscriptions to avoid memory leaks.
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private initializeFormListener() {
        // Clean up any existing subscription before creating a new one
        this.formValueSubscription?.unsubscribe?.();

        // Create new subscription
        this.formValueSubscription = this.form.valueChanges
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => {
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
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            this.changeDetectorRef.detectChanges();
            this.$store.setFormIsValid('invalid');
            requestAnimationFrame(() => {
                this.scrollToFirstError();
            });
            return;
        }

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
     * - disabledWYSIWYG: Preserves array format for WYSIWYG editor preferences
     * - Flattened fields: Joins array values with commas
     * - Calendar fields: Converts dates to UTC timestamps
     * - Null/undefined values: Converts to empty string
     *
     * @private
     * @param {Record<string, any>} value - The raw form value
     * @returns {FormValues} The processed form value ready for submission
     */
    private processFormValue(
        value: Record<string, string | string[] | Date | number | null | undefined>
    ): FormValues {
        return Object.fromEntries(
            Object.entries(value).map(([key, fieldValue]) => {
                // Handle disabledWYSIWYG as a special case - preserve array format
                if (key === DISABLED_WYSIWYG_FIELD) {
                    return [key, fieldValue || []];
                }

                const field = this.$formFields().find((f) => f.variable === key);

                if (!field) {
                    return [key, fieldValue];
                }

                const processedValue = processFieldValue(fieldValue, field);
                return [key, processedValue ?? ''];
            })
        );
    }

    /**
     * Creates form controls for each field in the content type.
     *
     * This method:
     * 1. Filters out non-form fields
     * 2. Creates form controls with appropriate initial values and validators
     * 3. Adds disabledWYSIWYG as a form control to track WYSIWYG editor preferences
     * 4. Combines all controls into a FormGroup
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

        // Add disabledWYSIWYG as a form control to track WYSIWYG editor preferences
        const contentlet = this.$store.contentlet();
        const disabledWYSIWYG = contentlet?.disabledWYSIWYG || [];

        controls[DISABLED_WYSIWYG_FIELD] = this.#fb.control(disabledWYSIWYG);

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

    /**
     * Handles changes to the disabledWYSIWYG attribute from field components.
     *
     * This method is triggered when any field component (WYSIWYG or textarea) changes
     * the disabledWYSIWYG configuration. It updates the form control to keep the form
     * data synchronized with the latest WYSIWYG editor preferences.
     *
     * Note: The contentlet in the store is already updated by the field components,
     * this method only ensures the form control reflects those changes.
     *
     * @param {string[]} disabledWYSIWYG - The updated disabledWYSIWYG array
     * @memberof DotEditContentFormComponent
     */
    onDisabledWYSIWYGChange(disabledWYSIWYG: string[]) {
        if (this.form && this.form.get('disabledWYSIWYG')) {
            this.form.get('disabledWYSIWYG')?.setValue(disabledWYSIWYG, { emitEvent: true });
        }
    }

    /**
     * Scrolls to the first field with validation errors and focuses on it for better accessibility.
     * Uses smooth scrolling with proper offset calculation and fallback mechanisms.
     */
    scrollToFirstError(): void {
        const errorElements =
            this.#document.querySelectorAll<HTMLDivElement>('.field-error-marker');

        if (errorElements.length === 0) {
            return;
        }

        const firstErrorField = errorElements[0];
        if (!firstErrorField) {
            return;
        }

        // Try multiple container selectors for better compatibility
        const scrollContainer = this.#document.querySelector<HTMLElement>(
            '.edit-content-layout__body'
        );
        if (!scrollContainer) {
            return;
        }

        this.#scrollToElement(firstErrorField, scrollContainer);
    }

    /**
     * Scrolls to the element within the specified container
     */
    #scrollToElement(element: HTMLElement, container: HTMLElement): void {
        const containerRect = container.getBoundingClientRect();
        const elementRect = element.getBoundingClientRect();

        // Calculate the position relative to the container
        const relativeTop = elementRect.top - containerRect.top + container.scrollTop;

        // Add some offset to ensure the element is not at the very top
        const offset = 80;
        const scrollPosition = Math.max(0, relativeTop - offset);

        container.scrollTo({
            top: scrollPosition,
            behavior: 'smooth'
        });
    }
}
