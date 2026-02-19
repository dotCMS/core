import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { TextareaModule } from 'primeng/textarea';

import { DotCategory } from '@dotcms/dotcms-models';
import { DotFieldRequiredDirective, DotMessagePipe } from '@dotcms/ui';

/**
 * Converts a string to camelCase, stripping all non-alphanumeric characters.
 */
function toCamelCaseVarName(value: string): string {
    return value
        .replace(/[^a-zA-Z0-9\s]/g, '')
        .split(/\s+/)
        .filter(Boolean)
        .map((word, index) =>
            index === 0
                ? word.toLowerCase()
                : word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()
        )
        .join('');
}

@Component({
    selector: 'dot-categories-create',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        InputTextModule,
        TextareaModule,
        ButtonModule,
        MessageModule,
        DotMessagePipe,
        DotFieldRequiredDirective
    ],
    templateUrl: './dot-categories-create.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCategoriesCreateComponent implements OnInit {
    readonly ref = inject(DynamicDialogRef);
    readonly config = inject(
        DynamicDialogConfig<{ category?: DotCategory; parentName?: string | null }>
    );

    private readonly fb = inject(FormBuilder);
    private readonly destroyRef = inject(DestroyRef);

    readonly form = this.fb.group({
        categoryName: ['', Validators.required],
        categoryVelocityVarName: [{ value: '', disabled: true }],
        key: [''],
        keywords: ['']
    });

    isEdit = false;
    parentName: string | null = null;

    ngOnInit(): void {
        this.parentName = this.config.data?.parentName ?? null;
        const category = this.config.data?.category;
        if (category) {
            this.isEdit = true;
            this.form.patchValue({
                categoryName: category.categoryName,
                categoryVelocityVarName: category.categoryVelocityVarName,
                key: category.key,
                keywords: category.keywords || ''
            });
        } else {
            this.form
                .get('categoryName')!
                .valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
                .subscribe((name) => {
                    this.form
                        .get('categoryVelocityVarName')!
                        .setValue(toCamelCaseVarName(name || ''));
                });
        }
    }

    onSubmit(): void {
        if (this.form.valid) {
            this.ref.close(this.form.getRawValue());
        }
    }
}
