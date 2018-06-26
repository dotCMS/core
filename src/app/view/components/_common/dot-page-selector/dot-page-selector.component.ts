import { Component, Output, EventEmitter, forwardRef, Input } from '@angular/core';
import { DotPageSelectorService, DotPageAsset } from './service/dot-page-selector.service';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { take } from 'rxjs/operators';

/**
 * Search and select a page asset
 *
 * @export
 * @class DotPageSelectorComponent
 * @implements {OnInit}
 */
@Component({
    selector: 'dot-page-selector',
    templateUrl: './dot-page-selector.component.html',
    styleUrls: ['./dot-page-selector.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotPageSelectorComponent)
        }
    ]
})
export class DotPageSelectorComponent implements ControlValueAccessor {
    @Output() selected = new EventEmitter<DotPageAsset>();
    @Input() style: any;
    @Input() label: string;
    @Input() hostIdentifier: string;
    @Input() floatingLabel = false;

    results: any[];
    val: DotPageAsset;

    constructor(private dotPageSelectorService: DotPageSelectorService) {}

    propagateChange = (_: any) => {};

    /**
     * Handle clear of the autocomplete
     *
     * @memberof DotPageSelectorComponent
     */
    onClear(): void {
        this.propagateChange(null);
        this.results = [];
    }

    /**
     * Handle option selected
     *
     * @param {DotPageAsset} item
     * @memberof DotPageSelectorComponent
     */
    onSelect(item: DotPageAsset): void {
        this.selected.emit(item);
        this.propagateChange(item.identifier);
    }

    /**
     * Get pages results and set it to the autotomplete
     *
     * @param {string} param
     * @memberof DotPageSelectorComponent
     */
    search(param: string): void {
        this.dotPageSelectorService.getPagesInFolder(param, this.hostIdentifier).subscribe((pages: DotPageAsset[]) => {
            this.results = pages;
        });
    }

    /**
     * Write a new value to the element
     *
     * @param {string} idenfier
     * @memberof DotPageSelectorComponent
     */
    writeValue(idenfier: string): void {
        if (idenfier) {
            this.dotPageSelectorService.getPage(idenfier).pipe(take(1)).subscribe((page: DotPageAsset) => {
                this.val = page;
            });
        }
    }

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param {*} fn
     * @memberof DotPageSelectorComponent
     */
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(_fn: any): void {}
}
