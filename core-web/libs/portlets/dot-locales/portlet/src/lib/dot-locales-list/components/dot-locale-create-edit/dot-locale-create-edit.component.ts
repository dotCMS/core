import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DropdownModule } from 'primeng/dropdown';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '@dotcms/ui';

import { DotLocalesListStore } from '../../store/dot-locales-list.store';

export interface DotLocaleCreateEditData {
    languages: [];
    countries: [];
    id: string;
    languageCode: string;
    language: string;
    countryCode: string;
    country: string;
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
    private readonly ref = inject(DynamicDialogRef);
    readonly config: DynamicDialogConfig<DotLocaleCreateEditData> = inject(DynamicDialogConfig);
    store = inject(DotLocalesListStore);

    form: FormGroup = new FormGroup({});
    data: DotLocaleCreateEditData = this.config.data || {
        languages: [],
        countries: [],
        id: '',
        languageCode: '',
        language: '',
        countryCode: '',
        country: ''
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
            language: new FormControl(this.data.id ? this.data.language : '', {
                validators: [Validators.required]
            }),
            country: new FormControl(this.data.id ? this.data.country : ''),
            localeId: new FormControl(this.data.id ? this.data.countryCode : '', {
                validators: [Validators.required]
            }),
            languageId: new FormControl(this.data.id || '')
        });
    }
}
