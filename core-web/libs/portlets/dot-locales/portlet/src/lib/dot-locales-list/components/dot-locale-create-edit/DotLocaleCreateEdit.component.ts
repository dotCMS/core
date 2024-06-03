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
import { DotISOItem, DotLanguage } from '@dotcms/dotcms-models';
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
            const { language, languageCode, country, countryCode } = this.form.value;

            this.ref.close({
                language,
                languageCode,
                country,
                countryCode,
                id: this.data.locale?.id
            });
        }
    }

    handleTypeChange(): void {
        this.form.get('languageDropdown')?.setValue(null);
        this.form.get('countryDropdown')?.setValue(null);
    }

    private initForm(): void {
        this.form = new FormGroup({
            language: new FormControl(this.data.locale?.language, {
                validators: [Validators.required]
            }),
            languageCode: new FormControl(this.data.locale?.languageCode, {
                validators: [Validators.required, Validators.pattern(/^[a-zA-Z0-9-]*$/)]
            }),
            country: new FormControl(this.data.locale?.country),
            countryCode: new FormControl(this.data.locale?.countryCode),
            id: new FormControl({ value: this.data.locale?.id, disabled: true }),
            isoCode: new FormControl({ value: this.data.locale?.isoCode, disabled: true }),
            languageDropdown: new FormControl(''),
            countryDropdown: new FormControl('')
        });

        this.languageId = this.form.get('id');

        this.form.get('languageDropdown')?.valueChanges.subscribe((language: DotISOItem) => {
            this.form.get('language')?.setValue(language?.name);
            this.form.get('languageCode')?.setValue(language?.code);
            this.setISOCode();
        });

        this.form.get('countryDropdown')?.valueChanges.subscribe((country: DotISOItem) => {
            this.form.get('country')?.setValue(country?.name);
            this.form.get('countryCode')?.setValue(country?.code);
            this.setISOCode();
        });
    }

    setISOCode(): void {
        const { languageCode, countryCode } = this.form.getRawValue();
        const isoCode = this.form.get('isoCode') as AbstractControl;

        isoCode.setValue(
            languageCode ? (countryCode ? `${languageCode}-${countryCode}` : languageCode) : ''
        );
    }
}
