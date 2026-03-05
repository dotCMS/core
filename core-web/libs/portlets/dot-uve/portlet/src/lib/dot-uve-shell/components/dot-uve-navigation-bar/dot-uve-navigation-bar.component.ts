import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    Output,
    inject
} from '@angular/core';
import { RouterModule } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';

import { NavigationBarItem } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
@Component({
    selector: 'dot-uve-navigation-bar',
    templateUrl: './dot-uve-navigation-bar.component.html',
    styleUrls: ['./dot-uve-navigation-bar.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [CommonModule, RouterModule, DotMessagePipe, TooltipModule, ButtonModule]
})
export class DotUveNavigationBarComponent {
    /**
     * List of items to display on the navigation bar
     *
     * @type {NavigationBarItem[]}
     * @memberof DotUveNavigationBarComponent
     */
    @Input() items: NavigationBarItem[];

    /**
     * Emits the id of the clicked item
     *
     * @type {EventEmitter<string>}
     * @memberof DotUveNavigationBarComponent
     */
    @Output() action: EventEmitter<string> = new EventEmitter();

    uveStore = inject(UVEStore);

    $params = this.uveStore.pageParams;

    /**
     * Handle the click event on the item
     *
     * @param {NavigationBarItem} item
     * @memberof DotUveNavigationBarComponent
     */
    itemAction(item: NavigationBarItem): void {
        this.action.emit(item.id);
    }
}
