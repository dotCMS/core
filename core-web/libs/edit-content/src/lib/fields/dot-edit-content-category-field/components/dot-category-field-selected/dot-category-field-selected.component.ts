import { animate, state, style, transition, trigger } from '@angular/animations';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldKeyValueObj } from '../../models/dot-category-field.models';
import { DotCategoryFieldSearchListComponent } from '../dot-category-field-search-list/dot-category-field-search-list.component';

/**
 * Represents the Dot Category Field Selected Component.
 * @class
 * @classdesc The Dot Category Field Selected Component is responsible for rendering the selected categories
 * in the Dot Category Field Component.
 */
@Component({
    selector: 'dot-category-field-selected',
    standalone: true,
    imports: [
        ButtonModule,
        DotMessagePipe,
        DotCategoryFieldSearchListComponent,
        ChipModule,
        TooltipModule
    ],
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

    /**
     * Represents an EventEmitter used for removing items. Emit the key
     * of the category
     */
    removeItem = output<string>();
}
