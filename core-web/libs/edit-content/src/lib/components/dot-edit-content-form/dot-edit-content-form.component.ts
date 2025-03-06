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
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormGroup,
    ReactiveFormsModule,
    ValidatorFn,
    Validators,
    FormsModule
} from '@angular/forms';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputSwitchChangeEvent, InputSwitchModule } from 'primeng/inputswitch';
import { MessagesModule } from 'primeng/messages';
import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

import { resolutionValue } from './utils';

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
 * This component is responsible for rendering and managing the form for editing content in DotCMS.
 * It provides a dynamic form based on the content type structure and handles form submission,
 * validation, and interaction with workflow actions. The component now uses a store to manage its state.
 *
 * Features:
 * - Dynamic form generation based on content type fields from the store
 * - Real-time form value updates
 * - Custom field type handling (e.g., calendar fields, flattened fields)
 * - Integration with DotCMS workflow actions
 * - Form validation including required fields and regex patterns
 * - Navigation back to content listing
 *
 * @example
 * <dot-edit-content-form></dot-edit-content-form>
 *
 * @export
 * @class DotEditContentFormComponent
 * @implements {OnInit}
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
     * Computed property that retrieves the first tab from the store.
     *
     * @memberof DotEditContentFormComponent
     */
    $firstTab = computed(() => this.$store.tabs()[0] || null);

    /**
     * Computed property that retrieves the rest of tabs from the store.
     *
     * @memberof DotEditContentFormComponent
     */
    $restOfTabs = computed(() => this.$store.tabs().slice(1));

    ngOnInit(): void {
        if (this.$store.tabs().length) {
            this.initializeForm();
            this.initializeFormListener();
        }
    }

    constructor() {
        /**
         * Effect that enables or disables the form based on the loading or locked state.
         */
        effect(() => {
            const isLoading = this.$store.isLoading();
            const isLocked = this.$store.isContentLocked();

            if (isLoading || isLocked) {
                this.form.disable();
            } else {
                this.form.enable();
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
     * Processes the form value, applying specific transformations for certain field types.
     *
     * @private
     * @param {Record<string, string>} value The raw form value
     * @returns {Record<string, string>} The processed form value
     * @memberof DotEditContentFormComponent
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
                    fieldValue = fieldValue
                        .toISOString()
                        .replace(/T|\.\d{3}Z/g, (match) => (match === 'T' ? ' ' : ''));
                }

                return [key, fieldValue ?? ''];
            })
        );
    }

    /**
     * Initializes the form by creating form controls for each field in the content type.
     * Skips filtered types.
     *
     * @private
     * @memberof DotEditContentFormComponent
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
     * Creates a form control for a given field.
     * Sets the initial value and validators for the control.
     *
     * @private
     * @param {DotCMSContentTypeField} field The field to create a control for
     * @returns {FormControl} The created form control
     * @memberof DotEditContentFormComponent
     */
    private createFormControl(field: DotCMSContentTypeField) {
        const initialValue = this.getInitialFieldValue(field, this.$store.contentlet());
        const validators = this.getFieldValidators(field);

        return this.#fb.control({ value: initialValue, disabled: field.readOnly }, { validators });
    }

    /**
     * Retrieves the initial value for a given field based on the contentlet data.
     *
     * This method uses a resolution function specific to the field type to extract
     * the initial value from the contentlet. If no resolution function is found for
     * the field type, it logs a warning and returns null. The resolved value is then
     * cast to the appropriate type using getFinalCastedValue.
     *
     * @private
     * @param {DotCMSContentTypeField} field - The field for which to get the initial value.
     * @param {DotCMSContentlet} contentlet - The contentlet object containing the field data.
     * @returns {any} The initial value for the field, or null if no value could be resolved.
     * @memberof DotEditContentFormComponent
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
     * Generates an array of validators for a given content type field.
     *
     * This method creates validators based on the field's properties:
     * - If the field is required, it adds a required validator.
     * - If the field has a regex check, it adds a pattern validator.
     *
     * If an invalid regex is provided, it logs an error but does not throw an exception.
     *
     * @private
     * @param {DotCMSContentTypeField} field - The field for which to generate validators.
     * @returns {ValidatorFn[]} An array of Angular validator functions for the field.
     * @memberof DotEditContentFormComponent
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
     * Fire the workflow action.
     *
     * @param {DotCMSWorkflowAction} action
     * @memberof EditContentLayoutComponent
     */
    fireWorkflowAction({
        actionId,
        inode,
        contentType,
        languageId,
        identifier
    }: DotWorkflowActionParams): void {
        this.$store.fireWorkflowAction({
            actionId,
            inode,
            data: {
                contentlet: {
                    ...this.processFormValue(this.form.value),
                    contentType,
                    languageId,
                    identifier
                }
            }
        });
    }

    /**
     * Navigates back to the content listing page with the current content type as a filter.
     *
     * This method retrieves the content type variable from the store, then uses
     * the Angular Router to navigate to the content search route. It includes
     * the content type variable as a filter in the query parameters.
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
     * Opens the preview URL in a new browser tab.
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
     * Sets the active tab index in the store when the tab view changes.
     *
     * @param {TabViewChangeEvent} event - The event object containing the active tab index.
     * @memberof DotEditContentFormComponent
     */
    onActiveIndexChange({ index }: TabViewChangeEvent) {
        this.$store.setActiveTab(index);
    }

    /**
     * Handles the change event for the content lock input switch.
     *
     * @param {InputSwitchChangeEvent} event - The event object containing the checked state.
     * @memberof DotEditContentFormComponent
     */
    onContentLockChange(event: InputSwitchChangeEvent) {
        event.checked ? this.$store.lockContent() : this.$store.unlockContent();
    }
}
