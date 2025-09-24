import { signalMethod } from '@ngrx/signals';

import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    forwardRef,
    inject,
    Injector,
    input,
    OnInit
} from '@angular/core';
import { NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldChipsComponent } from './../dot-category-field-chips/dot-category-field-chips.component';
import { DotCategoryFieldDialogComponent } from './../dot-category-field-dialog/dot-category-field-dialog.component';

import { BaseControlValueAccesor } from '../../../shared/base-control-value-accesor';
import { CategoriesService } from '../../services/categories.service';
import { CategoryFieldStore } from '../../store/content-category-field.store';

/**
 * @class
 * @name DotEditContentCategoryFieldComponent
 * @description Angular component for editing a content category field.
 *
 * The `DotEditContentCategoryFieldComponent` component provides functionality for editing a content category field.
 * It is responsible for handling user interactions and updating the state of the component.
 */
@Component({
    selector: 'dot-category-field',
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        DotCategoryFieldChipsComponent,
        DotCategoryFieldDialogComponent,
        DotMessagePipe
    ],
    templateUrl: './dot-category-field.component.html',
    styleUrl: './dot-category-field.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[class.dot-category-field__container--has-categories]': '$hasSelectedCategories()',
        '[class.dot-category-field__container]': '!$hasSelectedCategories()',
        '[class.dot-category-field__container--disabled]': '$isDisabled()'
    },
    providers: [
        CategoriesService,
        CategoryFieldStore,
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotCategoryFieldComponent)
        }
    ]
})
export class DotCategoryFieldComponent extends BaseControlValueAccesor<string[]> implements OnInit {
    readonly store = inject(CategoryFieldStore);
    readonly #injector = inject(Injector);
    /**
     * The `field` variable is of type `DotCMSContentTypeField` and is a required input.
     * @description The variable represents a field of a DotCMS content type and is a required input.
     */
    $field = input.required<DotCMSContentTypeField>({ alias: 'field' });
    /**
     * Represents a DotCMS contentlet and is a required input
     * @description DotCMSContentlet input representing a DotCMS contentlet.
     */
    $contentlet = input.required<DotCMSContentlet>({ alias: 'contentlet' });
    /**
     * Represents a boolean value and is a required input
     * @description Boolean input representing a boolean value.
     */
    $hasError = input.required<boolean>({ alias: 'hasError' });
    /**
     * The `$hasConfirmedCategories` variable is a computed property that returns a boolean value.
     *
     * @returns {Boolean} - True if there are selected categories, false otherwise.
     */
    $hasSelectedCategories = computed(() => this.store.selected().length > 0);

    constructor() {
        super();
        this.handleChangeValue(this.$value);
    }

    /**
     * Initialize the component.
     *
     * @memberof DotEditContentCategoryFieldComponent
     */
    ngOnInit(): void {
        // Initialize the store with field information only
        // The contentlet data will come through ControlValueAccessor's writeValue
        this.store.load({
            field: this.$field(),
            contentlet: this.$contentlet()
        });

        // Effect to sync selected categories with form control
        effect(
            () => {
                const categoryValues = this.store.selected();
                const inodes = categoryValues?.map((category) => category.inode) ?? [];

                // Notify form control of value change
                this.onChange(inodes);
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
        if (this.$isDisabled()) {
            return;
        }

        this.store.openDialog();
        this.onTouched();
    }

    readonly handleChangeValue = signalMethod<string[]>((value) => {
        if (!value) {
            this.store.setSelectedFromInodes([]);

            return;
        }

        if (!Array.isArray(value)) {
            return;
        }

        // Update store with the new value
        this.store.setSelectedFromInodes(value);
    });
}
