import { Subject } from 'rxjs';

import { Component, EventEmitter, Input, OnDestroy, OnInit, Output, inject } from '@angular/core';
import {
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { InputTextModule } from 'primeng/inputtext';

import { takeUntil } from 'rxjs/operators';

import { SiteService } from '@dotcms/dotcms-js';
import { DotCMSTempFile } from '@dotcms/dotcms-models';
import { DotMessagePipe, DotFieldValidationMessageComponent } from '@dotcms/ui';
import { camelCase } from '@dotcms/utils';

import { DotFileUpload } from '../../../../shared/models/dot-file-upload/dot-file-upload.model';
import { DotAutocompleteTagsComponent } from '../../_common/dot-autocomplete-tags/dot-autocomplete-tags.component';
import { DotSiteSelectorFieldComponent } from '../../_common/dot-site-selector-field/dot-site-selector-field.component';

@Component({
    selector: 'dot-create-persona-form',
    templateUrl: './dot-create-persona-form.component.html',
    styleUrls: ['./dot-create-persona-form.component.scss'],
    imports: [
        ReactiveFormsModule,
        FileUploadModule,
        InputTextModule,
        ButtonModule,
        DotMessagePipe,
        DotFieldValidationMessageComponent,
        DotSiteSelectorFieldComponent,
        DotAutocompleteTagsComponent
    ]
})
export class DotCreatePersonaFormComponent implements OnInit, OnDestroy {
    private fb = inject(UntypedFormBuilder);
    private siteService = inject(SiteService);

    @Input() personaName = '';
    @Output() isValid: EventEmitter<boolean> = new EventEmitter();

    form: UntypedFormGroup;
    tempUploadedFile: DotCMSTempFile;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit() {
        this.initPersonaForm();
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Handle the response of the p-fileUpload to update the form.
     *
     * @param {DotFileUpload} event
     * @memberof DotCreatePersonaFormComponent
     */
    onFileUpload(event: DotFileUpload) {
        const body = event.originalEvent.body;
        this.tempUploadedFile = body.tempFiles[0] as DotCMSTempFile;
        this.form.get('photo').setValue(this.tempUploadedFile.id);
    }

    /**
     * Remove selected image.
     *
     * @memberof DotCreatePersonaFormComponent
     */
    removeImage(): void {
        this.tempUploadedFile = null;
        this.form.get('photo').setValue('');
    }

    /**
     * Set the key tag attribute with camelCase standard based on the name.
     *
     * @memberof DotCreatePersonaFormComponent
     */
    setKeyTag(): void {
        this.form.get('keyTag').setValue(camelCase(this.form.get('name').value));
    }

    /**
     * Reset form to default values
     *
     * @memberof DotCreatePersonaFormComponent
     */
    resetForm(): void {
        this.tempUploadedFile = null;
        this.form.reset();
        this.form.get('hostFolder').setValue(this.siteService.currentSite.identifier);
    }

    private initPersonaForm(): void {
        this.form = this.fb.group({
            hostFolder: [this.siteService.currentSite.identifier, [Validators.required]],
            keyTag: [{ value: '', disabled: true }, [Validators.required]],
            name: [this.personaName, [Validators.required]],
            photo: null,
            tags: null
        });
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.isValid.emit(this.form.valid);
        });
        this.setKeyTag();
    }
}
