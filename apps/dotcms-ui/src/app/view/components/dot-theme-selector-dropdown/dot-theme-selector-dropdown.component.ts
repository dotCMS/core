import {
    AfterViewInit,
    Component,
    ElementRef,
    forwardRef,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { DotSiteSelectorComponent } from '@components/_common/dot-site-selector/dot-site-selector.component';
import { SearchableDropdownComponent } from '@components/_common/searchable-dropdown/component';
import { DotTheme } from '@models/dot-edit-layout-designer';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { PaginatorService } from '@services/paginator';
import { Site, SiteService } from '@dotcms/dotcms-js';
import { LazyLoadEvent } from 'primeng/api';
import { fromEvent, Subject } from 'rxjs';
import { debounceTime, filter, take, takeUntil } from 'rxjs/operators';

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
    implements OnInit, OnDestroy, ControlValueAccessor, AfterViewInit {
    themes: DotTheme[] = [];
    value: DotTheme = null;
    totalRecords: number = 0;
    currentOffset: number;
    currentSiteIdentifier: string;

    @ViewChild('searchableDropdown', { static: true })
    searchableDropdown: SearchableDropdownComponent;

    @ViewChild('searchInput', { static: false })
    searchInput: ElementRef;

    @ViewChild('siteSelector')
    siteSelector: DotSiteSelectorComponent;

    private initialLoad = true;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        public readonly paginatorService: PaginatorService,
        private readonly siteService: SiteService,
        private readonly themesService: DotThemesService
    ) {}

    ngOnInit(): void {
        const interval = setInterval(() => {
            try {
                this.currentSiteIdentifier = this.siteService.currentSite.identifier;
                clearInterval(interval);
            } catch (e) {}
        }, 0);
    }

    ngAfterViewInit(): void {
        if (this.searchInput) {
            fromEvent(this.searchInput.nativeElement, 'keyup')
                .pipe(debounceTime(500), takeUntil(this.destroy$))
                .subscribe(() => {
                    this.getFilteredThemes();
                });
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    onHide(): void {
        if (this.value) {
            this.siteService
                .getSiteById(this.value.hostId)
                .pipe(take(1))
                .subscribe((site) => {
                    this.siteSelector.updateCurrentSite(site);
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
                    this.siteService.getSiteById(this.value.hostId).pipe(take(1)).subscribe((site) => {
                        this.siteSelector?.updateCurrentSite(site);
                    });
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
        this.currentSiteIdentifier = event.identifier;
        this.setHostThemes(event.identifier);
    }
    /**
     * Sets the themes when the drop down is opened
     *
     * @memberof DotThemeSelectorDropdownComponent
     */
    onShow(): void {
        this.paginatorService.url = 'v1/themes';
        this.paginatorService.paginationPerPage = 5;

        if (this.value) {
            this.currentSiteIdentifier = this.value.hostId;
        }
        this.searchInput.nativeElement.value = '';
        this.setHostThemes(this.currentSiteIdentifier);
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
     * Handles page change for pagination purposes.
     *
     * @param {LazyLoadEvent} event
     * @return void
     * @memberof DotThemeSelectorDropdownComponent
     */
    handlePageChange(event: LazyLoadEvent): void {
        this.currentOffset = event.first;
        if (this.currentSiteIdentifier && this.paginatorService.url) {
            this.paginatorService
                .getWithOffset(event.first)
                /*
                We load the first page of themes (onShow) so we dont want to load them when the
                first paginate event from the dataview inside <dot-searchable-dropdown> triggers
                */
                .pipe(
                    take(1),
                    filter(() => !!(this.currentSiteIdentifier && this.themes.length))
                )
                .subscribe((themes) => {
                    this.themes = themes;
                });
        }
    }

    private getFilteredThemes(offset = 0): void {
        this.setHostThemes(this.currentSiteIdentifier, this.currentOffset || offset);
    }

    private setHostThemes(hostId: string, offset: number = 0) {
        this.siteService.getSiteById(hostId).pipe(take(1)).subscribe((site: Site) => {
            this.siteSelector.updateCurrentSite(site);
        });

        this.paginatorService.setExtraParams('hostId', hostId);
        this.paginatorService.searchParam = this.searchInput.nativeElement.value;
        this.paginatorService
            .getWithOffset(offset)
            .pipe(take(1))
            .subscribe((themes: DotTheme[]) => {
                if (themes.length || !this.initialLoad) {
                    this.themes = themes;
                    this.setTotalRecords();
                }
                this.initialLoad = false;
            });
    }

    private setTotalRecords() {
        this.totalRecords = 0;

        // Timeout to activate change of pagination to the first page
        setTimeout(() => {
            this.totalRecords = this.paginatorService.totalRecords;
        }, 0);
    }
}
