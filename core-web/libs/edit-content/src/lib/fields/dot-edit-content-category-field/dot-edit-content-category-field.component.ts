import {
    ChangeDetectionStrategy,
    Component,
    computed,
    effect,
    forwardRef,
    inject,
    Injector,
    input,
    OnInit,
    signal
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';

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
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        DotMessagePipe,
        DotCategoryFieldChipsComponent,
        DotCategoryFieldDialogComponent
    ],
    templateUrl: './dot-edit-content-category-field.component.html',
    styleUrl: './dot-edit-content-category-field.component.scss',
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
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotEditContentCategoryFieldComponent),
            multi: true
        }
    ]
})
export class DotEditContentCategoryFieldComponent implements OnInit, ControlValueAccessor {
    readonly store = inject(CategoryFieldStore);
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

    // ControlValueAccessor callbacks
    private onChange: (value: string[]) => void = () => {
        // Callback will be set by registerOnChange
    };
    private onTouched: () => void = () => {
        // Callback will be set by registerOnTouched
    };
    protected $isDisabled = signal(false);

    /**
     * The `$hasConfirmedCategories` variable is a computed property that returns a boolean value.
     *
     * @returns {Boolean} - True if there are selected categories, false otherwise.
     */
    $hasSelectedCategories = computed(() => this.store.selected().length > 0);

    /**
     * Initialize the component.
     *
     * @memberof DotEditContentCategoryFieldComponent
     */
    ngOnInit(): void {
        // Initialize the store with field information only
        // The contentlet data will come through ControlValueAccessor's writeValue
        this.store.load({
            field: this.field(),
            contentlet: this.contentlet()
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

    /**
     * Sets the value in the component when the form control value changes.
     * This method is called by Angular's forms system.
     *
     * @param value - Array of category inode strings
     */
    writeValue(value: string[]): void {
        if (!value) {
            this.store.setSelectedFromInodes([]);

            return;
        }

        if (!Array.isArray(value)) {
            return;
        }

        // Update store with the new value
        this.store.setSelectedFromInodes(value);
    }

    /**
     * Registers a callback function that is called when the control's value changes in the UI.
     *
     * @param fn - The callback function to register
     */
    registerOnChange(fn: (value: string[]) => void): void {
        this.onChange = fn;
    }

    /**
     * Registers a callback function that is called when the control is marked as touched in the UI.
     *
     * @param fn - The callback function to register
     */
    registerOnTouched(fn: () => void): void {
        this.onTouched = fn;
    }

    /**
     * Sets the disabled state of the component.
     * This method is called by Angular when the form control's disabled state changes.
     *
     * @param isDisabled - Whether the component should be disabled
     */
    setDisabledState(isDisabled: boolean): void {
        this.$isDisabled.set(isDisabled);
    }
}
