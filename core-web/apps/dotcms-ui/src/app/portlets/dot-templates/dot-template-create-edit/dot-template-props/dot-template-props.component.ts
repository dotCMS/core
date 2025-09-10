import { Observable } from 'rxjs';

import { Component, OnInit, inject } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { map, startWith } from 'rxjs/operators';

@Component({
    selector: 'dot-template-props',
    templateUrl: './dot-template-props.component.html',
    styleUrls: ['./dot-template-props.component.scss'],
    standalone: false
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
