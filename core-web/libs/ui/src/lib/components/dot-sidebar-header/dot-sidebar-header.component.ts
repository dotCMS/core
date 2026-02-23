import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, TemplateRef, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { Drawer } from 'primeng/drawer';

/**
 * Used to add a header bar to Sidebar (PrimeNg)
 * show a title, close icon and an optional button or action
 *
 * @export
 * @class DotSidebarHeaderComponent
 *
 */
@Component({
    selector: 'dot-sidebar-header',
    imports: [CommonModule, ButtonModule],
    templateUrl: './dot-sidebar-header.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'block w-full'
    }
})
export class DotSidebarHeaderComponent {
    private readonly sidebarComponent = inject(Drawer, { optional: true, host: true });

    /**
     * Title of the sidebar
     */
    @Input()
    dotTitle: string;

    /**
     * Action Button of the header
     * showed only if exist
     */
    @Input()
    actionButtonTpl?: TemplateRef<void>;

    constructor() {
        if (!this.sidebarComponent) {
            console.warn('DotSidebarHeaderComponent is for use inside of a PrimeNg Drawer');
        }
    }

    /**
     * Close the Host Sidebar
     *
     */
    closePrimeNgSidebar() {
        this.sidebarComponent.hide();
    }
}
