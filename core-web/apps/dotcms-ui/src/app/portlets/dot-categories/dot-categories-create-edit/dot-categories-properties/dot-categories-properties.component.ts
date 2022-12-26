import { Component, OnInit } from '@angular/core';
import { FormControl, UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { take, takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

import { MonacoEditor } from '@models/monaco-editor';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import {
    DotCategoriesPropertiesStore,
    DotCategoriesPropertiesState
} from './store/dot-categories-properties.store';
import { DotCategoryPayload } from '@dotcms/app/shared/models/dot-categories/dot-categories.model';

@Component({
    selector: 'dot-categories-properties',
    templateUrl: './dot-categories-properties.component.html',
    styleUrls: ['./dot-categories-properties.component.scss'],
    providers: [DotCategoriesPropertiesStore]
})
export class DotCategoriesPropertiesComponent implements OnInit {
    vm$ = this.store.vm$;
    editor: MonacoEditor;
    form: UntypedFormGroup;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private store: DotCategoriesPropertiesStore,
        private fb: UntypedFormBuilder,
        private dotRouterService: DotRouterService
    ) {
        //
    }

    ngOnInit(): void {
        this.store.category$.pipe(take(1)).subscribe((state: DotCategoriesPropertiesState) => {
            const { category } = state;
            this.form = this.fb.group({
                categoryName: new FormControl(category?.categoryName ?? '', [Validators.required]),
                categoryVelocityVarName: new FormControl(category?.categoryVelocityVarName ?? ''),
                key: new FormControl(category?.key ?? ''),
                keywords: new FormControl(category?.keyWords ?? '')
            });
        });
        this.form.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
            this.store.updateIsContentTypeButtonEnabled(this.form.invalid);
        });
    }

    slugify(text: string): string {
        return text
            ? text
                  .toString()
                  .toLowerCase()
                  .replace(/\s+/g, '-') // Replace spaces with -
                  .replace(/[^\w-]+/g, '') // Remove all non-word chars
                  .replace(/--+/g, '-') // Replace multiple - with single -
                  .replace(/^-+/, '') // Trim - from start of text
                  .replace(/-+$/, '') // Trim - from end of text
            : null;
    }

    /**
     * Updates or Saves the category based on the identifier form value.
     * @return void
     * @memberof DotCategoriesPropertiesComponent
     */
    save(): void {
        const formValues: DotCategoryPayload = this.form.value;

        if (formValues.identifier) {
            this.store.editCategory(formValues);
        } else {
            delete formValues.identifier;
            formValues.categoryVelocityVarName = this.slugify(
                this.form.value.categoryVelocityVarName
            );
            this.store.saveCategory(formValues);
        }
    }

    /**
     * This method navigates the user back to previous page.
     * @return void
     * @memberof DotCategoriesPropertiesComponent
     */
    cancel(): void {
        this.dotRouterService.goToURL('/categories');
    }

    /**
     * It returns the form control with the given name
     * @param {string} controlName - The name of the control you want to get.
     * @returns {FormControl} A FormControl
     */
    getFormControl(controlName: string): FormControl {
        return this.form.get(controlName) as FormControl;
    }
}
