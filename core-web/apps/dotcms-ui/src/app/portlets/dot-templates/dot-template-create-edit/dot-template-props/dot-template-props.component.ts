import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import {
    FormsModule,
    ReactiveFormsModule,
    UntypedFormBuilder,
    UntypedFormGroup,
    Validators
} from '@angular/forms';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputTextareaModule } from 'primeng/inputtextarea';

import { map, startWith } from 'rxjs/operators';

import { DotTempFileUploadService } from '@dotcms/data-access';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotFormDialogComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotTemplateThumbnailFieldComponent } from './dot-template-thumbnail-field/dot-template-thumbnail-field.component';

import { DotThemeSelectorDropdownComponent } from '../../../../view/components/dot-theme-selector-dropdown/dot-theme-selector-dropdown.component';

@Component({
    selector: 'dot-template-props',
    templateUrl: './dot-template-props.component.html',
    styleUrls: ['./dot-template-props.component.scss'],
    providers: [DotTempFileUploadService],
    imports: [
        CommonModule,
        DotFieldValidationMessageComponent,
        DotFormDialogComponent,
        FormsModule,
        InputTextModule,
        InputTextareaModule,
        ReactiveFormsModule,
        DotMessagePipe,
        DotTemplateThumbnailFieldComponent,
        DotThemeSelectorDropdownComponent,
        DotFieldRequiredDirective
    ]
})
export class DotTemplatePropsComponent implements OnInit {
    private ref = inject(DynamicDialogRef);
    private config = inject(DynamicDialogConfig);
    private fb = inject(UntypedFormBuilder);

    form: UntypedFormGroup;

    isFormValid$: Observable<boolean>;

    ngOnInit(): void {
        const { template } = this.config.data;

        const formGroupAttrs =
            template.theme !== undefined
                ? {
                      ...template,
                      title: [template.title, Validators.required],
                      theme: [template.theme]
                  }
                : {
                      ...template,
                      title: [template.title, Validators.required]
                  };

        this.form = this.fb.group(formGroupAttrs);

        this.isFormValid$ = this.form.valueChanges.pipe(
            map(() => {
                return (
                    JSON.stringify(this.form.value) !== JSON.stringify(template) && this.form.valid
                );
            }),
            startWith(false)
        );
    }

    /**
     * Handle save button
     *
     * @memberof DotTemplatePropsComponent
     */
    onSave(): void {
        this.config.data?.onSave?.(this.form.value);
        this.ref.close(false);
    }

    /**
     * Handle cancel button
     *
     * @memberof DotTemplatePropsComponent
     */
    onCancel(): void {
        this.ref.close(true);
    }
}
