import { fromEvent, Subject } from 'rxjs';

import {
    AfterViewInit,
    Component,
    ElementRef,
    forwardRef,
    inject,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { LazyLoadEvent } from 'primeng/api';

import { debounceTime, filter, take, takeUntil, tap } from 'rxjs/operators';

import { DotThemesService, PaginatorService } from '@dotcms/data-access';
import { Site, SiteService } from '@dotcms/dotcms-js';
import { DotTheme } from '@dotcms/dotcms-models';

import { DotSiteSelectorComponent } from '../_common/dot-site-selector/dot-site-selector.component';
import { SearchableDropdownComponent } from '../_common/searchable-dropdown/component/searchable-dropdown.component';

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
    ],
    standalone: false
})
export class DotThemeSelectorDropdownComponent
    implements OnInit, OnDestroy, ControlValueAccessor, AfterViewInit
{
    readonly paginatorService = inject(PaginatorService);
    private readonly siteService = inject(SiteService);
    private readonly themesService = inject(DotThemesService);

    themes: DotTheme[] = [];
    value: DotTheme = null;
    totalRecords = 0;
    currentOffset: number;
    currentSiteIdentifier: string;

    selectedOptionIndex = 0;
    selectedOptionValue = '';

    keyMap: string[] = [
        'Shift',
        'Alt',
        'Control',
        'Meta',
        'ArrowUp',
        'ArrowDown',
        'ArrowLeft',
        'ArrowRight'
    ];

    @ViewChild('searchableDropdown', { static: true })
    searchableDropdown: SearchableDropdownComponent;

    @ViewChild('searchInput', { static: false })
    searchInput: ElementRef;

    @ViewChild('siteSelector')
    siteSelector: DotSiteSelectorComponent;

    private initialLoad = true;
    private destroy$: Subject<boolean> = new Subject<boolean>();

    ngOnInit(): void {
        const interval = setInterval(() => {
            try {
                this.currentSiteIdentifier = this.siteService.currentSite.identifier;
                clearInterval(interval);
            } catch {
                /* */
            }
        }, 0);

        // Here we set the initial value of the dropdown as System Theme
        this.paginatorService.url = 'v1/themes';
        this.paginatorService.paginationPerPage = 5;
        this.paginatorService.setExtraParams('hostId', 'SYSTEM_HOST');
        this.paginatorService
            .get()
            .pipe(take(1))
            .subscribe((themes: DotTheme[]) => {
                this.value = themes[0];
                this.propagateChange(themes[0].identifier);
            });
    }

    ngAfterViewInit(): void {
        if (this.searchInput) {
            fromEvent(this.searchInput.nativeElement, 'keyup')
                .pipe(
                    tap((keyboardEvent: KeyboardEvent) => {
                        if (
                            keyboardEvent.key === 'ArrowUp' ||
                            keyboardEvent.key === 'ArrowDown' ||
                            keyboardEvent.key === 'Enter'
                        ) {
                            this.selectDropdownOption(keyboardEvent.key);
                        }
                    }),
                    debounceTime(500),
                    takeUntil(this.destroy$)
                )
                .subscribe((keyboardEvent: KeyboardEvent) => {
                    if (!this.isModifierKey(keyboardEvent.key)) {
                        this.getFilteredThemes();
                    }
                });
        }
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    onHide(): void {
        if (this.value) {
            this.selectedOptionIndex = null;
            this.siteService
                .getSiteById(this.value.hostId)
                .pipe(take(1))
                .subscribe((site) => {
                    this.siteSelector.updateCurrentSite(site);
                });
        }
    }

    propagateChange = (_: unknown) => {
        /* */
    };
    registerOnTouched(): void {
        /* */
    }

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
                    this.siteService
                        .getSiteById(this.value.hostId)
                        .pipe(take(1))
                        .subscribe((site) => {
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
        setTimeout(() => {
            this.searchInput.nativeElement.focus();
        }, 0);
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
                .getWithOffset<DotTheme[]>(event.first)
                /*
                We load the first page of themes (onShow) so we dont want to load them when the
                first paginate event from the dataview inside <dot-searchable-dropdown> triggers
                */
                .pipe(
                    take(1),
                    filter(() => !!(this.currentSiteIdentifier && this.themes.length))
                )
                .subscribe((themes: DotTheme[]) => {
                    this.themes = themes;
                });
        }
    }

    private selectDropdownOption(actionKey: string) {
        if (actionKey === 'ArrowDown' && this.themes.length - 1 > this.selectedOptionIndex) {
            this.selectedOptionIndex++;
            this.selectedOptionValue = this.themes[this.selectedOptionIndex][`name`];
        } else if (actionKey === 'ArrowUp' && 0 < this.selectedOptionIndex) {
            this.selectedOptionIndex--;
            this.selectedOptionValue = this.themes[this.selectedOptionIndex][`name`];
        } else if (actionKey === 'Enter' && this.selectedOptionIndex !== null) {
            this.onChange(this.themes[this.selectedOptionIndex]);
        }
    }

    private getFilteredThemes(offset = 0): void {
        this.setHostThemes(this.currentSiteIdentifier, this.currentOffset || offset);
    }

    private setHostThemes(hostId: string, offset = 0) {
        this.siteService
            .getSiteById(hostId)
            .pipe(take(1))
            .subscribe((site: Site) => {
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

                    this.selectedOptionValue = this.themes[0]['name'];
                    this.selectedOptionIndex = 0;
                }

                this.initialLoad = false;
            });
    }

    private isModifierKey(key: string): boolean {
        return this.keyMap.includes(key);
    }

    private setTotalRecords() {
        this.totalRecords = 0;

        // Timeout to activate change of pagination to the first page
        setTimeout(() => {
            this.totalRecords = this.paginatorService.totalRecords;
        }, 0);
    }
}
