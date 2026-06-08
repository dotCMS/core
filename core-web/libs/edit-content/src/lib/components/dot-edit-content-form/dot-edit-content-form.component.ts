import { race, Subscription, timer } from 'rxjs';

import { animate, style, transition, trigger } from '@angular/animations';
import { NgTemplateOutlet } from '@angular/common';
import {
    ApplicationRef,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    computed,
    DestroyRef,
    DOCUMENT,
    effect,
    inject,
    OnInit,
    output,
    signal,
    untracked
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidatorFn,
    Validators
} from '@angular/forms';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { TabsModule } from 'primeng/tabs';
import { Tag, TagModule } from 'primeng/tag';

import { filter, take } from 'rxjs/operators';

import {
    DotMessageService,
    DotWizardService,
    DotWorkflowEventHandlerService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentTypeField,
    DotCMSWorkflowAction,
    DotWorkflowPayload
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotContentletStatusPipe, DotMessagePipe } from '@dotcms/ui';

import { DotEditContentCommandBarActionsComponent } from './components/dot-edit-content-command-bar-actions/dot-edit-content-command-bar-actions.component';
import { resolutionValue } from './dot-edit-content-form-resolutions';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import { DISABLED_WYSIWYG_FIELD } from '../../models/disabledWYSIWYG.constant';
import { CONTENT_SEARCH_ROUTE } from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FormValues } from '../../models/dot-edit-content-form.interface';
import { DotWorkflowActionParams } from '../../models/dot-edit-content.model';
import { DotEditContentStore } from '../../store/edit-content.store';
import {
    generatePageEditUrl,
    generatePreviewUrl,
    getFinalCastedValue,
    isFilteredType,
    processFieldValue
} from '../../utils/functions.util';
import { blockEditorRequiredValidator } from '../../utils/validators';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

/**
 * Maps a contentlet status label to its PrimeNG Tag severity.
 *
 * Kept as a pure, exported function so the mapping stays unit-testable in isolation
 * (no component/store needed) and is consumed by the `$statusSeverity` computed.
 */
export function contentStatusSeverity(status: string): Tag['severity'] {
    switch (status) {
        case 'Published':
            return 'success';
        case 'Archived':
            return 'danger';
        case 'Revision':
            return 'info';
        case 'New':
            // Brand-new, unsaved content has no real status yet — a neutral gray pill reads as
            // informational instead of the warning-orange the other unsaved states use.
            return 'secondary';
        default:
            return 'warn';
    }
}

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
 * - Command bar with status, preview and overflow actions
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
    styleUrl: './dot-edit-content-form.component.scss',
    imports: [
        ReactiveFormsModule,
        DotEditContentFieldComponent,
        ButtonModule,
        TabsModule,
        TagModule,
        TabViewInsertDirective,
        DotMessagePipe,
        DotEditContentCommandBarActionsComponent,
        MessageModule,
        NgTemplateOutlet
    ],
    providers: [DotContentletStatusPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('fadeIn', [
            transition(':enter', [
                style({ opacity: 0 }),
                animate('250ms ease-in', style({ opacity: 1 }))
            ])
        ])
    ],
    host: {
        class: 'min-w-0 max-w-full overflow-auto overflow-x-hidden'
    }
})
export class DotEditContentFormComponent implements OnInit {
    readonly #rootStore = inject(GlobalStore);
    readonly $store = inject(DotEditContentStore);
    readonly #router = inject(Router);
    readonly #destroyRef = inject(DestroyRef);
    readonly #fb = inject(FormBuilder);
    readonly #dotWorkflowEventHandlerService = inject(DotWorkflowEventHandlerService);
    readonly #dotWizardService = inject(DotWizardService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #document = inject(DOCUMENT);
    readonly #appRef = inject(ApplicationRef);
    readonly #statusPipe = inject(DotContentletStatusPipe);

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
     * Shown for existing content that is either an HTML page or has a URL map.
     *
     * @memberof DotEditContentFormComponent
     */
    $showPreviewLink = computed(() => {
        const contentlet = this.$store.contentlet();

        return (
            !this.$store.isNew() &&
            (contentlet?.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE ||
                !!contentlet?.URL_MAP_FOR_CONTENT)
        );
    });

