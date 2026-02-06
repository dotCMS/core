import { CommonModule } from '@angular/common';
import {
    Component,
    computed,
    DestroyRef,
    ElementRef,
    HostListener,
    inject,
    signal,
    viewChild
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';

import { LazyLoadEvent, MenuItem } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';
import { Menu, MenuModule } from 'primeng/menu';
import { ProgressSpinnerModule } from 'primeng/progressspinner';

import { take } from 'rxjs/operators';

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
    DotSystemLanguage
} from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { DotAddToBundleComponent } from '@dotcms/ui';

import { TieredMenu } from 'primeng/tieredmenu';
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
    templateUrl: './dot-pages.component.html',
    imports: [
        MenuModule,
        CommonModule,
        RouterOutlet,
        ProgressSpinnerModule,
        DotAddToBundleComponent,
        DotPageFavoritesPanelComponent,
        DotPagesTableComponent,
        DotCreatePageDialogComponent,
        TieredMenu
    ],
    host: {
        class: 'h-full overflow-auto p-6 block'
    }
})
export class DotPagesComponent {
    readonly #dotRouterService = inject(DotRouterService);
    readonly #dotMessageDisplayService = inject(DotMessageDisplayService);
    readonly #dotEventsService = inject(DotEventsService);
    readonly #dotPageActionsService = inject(DotPageActionsService);
    readonly #element = inject(ElementRef);
    readonly #destroyRef = inject(DestroyRef);

    readonly #dotCMSPagesStore = inject(DotCMSPagesStore);
    readonly #globalStore = inject(GlobalStore);

    protected readonly $favoritePages = this.#dotCMSPagesStore.favoritePages;
    protected readonly $isFavoritePagesLoading = this.#dotCMSPagesStore.$isFavoritePagesLoading;
    protected readonly $pages = this.#dotCMSPagesStore.pages;
    protected readonly $isPagesLoading = this.#dotCMSPagesStore.$isPagesLoading;
    protected readonly $totalRecords = this.#dotCMSPagesStore.$totalRecords;
    protected readonly $showBundleDialog = this.#dotCMSPagesStore.$showBundleDialog;
    protected readonly $assetIdentifier = this.#dotCMSPagesStore.$assetIdentifier;
    protected readonly $systemLanguages = computed<DotSystemLanguage[]>(
        () => this.#globalStore.systemConfig()?.languages ?? []
    );
    protected readonly dialogVisible = signal<boolean>(false);

    readonly menu = viewChild<Menu>('menu');
    readonly menuItems = signal<MenuItem[]>([]);

    constructor() {
        this.listenSavePageEvent();
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
        this.menu()?.hide();
        this.menuItems.set([]);
    }

    /**
     * Event to show/hide actions menu when each contentlet is clicked
     *
     * @param {DotActionsMenuEventParams} event
     * @memberof DotPagesComponent
     */
    protected toggleMenu({ originalEvent, data }: DotActionsMenuEventParams): void {
        originalEvent.stopPropagation();
        if (this.menu()?.visible) {
            this.closeMenu();
            return;
        }
        this.openMenu({ originalEvent, data });
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
        this.#dotCMSPagesStore.searchPages(keyword);
    }

    /**
     * Filter pages by language
     *
     * @param {number} languageId
     * @memberof DotPagesComponent
     */
    protected onLanguageChange(languageId: number): void {
        this.#dotCMSPagesStore.filterByLanguage(languageId);
    }

    /**
     * Filter pages by archived
     *
     * @param {boolean} archived
     * @memberof DotPagesComponent
     */
    protected onArchivedChange(archived: boolean): void {
        this.#dotCMSPagesStore.filterByArchived(archived);
    }

    /**
     * Lazy load pages
     *
     * @param {LazyLoadEvent} event
     * @memberof DotPagesComponent
     */
    protected onLazyLoad(event: LazyLoadEvent): void {
        this.#dotCMSPagesStore.onLazyLoad(event);
    }

    /**
     * Close the bundle dialog
     *
     * @memberof DotPagesComponent
     */
    protected onCloseBundleDialog(): void {
        this.#dotCMSPagesStore.hideBundleDialog();
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
                    this.#dotCMSPagesStore.updateFavoritePageNode(baseIdentifier);
                } else {
                    this.#dotCMSPagesStore.updatePageNode(baseIdentifier);
                }

                this.#dotMessageDisplayService.push({
                    life: 3000,
                    message: value,
                    severity: DotMessageSeverity.SUCCESS,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
            });
    }

    protected getPages(): void {
        this.#dotCMSPagesStore.getPages({ offset: 0 });
    }

    /**
     * Opens the PrimeNG popup menu for a page row (three-dots button click).
     * Positions at the button using anchor; menu items are loaded asynchronously.
     *
     * @param event Menu trigger payload containing the original mouse event and the page contentlet.
     */
    private openMenu({ originalEvent, data }: DotActionsMenuEventParams): void {
        const anchor = originalEvent.currentTarget || originalEvent.target;
        const { clientX, clientY } = originalEvent;
        this.#dotPageActionsService
            .getItems(data)
            .pipe(take(1))
            .subscribe((actions) => {
                this.menu()?.show({
                    currentTarget: anchor,
                    target: anchor,
                    clientX,
                    clientY
                } as unknown as MouseEvent);
                this.menuItems.set(actions);
            });
    }
}
