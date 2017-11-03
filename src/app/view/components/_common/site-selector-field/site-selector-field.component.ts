import { Component, OnInit, Renderer2, ElementRef, forwardRef, Input } from '@angular/core';
import { SelectControlValueAccessor, ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Site, SiteService } from 'dotcms-js/dotcms-js';
import { Subscription } from 'rxjs/Subscription';
/**
 * Form control to select DotCMS instance host identifier.
 *
 * @export
 * @class SiteSelectorFieldComponent
 * @implements {ControlValueAccessor}
 */
@Component({
    selector: 'dot-site-selector-field',
    templateUrl: './site-selector-field.component.html',
    styleUrls: ['./site-selector-field.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => SiteSelectorFieldComponent)
        }
    ]
})

export class SiteSelectorFieldComponent implements ControlValueAccessor {
    @Input() archive: boolean;
    @Input() live: boolean;
    @Input() system: boolean;

    value: string;

    private currentSiteSubscription: Subscription;

    constructor(private siteService: SiteService) {}

    propagateChange = (_: any) => {};

    /**
     * Set the function to be called when the control receives a change event.
     * @param {any} fn
     * @memberof SearchableDropdownComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    registerOnTouched(): void {}

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
     * @param {*} value
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

    private isCurrentSiteSubscripted(): boolean {
        return this.currentSiteSubscription && !this.currentSiteSubscription.closed;
    }

    private isSelectingNewValue(site: Site): boolean {
        return site.identifier !== this.siteService.currentSite.identifier;
    }
}
