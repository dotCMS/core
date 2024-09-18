import { NgClass } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    inject,
    Injector,
    input,
    OnInit
} from '@angular/core';
import { ControlContainer, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldChipsComponent } from './components/dot-category-field-chips/dot-category-field-chips.component';
import { DotCategoryFieldDialogComponent } from './components/dot-category-field-dialog/dot-category-field-dialog.component';
import { CategoriesService } from './services/categories.service';
import { CategoryFieldStore } from './store/content-category-field.store';

/**
 * @class
 * @name DotEditContentCategoryFieldComponent
 * @description Angular component for editing a content category field.
 *
 * The `DotEditContentCategoryFieldComponent` component provides functionality for editing a content category field.
 * It is responsible for handling user interactions and updating the state of the component.
 */
@Component({
    selector: 'dot-edit-content-category-field',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        NgClass,
        DotMessagePipe,
        DotCategoryFieldChipsComponent,
        DotCategoryFieldDialogComponent
    ],
    templateUrl: './dot-edit-content-category-field.component.html',
    styleUrl: './dot-edit-content-category-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class.dot-category-field__container--has-categories]': '$hasSelectedCategories()',
        '[class.dot-category-field__container]': '!$hasSelectedCategories()'
    },
    viewProviders: [
        {
            provide: ControlContainer,
            useFactory: () => inject(ControlContainer, { skipSelf: true })
        }
    ],
    providers: [CategoriesService, CategoryFieldStore]
})
export class DotEditContentCategoryFieldComponent implements OnInit {
    readonly store = inject(CategoryFieldStore);
    readonly #form = inject(ControlContainer).control as FormGroup;
    readonly #injector = inject(Injector);

    /**
     * The `field` variable is of type `DotCMSContentTypeField` and is a required input.
     * @description The variable represents a field of a DotCMS content type and is a required input.
     */
    field = input.required<DotCMSContentTypeField>();
    /**
     * Represents a DotCMS contentlet and is a required input
     * @description DotCMSContentlet input representing a DotCMS contentlet.
     */
    contentlet = input.required<DotCMSContentlet>();
    /**
     * The `$hasConfirmedCategories` variable is a computed property that returns a boolean value.
     *
     * @returns {Boolean} - True if there are selected categories, false otherwise.
     */
    $hasSelectedCategories = computed(() => !!this.store.selected());
    /**
     * Getter to retrieve the category field control.
     *
     * @return {FormControl} The category field control.
     */
    get categoryFieldControl(): FormControl {
        return this.#form.get(this.store.fieldVariableName()) as FormControl;
    }
    /**
     * Initialize the component.
     *
     * @memberof DotEditContentCategoryFieldComponent
     */
    ngOnInit(): void {
        this.store.load({
            field: this.field(),
            contentlet: this.contentlet()
        });
        effect(
            () => {
                const categoryValues = this.store.selected();

                if (this.categoryFieldControl) {
                    this.categoryFieldControl.setValue(categoryValues);
                }
            },
            {
                injector: this.#injector
            }
        );
    }
    /**
     * Open the categories dialog.
     *
     * @memberof DotEditContentCategoryFieldComponent
     */
    openCategoriesDialog(): void {
        this.store.openDialog();
    }
}
