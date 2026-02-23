import { Directive, Input, inject } from '@angular/core';

import { Drawer } from 'primeng/drawer';

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
 * Note: In PrimeNG v21, position must be set via template: [position]="'right'"
 *
 */
@Directive({
    selector: '[dotSidebar]'
})
export class DotSidebarDirective {
    private readonly primeSidebar = inject(Drawer, { optional: true, self: true });

    constructor() {
        if (this.primeSidebar) {
            // Set other properties that are not signals
            this.primeSidebar.dismissible = false;
            this.primeSidebar.closeOnEscape = false;

            // Note: position is an InputSignal in PrimeNG v21 and cannot be set programmatically
            // Components using this directive should set position="right" in the template
        } else {
            console.warn('DotSidebarDirective is for use with PrimeNg Drawer');
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
        } else if (size === SIDEBAR_SIZES.SM) {
            this.primeSidebar.style = { width: '30%' };
        } else {
            this.primeSidebar.style = { width: '40%' };
        }
    }
}
