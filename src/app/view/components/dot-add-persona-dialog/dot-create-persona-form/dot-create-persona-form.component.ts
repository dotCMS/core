import { Component, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { take, takeUntil } from 'rxjs/operators';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DotMessageService } from '@services/dot-messages-service';
import * as _ from 'lodash';
import { Subject } from 'rxjs';
import { DotFileUpload } from '@models/dot-file-upload/dot-file-upload.model';
import { DotCMSTempFile } from 'dotcms-models';
import { SiteService } from 'dotcms-js';

@Component({
    selector: 'dot-create-persona-form',
    templateUrl: './dot-create-persona-form.component.html',
    styleUrls: ['./dot-create-persona-form.component.scss']
})
export class DotCreatePersonaFormComponent implements OnInit, OnDestroy {
    @Output() isValid: EventEmitter<Boolean> = new EventEmitter();

    form: FormGroup;
    imageName: string;
    messagesKey: { [key: string]: string } = {};

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public dotMessageService: DotMessageService,
        private fb: FormBuilder,
        private siteService: SiteService
    ) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'modes.persona.upload.file',
                'modes.persona.name',
                'modes.persona.key.tag',
                'dot.common.choose',
                'dot.common.remove',
                'modes.persona.host',
                'modes.persona.name.error.required'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
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
        const response = JSON.parse(event.xhr.response);
        const tempFile: DotCMSTempFile = response.tempFiles[0];
        this.imageName = event.files[0].name;
        this.form.get('photo').setValue(tempFile.id);
    }

    /**
     * Remove selected image.
     *
     * @memberof DotCreatePersonaFormComponent
     */
    removeImage(): void {
        this.imageName = null;
        this.form.get('photo').setValue('');
    }

    /**
     * Set the key tag attribute with camelCase standard based on the name.
     *
     * @memberof DotCreatePersonaFormComponent
     */
    setKeyTag(): void {
        this.form.get('keyTag').setValue(_.camelCase(this.form.get('name').value));
    }

    /**
     * Reset form to default values
     *
     * @memberof DotCreatePersonaFormComponent
     */
    resetForm(): void {
        this.imageName = null;
        this.form.reset();
        this.form.get('hostFolder').setValue(this.siteService.currentSite.identifier);
    }

    private initPersonaForm(): void {
        this.form = this.fb.group({
            hostFolder: [this.siteService.currentSite.identifier, [Validators.required]],
            keyTag: [{ value: null, disabled: true }, [Validators.required]],
            name: [null, [Validators.required]],
            photo: null
        });
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.isValid.emit(this.form.valid);
        });
    }
}
