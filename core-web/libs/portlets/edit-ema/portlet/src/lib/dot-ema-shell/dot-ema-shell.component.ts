import { patchState, signalMethod } from '@ngrx/signals';

import { Location } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    OnDestroy,
    OnInit,
    signal,
    ViewChild
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router, RouterModule } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';

import { filter } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import {
    DEFAULT_VARIANT_ID,
    DotCMSContentlet,
    DotPageToolUrlParams,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import {
    DotPageScannerReportComponent,
    DotPageToolsSeoComponent,
    PageScannerToolType
} from '@dotcms/portlets/dot-ema/ui';
import { GlobalStore } from '@dotcms/store';
import { DotCMSPage, UVE_MODE } from '@dotcms/types';
import { DotInfoPageComponent, DotMessagePipe, DotNotLicenseComponent, InfoPage } from '@dotcms/ui';

import { EditEmaNavigationBarComponent } from './components/edit-ema-navigation-bar/edit-ema-navigation-bar.component';

import { DotEmaDialogComponent } from '../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageAssetKeys } from '../services/dot-page-api/dot-page-api.service';
import { DEFAULT_PERSONA, PERSONA_KEY } from '../shared/consts';
import { NG_CUSTOM_EVENTS, UVE_STATUS } from '../shared/enums';
import { DialogAction, DotPageAssetParams, NavigationBarItem } from '../shared/models';
import { UVEStore } from '../store/dot-uve.store';
import { DotUveViewParams } from '../store/models';
import {
    checkClientHostAccess,
    getErrorPayload,
    getRequestHostName,
    getTargetUrl,
    normalizeQueryParams,
    sanitizeURL,
    shouldNavigate
} from '../utils';
import { readExperimentsPortletSwitch } from '../utils/experiments-portlet-switch.util';

/**
 * Query params for the breadcrumb's address — the same page, spelled the way `editEmaGuard` wants
 * to read it.
 *
 * The address bar and the crumb are two different consumers. `normalizeQueryParams` shortens the
 * address for humans, and part of that is dropping the persona when it is the default one. But
 * `editEmaGuard` treats a missing persona as an incomplete URL and **redirects** to complete it,
 * so a crumb built from the shortened form points at an address nobody ever lands on: the router
 * reports the redirected URL, which no longer equals any crumb in the trail.
 *
 * That comparison is what `processSpecialRoute` uses to decide whether a navigation is a step
 * *back* into the trail (truncate it) or a step forward (append). With the crumb never matching,
 * returning to the editor from a deeper screen appended a second copy of the page instead of
 * rewinding — leaving the screen you just left sitting in the trail behind you.
 *
 * So the crumb states the persona explicitly, under the key the guard looks for.
 */
function crumbQueryParams(cleanedParams: Params, pageParams: Params | null): Params {
    const { personaId, ...rest } = cleanedParams;

    return {
        ...rest,
        [PERSONA_KEY]: personaId ?? pageParams?.[PERSONA_KEY] ?? DEFAULT_PERSONA.identifier
    };
}

/** Structural shape of `EditEmaEditorComponent.openContentForEdit` (the 'content' child route). */
interface RouteWithOpenContentForEdit {
    openContentForEdit(contentlet: DotCMSContentlet): void;
}

/**
 * Duck-typed guard instead of `instanceof EditEmaEditorComponent`: that class is lazy-loaded via
 * `loadComponent` in `lib.routes.ts`, and a value import here (required by `instanceof`) would pull
 * it into this shell's eager chunk, defeating the code-split.
 */
function hasOpenContentForEdit(component: unknown): component is RouteWithOpenContentForEdit {
    return (
        !!component &&
        typeof (component as RouteWithOpenContentForEdit).openContentForEdit === 'function'
    );
}

@Component({
    selector: 'dot-ema-shell',
    templateUrl: './dot-ema-shell.component.html',
    styleUrls: ['./dot-ema-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        ButtonModule,
        ConfirmDialogModule,
        ToastModule,
        EditEmaNavigationBarComponent,
        RouterModule,
        DotPageToolsSeoComponent,
        DotPageScannerReportComponent,
        DotEmaDialogComponent,
        DotInfoPageComponent,
        DotNotLicenseComponent,
        MessageModule,
        DotMessagePipe
    ],
    providers: [ConfirmationService]
})
export class DotEmaShellComponent implements OnInit, OnDestroy {
    @ViewChild('dialog') dialog!: DotEmaDialogComponent;
    @ViewChild('pageTools') pageTools!: DotPageToolsSeoComponent;
    @ViewChild('pageScanner') pageScanner!: DotPageScannerReportComponent;

    /**
     * The active child route's component, when it's the 'content' route (`EditEmaEditorComponent`)
     * — captured via the router-outlet `(activate)`/`(deactivate)` below. `null` while on a sibling
     * route ('layout', 'rules', 'experiments') that doesn't expose `openContentForEdit`.
     */
    #activeEditor: RouteWithOpenContentForEdit | null = null;

    readonly uveStore = inject(UVEStore);
    readonly destroyRef = inject(DestroyRef);
    readonly #activatedRoute = inject(ActivatedRoute);
    readonly #router = inject(Router);
    readonly #siteService = inject(SiteService);
    readonly #location = inject(Location);
    readonly #globalStore = inject(GlobalStore);
    readonly #dotMessageService = inject(DotMessageService);
    protected readonly $lockOptions = this.uveStore.$lockOptions;
    protected readonly $workflowLockIsLoading = this.uveStore.workflowLockIsLoading;
    protected readonly $lockedByDisplay = computed(
        () =>
            this.$lockOptions()?.lockedBy ??
            this.#dotMessageService.get('uve.shell.page.locked.unknown.user')
    );
    protected readonly $showLockBanner = computed(() => {
        const lockOptions = this.$lockOptions();
        return !!lockOptions?.isLocked && !lockOptions.isLockedByCurrentUser;
    });

    protected readonly $showBanner = signal<boolean>(true);

    protected readonly $showPageScanner = computed<boolean>(
        () => this.uveStore.flags()[FeaturedFlags.FEATURE_FLAG_PAGE_SCANNER] === true
    );

    /**
     * The UVE Experiments entry-point switch (#37005), read once per shell construction.
     *
     * Read into a signal rather than at the click, unlike the toolbar's return leg: the item's
     * `href` is *rendered*, and `$activeHref` highlights against it, so the value has to be known
     * synchronously when the menu is built. An action can afford an async read; a rendered
     * destination cannot.
     *
     * Once per shell is what the spec sanctions — "the switch is read once per full application
     * load … a stale value until the next reload is acceptable" — and `getFreshFeatureFlag` is
     * uncached, so each construction really re-fetches rather than reusing a value cached for the
     * SPA session. An operator flips it and reloads the editor: SC-002's under a minute, no
     * restart.
     *
     * A failed read resolves to `false` inside {@link readExperimentsPortletSwitch}, so the item
     * falls back to the legacy destination rather than going inert (FR-015).
     */
    protected readonly $experimentsPortletEnabled = toSignal(readExperimentsPortletSwitch(), {
        initialValue: false
    });

    // Component builds its own menu items locally
    protected readonly $menuItems = computed<NavigationBarItem[]>(() => {
        const page = this.uveStore.pageAsset()?.page;
        const template = this.uveStore.pageAsset()?.template;
        const isLoading = this.uveStore.uveStatus() === UVE_STATUS.LOADING;
        const templateDrawed = template?.drawed;
        const isLayoutDisabled = !this.uveStore.editorCanEditLayout();
        const canSeeRulesExists = page && 'canSeeRules' in page;
        const experimentsPortletEnabled = this.$experimentsPortletEnabled();

        return [
            {
                materialIcon: 'description',
                label: 'editema.editor.navbar.content',
                href: 'content',
                id: 'content'
            },
            {
                materialIcon: 'space_dashboard',
                label: 'editema.editor.navbar.layout',
                href: 'layout',
                id: 'layout',
                isDisabled: isLayoutDisabled,
                tooltip: templateDrawed
                    ? null
                    : 'editema.editor.navbar.layout.tooltip.cannot.edit.advanced.template'
            },
            {
                materialIcon: 'fork_left',
                label: 'editema.editor.navbar.rules',
                id: 'rules',
                href: `rules/${page?.identifier}`,
                isDisabled: (canSeeRulesExists && !page.canSeeRules) || !page?.canEdit
            },
            {
                materialIcon: 'science',
                label: 'editema.editor.navbar.experiments',
                // The switch selects the destination and nothing else: `isDisabled` is the same
                // rule on both sides, so an editor who cannot see experiments for this page does
                // not gain access through the new one (FR-023).
                ...(experimentsPortletEnabled
                    ? {
                          href: '/experiments',
                          queryParams: { pageAsset: page?.identifier }
                      }
                    : { href: `experiments/${page?.identifier}` }),
                id: 'experiments',
                isDisabled: !page?.canEdit
            },
            {
                materialIcon: 'health_and_safety',
                label: 'editema.editor.navbar.page-tools',
                id: 'page-tools'
            },
            {
                materialIcon: 'settings',
                label: 'editema.editor.navbar.properties',
                id: 'properties',
                isDisabled: isLoading
            }
        ];
    });

    // Component builds SEO params locally
    protected readonly $seoParams = computed<DotPageToolUrlParams>(() => {
        const url = sanitizeURL(this.uveStore.pageAsset()?.page?.pageURI);
        const currentUrl = url.startsWith('/') ? url : '/' + url;
        const requestHostName = getRequestHostName(
            this.uveStore.pageParams(),
            this.uveStore.pageAsset()?.site?.hostname
        );

        return {
            siteId: this.uveStore.pageAsset()?.site?.identifier,
            languageId: this.uveStore.pageAsset()?.viewAs?.language?.id,
            currentUrl,
            requestHostName
        };
    });

    // Component builds error display locally
    protected readonly $errorDisplay = computed<{ code: number; pageInfo: InfoPage } | null>(() => {
        const errorCode = this.uveStore.pageErrorCode();
        if (!errorCode) return null;

        return getErrorPayload(errorCode);
    });

    // Component determines read permissions locally
    protected readonly $canRead = computed<boolean>(() => {
        // Removed pageAPIResponse - use normalized accessors
        return this.uveStore.pageAsset()?.page?.canRead ?? false;
    });

    /**
     * Handle the update of the page params
     * When the page params change, we update the location
     *
     * @memberof DotEmaShellComponent
     */
    readonly $updateQueryParamsEffect = effect(() => {
        const params = this.uveStore.pageFriendlyParams();

        const { data } = this.#activatedRoute.snapshot;

        const baseClientHost = data?.uveConfig?.url;

        const cleanedParams = normalizeQueryParams(params, baseClientHost);

        this.#updateLocation(cleanedParams);
    });

    readonly $breadcrumbPage = computed<DotCMSPage | null>(() => {
        const page = this.uveStore.pageAsset()?.page;

        const status = this.uveStore.uveStatus();

        return page && status === UVE_STATUS.LOADED ? page : null;
    });

    readonly $updateBreadcrumb = signalMethod<DotCMSPage | null>((page) => {
        if (!page || !this.uveStore.pageParams()) return;

        const params = this.uveStore.pageFriendlyParams();
        const baseClientHost = this.#activatedRoute.snapshot.data?.uveConfig?.url;
        const cleanedParams = normalizeQueryParams(params, baseClientHost);
        const urlTree = this.#router.createUrlTree([], {
            queryParams: crumbQueryParams(cleanedParams, this.uveStore.pageParams())
        });
        const urlContentMap = this.uveStore.pageAsset()?.urlContentMap;
        const label = urlContentMap?.title ?? page.title;
        const identifier = urlContentMap?.identifier ?? page.identifier;

        this.#globalStore.addNewBreadcrumb({
            label,
            // Required, not decorative: PrimeNG's breadcrumb binds `[attr.target]="item.target"`,
            // so an item without one renders `target="undefined"` — a *named browsing context*.
            // The crumb then opened the editor in a new window instead of navigating, which read
            // as the link doing nothing. Every other crumb author in the app passes `_self`.
            target: '_self',
            url: `/dotAdmin/#${urlTree.toString()}`,
            id: `${identifier}`
        });
    });

    constructor() {
        this.$updateBreadcrumb(this.$breadcrumbPage);
    }

    ngOnInit(): void {
        const params = this.#getPageParams();
        const viewParams = this.#getViewParams(params.mode);

        // Initialize view viewParams from query parameters
        patchState(this.uveStore, { viewParams });

        // Check if we already have page data loaded with matching params
        const currentPageParams = this.uveStore.pageParams();

        const hasPageData = !!this.uveStore.pageAsset()?.page;
        const paramsMatch =
            currentPageParams &&
            currentPageParams.url === params.url &&
            currentPageParams.language_id === params.language_id &&
            currentPageParams.mode === params.mode &&
            currentPageParams.variantName === params.variantName &&
            currentPageParams[PERSONA_KEY] === params.personaId;

        if (!hasPageData || !paramsMatch) {
            this.uveStore.pageLoad(params);
        }

        this.#siteService.switchSite$
            .pipe(
                filter((site) => site?.identifier !== this.uveStore.pageAsset()?.site?.identifier),
                takeUntilDestroyed(this.destroyRef)
            )
            .subscribe(() => this.#router.navigate(['/pages']));
    }

    ngOnDestroy(): void {
        this.uveStore.resetPageParams();
    }

    handleNgEvent({ event }: DialogAction) {
        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.UPDATE_WORKFLOW_ACTION: {
                this.uveStore.workflowFetch(this.uveStore.pageAsset()?.page?.inode);
                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                this.handleSavePageEvent(event);
                break;
            }

            case NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED: {
                // Fired by the edit content portlet when a page is saved in a new language
                // (workingContentletInode is empty for a new version, so SAVE_PAGE is not
                // emitted). Reload to refresh pageLanguages so the UVE toolbar language
                // dropdown reflects the newly created version.
                this.uveStore.pageReload();
                break;
            }
        }
    }

    /**
     * Handles the save page event triggered from the dialog.
     *
     * @param {CustomEvent} event - The event object containing details about the save action.
     * @return {void}
     */
    private handleSavePageEvent(event: CustomEvent): void {
        const htmlPageReferer = event.detail.payload?.htmlPageReferer;

        if (!htmlPageReferer) {
            this.uveStore.pageReload();

            return;
        }

        const url = new URL(htmlPageReferer, window.location.origin); // Add base for relative URLs
        const targetUrl = getTargetUrl(url.pathname, this.uveStore.pageAsset()?.urlContentMap);

        if (shouldNavigate(targetUrl, this.uveStore.pageParams().url)) {
            // Navigate to the new URL if it's different from the current one
            this.uveStore.pageLoad({ url: targetUrl });

            return;
        }

        this.uveStore.pageReload();
    }

    /**
     * Tracks the active child route's component (bound to the `router-outlet` in the template) so
     * "Properties" can route through the new editor's side panel when it's mounted (the 'content'
     * route). See {@link RouteWithOpenContentForEdit} for why this isn't `instanceof`-based.
     */
    onRouteActivate(component: unknown): void {
        this.#activeEditor = hasOpenContentForEdit(component) ? component : null;
    }

    /** Clears the active-editor reference so a sibling route (layout/rules/experiments) falls back. */
    onRouteDeactivate(): void {
        this.#activeEditor = null;
    }

    /**
     * Handle actions from nav bar
     *
     * @param {string} itemId
     * @memberof DotEmaShellComponent
     */
    handleItemAction(itemId: string) {
        if (itemId === 'page-tools') {
            this.pageTools.toggleDialog();
        } else if (itemId === 'properties') {
            const page = this.uveStore.pageAsset()?.page;
            if (!page) {
                return;
            }

            // Editing the page's own properties is editing its contentlet — route it through the
            // same feature-flag-aware entry point as every other edit flow (new editor/side panel
            // when enabled for the page's content type, legacy dialog otherwise). Only available
            // while the 'content' child route is mounted; on 'layout'/'rules'/'experiments' fall
            // back to this shell's own legacy dialog (previous, route-independent behavior).
            if (this.#activeEditor) {
                this.#activeEditor.openContentForEdit(page as unknown as DotCMSContentlet);
                return;
            }

            this.dialog.editContentlet({
                inode: page.inode,
                title: page.title,
                identifier: page.identifier,
                contentType: page.contentType,
                angularCurrentPortlet: 'edit-page'
            });
        }
    }

    /**
     * Handle scanner tool click from the page tools panel.
     * Opens the page scanner report dialog with the selected tool type.
     *
     * The scanner is an external service that fetches the URL over the public
     * internet, so the URL must point at this authoring instance
     * (`window.location.origin`) — never the page's content-site hostname or a
     * headless `clientHost`, which may not be publicly reachable.
     *
     * To re-render the exact page the user is looking at, every page-resolving
     * param the editor is using is forwarded onto the scanned URL:
     * - `host_id` — disambiguates the site for multisite pages sharing a path
     *   (e.g. `/index`); dotCMS resolves it for the backend user regardless of host.
     * - `language_id`, `personaId`, `variantName`, `mode` and `publishDate`
     *   (time machine) — taken from the current page params so the scanner sees
     *   the same language, persona, variant, mode and point-in-time as the editor.
     *
     * @param {PageScannerToolType} type
     * @memberof DotEmaShellComponent
     */
    handleScannerToolClick(type: PageScannerToolType): void {
        const { currentUrl, siteId } = this.$seoParams();
        const url = new URL(currentUrl ?? '/', window.location.origin);

        if (siteId) {
            url.searchParams.set('host_id', siteId);
        }

        for (const [key, value] of Object.entries(this.#getScannerPageParams())) {
            url.searchParams.set(key, value);
        }

        this.pageScanner.open(type, url.toString());
    }

    /**
     * Build the page-resolving query params forwarded to the scanner from the
     * params the editor is currently rendering with.
     *
     * Only the params that change which page dotCMS resolves are kept
     * (`language_id`, persona, `variantName`, `mode`, `publishDate`). The page
     * path, `clientHost` and `depth` are excluded: the path is already the URL
     * itself, and `clientHost`/`depth` are editor-fetch concerns the public
     * scanner must not inherit.
     *
     * Unlike SPA navigation, this URL is fetched and rendered by the dotCMS
     * backend, so the persona must use the backend request param key
     * (`com.dotmarketing.persona.id`, `WebKeys.CMS_PERSONA_PARAMETER`) — NOT the
     * SPA-friendly `personaId`. Sending `personaId` would be silently ignored and
     * the page would render with no persona. The default persona and default
     * variant are dropped so the scanner falls back to the same implicit defaults
     * as the editor.
     *
     * @return {Record<string, string>}
     * @memberof DotEmaShellComponent
     */
    #getScannerPageParams(): Record<string, string> {
        const params = this.uveStore.pageParams() ?? ({} as DotPageAssetParams);

        const forwarded: Record<string, unknown> = {
            [DotPageAssetKeys.LANGUAGE_ID]: params.language_id,
            [DotPageAssetKeys.MODE]: params.mode,
            [DotPageAssetKeys.PUBLISH_DATE]: params.publishDate
        };

        // Persona keeps the backend request param key — the scanner is a backend
        // page render, not SPA navigation. Drop the default persona (implicit).
        const persona = params[PERSONA_KEY];
        if (persona && persona !== DEFAULT_PERSONA.identifier) {
            forwarded[PERSONA_KEY] = persona;
        }

        // The default variant is implicit — omit it to keep the URL clean.
        if (params.variantName && params.variantName !== DEFAULT_VARIANT_ID) {
            forwarded[DotPageAssetKeys.VARIANT_NAME] = params.variantName;
        }

        return Object.fromEntries(
            Object.entries(forwarded)
                .filter(([, value]) => value !== undefined && value !== null && value !== '')
                .map(([key, value]) => [key, String(value)])
        );
    }

    /**
     * Reloads the component from the dialog.
     */
    reloadFromDialog() {
        this.uveStore.pageReload();
    }

    /**
     * Handles closing the banner message by setting showBanner to false
     */
    onCloseMessage() {
        this.$showBanner.set(false);
    }

    /**
     * Toggles the lock state of the current page
     * Gets lock options from $lockOptions signal and calls store method to handle the lock/unlock
     */
    toggleLock() {
        const { inode, isLocked, isLockedByCurrentUser, lockedBy } = this.$lockOptions();
        this.uveStore.workflowToggleLock(inode, isLocked, isLockedByCurrentUser, lockedBy);
    }

    /**
     * Get the query params from the Router
     *
     * @return {*}  {DotPageApiParams}
     * @memberof DotEmaShellComponent
     */
    #getPageParams(): DotPageAssetParams {
        const { queryParams, data } = this.#activatedRoute.snapshot;
        const uveConfig = data?.uveConfig;
        const allowedDevURLs = uveConfig?.options?.allowedDevURLs;

        // Clone queryParams to avoid mutation errors
        const params = { ...queryParams };
        const validHost = checkClientHostAccess(params.clientHost, allowedDevURLs);

        //Sanitize the url
        params.url = sanitizeURL(params.url);

        if (!validHost) {
            delete params.clientHost;
        }

        if (uveConfig?.url && !validHost) {
            params.clientHost = uveConfig.url;
        }

        // If the editor mode is not valid, set it to edit mode
        const UVE_MODES = Object.values(UVE_MODE);

        if (!params.mode || !UVE_MODES.includes(params.mode)) {
            params.mode = UVE_MODE.EDIT;
        }

        if (params.mode !== UVE_MODE.LIVE && params.publishDate) {
            delete params?.['publishDate'];
        }

        if (queryParams['personaId']) {
            params[PERSONA_KEY] = queryParams['personaId'];
            delete params['personaId'];
        }

        return params as DotPageAssetParams;
    }

    #getViewParams(uveMode: UVE_MODE): DotUveViewParams {
        const { queryParams } = this.#activatedRoute.snapshot;

        const isPreviewMode = uveMode === UVE_MODE.PREVIEW || uveMode === UVE_MODE.LIVE;

        const viewParams: DotUveViewParams = {
            device: queryParams.device,
            orientation: queryParams.orientation,
            seo: queryParams.seo
        };

        return isPreviewMode
            ? viewParams
            : { device: undefined, orientation: undefined, seo: undefined };
    }

    /**
     * Update the location with the new query params
     *
     * Note: This method does not trigger a navigation event
     *
     * @param {Params} queryParams
     * @memberof DotEmaShellComponent
     */
    #updateLocation(queryParams: Params = {}): void {
        const urlTree = this.#router.createUrlTree([], { queryParams });
        this.#location.go(urlTree.toString());
    }
}
