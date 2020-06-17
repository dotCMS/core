import {
    Component,
    OnInit,
    Input,
    Output,
    EventEmitter,
    ViewChild,
    ElementRef
} from '@angular/core';
import { FormControl, FormGroup, Validators, NgForm } from '@angular/forms';
import { DotAppsSecrets } from '@shared/models/dot-apps/dot-apps.model';

const getFieldValueFn = {
    BOOL: (field: DotAppsSecrets) => {
        return field.value ? JSON.parse(field.value) : field.value;
    },
    SELECT: (field: DotAppsSecrets) => {
        return field.value === '' ? field.options[0].value : field.value;
    }
};

@Component({
    selector: 'dot-apps-configuration-detail-form',
    templateUrl: './dot-apps-configuration-detail-form.component.html',
    styleUrls: ['./dot-apps-configuration-detail-form.component.scss']
})
export class DotAppsConfigurationDetailFormComponent implements OnInit {
    @ViewChild('form') public form: NgForm;
    @ViewChild('formContainer') public formContainer: ElementRef;

    @Input() formFields: DotAppsSecrets[];
    @Output() data = new EventEmitter<{ [key: string]: string }>();
    @Output() valid = new EventEmitter<boolean>();
    myFormGroup: FormGroup;

    constructor() {}

    ngOnInit() {
        const group = {};
        this.formFields.forEach((field: DotAppsSecrets) => {
            group[field.name] = new FormControl(
                this.getFieldValue(field),
                field.required ? Validators.required : null
            );
        });
        this.myFormGroup = new FormGroup(group);

        this.myFormGroup.valueChanges.subscribe(() => {
            this.emitValues();
        });

        this.emitValues();

        setTimeout(() => {
            this.formContainer.nativeElement.querySelector(`#${this.formFields[0].name}`).focus();
        }, 0);
    }

    private getFieldValue(field: DotAppsSecrets): string | boolean {
        return getFieldValueFn[field.type] ? getFieldValueFn[field.type](field) : field.value;
    }

    private emitValues(): void {
        this.data.emit(this.myFormGroup.value);
        this.valid.emit(this.myFormGroup.status === 'VALID');
    }
}
