import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnInit,
    Output,
    inject
} from '@angular/core';
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
import { DotMessagePipe } from '@dotcms/ui';

import {
    CALENDAR_FIELD_TYPES,
    FLATTENED_FIELD_TYPES
} from '../../models/dot-edit-content-field.constant';
import { FILTERED_TYPES } from '../../models/dot-edit-content-form.enum';
import { EditContentPayload } from '../../models/dot-edit-content-form.interface';
import { getFinalCastedValue, transformLayoutToTabs } from '../../utils/functions.util';
import { DotEditContentAsideComponent } from '../dot-edit-content-aside/dot-edit-content-aside.component';
import { DotEditContentFieldComponent } from '../dot-edit-content-field/dot-edit-content-field.component';
import { FIELD_TYPES } from '../dot-edit-content-field/utils';

@Component({
    selector: 'dot-edit-content-form',
    standalone: true,
    templateUrl: './dot-edit-content-form.component.html',
    styleUrls: ['./dot-edit-content-form.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        CommonModule,
        ReactiveFormsModule,
        DotEditContentFieldComponent,
        ButtonModule,
        DotMessagePipe,
        DotEditContentAsideComponent,
        TabViewModule
    ]
})
export class DotEditContentFormComponent implements OnInit {
    @Input() formData!: EditContentPayload;
    @Output() changeValue = new EventEmitter();

    private readonly fb = inject(FormBuilder);
    private readonly dotMessageService = inject(DotMessageService);

    protected tabs: DotCMSContentTypeLayoutTab[] = [];
    form!: FormGroup;

    get areMultipleTabs(): boolean {
        return this.tabs.length > 1;
    }

    ngOnInit() {
        if (this.formData) {
            this.initilizeForm();
            this.setLayoutTabs();
        }
    }

    /**
     * Initializes the form group with form controls for each field in the `formData` array.
     *
     * @memberof DotEditContentFormComponent
     */
    initilizeForm() {
        this.form = this.fb.group({});

        this.formData.fields.forEach((field) => {
            if (Object.values(FILTERED_TYPES).includes(field.fieldType as FILTERED_TYPES)) {
                return;
            }

            const fieldControl = this.initializeFormControl(field);
            this.form.addControl(field.variable, fieldControl);
        });

        this.form.valueChanges.subscribe((value) => {
            this.onFormChange(value);
        });
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

        const value = this.formData.contentlet?.[field.variable] ?? field.defaultValue;

        if (field.required) validators.push(Validators.required);
        if (field.regexCheck) {
            try {
                const regex = new RegExp(field.regexCheck);
                validators.push(Validators.pattern(regex));
            } catch (e) {
                console.error('Invalid regex', e);
            }
        }

        return this.fb.control(
            {
                value: getFinalCastedValue(value, field) ?? null,
                disabled: field.readOnly
            },
            { validators }
        );
    }

    /**
    /**
     * Emits the form value through the `formSubmit` event.
     *
     * @param {*} value
     * @memberof DotEditContentFormComponent
     */
    onFormChange(value) {
        this.formData.fields.forEach(({ variable, fieldType }) => {
            // Shorthand for conditional assignment

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
     * Sets the layout tabs based on the `formData.layout` property.
     *
     * @private
     * @memberof DotEditContentFormComponent
     */
    private setLayoutTabs() {
        this.tabs = transformLayoutToTabs(
            this.dotMessageService.get('Content'),
            this.formData.layout
        );
    }
}
