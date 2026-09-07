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

import { ComponentStatus, DotCMSContentlet, DotCMSContentTypeField } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldChipsComponent } from './../dot-category-field-chips/dot-category-field-chips.component';
import { DotCategoryFieldDialogComponent } from './../dot-category-field-dialog/dot-category-field-dialog.component';

import { BaseControlValueAccessor } from '../../../shared/base-control-value-accesor';
import { CategoriesService } from '../../services/categories.service';
import { CategoryFieldStore } from '../../store/content-category-field.store';
import { sameInodes } from '../../utils/category-field.utils';

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
    styleUrls: ['./dot-category-field.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'dot-category-field__container',
        '[class.dot-category-field__container--has-categories]': '$hasSelectedCategories()',
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
export class DotCategoryFieldComponent
    extends BaseControlValueAccessor<string[]>
    implements OnInit
{
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

    /**
     * Initialize the component.
     *
     * @memberof DotEditContentCategoryFieldComponent
     */
    ngOnInit(): void {
        this.store.load({
            field: this.$field(),
            contentlet: this.$contentlet()
        });

        // Effect to sync selected categories with form control.
        // Only emit once the store has successfully LOADED: the async hierarchy
        // fetch starts with `selected = []`, and emitting that empty value into
        // the form control races with save and can blank the field.
        // ERROR is intentionally skipped too — we'd rather keep whatever the
        // form control holds from `writeValue` (stale but truthful) than
        // overwrite it with an empty array derived from a failed fetch.
        effect(
            () => {
                const state = this.store.state();
                if (state !== ComponentStatus.LOADED) {
                    return;
                }

                const inodes = this.store.selected().map((category) => category.inode);

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
     * Clear all selected categories from the field without opening the dialog.
     *
     * @memberof DotEditContentCategoryFieldComponent
     */
    clearAllSelected(): void {
        if (this.$isDisabled()) {
            return;
        }

        this.store.removeRootSelected(this.store.selected().map((category) => category.key));
        this.onTouched();
    }

    override writeValue(value: string[]): void {
        super.writeValue(value);

        if (this.store.state() !== ComponentStatus.LOADED) {
            return;
        }

        const inodes = this.store.selected().map((category) => category.inode);
        if (inodes.length === 0) {
            return;
        }

        if (Array.isArray(value) && sameInodes(value, inodes)) {
            return;
        }

        // Defer to microtask: Angular calls writeValue from setUpControl
        // BEFORE assigning dir.control, so a sync onChange throws.
        queueMicrotask(() => this.onChange(inodes));
    }
}
