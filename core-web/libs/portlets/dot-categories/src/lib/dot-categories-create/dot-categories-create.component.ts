import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';

import { DotCategory } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-categories-create',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        InputTextModule,
        InputNumberModule,
        CheckboxModule,
        TextareaModule,
        ButtonModule,
        DotMessagePipe,
        DotFieldRequiredDirective
    ],
    templateUrl: './dot-categories-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoriesCreateComponent implements OnInit {
    readonly ref = inject(DynamicDialogRef);
    readonly config = inject(DynamicDialogConfig<{ category?: DotCategory }>);

    private readonly fb = inject(FormBuilder);

    readonly form = this.fb.group({
        categoryName: ['', Validators.required],
        key: [''],
        categoryVelocityVarName: [''],
        sortOrder: [0],
        active: [true],
        description: [''],
        keywords: ['']
    });

    isEdit = false;

    ngOnInit(): void {
        const category = this.config.data?.category;
        if (category) {
            this.isEdit = true;
            this.form.patchValue({
                categoryName: category.categoryName,
                key: category.key,
                categoryVelocityVarName: category.categoryVelocityVarName,
                sortOrder: category.sortOrder,
                active: category.active,
                description: category.description || '',
                keywords: category.keywords || ''
            });
        }
    }

    onSubmit(): void {
        if (this.form.valid) {
            this.ref.close(this.form.value);
        }
    }
}
