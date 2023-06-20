import { fromEvent as observableFromEvent, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild
} from '@angular/core';

import { LazyLoadEvent, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DataView, DataViewModule } from 'primeng/dataview';
import { DropdownModule } from 'primeng/dropdown';
import { DialogService, DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { debounceTime, take, takeUntil } from 'rxjs/operators';

import { DotMessageService, PaginatorService } from '@dotcms/data-access';
import { Site, SiteService } from '@dotcms/dotcms-js';
import { DotTheme } from '@dotcms/dotcms-models';
import { DotMessagePipeModule, DotSiteSelectorDirective } from '@dotcms/ui';

/**
 * The DotThemeSelectorComponent is modal that
 * show the themes of the available hosts
 * @export
 * @class DotThemeSelectorComponent
 */
@Component({
    selector: 'dotcms-template-builder-theme-selector',
    providers: [PaginatorService, DialogService, MessageService],
    imports: [
        ButtonModule,
        DropdownModule,
        DotSiteSelectorDirective,
        DataViewModule,
        CommonModule,
        DotMessagePipeModule
    ],
    standalone: true,
    templateUrl: './template-builder-theme-selector.component.html',
    styleUrls: ['./template-builder-theme-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TemplateBuilderThemeSelectorComponent implements OnInit, OnDestroy {
    ref: DynamicDialogRef;
    themes: DotTheme[] = [];

    @Input()
    value: DotTheme;

    @Output()
    selected = new EventEmitter<DotTheme>();

    @Output()
    shutdown = new EventEmitter<boolean>();

    @ViewChild('searchInput', { static: true })
    searchInput: ElementRef;

    @ViewChild('dataView', { static: true })
    dataView: DataView;

    @ViewChild('siteSelector', { static: true })
    current: DotTheme;
    visible = true;

    private destroy$: Subject<boolean> = new Subject<boolean>();
    private SEARCH_PARAM = 'searchParam';
    private initialLoad = true;

    constructor(
        private dotMessageService: DotMessageService,
        public dialogService: DialogService,
        public messageService: MessageService,
        public paginatorService: PaginatorService,
        private siteService: SiteService,
        public cd: ChangeDetectorRef,
        private config: DynamicDialogConfig
    ) {}

    ngOnInit() {
        this.current = this.value;
        this.paginatorService.url = 'v1/themes';
        this.paginatorService.setExtraParams(
            'hostId',
            this.current?.hostId || this.siteService.currentSite.identifier
        );
        this.paginatorService.deleteExtraParams(this.SEARCH_PARAM);
        this.paginatorService.paginationPerPage = 8;
        observableFromEvent(this.searchInput.nativeElement, 'keyup')
            .pipe(debounceTime(500), takeUntil(this.destroy$))
            .subscribe((keyboardEvent: Event) => {
                this.filterThemes(keyboardEvent.target['value']);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Load new page of themes.
     * @param LazyLoadEvent event
     *
     * @memberof DotThemeSelectorComponent
     */
    paginate($event: LazyLoadEvent): void {
        this.paginatorService
            .getWithOffset($event.first)
            .pipe(take(1))
            .subscribe((themes: DotTheme[]) => {
                if (this.noThemesInInitialLoad(themes, $event)) {
                    this.siteService.getSiteById('SYSTEM_HOST').subscribe((site: Site) => {
                        // eslint-disable-next-line no-console
                        console.log(site);
                        /*    this.siteSelector.searchableDropdown.handleClick(site); */
                        this.cd.detectChanges();
                    });
                } else {
                    this.themes = themes;
                    this.cd.detectChanges();
                }

                this.initialLoad = false;
            });
        this.dataView.first = $event.first;
    }

    /**
     * Handle change in the host to load the corresponding themes.
     * @param Site site
     *
     * @memberof DotThemeSelectorComponent
     */
    siteChange(site: Site): void {
        this.searchInput.nativeElement.value = null;
        this.paginatorService.setExtraParams('hostId', site.identifier);
        this.filterThemes('');
    }

    /**
     * Set the selected Theme by the user while the modal is open.
     * @param DotTheme theme
     *
     * @memberof DotThemeSelectorComponent
     */
    selectTheme(theme: DotTheme): void {
        this.current = theme;
    }

    /**
     * Propagate the selected theme once the user apply the changes.
     *
     * @memberof DotThemeSelectorComponent
     */
    apply(): void {
        this.config?.data?.onSelectTheme(this.current);
    }

    /**
     * Propagate the shutdown event when the modal closes.
     *
     * @memberof DotThemeSelectorComponent
     */
    hideDialog(): void {
        this.shutdown.emit(false);
    }

    private filterThemes(searchCriteria?: string): void {
        this.paginatorService.setExtraParams(this.SEARCH_PARAM, searchCriteria);
        this.paginate({ first: 0 });
    }

    private noThemesInInitialLoad(themes: DotTheme[], $event: LazyLoadEvent): boolean {
        return this.initialLoad && !themes.length && !$event.first;
    }
}
