import { animate, state, style, transition, trigger } from '@angular/animations';
import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { DotCategoryFieldKeyValueObj } from '../../models/dot-category-field.models';
import { DotCategoryFieldSearchListComponent } from '../dot-category-field-search-list/dot-category-field-search-list.component';

@Component({
    selector: 'dot-category-field-selected',
    standalone: true,
    imports: [
        CommonModule,
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

    @Output()
    removeItem = new EventEmitter<string>();

    private convertPathToArray(path: string): string[] {
        if (!path) {
            return [];
        }

        return path.split(' / ');
    }
}
