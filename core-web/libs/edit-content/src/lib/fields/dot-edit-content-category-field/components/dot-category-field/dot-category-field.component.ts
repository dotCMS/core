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
        // `store.load()` reads selected categories directly from the contentlet
        // — that is the single source of truth on mount. We intentionally do NOT
        // wire `handleChangeValue` (writeValue → store) anymore: when the parent
        // form is rebuilt (e.g. after a reset-workflow subaction changes the
        // contentlet's modDate), writeValue and load() ran in parallel and both
        // hit `setSelectedFromInodes`. A racing empty/early-exit branch could
        // set `selected: []` and `state: LOADED`, causing the effect below to
        // emit `onChange([])` and blank the form control — even though chips
        // were still rendered from the in-flight load result.
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
     * When the parent form is rebuilt (e.g. after a reset-workflow subaction
     * patches the contentlet with a new modDate), this component instance is
     * reused but the new form control writes its raw initial value — a CSV
     * string produced by `castSingleSelectableValue`, not the inode array the
     * LOADED effect last emitted. Because `store.state()` doesn't change on
     * reuse (it's already LOADED), the effect won't refire and the form keeps
     * the stale string, which `processFormValue` later turns into `[]`,
     * blanking required category fields on save.
     *
     * On every writeValue, if the store already has selected categories from
     * a prior load, push their inodes back into the form control to keep it
     * in sync with the visible chips. The onChange call is deferred to a
     * microtask because writeValue is invoked synchronously inside
     * `FormGroupDirective._updateDomValue` -> `setUpControl(...)`, BEFORE
     * Angular assigns `dir.control = newCtrl`. Calling onChange synchronously
     * inside setUpControl walks through `viewToModelUpdate` and dereferences
     * the not-yet-assigned `dir.control`, throwing
     * "no FormControl instance attached".
     */
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

        queueMicrotask(() => this.onChange(inodes));
    }
}

function sameInodes(value: string[], inodes: string[]): boolean {
    if (value.length !== inodes.length) {
        return false;
    }

    for (let i = 0; i < inodes.length; i++) {
        if (value[i] !== inodes[i]) {
            return false;
        }
    }

    return true;
}
