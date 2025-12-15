import { BehaviorSubject } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { SelectItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputGroupModule } from 'primeng/inputgroup';
import { InputGroupAddonModule } from 'primeng/inputgroupaddon';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotISOItem, DotLanguage } from '@dotcms/dotcms-models';
import { DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

export interface DotLocaleCreateEditData {
    languages: DotISOItem[];
    countries: DotISOItem[];
    locale: DotLanguage | null;
    localeList: DotLanguage[];
}

/**
 * Form that allows to create or edit a locale, pass the locale.id will mean is an edit operation
 * since is locale that already exist.
 */
@Component({
    selector: 'dot-locale-create-edit',
    imports: [
        CommonModule,
        SelectModule,
        ReactiveFormsModule,
        ButtonModule,
        DotMessagePipe,
        InputGroupModule,
        InputGroupAddonModule,
        InputTextModule,
        TooltipModule,
        DotCopyButtonComponent
    ],
    templateUrl: './dot-locale-create-edit.component.html',
    styleUrl: './dot-locale-create-edit.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocaleCreateEditComponent implements OnInit {
    readonly config: DynamicDialogConfig<DotLocaleCreateEditData> = inject(DynamicDialogConfig);
    private readonly dotMessageService = inject(DotMessageService);
    private destroyRef = inject(DestroyRef);

    ref = inject(DynamicDialogRef);
    languageId: AbstractControl<string, string> | null | undefined;
    form: FormGroup = new FormGroup({});
    data: DotLocaleCreateEditData = this.config.data || {
        languages: [],
        countries: [],
        locale: null,
        localeList: []
    };
    showError = new BehaviorSubject<boolean>(false);
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

            if (this.isEditMode() || !this.isDuplicate()) {
                this.ref.close({
                    language,
                    languageCode,
                    country,
                    countryCode,
                    id: this.data.locale?.id
                });
            } else {
                this.showError.next(true);
            }
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

        this.subscribeToLanguageDropdown();
        this.subscribeToCountryDropdown();

        this.languageId = this.form.get('id');
    }

    private isEditMode(): boolean {
        return !!this.data.locale?.id;
    }

    private isDuplicate(): boolean {
        return (
            !this.isEditMode() &&
            this.data.localeList.some(
                (locale) => locale.isoCode?.toLowerCase() === this.getISOCode()
            )
        );
    }

    private subscribeToLanguageDropdown(): void {
        this.form
            .get('languageDropdown')
            ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((language: DotISOItem) => {
                this.form.get('language')?.setValue(language?.name);
                this.form.get('languageCode')?.setValue(language?.code);
                this.form.get('isoCode')?.setValue(this.getISOCode());
                this.showError.next(false);
            });
    }

    private subscribeToCountryDropdown(): void {
        this.form
            .get('countryDropdown')
            ?.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((country: DotISOItem) => {
                this.form.get('country')?.setValue(country?.name);
                this.form.get('countryCode')?.setValue(country?.code);
                this.form.get('isoCode')?.setValue(this.getISOCode());
                this.showError.next(false);
            });
    }

    private getISOCode(): string {
        const { languageCode, countryCode } = this.form.getRawValue();

        return (
            languageCode ? (countryCode ? `${languageCode}-${countryCode}` : languageCode) : ''
        ).toLowerCase();
    }
}
