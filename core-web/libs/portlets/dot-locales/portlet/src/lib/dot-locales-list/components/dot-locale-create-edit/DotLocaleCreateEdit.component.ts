import { CdkCopyToClipboard } from '@angular/cdk/clipboard';
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { SelectItem } from 'primeng/api/selectitem';
import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

export interface DotLocaleCreateEditData {
    languages: [];
    countries: [];
    locale: DotLanguage | null;
}

@Component({
    selector: 'dot-locale-create-edit',
    standalone: true,
    imports: [
        CommonModule,
        DropdownModule,
        ReactiveFormsModule,
        ButtonModule,
        DotMessagePipe,
        InputTextModule,
        CdkCopyToClipboard,
        TooltipModule
    ],
    templateUrl: './DotLocaleCreateEdit.component.html',
    styleUrl: './DotLocaleCreateEdit.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocaleCreateEditComponent implements OnInit {
    readonly config: DynamicDialogConfig<DotLocaleCreateEditData> = inject(DynamicDialogConfig);
    private readonly dotMessageService = inject(DotMessageService);

    ref = inject(DynamicDialogRef);

    languageId: AbstractControl<string, string> | null | undefined;

    form: FormGroup = new FormGroup({});
    data: DotLocaleCreateEditData = this.config.data || {
        languages: [],
        countries: [],
        locale: null
    };

    localeType: SelectItem[] = [
        { label: this.dotMessageService.get('locale.standard.locale'), value: 1 },
        { label: this.dotMessageService.get('locale.Custom.locale'), value: 2 }
    ];

    ngOnInit(): void {
        this.initForm();
    }

    handleSubmit(): void {
        if (this.form.valid) {
            const value = this.form.value;
            if (this.data.locale?.id) {
                // edit
                this.ref.close(value);
            } else {
                // add
            }
        }

        this.ref.close(this.form.value);
    }

    private initForm(): void {
        this.form = new FormGroup({
            language: new FormControl(this.data.locale?.language, {
                validators: [Validators.required]
            }),
            languageCode: new FormControl(this.data.locale?.languageCode),
            country: new FormControl(this.data.locale?.country),
            countryCode: new FormControl(this.data.locale?.countryCode),
            id: new FormControl({ value: this.data.locale?.id, disabled: true }),
            isoCode: new FormControl({ value: this.data.locale?.isoCode, disabled: true }),
            localeId: new FormControl(
                {
                    value: this.data.locale?.countryCode,
                    disabled: !!this.data.locale?.id
                },
                {
                    validators: [Validators.required]
                }
            ),
            languageId: new FormControl({ value: this.data.locale?.id || '', disabled: true })
        });

        this.languageId = this.form.get('languageId');
    }
}
