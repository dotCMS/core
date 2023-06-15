import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    Host,
    Input,
    Optional,
    TemplateRef
} from '@angular/core';

import { Sidebar } from 'primeng/sidebar';

import { UiDotIconButtonModule } from '@dotcms/ui';

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
    standalone: true,
    imports: [CommonModule, UiDotIconButtonModule],
    templateUrl: './dot-sidebar-header.component.html',
    styleUrls: ['./dot-sidebar-header.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotSidebarHeaderComponent {
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

    constructor(@Optional() @Host() private readonly sidebarComponent: Sidebar) {
        if (!sidebarComponent) {
            console.warn('DotSidebarHeaderComponent is for use inside of a PrimeNg Sidebar');
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
