import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'dot-template-props',
    templateUrl: './dot-template-props.component.html',
    styleUrls: ['./dot-template-props.component.scss']
})
export class DotTemplatePropsComponent implements OnInit {
    form: FormGroup;

    isFormValid$: Observable<boolean>;

    constructor(
        private ref: DynamicDialogRef,
        private config: DynamicDialogConfig,
        private fb: FormBuilder
    ) {}

    ngOnInit(): void {
        const { template } = this.config.data;

        this.form = this.fb.group({
            ...template,
            title: [template.title, Validators.required]
        });

        this.isFormValid$ = this.form.valueChanges.pipe(
            map(() => {
                return (
                    JSON.stringify(this.form.value) !== JSON.stringify(template) && this.form.valid
                );
            })
        );
    }

    /**
     * Handle save button
     *
     * @memberof DotTemplatePropsComponent
     */
    onSave(): void {
        this.config.data?.onSave?.(this.form.value);
        this.ref.close();
    }

    /**
     * Handle cancel button
     *
     * @memberof DotTemplatePropsComponent
     */
    onCancel(): void {
        this.config.data?.onCancel?.();
        this.ref.close();
    }
}
