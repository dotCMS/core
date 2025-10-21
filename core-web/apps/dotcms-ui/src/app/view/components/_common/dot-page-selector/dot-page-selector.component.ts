import { Observable, of, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    Component,
    EventEmitter,
    forwardRef,
    Input,
    Output,
    ViewChild,
    inject
} from '@angular/core';
import { ControlValueAccessor, FormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';

import { AutoComplete, AutoCompleteModule, AutoCompleteSelectEvent } from 'primeng/autocomplete';

import { switchMap, take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { Site } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';

import {
    CompleteEvent,
    DotFolder,
    DotPageSelectorItem,
    DotSimpleURL
} from './models/dot-page-selector.models';
import { DotPageAsset, DotPageSelectorService } from './service/dot-page-selector.service';

import { DotDirectivesModule } from '../../../../shared/dot-directives.module';
import { DotFieldHelperModule } from '../../dot-field-helper/dot-field-helper.module';

const NO_SPECIAL_CHAR = /^[a-zA-Z0-9._/-]*$/g;
const REPLACE_SPECIAL_CHAR = /[^a-zA-Z0-9._/-]/g;
const NO_SPECIAL_CHAR_WHITE_SPACE = /^[a-zA-Z0-9._/-\s]*$/g;
const REPLACE_SPECIAL_CHAR_WHITE_SPACE = /[^a-zA-Z0-9._/-\s]/g;

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
    ],
    imports: [
        CommonModule,
        FormsModule,
        AutoCompleteModule,
        DotDirectivesModule,
        DotFieldHelperModule,
        DotMessagePipe
    ]
})
export class DotPageSelectorComponent implements ControlValueAccessor {
    private dotPageSelectorService = inject(DotPageSelectorService);
    private dotMessageService = inject(DotMessageService);

    @Output() selected = new EventEmitter<DotPageAsset | string>();
    @Input() folderSearch = false;

    @ViewChild('autoComplete') autoComplete: AutoComplete;

    val: DotPageSelectorItem;
    suggestions$: Subject<DotPageSelectorItem[]> = new Subject<DotPageSelectorItem[]>();
    message: string;
    searchType: string;
    isError = false;
    private currentHost: Site;
    private invalidHost = false;

    propagateChange = (_: unknown) => {
        /* */
    };

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
        if (query) {
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
    onSelect(event: AutoCompleteSelectEvent): void {
        const { originalEvent, value } = event;

        if (this.searchType === 'site') {
            const site: Site = <Site>value.payload;
            this.currentHost = site;
            this.autoComplete.completeMethod.emit({
                query: `//${site.hostname}/`,
                originalEvent
            });
        } else if (this.searchType === 'page') {
            const page: DotPageAsset = <DotPageAsset>value.payload;
            this.selected.emit(page);
            this.propagateChange(page.identifier);
        } else if (this.searchType === 'folder') {
            this.handleFolderSelection(<DotFolder>value.payload);
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
     * @param {(params) => void} fn
     * @memberof DotPageSelectorComponent
     */
    registerOnChange(fn: (params) => void): void {
        this.propagateChange = fn;
    }

    registerOnTouched(_fn: () => void): void {
        /* */
    }

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
        } else if (
            this.searchType === SearchType.FOLDER &&
            this.currentHost &&
            (<DotFolder>data[0].payload).path === '/'
        ) {
            //select the root folder of a host.
            this.handleFolderSelection(<DotFolder>data[0].payload);
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
        const host = decodeURI(this.parseUrl(param).host);

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

    private isTwoStepSearch(query: string): boolean {
        return (
            query.startsWith('//') &&
            query.length > 2 &&
            (this.isHostAndPath(query) || query.endsWith('/'))
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
        return query.startsWith('//') && (this.getSiteName(query) ? !query.endsWith('/') : true);
    }

    private getSiteName(site: string): string {
        return site.replace(/\//g, '');
    }

    private resetResults(): void {
        this.suggestions$.next([]);
    }

    private cleanAndValidateQuery(query: string): string {
        let cleanedQuery = '';
        if (this.isTwoStepSearch(query)) {
            const url = this.parseUrl(query);
            url.host = this.cleanHost(decodeURI(url.host));
            url.pathname = this.cleanPath(url.pathname);
            cleanedQuery = `//${url.host}/${url.pathname}`;
        } else if (query.startsWith('//')) {
            cleanedQuery = this.cleanHost(query);
        } else {
            cleanedQuery = this.cleanPath(query);
        }

        this.autoComplete.inputEL.nativeElement.value = cleanedQuery;

        return cleanedQuery.startsWith('//')
            ? cleanedQuery
            : cleanedQuery.length >= 3
              ? cleanedQuery
              : '';
    }

    private cleanHost(query: string): string {
        return !NO_SPECIAL_CHAR_WHITE_SPACE.test(query)
            ? query.replace(REPLACE_SPECIAL_CHAR_WHITE_SPACE, '')
            : query;
    }

    private cleanPath(query: string): string {
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

    private handleFolderSelection(folder: DotFolder): void {
        if (folder.addChildrenAllowed) {
            this.selected.emit(`//${folder.hostName}${folder.path}`);
            this.propagateChange(`//${folder.hostName}${folder.path}`);
        } else {
            this.message = this.dotMessageService.get('page.selector.folder.permissions');
            this.isError = true;
        }
    }
}
