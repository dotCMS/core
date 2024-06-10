import { ChangeDetectionStrategy, Component, EventEmitter } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SidebarModule } from 'primeng/sidebar';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Component for the sidebar that appears when editing content category field.
 */
@Component({
    selector: 'dot-edit-content-category-field-sidebar',
    standalone: true,
    imports: [DialogModule, ButtonModule, DotMessagePipe, SidebarModule],
    templateUrl: './dot-edit-content-category-field-sidebar.component.html',
    styleUrl: './dot-edit-content-category-field-sidebar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentCategoryFieldSidebarComponent {
    /**
     * Indicates whether the sidebar is visible or not.
     *
     */
    visible = true;

    /**
     * The event is fired whenever the sidebar is closed either by hitting 'Escape',
     * clicking on the overlay, or on the back button.
     *
     */
    closedSidebar = new EventEmitter<void>();
}
