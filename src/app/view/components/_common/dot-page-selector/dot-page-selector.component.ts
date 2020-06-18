import { Component, Output, EventEmitter, forwardRef, Input } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { take } from 'rxjs/operators';

import { Site } from 'dotcms-js';

import { DotPageSelectorService, DotPageAsset } from './service/dot-page-selector.service';
import { DotPageSelectorResults, DotPageSeletorItem } from './models/dot-page-selector.models';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

const NO_SPECIAL_CHAR = /^[a-zA-Z0-9._/-]*$/g;
const REPLACE_SPECIAL_CHAR = /[^a-zA-Z0-9._/-]/g;

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

    results: DotPageSelectorResults = {
        data: [],
        query: '',
        type: ''
    };
    val: DotPageSeletorItem;
    message: string;

    constructor(
        private dotPageSelectorService: DotPageSelectorService,
        private dotMessageService: DotMessageService
    ) {}

    propagateChange = (_: any) => {};

    /**
     * Handle clear of the autocomplete
     *
     * @memberof DotPageSelectorComponent
     */
    onClear(): void {
        this.propagateChange(null);
        this.results.data = [];
    }

    /**
     * Handle option selected
     *
     * @param DotPageAsset item
     * @memberof DotPageSelectorComponent
     */
    onSelect(item: DotPageSeletorItem): void {
        if (this.results.type === 'site') {
            const site: Site = <Site>item.payload;
            this.dotPageSelectorService.setCurrentHost(site);
        } else if (this.results.type === 'page') {
            const page: DotPageAsset = <DotPageAsset>item.payload;
            this.selected.emit(page);
            this.propagateChange(page.identifier);
        }

        this.resetResults();
    }

    /**
     * Prevent enter to propagate on selection
     *
     * @param {KeyboardEvent} $event
     * @memberof DotTextareaContentComponent
     */
    onKeyEnter($event: KeyboardEvent): void {
        $event.stopPropagation();

        if (this.shouldAutoFill()) {
            this.autoFillField($event);
            this.autoSelectUniqueResult();
        }
    }

    /**
     * Get pages results and set it to the autotomplete
     *
     * @param string param
     * @memberof DotPageSelectorComponent
     */
    search(param: any): void {
        if (this.validSearch(param)) {
            this.dotPageSelectorService
                .search(this.cleanQuery(param.query))
                .pipe(take(1))
                .subscribe((results: DotPageSelectorResults) => {
                    this.results = results;
                    this.message = this.getErrorMessage();

                    if (
                        this.shouldAutoFill() &&
                        this.isOnlyFullHost(this.cleanQuery(param.query))
                    ) {
                        this.autoSelectUniqueResult();
                    }
                });
        } else {
            this.results.data = [];
        }
    }

    /**
     * Write a new value to the element
     *
     * @param string idenfier
     * @memberof DotPageSelectorComponent
     */
    writeValue(idenfier: string): void {
        if (idenfier) {
            this.dotPageSelectorService
                .getPageById(idenfier)
                .pipe(take(1))
                .subscribe((item: DotPageSeletorItem) => {
                    this.val = item;
                });
        }
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

    private autoFillField($event: KeyboardEvent): void {
        const input: HTMLInputElement = <HTMLInputElement>$event.target;

        if (this.results.type === 'site') {
            const host = <Site>this.results.data[0].payload;
            input.value = `//${host.hostname}/`;
        } else if (this.results.type === 'page') {
            const page = <DotPageAsset>this.results.data[0].payload;
            input.value = `//demo.dotcms.com${page.path}`;
        }

        this.resetResults();
    }

    private shouldAutoFill(): boolean {
        return this.results.data.length === 1;
    }

    private resetResults(): void {
        this.results = {
            data: [],
            query: '',
            type: ''
        };
    }

    private getErrorMessage(): string {
        return this.results.data.length === 0
            ? this.results.type === 'site'
              ? this.dotMessageService.get('page.selector.no.sites.results')
              : this.dotMessageService.get('page.selector.no.page.results')
            : null;
    }

    private isOnlyFullHost(host: string): boolean {
        return /\/\/[-a-zA-Z0-9@:%._\+~#=]{2,256}\.[a-z]{2,6}\/*$/.test(host);
    }

    private autoSelectUniqueResult(): void {
        this.onSelect(this.results.data[0]);
    }

    private validSearch(param: any): boolean {
        return (
            this.cleanInput(param).length &&
            (!param.query.startsWith('//') ? param.query.length >= 3 : true)
        );
    }

    private cleanInput(event: any): string {
        event.originalEvent.target.value = this.cleanQuery(event.query);
        return event.originalEvent.target.value;
    }

    private cleanQuery(query: string): string {
        return !NO_SPECIAL_CHAR.test(query) ? query.replace(REPLACE_SPECIAL_CHAR, '') : query;
    }
}
