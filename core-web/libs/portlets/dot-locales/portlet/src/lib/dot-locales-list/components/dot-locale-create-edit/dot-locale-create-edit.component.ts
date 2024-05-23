import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import {
    AbstractControl,
    FormControl,
    FormGroup,
    ReactiveFormsModule,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotLanguage } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotLocalesListStore } from '../../store/dot-locales-list.store';

export interface DotLocaleCreateEditData {
    languages: [];
    countries: [];
    locale: DotLanguage | null;
}

@Component({
    selector: 'dot-dot-locale-create-edit',
    standalone: true,
    imports: [DropdownModule, ReactiveFormsModule, InputTextModule, ButtonModule, DotMessagePipe],
    templateUrl: './dot-locale-create-edit.component.html',
    styleUrl: './dot-locale-create-edit.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotLocaleCreateEditComponent implements OnInit {
    readonly config: DynamicDialogConfig<DotLocaleCreateEditData> = inject(DynamicDialogConfig);
    ref = inject(DynamicDialogRef);
    store = inject(DotLocalesListStore);
    languageId: AbstractControl<string, string> | null | undefined;

    form: FormGroup = new FormGroup({});
    data: DotLocaleCreateEditData = this.config.data || {
        languages: [],
        countries: [],
        locale: null
    };

    ngOnInit(): void {
        this.initForm();
    }

    handleSubmit(): void {
        this.store.addLocale(this.form.value);
        this.ref.close();
    }

    private initForm(): void {
        this.form = new FormGroup({
            language: new FormControl(this.data.locale?.id ? this.data.locale : '', {
                validators: [Validators.required]
            }),
            country: new FormControl(this.data.locale?.id ? this.data.locale.country : ''),
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
