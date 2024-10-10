import { Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    inject,
    input,
    OnInit,
    output,
    signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    FormBuilder,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { TabViewModule } from 'primeng/tabview';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField, DotCMSContentTypeLayoutTab } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotWorkflowActionsComponent } from '@dotcms/ui';

import { resolutionValue } from './utils';

import { TabViewInsertDirective } from '../../directives/tab-view-insert/tab-view-insert.directive';
import { DotEditContentStore } from '../../feature/edit-content/store/edit-content.store';
import {
    CALENDAR_FIELD_TYPES,
    FLATTENED_FIELD_TYPES
} from '../../models/dot-edit-content-field.constant';
import { FIELD_TYPES } from '../../models/dot-edit-content-field.enum';
import { FILTERED_TYPES } from '../../models/dot-edit-content-form.enum';
import { EditContentForm } from '../../models/dot-edit-content-form.interface';
import { DotWorkflowActionParams } from '../../models/dot-edit-content.model';
import { getFinalCastedValue, transformLayoutToTabs } from '../../utils/functions.util';
import { DotEditContentAsideComponent } from '../dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';

@Component({
    selector: 'dot-edit-content-form',
    standalone: true,
    templateUrl: './dot-edit-content-form.component.html',
    styleUrls: ['./dot-edit-content-form.component.scss'],
    imports: [
        ReactiveFormsModule,
        DotEditContentFieldComponent,
        ButtonModule,
        DotMessagePipe,
        DotEditContentAsideComponent,
        TabViewModule,
        DotWorkflowActionsComponent,
        TabViewInsertDirective
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentFormComponent implements OnInit {
    readonly #location = inject(Location);
    readonly $store = inject(DotEditContentStore);
    readonly #destroyRef = inject(DestroyRef);

    $tabs = signal<DotCMSContentTypeLayoutTab[]>([]);

    /**
     * This property is required and expects an object of type EditContentForm.
     * It is used to populate the form with initial data and structure.
     *
     * @type {EditContentForm}
     * @memberof DotEditContentFormComponent
     */
    $formData = input.required<EditContentForm>({ alias: 'formData' });

    /**
     * Output event emitter that informs when the form has changed.
     * Emits an object of type Record<string, string> containing the updated form values.
     *
     * @memberof DotEditContentFormComponent
     */
    changeValue = output<Record<string, string>>();

    form!: FormGroup;

    readonly fieldTypes = FIELD_TYPES;
    readonly filteredTypes = FILTERED_TYPES;

    readonly #fb = inject(FormBuilder);
    readonly #dotMessageService = inject(DotMessageService);

    /**
     * Computed property that determines if the content type has multiple tabs.
     *
     * @returns {boolean} True if there are multiple tabs, false otherwise.
     * @memberof DotEditContentFormComponent
     */
    $areMultipleTabs = computed(() => this.$tabs().length > 1);

    ngOnInit() {
        if (this.$formData()) {
            this.initilizeForm();
            this.setLayoutTabs();
        }
    }

    private createFormControl(field: DotCMSContentTypeField) {
        const validators = this.getFieldValidators(field);
        const initialValue = this.getInitialValue(field);

        return this.#fb.control({ value: initialValue, disabled: field.readOnly }, validators);
    }

    private getFieldValidators(field: DotCMSContentTypeField) {
        const validators = [];
        if (field.required) validators.push(Validators.required);
        if (field.regexCheck) validators.push(Validators.pattern(field.regexCheck));

        return validators;
    }

    private getInitialValue(field: DotCMSContentTypeField) {
        const contentlet = this.$store.contentlet();

        return contentlet[field.variable] || null;
    }

    /**
     * Initializes the form group with form controls for each field in the `formData` array.
     *
     * @memberof DotEditContentFormComponent
     */
    initilizeForm() {
        this.form = this.#fb.group({});

        this.form.valueChanges.pipe(takeUntilDestroyed(this.#destroyRef)).subscribe((value) => {
            this.onFormChange(value);
        });

        this.$formData().contentType.fields.forEach((field) => {
            if (this.isFilteredType(field)) {
                return;
            }

            const fieldControl = this.initializeFormControl(field);
            this.form.addControl(field.variable, fieldControl);
        });
    }

    /**
     * Check if the field is a filtered type.
     *
     * @param {DotCMSContentTypeField} field
     * @returns {boolean}
     * @memberof DotEditContentFormComponent
     */
    isFilteredType(field: DotCMSContentTypeField): boolean {
        return Object.values(FILTERED_TYPES).includes(field.fieldType as FILTERED_TYPES);
    }

    /**
     /**
     * Emits the form value through the `formSubmit` event.
     *
     * @param {*} value
     * @memberof DotEditContentFormComponent
     */
    onFormChange(value) {
        this.$formData().contentType.fields.forEach(({ variable, fieldType }) => {
            if (FLATTENED_FIELD_TYPES.includes(fieldType as FIELD_TYPES)) {
                value[variable] = value[variable]?.join(',');
            }

            if (CALENDAR_FIELD_TYPES.includes(fieldType as FIELD_TYPES)) {
                value[variable] = value[variable]
                    ?.toISOString()
                    .replace(/T|\.\d{3}Z/g, (match: string) => (match === 'T' ? ' ' : '')); // To remove the T and .000Z from the date)
            }
        });

        this.changeValue.emit(value);
    }

    /**
     * Initializes a form control for a given DotCMSContentTypeField.
     *
     * @private
     * @param {DotCMSContentTypeField} field
     * @return {*}
     * @memberof DotEditContentFormComponent
     */
    private initializeFormControl(field: DotCMSContentTypeField): FormControl {
        const validators = [];

        const resolutionFn = resolutionValue[field.fieldType];
        const value = resolutionFn ? resolutionFn(this.$formData().contentlet, field) : '';

        if (field.required) validators.push(Validators.required);
        if (field.regexCheck) {
            try {
                const regex = new RegExp(field.regexCheck);
                validators.push(Validators.pattern(regex));
            } catch (e) {
                console.error('Invalid regex', e);
            }
        }

        return this.#fb.control(
            {
                value: getFinalCastedValue(value, field) ?? null,
                disabled: field.readOnly
            },
            { validators }
        );
    }

    /**
     * Sets the layout tabs based on the `formData.layout` property.
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private setLayoutTabs() {
        this.$tabs.set(
            transformLayoutToTabs(
                this.#dotMessageService.get('Content'),
                this.$formData().contentType.layout
            )
        );
    }

    /**
     * Fire the workflow action.
     *
     * @param {DotCMSWorkflowAction} action
     * @memberof EditContentLayoutComponent
     */
    fireWorkflowAction({ actionId, inode, contentType }: DotWorkflowActionParams): void {
        this.$store.fireWorkflowAction({
            actionId,
            inode,
            data: {
                contentlet: {
                    ...this.form.value,
                    contentType
                }
            }
        });
    }

    /**
     * Navigates back to the previous page.
     *
     * This method uses the Angular Location service to navigate
     * to the previous page in the browser's history.
     *
     * @memberof DotEditContentFormComponent
     */
    goBack(): void {
        this.#location.back();
    }
}
