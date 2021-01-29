import { AfterViewInit, Component, ElementRef, forwardRef, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { SearchableDropdownComponent } from '@components/_common/searchable-dropdown/component';
import { DotTheme } from '@models/dot-edit-layout-designer';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { PaginatorService } from '@services/paginator';
import { Site, SiteService } from 'dotcms-js';
import { LazyLoadEvent } from 'primeng/api';
import { fromEvent } from 'rxjs';
import { debounceTime, mergeMap, pluck, take } from 'rxjs/operators';

@Component({
    selector: 'dot-theme-selector-dropdown',
    templateUrl: './dot-theme-selector-dropdown.component.html',
    styleUrls: ['./dot-theme-selector-dropdown.component.scss'],
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotThemeSelectorDropdownComponent)
        }
    ]
})
export class DotThemeSelectorDropdownComponent
    implements OnInit, ControlValueAccessor, AfterViewInit {
    themes: DotTheme[] = [];
    value: DotTheme = null;
    totalRecords: number = 0;
    currentOffset: number;

    @ViewChild('searchableDropdown', { static: true })
    searchableDropdown: SearchableDropdownComponent;

    @ViewChild('searchInput', { static: false })
    searchInput: ElementRef;

    constructor(
        public readonly paginatorService: PaginatorService,
        private readonly siteService: SiteService,
        private readonly themesService: DotThemesService
    ) {}

    ngOnInit(): void {
        this.paginatorService.url = 'v1/themes';
        this.paginatorService.paginationPerPage = 5;

        this.siteService
            .getCurrentSite()
            .pipe(
                pluck('identifier'),
                mergeMap((identifier: string) => {
                    this.paginatorService.setExtraParams('hostId', identifier);
                    return this.paginatorService.getWithOffset(0).pipe(take(1));
                }),
                take(1)
            )
            .subscribe((themes: DotTheme[]) => {
                this.themes = themes;
                this.setTotalRecords();
            });
    }

    ngAfterViewInit(): void {
        if (this.searchInput) {
            fromEvent(this.searchInput.nativeElement, 'keyup')
                .pipe(debounceTime(500))
                .subscribe((keyboardEvent: KeyboardEvent) => {
                    this.getFilteredThemes(keyboardEvent.target['value']);
                });
        }
    }

    propagateChange = (_: any) => {};
    registerOnTouched(): void {}

    /**
     * Set the function to be called when the control receives a change event.
     * @param any fn
     * @memberof SearchableDropdownComponent
     */
    registerOnChange(fn): void {
        this.propagateChange = fn;
    }

    /**
     * Writes a new value to the element
     *
     * @param {string} identifier
     * @memberof DotThemeSelectorDropdownComponent
     */
    writeValue(identifier: string): void {
        if (identifier) {
            this.themesService
                .get(identifier)
                .pipe(take(1))
                .subscribe((theme: DotTheme) => {
                    this.value = theme;
                });
        }
    }
    /**
     *  Sets the themes on site host change
     *
     * @param {Site} event
     * @memberof DotThemeSelectorDropdownComponent
     */
    siteChange(event: Site): void {
        this.setHostThemes(event.identifier);
    }

    /**
     * Handles the onChange behavior of the select input
     *
     * @param {DotTheme} theme
     * @memberof DotThemeSelectorDropdownComponent
     */
    onChange(theme: DotTheme) {
        this.value = theme;
        this.propagateChange(theme.identifier);
        this.searchableDropdown.toggleOverlayPanel();
    }

    /**
     *  Fetch theme list via the DotThemeSelectorDropdownComponent input text
     *
     * @param {string} filter
     * @memberof DotThemeSelectorDropdownComponent
     */
    handleFilterChange(filter: string): void {
        this.getFilteredThemes(filter);
    }

    /**
     * Handles page change for pagination purposes.
     *
     * @param {LazyLoadEvent} event
     * @return void
     * @memberof DotThemeSelectorDropdownComponent
     */
    handlePageChange(event: LazyLoadEvent): void {
        this.currentOffset = event.first;

        this.paginatorService
            .getWithOffset(event.first)
            .pipe(take(1))
            .subscribe((themes) => {
                this.themes = themes;
            });
    }

    private setHostThemes(identifier: string) {
        this.paginatorService.setExtraParams('hostId', identifier);

        this.paginatorService
            .getWithOffset(0)
            .pipe(take(1))
            .subscribe((themes: DotTheme[]) => {
                this.themes = themes;
                this.setTotalRecords();
            });
    }

    private getFilteredThemes(filter = '', offset = 0): void {
        this.paginatorService.searchParam = filter;

        this.paginatorService
            .getWithOffset(this.currentOffset || offset)
            .pipe(take(1))
            .subscribe((themes: DotTheme[]) => {
                this.themes = themes;
                this.setTotalRecords();
            });
    }

    private setTotalRecords() {
        this.totalRecords = this.paginatorService.totalRecords;
    }
}
