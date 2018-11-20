import { Component, Output, EventEmitter, forwardRef, Input } from '@angular/core';
import { DotPageSelectorService, DotPageAsset, DotPageSeletorItem } from './service/dot-page-selector.service';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Site } from 'dotcms-js';

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
    @Input() floatingLabel = false;

    results: DotPageSeletorItem[];
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
     * @param DotPageAsset item
     * @memberof DotPageSelectorComponent
     */
    onSelect(item: DotPageSeletorItem): void {
        console.log('select', item);
        if (item.isHost) {
            this.dotPageSelectorService.setCurrentHost(<Site>item.payload);
        }
        // TODO: complete selection of host if apply && don't emmit selected yet.
        // this.selected.emit(item);
        // this.propagateChange(item.identifier);
    }

    /**
     * Get pages results and set it to the autotomplete
     *
     * @param string param
     * @memberof DotPageSelectorComponent
     */
    search(param: string): void {
        console.log('search.component', param);
        this.dotPageSelectorService.search(param).subscribe((data: DotPageSeletorItem[]) => {
            this.results = data;
        });
    }

    /**
     * Write a new value to the element
     *
     * @param string idenfier
     * @memberof DotPageSelectorComponent
     */
    writeValue(idenfier: string): void {
        if (idenfier) {
            // this.dotPageSelectorService
            //     .getPage(idenfier)
            //     .pipe(take(1))
            //     .subscribe((page: DotPageAsset) => {
            //         this.val = page;
            //     });
        }
    }

    /**
     * Prevent enter to propagate on selection
     *
     * @param {KeyboardEvent} $event
     * @memberof DotTextareaContentComponent
     */
    onKeyEnter($event: KeyboardEvent): void {
        const input: HTMLInputElement = <HTMLInputElement>$event.target;
        console.log('onKeyEnter', input.value);
        $event.stopPropagation();
    }

    /**
     * Set the function to be called when the control receives a change event.
     *
     * @param * fn
     * @memberof DotPageSelectorComponent
     */
    registerOnChange(fn: any): void {
        this.propagateChange = fn;
    }

    registerOnTouched(_fn: any): void {}
}