    /**
     * Computed property that returns true when the contentlet has at least one page reference.
     *
     * @memberof DotEditContentFormComponent
     */
    $hasReferences = computed(() => {
        const relatedContent = this.$store.information.relatedContent();

        return !!relatedContent && relatedContent !== '0';
    });

    /**
     * Status label shown in the command-bar tag. A brand-new contentlet has no status yet,
     * so it shows "New"; otherwise the contentlet state is mapped via DotContentletStatusPipe.
     *
     * @memberof DotEditContentFormComponent
     */
    $contentStatus = computed(() =>
        this.$store.isNew() ? 'New' : this.#statusPipe.transform(this.$store.contentlet())
    );

    /**
     * PrimeNG Tag severity derived from the current status label. A computed (not a template
     * method) so it is memoized and only recomputes when the status changes.
     *
     * @memberof DotEditContentFormComponent
     */
    $statusSeverity = computed<Tag['severity']>(() => contentStatusSeverity(this.$contentStatus()));

    /**
     * FormGroup instance that contains the form controls for the fields in the content type
     *
     * @type {FormGroup}
     * @memberof DotEditContentFormComponent
     */
    form!: FormGroup;

    protected readonly $shouldRenderFields = signal(true);
    protected readonly $shouldRenderPreservedFields = signal(true);

    /**
     * Subscription for form value changes - using this to manage the listener lifecycle
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private formValueSubscription?: Subscription;

    /**
     * Tracks the last contentlet version used to build the form. The effect that
     * reinitializes the form compares this against the current contentlet's
     * identifier|inode|modDate so that state-only updates (e.g. lock/unlock)
     * don't discard in-flight field state.
     */
    #lastContentletRevisionKey: string | null = null;

    #flushTimeoutId: ReturnType<typeof setTimeout> | null = null;

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
     * Context for the append template passed to TabViewInsertDirective.
     * Required for embedded view to access component variables. A computed (not a getter) so
     * the object reference is memoized and only changes when its signal dependencies change —
     * otherwise every change-detection cycle would produce a new object and re-render the
     * embedded view.
     */
    $appendContext = computed(() => {
        const currentLocale = this.$store.currentLocale();

        return {
            $store: this.$store,
            showSidebar: this.$store.isSidebarOpen(),
            $showPreviewLink: this.$showPreviewLink,
            $isPage: this.$store.isPage,
            $hasReferences: this.$hasReferences,
            contentlet: this.$store.contentlet(),
            contentType: this.$store.contentType(),
            currentLocaleId: currentLocale ? currentLocale.id.toString() : '',
            currentIdentifier: this.$store.currentIdentifier(),
            $contentStatus: this.$contentStatus,
            $statusSeverity: this.$statusSeverity,
            showPreview: () => this.showPreview()
        };
    });

    changeDetectorRef = inject(ChangeDetectorRef);

    /**
     * The system timezone.
     */
    $systemTimezone = computed(() => this.#rootStore.systemTimezone());

    ngOnInit(): void {
        if (this.$store.tabs().length) {
            const contentlet = this.$store.contentlet();
            if (contentlet) {
                this.#lastContentletRevisionKey = `${contentlet.identifier}|${contentlet.inode}|${contentlet.modDate}`;
            }
            this.initializeForm();
            this.initializeFormListener();
            this.#scheduleMarkPristineAfterInit();
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
                const revisionKey = `${contentlet.identifier}|${contentlet.inode}|${contentlet.modDate}`;

                // Skip rebuild when only lock/volatile state changed — rebuilding the
                // form here races with async child components (e.g. category field)
                // and can wipe the user's current selection.
                if (revisionKey === this.#lastContentletRevisionKey) {
                    return;
                }

                this.#lastContentletRevisionKey = revisionKey;
                this.initializeForm();
                this.initializeFormListener();
                this.#scheduleMarkPristineAfterInit();
            }
        });

