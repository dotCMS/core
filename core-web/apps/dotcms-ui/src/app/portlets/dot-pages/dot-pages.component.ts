import { signalMethod } from '@ngrx/signals';

import { CommonModule } from '@angular/common';
import {
    Component,
    DestroyRef,
    ElementRef,
    HostListener,
    inject,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { LazyLoadEvent, MenuItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Menu, MenuModule } from 'primeng/menu';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import {
    DotESContentService,
    DotEventsService,
    DotFavoritePageService,
    DotMessageDisplayService,
    DotPageRenderService,
    DotPageTypesService,
    DotPageWorkflowsActionsService,
    DotRouterService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService,
    DotWorkflowEventHandlerService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import {
    DotCMSContentlet,
    DotEvent,
    DotMessageSeverity,
    DotMessageType,
    SiteEntity
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { DotCreatePageDialogComponent } from './dot-create-page-dialog/dot-create-page-dialog.component';
import { DotPageFavoritesPanelComponent } from './dot-page-favorites-panel/dot-page-favorites-panel.component';
import { DotPageStore } from './dot-pages-store/dot-pages.store';
import { DotPagesTableComponent } from './dot-pages-table/dot-pages-table.component';
import { DotPageActionsService } from './services/dot-page-actions.service';
import { DotPageListService } from './services/dot-page-list.service';
import { DotCMSPagesStore } from './store/store';

export interface DotActionsMenuEventParams {
    originalEvent: MouseEvent;
    data: DotCMSContentlet;
}

type SavePageEventData = {
    payload?: {
        identifier?: string;
        contentletIdentifier?: string;
        contentType?: string;
        contentletType?: string;
    };
    value?: string;
};

@Component({
    providers: [
        DotPageStore,
        DialogService,
        DotESContentService,
        DotPageListService,
        DotPageRenderService,
        DotPageTypesService,
        DotTempFileUploadService,
        DotWorkflowsActionsService,
        DotPageWorkflowsActionsService,
        DotWorkflowActionsFireService,
        DotWorkflowEventHandlerService,
        DotRouterService,
        DotFavoritePageService,
        DotCMSPagesStore,
        DotPageActionsService
    ],
    selector: 'dot-pages',
    styleUrls: ['./dot-pages.component.scss'],
    templateUrl: './dot-pages.component.html',
    imports: [
        MenuModule,
        CommonModule,
        ProgressSpinnerModule,
        DotAddToBundleComponent,
        DotPageFavoritesPanelComponent,
        DotPagesTableComponent,
        DotCreatePageDialogComponent
    ]
})
export class DotPagesComponent {
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);
    readonly #dotEventsService = inject(DotEventsService);
    readonly #dotPageActionsService = inject(DotPageActionsService);
    readonly #element = inject(ElementRef);
    readonly #destroyRef = inject(DestroyRef);

    readonly dotCMSPagesStore = inject(DotCMSPagesStore);
    readonly globalStore = inject(GlobalStore);

    protected readonly dialogVisible = signal<boolean>(false);

    readonly menu = viewChild<Menu>('menu');
    readonly menuItems = signal<MenuItem[]>([{ label: 'Test' }]);

    /**
     * Handle switch site
     * This will trigger a new fetch of pages when site loads for the first time
     *
     * @param {SiteEntity} _site
     * @memberof DotPagesComponent
     */
    readonly handleSwitchSite = signalMethod<SiteEntity>((_site: SiteEntity) => {
        this.dotCMSPagesStore.getPages({ offset: 0 });
        this.scrollToTop(); // To reset the scroll so it shows the data it retrieves
    });

    constructor() {
        // this.dotCMSPagesStore.getPages();
        this.dotCMSPagesStore.getFavoritePages();

        this.listenSavePageEvent();
        this.handleSwitchSite(this.globalStore.siteDetails());
    }

    /**
     * Event to redirect to Edit Page when Page selected
     * @param {string} url
     * @memberof DotPagesComponent
     */
    protected navigateToPage(url: string): void {
        const splittedUrl = url.split('?');
        const urlParams = { url: splittedUrl[0] };
        const searchParams = new URLSearchParams(splittedUrl[1]);

        for (const entry of searchParams) {
            urlParams[entry[0]] = entry[1];
        }

        this.#dotRouterService.goToEditPage(urlParams);
    }

    /**
     * Closes the menu when the user clicks outside of it
     *
     * @memberof DotPagesComponent
     */
    @HostListener('window:click')
    closeMenu(): void {
        this.menu().hide();
    }

    /**
     * Event to show/hide actions menu when each contentlet is clicked
     *
     * @param {DotActionsMenuEventParams} event
     * @memberof DotPagesComponent
     */
    protected openMenu({ originalEvent, data }: DotActionsMenuEventParams): void {
        originalEvent.stopPropagation();
        this.menu().toggle(originalEvent);
        this.#dotPageActionsService.getItems(data).subscribe((actions) => {
            this.menuItems.set(actions);
        });
    }

    /**
     * Scroll to top of the page
     *
     * @memberof DotPagesComponent
     */
    scrollToTop(): void {
        this.#element.nativeElement?.scroll({
            top: 0,
            left: 0
        });
    }

    /**
     * Search pages
     *
     * @param {string} keyword
     * @memberof DotPagesComponent
     */
    protected onSearch(keyword: string): void {
        this.dotCMSPagesStore.searchPages(keyword);
    }

    /**
     * Filter pages by language
     *
     * @param {number} languageId
     * @memberof DotPagesComponent
     */
    protected onLanguageChange(languageId: number): void {
        this.dotCMSPagesStore.filterByLanguage(languageId);
    }

    /**
     * Filter pages by archived
     *
     * @param {boolean} archived
     * @memberof DotPagesComponent
     */
    protected onArchivedChange(archived: boolean): void {
        this.dotCMSPagesStore.filterByArchived(archived);
    }

    /**
     * Lazy load pages
     *
     * @param {LazyLoadEvent} event
     * @memberof DotPagesComponent
     */
    protected onLazyLoad(event: LazyLoadEvent): void {
        this.dotCMSPagesStore.onLazyLoad(event);
    }

    /**
     * Listen to the save page event
     *
     * @memberof DotPagesComponent
     */
    private listenSavePageEvent(): void {
        this.#dotEventsService
            .listen('save-page')
            .pipe(takeUntilDestroyed(this.#destroyRef))
            .subscribe((event: DotEvent<SavePageEventData>) => {
                const { data } = event;
                const { value, payload } = data;
                const { contentletIdentifier, identifier, contentletType, contentType } =
                    payload ?? {};
                const baseType = contentType ?? contentletType;
                const baseIdentifier = identifier ?? contentletIdentifier;

                if (baseType === 'dotFavoritePage') {
                    this.dotCMSPagesStore.updateFavoritePageNode(baseIdentifier);
                } else {
                    this.dotCMSPagesStore.updatePageNode(baseIdentifier);
                }

                this.#dotMessageDisplayService.push({
                    life: 3000,
                    message: value,
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
            });
    }
}
