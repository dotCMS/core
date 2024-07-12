import { animate, state, style, transition, trigger } from '@angular/animations';
import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    EventEmitter,
    input,
    Output
} from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldKeyValueObj } from '../../models/dot-category-field.models';
import { DotCategoryFieldSearchListComponent } from '../dot-category-field-search-list/dot-category-field-search-list.component';

@Component({
    selector: 'dot-category-field-selected',
    standalone: true,
    imports: [CommonModule, ButtonModule, DotMessagePipe, DotCategoryFieldSearchListComponent],
    templateUrl: './dot-category-field-selected.component.html',
    styleUrl: './dot-category-field-selected.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('fadeAnimation', [
            state(
                'void',
                style({
                    opacity: 0
                })
            ),
            transition(':enter, :leave', [animate('50ms ease-in-out')])
        ])
    ]
})
export class DotCategoryFieldSelectedComponent {
    /**
     * Represents the array of selected categories.
     */
    $categories = input<DotCategoryFieldKeyValueObj[]>([], {
        alias: 'categories'
    });

    $fix = computed(() => {
        return this.$categories().map((category) => ({
            ...category,
            pathArray: this.convertPathToArray(category.path)
        }));
    });

    @Output()
    removeItem = new EventEmitter<string>();

    private convertPathToArray(path: string): string[] {
        if (!path) {
            return [];
        }

        const parts = path.split(' / ');
        if (parts.length <= 3) {
            return parts;
        }

        const lastThree = parts.slice(-3);
        lastThree[0] = `...${lastThree[0]}`;

        return lastThree;
    }
}
