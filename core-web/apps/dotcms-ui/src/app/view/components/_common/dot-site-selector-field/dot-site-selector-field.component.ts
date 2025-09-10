import { Subscription } from 'rxjs';

import { Component, forwardRef, Input, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { Site, SiteService } from '@dotcms/dotcms-js';
/**
 * Form control to select DotCMS instance host identifier.
 *
 * @export
 * @class DotSiteSelectorFieldComponent
 * @implements {ControlValueAccessor}
 */
@Component({
    selector: 'dot-site-selector-field',
    templateUrl: './dot-site-selector-field.component.html',
    styleUrls: ['./dot-site-selector-field.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSiteSelectorFieldComponent)
        }
    ],
    standalone: false
})
export class DotSiteSelectorFieldComponent implements ControlValueAccessor {
    private siteService = inject(SiteService);

    @Input()
    archive: boolean;
    @Input()
    live: boolean;
    @Input()
    system: boolean;
    @Input()
    width: string;

    value: string;

    private currentSiteSubscription: Subscription;

    propagateChange = (_: unknown) => undefined;

    /**
     * Set the function to be called when the control receives a change event.
     * @param any fn
     * @memberof SearchableDropdownComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
        this.propagateCurrentSiteId();
    }

    registerOnTouched(): void {
        //
    }

    setValue(site: Site): void {
        /*
            TODO: we have an issue (ExpressionChangedAfterItHasBeenCheckedError) here with the
            form in content types when the current site is set for the first time, I'll debug
            and fix this later. --fmontes
        */
        setTimeout(() => {
            this.propagateChange(site.identifier);
        }, 0);

        if (this.isCurrentSiteSubscripted() && this.isSelectingNewValue(site)) {
            this.currentSiteSubscription.unsubscribe();
        }
    }

    /**
     * Write a new value to the element
     * @param * value
     * @memberof SearchableDropdownComponent
     */
    writeValue(value: string): void {
        if (value) {
            this.value = value;

            if (this.isCurrentSiteSubscripted()) {
                this.currentSiteSubscription.unsubscribe();
            }
        } else {
            this.currentSiteSubscription = this.siteService.switchSite$.subscribe((site: Site) => {
                this.value = site.identifier;
                this.propagateChange(site.identifier);
            });
        }
    }

    private propagateCurrentSiteId(): void {
        if (this.siteService.currentSite) {
            this.value = this.value || this.siteService.currentSite.identifier;
            this.propagateChange(this.value);
        } else {
            this.siteService.getCurrentSite().subscribe((currentSite: Site) => {
                if (!this.value) {
                    this.value = currentSite.identifier;
                }

                this.propagateChange(this.value);
            });
        }
    }

    private isCurrentSiteSubscripted(): boolean {
        return this.currentSiteSubscription && !this.currentSiteSubscription.closed;
    }

    private isSelectingNewValue(site: Site): boolean {
        return site.identifier !== this.siteService.currentSite.identifier;
    }
}
