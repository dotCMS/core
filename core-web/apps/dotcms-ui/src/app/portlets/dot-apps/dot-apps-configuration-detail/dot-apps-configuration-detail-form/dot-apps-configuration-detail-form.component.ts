import {
    AfterViewInit,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';
import { NgForm, UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';

import { DotAppsSecret } from '@dotcms/dotcms-models';
import { DotMessageService } from '@dotcms/data-access';

const getFieldValueFn = {
    BOOL: (field: DotAppsSecret) => {
        return field.value ? JSON.parse(field.value) : field.value;
    },
    SELECT: (field: DotAppsSecret) => {
        return field.value === '' ? field.options[0].value : field.value;
    }
};

@Component({
    selector: 'dot-apps-configuration-detail-form',
    templateUrl: './dot-apps-configuration-detail-form.component.html',
    styleUrls: ['./dot-apps-configuration-detail-form.component.scss']
})
export class DotAppsConfigurationDetailFormComponent implements OnInit, AfterViewInit {
    @ViewChild('form', { static: true }) public form: NgForm;
    @ViewChild('formContainer', { static: true }) public formContainer: ElementRef;

    @Input() formFields: DotAppsSecret[];
    @Input() appConfigured = false;
    @Output() data = new EventEmitter<{ [key: string]: string }>();
    @Output() valid = new EventEmitter<boolean>();
    myFormGroup: UntypedFormGroup;

    constructor(private dotMessageService: DotMessageService) {}

    ngOnInit() {
        const group = {};

        this.formFields.forEach((field: DotAppsSecret) => {
            const fieldValue = !!field.editable
                ? field.value
                : this.dotMessageService.get('apps.param.set.from.env');
            group[field.name] = new UntypedFormControl(
                field.type === 'STRING'
                    ? { value: fieldValue, disabled: !field.editable }
                    : fieldValue,
                field.required && !!field.editable ? Validators.required : null
            );
        });
        this.myFormGroup = new UntypedFormGroup(group);

        this.myFormGroup.valueChanges.subscribe(() => {
            this.emitValues();
        });

        setTimeout(() => this.emitValues());
    }

    ngAfterViewInit() {
        // Do it this way because the form is rendered dynamically
        this.formContainer.nativeElement.querySelector(`#${this.formFields[0].name}`).focus();
    }

    /**
     * Handle integration action of third parties in a new window
     * @param {string} url
     * @memberof DotAppsConfigurationDetailFormComponent
     */
    onIntegrate(url: string): void {
        window.open(url, '_blank');
    }

    private getFieldValue(field: DotAppsSecret): string | boolean {
        return getFieldValueFn[field.type] ? getFieldValueFn[field.type](field) : field.value;
    }

    private emitValues(): void {
        this.data.emit(this.myFormGroup.value);
        this.valid.emit(this.myFormGroup.status === 'VALID');
    }
}
