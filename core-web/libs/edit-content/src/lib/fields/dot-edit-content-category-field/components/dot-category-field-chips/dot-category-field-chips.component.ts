import {
    ChangeDetectionStrategy,
    Component,
    computed,
    EventEmitter,
    inject,
    input,
    Output,
    signal
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';

import { MAX_CHIPS } from '../../dot-edit-content-category-field.const';
import { DotCategoryFieldKeyValueObj } from '../../models/dot-category-field.models';

/**
 * Represents the Dot Category Field Chips component.
 *
 * @export
 * @class DotCategoryFieldChipsComponent
 */
@Component({
    selector: 'dot-category-field-chips',
    standalone: true,
    imports: [ButtonModule, ChipModule, TooltipModule],
    templateUrl: './dot-category-field-chips.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'dot-category-field__categories'
    }
})
export class DotCategoryFieldChipsComponent {
    /**
     * Represents the variable 'showAll' which is of type 'signal<boolean>'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    $showAll = signal(false);
    /**
     * Represents the variable 'max' which is of type 'number'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    $max = input<number>(MAX_CHIPS, { alias: 'max' });
    /**
     * Represents the variable 'categories' which is of type 'DotCategoryFieldKeyValueObj[]'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    $categories = input<DotCategoryFieldKeyValueObj[]>([], { alias: 'categories' });
    /**
     * Represents the variable 'label' which is of type 'string'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    $categoriesToShow = computed(() => {
        const categories = this.$categories();
        if (this.$showAll()) {
            return categories;
        }

        return categories.slice(0, this.$max());
    });
    /**
     * Represents the variable '$showAllBtn' which is of type 'computed<boolean>'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    $showAllBtn = computed(() => {
        const size = this.$categories().length;

        if (size > this.$max()) {
            return true;
        }

        return false;
    });
    /**
     * Represents the output 'remove' which is of type 'EventEmitter<string>'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    @Output() remove = new EventEmitter<string>();
    /**
     *  Represents the variable 'dotMessageService' which is of type 'DotMessageService'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    readonly #dotMessageService = inject(DotMessageService);
    /**
     * Represents the variable 'btnLabel' which is of type 'computed<string>'.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    $btnLabel = computed(() => {
        const size = this.$categories().length;
        const max = this.$max();

        if (this.$showAll()) {
            return this.#dotMessageService.get('edit.content.category-field.list.show.less');
        }

        if (size > max) {
            return this.#dotMessageService.get(
                'edit.content.category-field.list.show.more',
                `${size - max}`
            );
        }

        return null;
    });

    /**
     * Method to toogle the show all categories.
     *
     * @memberof DotCategoryFieldChipsComponent
     */
    toogleShowAll(): void {
        this.$showAll.update((showAll) => !showAll);
    }
}
