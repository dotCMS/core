import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { Observable } from 'rxjs';
import { map, take } from 'rxjs/operators';

@Component({
    selector: 'dot-dot-template-props',
    templateUrl: './dot-template-props.component.html',
    styleUrls: ['./dot-template-props.component.scss']
})
export class DotTemplatePropsComponent implements OnInit {
    form: FormGroup;

    isFormValid$: Observable<boolean>;

    constructor(
        private ref: DynamicDialogRef,
        private config: DynamicDialogConfig,
        private fb: FormBuilder,
        private dotTemplatesService: DotTemplatesService
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
        this.dotTemplatesService
            .update(this.form.value)
            .pipe(take(1))
            .subscribe(() => {
                this.config.data.doSomething(this.form.value);
                this.ref.close();
            });
    }
}
