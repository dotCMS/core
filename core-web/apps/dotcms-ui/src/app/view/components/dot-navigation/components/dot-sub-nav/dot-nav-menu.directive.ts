import { CdkMenuItem, CdkMenuTrigger } from '@angular/cdk/menu';
import { Directive, forwardRef } from '@angular/core';

/** Navigation links keep their menu open rather than dismissing it like an action menu. */
@Directive({
    selector: '[dotNavMenuItem]',
    providers: [{ provide: CdkMenuItem, useExisting: forwardRef(() => DotNavMenuItemDirective) }]
})
export class DotNavMenuItemDirective extends CdkMenuItem {
    override trigger(): void {
        super.trigger({ keepOpen: true });
    }
}

/** Clicking an already hovered navigation group must not toggle its flyout closed. */
@Directive({
    selector: '[dotNavMenuTrigger]',
    exportAs: 'dotNavMenuTrigger',
    providers: [
        { provide: CdkMenuTrigger, useExisting: forwardRef(() => DotNavMenuTriggerDirective) }
    ]
})
export class DotNavMenuTriggerDirective extends CdkMenuTrigger {
    override toggle(): void {
        this.open();
    }
}
