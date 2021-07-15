import {
    Component,
    Output,
    EventEmitter,
    forwardRef,
    Input,
    OnInit,
    ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { switchMap, take } from 'rxjs/operators';

import { Site } from '@dotcms/dotcms-js';

import { DotPageSelectorService, DotPageAsset } from './service/dot-page-selector.service';
import {
    DotPageSelectorItem,
    DotFolder,
    DotSimpleURL,
    CompleteEvent
} from './models/dot-page-selector.models';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { AutoComplete } from 'primeng/autocomplete';
import { of } from 'rxjs';
import { Observable, Subject } from 'rxjs';

const NO_SPECIAL_CHAR = /^[a-zA-Z0-9._/-]*$/g;
const REPLACE_SPECIAL_CHAR = /[^a-zA-Z0-9._/-]/g;
enum SearchType {
    SITE = 'site',
    FOLDER = 'folder',
    PAGE = 'page'
}

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
    @Output() selected = new EventEmitter<DotPageAsset | string>();
    @Input() label: string;
    @Input() folderSearch = false;

    @ViewChild('autoComplete') autoComplete: AutoComplete;

    val: DotPageSelectorItem;
    suggestions$: Subject<DotPageSelectorItem[]> = new Subject<DotPageSelectorItem[]>();
    message: string;
    searchType: string;
    isError = false;
    private currentHost: Site;
    private invalidHost = false;

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
        this.resetResults();
    }

    /**
     * Get results and set it to the autotomplete
     *
     * @param CompleteEvent param
     * @memberof DotPageSelectorComponent
     */
    search(event: CompleteEvent): void {
        this.propagateChange(null);
        const query = this.cleanAndValidateQuery(event.query);
        this.message = null;
        this.invalidHost = false;
        this.isError = false;
        if (!!query) {
            this.handleSearchType(query)
                .pipe(take(1))
                .subscribe((data) => {
                    this.handleDataAndErrors(data, query);
                });
        } else {
            this.resetResults();
        }
    }

    /**
     * Handle option selected
     *
     * @param DotPageAsset item
     * @memberof DotPageSelectorComponent
     */
    onSelect(item: DotPageSelectorItem): void {
        if (this.searchType === 'site') {
            const site: Site = <Site>item.payload;
            this.currentHost = site;
            this.autoComplete.completeMethod.emit({ query: `//${site.hostname}/` });
        } else if (this.searchType === 'page') {
            const page: DotPageAsset = <DotPageAsset>item.payload;
            this.selected.emit(page);
            this.propagateChange(page.identifier);
        } else if (this.searchType === 'folder') {
            const folder = <DotFolder>item.payload;
            if (folder.addChildrenAllowed) {
                this.selected.emit(`//${folder.hostName}${folder.path}`);
                this.propagateChange(`//${folder.hostName}${folder.path}`);
            } else {
                this.message = this.dotMessageService.get('page.selector.folder.permissions');
                this.isError = true;
            }
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
                .subscribe((item: DotPageSelectorItem) => {
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

    private handleDataAndErrors(data: DotPageSelectorItem[], query: string): void {
        if (data.length === 0) {
            if (this.invalidHost) {
                this.message = this.getEmptyMessage(SearchType.SITE);
            } else if (this.isFolderAndHost(query)) {
                this.propagateChange(this.autoComplete.inputEL.nativeElement.value);
                this.message = this.dotMessageService.get('page.selector.folder.new');
            } else {
                this.message = this.getEmptyMessage(this.searchType);
            }
        }
        this.suggestions$.next(data);
        this.autoComplete.show();
    }

    private isFolderAndHost(query: string): boolean {
        return (
            this.searchType === SearchType.FOLDER && !query.endsWith('/') && query.startsWith('//')
        );
    }

    private handleSearchType(query: string): Observable<DotPageSelectorItem[]> {
        if (this.isTwoStepSearch(query)) {
            this.searchType = this.folderSearch ? SearchType.FOLDER : SearchType.PAGE;
            return this.fullSearch(query);
        } else {
            this.currentHost = null;
            this.searchType = this.setSearchType(query);
            return this.getConditionalSearch(query);
        }
    }

    private fullSearch(param: string): Observable<DotPageSelectorItem[]> {
        const host = this.parseUrl(param).host;
        return this.dotPageSelectorService.getSites(host, true).pipe(
            take(1),
            switchMap((results: DotPageSelectorItem[]) => {
                if (results.length) {
                    this.currentHost = <Site>results[0].payload;
                    return this.getSecondStepData(param);
                } else {
                    this.invalidHost = true;
                    return of([]);
                }
            })
        );
    }

    private setSearchType(query: string): string {
        return this.isSearchingForHost(query)
            ? SearchType.SITE
            : this.folderSearch
            ? SearchType.FOLDER
            : SearchType.PAGE;
    }

    private getConditionalSearch(param: string): Observable<DotPageSelectorItem[]> {
        return this.searchType === SearchType.SITE
            ? this.dotPageSelectorService.getSites(this.getSiteName(param))
            : this.getSecondStepData(param);
    }

    private getSecondStepData(param: string): Observable<DotPageSelectorItem[]> {
        return this.folderSearch
            ? this.dotPageSelectorService.getFolders(param)
            : this.dotPageSelectorService.getPages(param);
    }

    private isTwoStepSearch(param: string): boolean {
        return (
            param.startsWith('//') &&
            param.length > 2 &&
            (this.isHostAndPath(param) || param.endsWith('/'))
        );
    }

    private isHostAndPath(param: string): boolean {
        const url: DotSimpleURL | { [key: string]: string } = this.parseUrl(param);
        return url && !!(url.host && url.pathname.length > 0);
    }

    private parseUrl(query: string): DotSimpleURL {
        try {
            const url = new URL(`http:${query}`);
            return { host: url.host, pathname: url.pathname.substr(1) };
        } catch {
            return null;
        }
    }

    private isSearchingForHost(query: string): boolean {
        return query.startsWith('//') && (!!this.getSiteName(query) ? !query.endsWith('/') : true);
    }

    private getSiteName(site: string): string {
        return site.replace(/\//g, '');
    }

    private resetResults(): void {
        this.suggestions$.next([]);
    }

    private cleanAndValidateQuery(query: string): string {
        const cleanedQuery = this.cleanQuery(query);
        this.autoComplete.inputEL.nativeElement.value = cleanedQuery;
        return cleanedQuery.startsWith('//')
            ? cleanedQuery
            : cleanedQuery.length >= 3
            ? cleanedQuery
            : '';
    }

    private cleanQuery(query: string): string {
        return !NO_SPECIAL_CHAR.test(query) ? query.replace(REPLACE_SPECIAL_CHAR, '') : query;
    }

    private getEmptyMessage(type: string): string {
        this.isError = true;
        switch (type) {
            case 'site':
                return this.dotMessageService.get('page.selector.no.sites.results');
            case 'page':
                return this.dotMessageService.get('page.selector.no.page.results');
            case 'folder':
                return this.dotMessageService.get('page.selector.no.folder.results');
        }
    }
}
