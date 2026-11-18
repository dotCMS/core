import { MarkdownComponent } from 'ngx-markdown';
import { Subscription } from 'rxjs';

import { NgTemplateOutlet } from '@angular/common';
import {
    Component,
    computed,
    effect,
    ElementRef,
    inject,
    input,
    OnDestroy,
    OnInit,
    output,
    viewChild,
    ChangeDetectionStrategy
} from '@angular/core';
import {
    FormGroupDirective,
    ReactiveFormsModule,
    UntypedFormControl,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotAppsSecret } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotIconComponent } from '@dotcms/ui';

import { DotAppsCodeBlocksDirective } from '../../directives/dot-apps-code-block.directive';
import { DotAppsConfigurationDetailGeneratedStringFieldComponent } from '../dot-apps-configuration-detail-generated-string-field/dot-apps-configuration-detail-generated-string-field.component';
import { DotAppsConfigurationDetailJsonFieldComponent } from '../dot-apps-configuration-detail-json-field/dot-apps-configuration-detail-json-field.component';
import { DotAppsUveConfigFieldComponent } from '../dot-apps-uve-config-field/dot-apps-uve-config-field.component';
import { UVE_APP_KEY } from '../dot-apps-uve-config-field/dot-apps-uve-config.utils';

/** Apps whose configuration form is centered on the page; add keys as more apps are redesigned. */
const CENTERED_FORM_APP_KEYS: readonly string[] = [UVE_APP_KEY];

enum FieldStatus {
    EDITABLE,
    DISABLED,
    DISABLED_WITH_MESSAGE
}

@Component({
    selector: 'dot-apps-configuration-detail-form',
    templateUrl: './dot-apps-configuration-detail-form.component.html',
    styleUrls: ['./dot-apps-configuration-detail-form.component.scss'],
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        CheckboxModule,
        SelectModule,
        InputTextModule,
        TextareaModule,
        TooltipModule,
        DotIconComponent,
        DotFieldRequiredDirective,
        MarkdownComponent,
        DotAppsCodeBlocksDirective,
        DotAppsConfigurationDetailGeneratedStringFieldComponent,
        DotAppsConfigurationDetailJsonFieldComponent,
        DotAppsUveConfigFieldComponent,
        NgTemplateOutlet
    ],
    changeDetection: ChangeDetectionStrategy.Eager,
    providers: [FormGroupDirective]
})
export class DotAppsConfigurationDetailFormComponent implements OnInit, OnDestroy {
    #dotMessageService = inject(DotMessageService);

    $formContainer = viewChild<ElementRef>('formContainer');

    $formFields = input<DotAppsSecret[]>([], { alias: 'formFields' });
    $appConfigured = input<boolean>(false, { alias: 'appConfigured' });
    /** Key of the app being configured; the UVE app gets a rule editor for its JSON param. */
    $appKey = input<string>('', { alias: 'appKey' });

    /** True on the UVE app, whose JSON param gets the route editor. */
    readonly $isUveApp = computed(() => this.$appKey() === UVE_APP_KEY);

    /** True for apps that use the centered form layout. */
    readonly $shouldCenterForm = computed(() => CENTERED_FORM_APP_KEYS.includes(this.$appKey()));

    readonly data = output<{ [key: string]: string }>();
    readonly valid = output<boolean>();

    myFormGroup: UntypedFormGroup = new UntypedFormGroup({});
    private valueChangesSubscription?: Subscription;
    private isDestroyed = false;

    constructor() {
        // TODO: (migration) this is not working, but is not working in demo either
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

        this.$formFields()
            .filter((field: DotAppsSecret) => field.type !== 'HEADING' && field.type !== 'INFO')
            .forEach((field: DotAppsSecret) => {
                const status = this.resolveFieldStatus(field);
                group[field.name] = new UntypedFormControl(
                    this.getFieldValue(field, status),
                    field.required && status === FieldStatus.EDITABLE ? Validators.required : null
                );
            });

        this.myFormGroup = new UntypedFormGroup(group);

        this.valueChangesSubscription = this.myFormGroup.valueChanges.subscribe(() => {
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
        JSON: (field: DotAppsSecret, status: FieldStatus) => {
            return this.getFieldValueFn.STRING(field, status);
        },
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
        if (this.isDestroyed) {
            return;
        }
        this.data.emit(this.myFormGroup.value);
        this.valid.emit(this.myFormGroup.status === 'VALID');
    }

    private resolveFieldStatus(field: DotAppsSecret): FieldStatus {
        if ((!field.hasEnvVar && !field.value) || !field.hasEnvVarValue) {
            return FieldStatus.EDITABLE;
        }

        return field.envShow ? FieldStatus.DISABLED : FieldStatus.DISABLED_WITH_MESSAGE;
    }

    ngOnDestroy(): void {
        this.isDestroyed = true;
        if (this.valueChangesSubscription) {
            this.valueChangesSubscription.unsubscribe();
        }
    }
}
