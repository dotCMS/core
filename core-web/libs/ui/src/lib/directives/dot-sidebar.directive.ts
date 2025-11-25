import { Directive, Input, Optional, Self } from '@angular/core';

import { Sidebar } from 'primeng/sidebar';

export const enum SIDEBAR_PLACEMENT {
    RIGHT = 'right'
}

export enum SIDEBAR_SIZES {
    SM = 'p-sidebar-sm',
    MD = 'p-sidebar-md',
    LG = 'p-sidebar-lg'
}

/**
 * Directive to configure the default options of Sidebar (PrimeNG)
 *
 */
@Directive({
    standalone: true,
    selector: '[dotSidebar]'
})
export class DotSidebarDirective {
    constructor(@Optional() @Self() private readonly primeSidebar: Sidebar) {
        if (primeSidebar) {
            primeSidebar.position = SIDEBAR_PLACEMENT.RIGHT;
            primeSidebar.styleClass = SIDEBAR_SIZES.MD;
            primeSidebar.showCloseIcon = false;
            primeSidebar.dismissible = false;
            primeSidebar.closeOnEscape = false;
        } else {
            console.warn('DotSidebarDirective is for use with PrimeNg Sidebar');
        }
    }

    /**
     * Change the default width of the sidebar
     * @param {string} size
     */
    @Input()
    set dotSize(size: string) {
        if (size === SIDEBAR_SIZES.LG) {
            this.primeSidebar.style = { width: '60%' };
        } else {
            this.primeSidebar.styleClass = size;
        }
    }
}