        /**
         * Effect that enables or disables the form based on the loading state.
         *
         * `isViewingHistoricalVersion` was intentionally dropped from the condition for now;
         * it will be restored once all fields support a disabled state (see the TODO below).
         *
         * `contentlet()` is read with `untracked` as a mere existence guard, so the effect is
         * driven only by `isLoading`: a lock/unlock that replaces the contentlet reference
         * without changing any field must not re-run it. The enable/disable is also kept
         * idempotent (toggle only when the current state differs) and uses `{ emitEvent: false }`,
         * because a redundant `form.enable()` makes async field CVAs (e.g. the date field)
         * re-emit their value and wrongly mark the form dirty, triggering the unsaved-changes
         * guard on a plain lock toggle (#35754).
         */
        effect(() => {
            const isLoading = this.$store.isLoading();
            // const isViewingHistoricalVersion = this.$store.isViewingHistoricalVersion();
            const hasContentlet = untracked(() => !!this.$store.contentlet());

            // Only apply state changes if form exists
            if (this.form && hasContentlet) {
                // TODO: put back isViewingHistoricalVersion in the
                // condition after all fields have disabled state
                if (isLoading && this.form.enabled) {
                    this.form.disable({ emitEvent: false });
                } else if (!isLoading && this.form.disabled) {
                    this.form.enable({ emitEvent: false });
                }
            }
        });

        /**
         * Effect that initializes the form and form listener when copying locale.
         *
         * This effect listens for changes in the `isCopyingLocale` state from the store.
         * If `isCopyingLocale` is true, it initializes the form and sets up the form listener.
         * For manual translation, field components are destroyed and recreated so that
         * components with internal state (binary, date, relationship) start fresh with empty values.
         *
         * All work inside the `isCopyingLocale` branch runs inside `untracked` so that
         * `isManualTranslation` and other reads do not become reactive dependencies of this
         * effect — only `isCopyingLocale` should drive re-execution.
         */
        effect(() => {
            const isCopyingLocale = this.$store.isCopyingLocale();
            if (!isCopyingLocale) {
                return;
            }

            untracked(() => {
                const isManualTranslation = this.$store.isManualTranslation();

                // Capture values for preserved fields before form reinit so they survive
                // the new FormGroup (contentlet is null in manual translation).
                const preserved = isManualTranslation ? this.#capturePreservedFields() : null;

                this.initializeForm();
                this.initializeFormListener();
                this.#scheduleMarkPristineAfterInit();

                if (preserved) {
                    this.#restorePreservedFields(preserved);
                }

                // Keep the revision key in sync so the main reinit effect
                // doesn't rebuild the form again for the same contentlet.
                const contentlet = this.$store.contentlet();
                if (contentlet) {
                    this.#lastContentletRevisionKey = `${contentlet.identifier}|${contentlet.inode}|${contentlet.modDate}`;
                }

                if (isManualTranslation) {
                    // Only flush non-preserved fields so HOST_FOLDER and RELATIONSHIP
                    // components stay alive and keep their internal state.
                    this.#flushNonPreservedFieldsForRerender();
                } else {
                    // Populate: flush everything so binary component visual state resets.
                    this.#flushFieldsForRerender();
                }
            });
        });

