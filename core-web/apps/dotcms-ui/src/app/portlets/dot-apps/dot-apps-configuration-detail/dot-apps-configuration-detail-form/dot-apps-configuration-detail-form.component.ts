import {
    Component,
    effect,
    ElementRef,
    inject,
    input,
    OnInit,
    output,
    viewChild
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';

import { DotMessageService } from '@dotcms/data-access';
import { DotAppsSecret } from '@dotcms/dotcms-models';

enum FieldStatus {
    EDITABLE,
    DISABLED,
    DISABLED_WITH_MESSAGE
}

@Component({
    selector: 'dot-apps-configuration-detail-form',
    templateUrl: './dot-apps-configuration-detail-form.component.html',
    styleUrls: ['./dot-apps-configuration-detail-form.component.scss'],
    standalone: false
})
export class DotAppsConfigurationDetailFormComponent implements OnInit {
    #dotMessageService = inject(DotMessageService);

    $formContainer = viewChild<ElementRef>('formContainer');

    $formFields = input<DotAppsSecret[]>([], { alias: 'formFields' });
    $appConfigured = input<boolean>(false, { alias: 'appConfigured' });

    readonly data = output<{ [key: string]: string }>();
    readonly valid = output<boolean>();

    myFormGroup: UntypedFormGroup;

    constructor() {
        effect(() => {
            const formFields = this.$formFields();
            const formContainer = this.$formContainer();

            if (formFields.length > 0 && formContainer) {
                const firstField = formContainer.nativeElement?.querySelector(
                    `#${formFields[0].name}`
                );
                if (firstField && typeof firstField.focus === 'function') {
                    firstField.focus();
                }
            }
        });
    }

    ngOnInit() {
        const group = {};

        this.$formFields().forEach((field: DotAppsSecret) => {
            const status = this.resolveFieldStatus(field);
            group[field.name] = new UntypedFormControl(
                this.getFieldValue(field, status),
                field.required && status === FieldStatus.EDITABLE ? Validators.required : null
            );
        });

        this.myFormGroup = new UntypedFormGroup(group);

        this.myFormGroup.valueChanges.subscribe(() => {
            this.emitValues();
        });

        setTimeout(() => this.emitValues());
    }

    /**
     * Handle integration action of third parties in a new window
     * @param {string} url
     * @memberof DotAppsConfigurationDetailFormComponent
     */
    onIntegrate(url: string): void {
        window.open(url, '_blank');
    }

    private getFieldValueFn = {
        BOOL: (field: DotAppsSecret) => {
            return field.value ? JSON.parse(field.value) : field.value;
        },
        SELECT: (field: DotAppsSecret) => {
            return field.value === '' ? field.options[0].value : field.value;
        },
        STRING: (field: DotAppsSecret, status: FieldStatus) => {
            const fieldValue =
                status === FieldStatus.DISABLED_WITH_MESSAGE
                    ? this.#dotMessageService.get('apps.param.set.from.env')
                    : field.value;

            return {
                value: fieldValue,
                disabled:
                    status === FieldStatus.DISABLED || status === FieldStatus.DISABLED_WITH_MESSAGE
            };
        }
    };

    private getFieldValue(field: DotAppsSecret, status: FieldStatus): string | boolean {
        return this.getFieldValueFn[field.type]
            ? this.getFieldValueFn[field.type](field, status)
            : field.value;
    }

    private emitValues(): void {
        this.data.emit(this.myFormGroup.value);
        this.valid.emit(this.myFormGroup.status === 'VALID');
    }

    private resolveFieldStatus(field: DotAppsSecret): FieldStatus {
        if ((!field.hasEnvVar && !field.value) || !field.hasEnvVarValue) {
            return FieldStatus.EDITABLE;
        }

        return field.envShow ? FieldStatus.DISABLED : FieldStatus.DISABLED_WITH_MESSAGE;
    }
}