        this.#destroyRef.onDestroy(() => clearTimeout(this.#flushTimeoutId));
    }

    /**
     * Marks the form pristine once the app finishes its initial async work.
     *
     * Async ControlValueAccessors — most notably the Block Editor — call
     * their registered `onChange` callback during their own initialization
     * cycle (`writeValue` → editor `create` event → `setEditorContent` →
     * `ngModelChange` → `onChange?.(...)`). Angular Forms treats that as a
     * user edit and marks the corresponding control dirty, which would
     * trigger the unsaved-changes guard / `beforeunload` prompt the moment
     * the user opens a contentlet, before they have touched anything.
     *
     * Wait for `ApplicationRef.isStable` (with a 500 ms safety fallback in
     * case the app has already settled) and reset the form to pristine.
     * Real user interactions happen well after this window — they will
     * re-mark the form dirty as expected.
     */
    #scheduleMarkPristineAfterInit(): void {
        race(this.#appRef.isStable.pipe(filter(Boolean)), timer(500))
            .pipe(take(1), takeUntilDestroyed(this.#destroyRef))
            .subscribe(() => {
                this.form?.markAsPristine();
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
            this.$store.setFormStatus('invalid');
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

                if (field.fieldType === FIELD_TYPES.CATEGORY) {
                    return [key, Array.isArray(fieldValue) ? fieldValue : []];
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
        const initialValue = this.getInitialFieldValue({
            field,
            contentlet: this.$store.contentlet(),
            isManualTranslation: this.$store.isManualTranslation()
        });
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
    private getInitialFieldValue({
        field,
        contentlet,
        isManualTranslation = false
    }: {
        field: DotCMSContentTypeField;
        contentlet: DotCMSContentlet | null;
        isManualTranslation?: boolean;
    }): unknown {
        const resolutionFn = resolutionValue[field.fieldType as FIELD_TYPES];
        if (!resolutionFn) {
            console.warn(`No resolution function found for field type: ${field.fieldType}`);

            return null;
        }

        const queryParams = this.$store.queryParams();
        const value = resolutionFn(contentlet, field, queryParams, isManualTranslation);

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
            // Block Editor needs a custom validator that checks for actual text content,
            // not just the presence of a JSON structure
            if (field.fieldType === FIELD_TYPES.BLOCK_EDITOR) {
                validators.push(blockEditorRequiredValidator());
            } else {
                validators.push(Validators.required);
            }
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
     * For HTML pages the edit-page URL is generated from the contentlet's `url`,
     * otherwise the preview URL is generated from `URL_MAP_FOR_CONTENT`.
     * Opens the resulting URL in a new tab. Logs a warning if the URL cannot be generated.
     *
     * @memberof DotEditContentFormComponent
     */
    showPreview(): void {
        const contentlet = this.$store.contentlet();
        const realUrl =
            contentlet?.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE
                ? generatePageEditUrl(contentlet)
                : generatePreviewUrl(contentlet);

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
     * This method is triggered by the PrimeNG Tabs component when the active tab changes.
     * It synchronizes the UI state with the store to maintain tab selection across renders.
     *
     * @param value - The index of the active tab
     * @memberof DotEditContentFormComponent
     */
    onActiveIndexChange(value: number | string) {
        const numberValue = Number(value);
        if (isNaN(numberValue)) {
            return;
        }
        this.$store.setActiveTab(numberValue);
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
     * Field types whose values and component state are preserved during manual translation.
     * Add to this list to protect additional fields from being cleared on locale copy.
     */
    readonly #preservedFieldTypesOnManualTranslation: FIELD_TYPES[] = [
        FIELD_TYPES.HOST_FOLDER,
        FIELD_TYPES.RELATIONSHIP
    ];

    /**
     * Returns true if the field type should survive a manual-translation reinit.
     * Used in the template to skip the flush for these fields.
     */
    isPreservedField(fieldType: string): boolean {
        return this.#preservedFieldTypesOnManualTranslation.includes(fieldType as FIELD_TYPES);
    }

    /**
     * Captures the current FormControl values for preserved fields before form reinit.
     */
    #capturePreservedFields(): Record<string, unknown> {
        return (this.$store.contentType()?.fields ?? [])
            .filter((f) =>
                this.#preservedFieldTypesOnManualTranslation.includes(f.fieldType as FIELD_TYPES)
            )
            .reduce(
                (acc, f) => {
                    const value = this.form?.get(f.variable)?.value;
                    if (value != null) {
                        acc[f.variable] = value;
                    }

                    return acc;
                },
                {} as Record<string, unknown>
            );
    }

    /**
     * Restores previously captured field values into the rebuilt form without triggering value-change events.
     */
    #restorePreservedFields(preserved: Record<string, unknown>): void {
        for (const [variable, value] of Object.entries(preserved)) {
            this.form.get(variable)?.setValue(value, { emitEvent: false });
        }
    }

    /**
     * Flushes only non-preserved field components. Used for manual translation so that
     * HOST_FOLDER and RELATIONSHIP components stay alive and keep their internal state.
     */
    #flushNonPreservedFieldsForRerender(): void {
        clearTimeout(this.#flushTimeoutId);
        this.$shouldRenderFields.set(false);
        this.#flushTimeoutId = setTimeout(() => this.$shouldRenderFields.set(true));
    }

    /**
     * Flushes ALL field components including preserved ones. Used for populate so that
     * the binary component's visual state resets completely.
     */
    #flushFieldsForRerender(): void {
        clearTimeout(this.#flushTimeoutId);
        this.$shouldRenderFields.set(false);
        this.$shouldRenderPreservedFields.set(false);
        this.#flushTimeoutId = setTimeout(() => {
            this.$shouldRenderFields.set(true);
            this.$shouldRenderPreservedFields.set(true);
        });
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
