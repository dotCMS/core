import { signalMethod } from '@ngrx/signals';
import { Events } from '@ngrx/signals/events';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, Observable, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import {
    Component,
    DestroyRef,
    Injector,
    computed,
    inject,
    signal,
    viewChild
} from '@angular/core';
import { Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';

import { catchError, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import {
    DotContentEditor,
    DotContentEditorChoice,
    DotContentletEditUrlService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotContentletCanLock,
    DotLanguage
} from '@dotcms/dotcms-models';
import {
    DotEditContentOpenerService,
    DotEditContentRequest,
    DotLegacyScreenEvent,
    DotLegacyScreenRequest,
    DotPageEditorRequest,
    DotSourceEditorRequest,
    injectEditContentPanel,
    loadDotLegacyScreenSidePanel,
    loadDotSourceEditorSidePanel,
    toSourceEditorRequest
} from '@dotcms/edit-content';
import { DotCMSInlineEditingPayload, DotCMSPage, DotCMSURLContentMap } from '@dotcms/types';
import {
    DotCopyContentModalService,
    DotSidePanelRef,
    DotSidePanelService,
    injectSidePanel
} from '@dotcms/ui';

import {
    DotUveAddContentRequest,
    DotUveCreateContentRequest,
    DotUveEditableContentlet,
    DotUveEditContentRequest,
    DotUveTranslatePageRequest,
    uveContentEditorEvents
} from './dot-uve-content-editor.events';

import { DotBlockEditorSidebarComponent } from '../../../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { EmaFormSelectorComponent } from '../../../components/ema-form-selector/ema-form-selector.component';
import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { injectContainerInsert } from '../../../shared/container-insert';
import { NG_CUSTOM_EVENTS, UVE_STATUS } from '../../../shared/enums';
import { ActionPayload, ReorderMenuPayload, VTLFile } from '../../../shared/models';
import { UVEStore, UVEStoreInstance } from '../../../store/dot-uve.store';
import { TranslateProps } from '../../../store/models';
import {
    createAddContentletUrl,
    createReorderMenuURL,
    createTranslatePageUrl,
    pageRequestContext,
    shouldNavigate,
    withPageContext
} from '../../../utils';

/** How {@link DotUveContentEditorComponent} opens a request in the Edit Content panel. */
interface OpenContentOptions {
    /** Where a create places its contentlet, on the first save that a container takes. */
    actionPayload?: ActionPayload;
    /** The request came from the SDK, whose edits also ask the client to reload. */
    fromSdk?: boolean;
    /** The request edits the content the URL-mapped page shows. */
    urlMap?: boolean;
}

/**
 * The one place in the UVE editor that opens a content editor: the Edit Content side panel, in
 * whichever editor the content type uses (#38303), the legacy admin screens (content selector,
 * menu reorder) and the form selector in side panels, the VTL source editor and the block editor
 * sidebar. Any part of the editor asks for one by dispatching a {@link uveContentEditorEvents}
 * request; this component builds the request with the page's context, hands it to the opener and
 * reloads the page when it saves. When a request carries a container position, it also inserts
 * the new or picked contentlet there, through the store. It also offers to create the page's
 * version in a language the page loaded in without one.
 */
@Component({
    selector: 'dot-uve-content-editor',
    imports: [DotBlockEditorSidebarComponent],
    templateUrl: './dot-uve-content-editor.component.html',
    providers: [DotCopyContentModalService],
    host: { class: 'contents' }
})
export class DotUveContentEditorComponent {
    protected readonly uveStore = inject(UVEStore) as UVEStoreInstance;
    readonly #events = inject(Events);
    readonly #injector = inject(Injector);
    readonly #destroyRef = inject(DestroyRef);
    readonly #sidePanels = inject(DotSidePanelService);
    readonly #opener = inject(DotEditContentOpenerService);
    readonly #editUrl = inject(DotContentletEditUrlService);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #contentletService = inject(DotContentletService);
    readonly #copyContentService = inject(DotCopyContentService);
    readonly #copyContentModalService = inject(DotCopyContentModalService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #pageApiService = inject(DotPageApiService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #router = inject(Router);
    readonly #iframeMessenger = inject(UveIframeMessengerService);
    /** Inserts what the user creates or picks, and tells them why when the container refuses it. */
    readonly #insert = injectContainerInsert();

    /**
     * The block editor sidebar. Only rendered while the user can edit the page's content.
     * TS-private because Angular queries cannot target `#private` members.
     */
    private readonly $blockSidebar = viewChild<DotBlockEditorSidebarComponent>('blockSidebar');

    /**
     * What the Edit Content side panel shows, or `null` while it is closed (#38303). One request
     * for either editor, bound through `injectEditContentPanel` in the constructor: the opener picks
     * the editor from the content type. Every editor entry point sets it, through
     * {@link #openContent}.
     */
    protected readonly $editRequest = signal<DotEditContentRequest | null>(null);

    /**
     * Where a create opened from a palette drop or the content selector places the new contentlet,
     * and whether a container took it yet. Later saves of the same panel only reload the page.
     * `null` for an edit, or a create that places nothing (the SDK's `createContentlet()`).
     */
    #insertion: { actionPayload: ActionPayload; inserted: boolean } | null = null;

    /**
     * The open request came from the SDK's `editContentlet()`: a save also asks the client to
     * reload itself, since a headless page renders its own content.
     */
    #fromSdkEdit = false;

    /**
     * The open request edits the content the URL-mapped page shows (the toolbar's banner): a save
     * that moves the content's URL loads the page at the new one. Any other edit, a pencil on a
     * URL-mapped contentlet placed on another page included, keeps the author on this page.
     */
    #urlMapEdit = false;

    /** The editor the open Edit Content panel shows, once the opener has resolved it. */
    #panelEditor: DotContentEditor | null = null;

    /**
     * The open panel asked for the page editor and that page is loading: the close the legacy
     * panel sends right after must not reload it again. Reset by the close and by the next open.
     */
    #pageEditorLoaded = false;

    /**
     * The legacy (JSP) screen open in the generic legacy-screen side panel, or `null` while it is
     * closed (#38303): the content selector or the menu reorder screen. Bound with
     * `injectSidePanel` in the constructor; its events arrive through {@link #onLegacyEvent}.
     */
    protected readonly $legacyScreenRequest = signal<DotLegacyScreenRequest | null>(null);

    /** The container the open content selector adds to, or `null` for any other screen. */
    #legacyScreenPayload: ActionPayload | null = null;

    /**
     * Drives the Edit Code side panel: the VTL file whose source is open in Monaco, or `null`
     * when closed. The panel is open while it is set (see the constructor).
     */
    protected readonly $sourceEditorPanel = signal<DotSourceEditorRequest | null>(null);

    // `mergeMap`: every request runs on its own, as separate clicks always have. Each one
    // handles its own errors, so a failed request never stops the listener.

    /** Opens the editor each edit request asks for. */
    readonly #handleEditContentRequest = rxMethod<DotUveEditContentRequest>(
        pipe(mergeMap((request) => this.#editContent(request)))
    );

    /** Opens the source of each VTL file an edit-source request names. */
    readonly #handleEditSourceRequest = rxMethod<VTLFile>(
        pipe(mergeMap((file) => this.#editSource(file)))
    );

    /** Opens the create each create request asks for. */
    readonly #handleCreateContentRequest = rxMethod<DotUveCreateContentRequest>(
        pipe(tap((request: DotUveCreateContentRequest) => this.#createContent(request)))
    );

    /** Opens the content selector or the form selector for each add request. */
    readonly #handleAddContentRequest = rxMethod<DotUveAddContentRequest>(
        pipe(tap((request: DotUveAddContentRequest) => this.#addContent(request)))
    );

    /** Opens each new language version of the page a translate request asks for. */
    readonly #handleTranslatePageRequest = rxMethod<DotUveTranslatePageRequest>(
        pipe(mergeMap((request) => this.#translatePage(request)))
    );

    /** Opens the contentlet each URL-mapped page request names, as the page's URL-map edit. */
    readonly #handleEditUrlContentMapRequest = rxMethod<DotCMSURLContentMap>(
        pipe(tap((contentlet: DotCMSURLContentMap) => this.#editUrlContentMap(contentlet)))
    );

    /** Opens the menu reorder screen for each reorder request. */
    readonly #handleReorderMenuRequest = rxMethod<ReorderMenuPayload>(
        pipe(tap((request: ReorderMenuPayload) => this.#reorderMenu(request)))
    );

    /** Opens the block editor sidebar on each Block Editor field a request names. */
    readonly #handleEditBlockEditorFieldRequest = rxMethod<DotCMSInlineEditingPayload>(
        pipe(tap((field: DotCMSInlineEditingPayload) => this.$blockSidebar()?.open(field)))
    );

    /**
     * The page's language and load status, as a new object each time either changes, so the
     * language check below runs on every change of either.
     */
    readonly #pageLanguage = computed(() => ({
        ...this.uveStore.pageTranslateProps(),
        status: this.uveStore.uveStatus()
    }));

    /**
     * A page can load in a language it has no version in; the user is then offered to create it.
     * Only a page that finished loading counts: while one loads, the language may still be the
     * previous page's. The confirm dialog reads PrimeNG-internal signals, which `signalMethod`
     * leaves untracked: tracking them would ask again every time it opens or closes.
     */
    readonly #handlePageLanguage = signalMethod<TranslateProps & { status: UVE_STATUS }>(
        ({ page, currentLanguage, status }) => {
            if (status === UVE_STATUS.LOADED && currentLanguage && !currentLanguage.translated) {
                this.#askToTranslate(currentLanguage, page);
            }
        }
    );

    constructor() {
        // While it is placed, the shell sends it the page's Properties instead of opening them
        // itself.
        this.uveStore.editorSetContentEditorPlaced(true);
        this.#destroyRef.onDestroy(() => this.uveStore.editorSetContentEditorPlaced(false));

        // The Edit Content side panel, open while `$editRequest` is set, in whichever editor the
        // content type uses (#38303). The page refreshes on every save while the panel stays open.
        injectEditContentPanel(this.$editRequest, {
            saved: (contentlet) => this.#onPanelSaved(contentlet),
            languageChanged: (languageId) =>
                this.uveStore.pageLoad({ language_id: String(languageId) }),
            pageEditorRequested: (page) => this.#onPageEditorRequested(page),
            deleted: () => this.uveStore.pageReload(),
            closed: () => this.#onPanelClosed(),
            editorResolved: (editor) => (this.#panelEditor = editor)
        });

        // A legacy admin screen, open while `$legacyScreenRequest` is set, in the generic
        // legacy-screen panel. It passes every `ng-event` the screen sends; closing clears it.
        injectSidePanel(this.$legacyScreenRequest, {
            component: loadDotLegacyScreenSidePanel,
            heading: (request) => request.title,
            inputs: (request) => ({ request }),
            outputs: {
                legacyEvent: (event: DotLegacyScreenEvent) => this.#onLegacyEvent(event)
            },
            closed: () => this.#closeLegacyScreen()
        });

        // The Edit Code side panel, open while `$sourceEditorPanel` is set. A save writes the
        // file's working version, which edit mode renders, so the page reloads to show it; the
        // panel then closes itself. Loaded on its first open, so Monaco stays out of this
        // portlet's chunk.
        injectSidePanel(this.$sourceEditorPanel, {
            component: loadDotSourceEditorSidePanel,
            heading: (request) => request.title,
            inputs: (request) => ({ request }),
            outputs: { saved: () => this.uveStore.pageReload() },
            closed: () => this.$sourceEditorPanel.set(null)
        });

        this.#handlePageLanguage(this.#pageLanguage);

        this.#handleEditContentRequest(
            this.#events
                .on(uveContentEditorEvents.uveEditContentRequested)
                .pipe(map(({ payload }) => payload))
        );

        this.#handleEditSourceRequest(
            this.#events
                .on(uveContentEditorEvents.uveEditSourceRequested)
                .pipe(map(({ payload }) => payload))
        );

        this.#handleCreateContentRequest(
            this.#events
                .on(uveContentEditorEvents.uveCreateContentRequested)
                .pipe(map(({ payload }) => payload))
        );

        this.#handleAddContentRequest(
            this.#events
                .on(uveContentEditorEvents.uveAddContentRequested)
                .pipe(map(({ payload }) => payload))
        );

        this.#handleTranslatePageRequest(
            this.#events
                .on(uveContentEditorEvents.uveTranslatePageRequested)
                .pipe(map(({ payload }) => payload))
        );

        this.#handleEditUrlContentMapRequest(
            this.#events
                .on(uveContentEditorEvents.uveEditUrlContentMapRequested)
                .pipe(map(({ payload }) => payload))
        );

        this.#handleReorderMenuRequest(
            this.#events
                .on(uveContentEditorEvents.uveReorderMenuRequested)
                .pipe(map(({ payload }) => payload))
        );

        this.#handleEditBlockEditorFieldRequest(
            this.#events
                .on(uveContentEditorEvents.uveEditBlockEditorFieldRequested)
                .pipe(map(({ payload }) => payload))
        );
    }

    /**
     * A contentlet in the Story Block the block editor drawer shows asked to be edited: it opens
     * in the side panel, above the drawer, in whichever editor its content type uses (#38303).
     *
     * @param contentlet The contentlet's inode and content type, as the block editor names them.
     */
    openBlockContentlet({ inode, contentType }: { inode: string; contentType: string }): void {
        this.#openContent(this.#buildRequest({ mode: 'edit', inode, contentType }));
    }

    /**
     * Opens the editor a request asks for. A contentlet picked from a container that is on more
     * than one page first asks whether to edit it on every page or a copy on this page only; the
     * copy is made in place, the page reloads, and the copy opens. The opener then picks the
     * editor from the content type.
     *
     * @param request what to edit, and how
     * @returns the work the request does, completing when it is done
     */
    #editContent({ contentlet, forkFrom, fromSdk }: DotUveEditContentRequest): Observable<unknown> {
        const onMultiplePages = Number(contentlet.onNumberOfPages ?? 1) > 1;

        if (!forkFrom || !onMultiplePages) {
            this.#openContent(this.#editRequestFor(contentlet), { fromSdk });

            return EMPTY;
        }

        const treeNode = this.uveStore.getCurrentTreeNode(forkFrom, contentlet);

        return this.#copyContentModalService.open().pipe(
            switchMap(({ shouldCopy }) =>
                shouldCopy ? this.#copyContentService.copyInPage(treeNode) : of(null)
            ),
            catchError((error: HttpErrorResponse) => {
                this.#httpErrorManager.handle(error);

                return EMPTY;
            }),
            // No copy: edit the original, on every page. A copy: edit it, on this page only.
            tap((copied: DotCMSContentlet | null) => {
                if (copied) {
                    this.uveStore.pageReload();
                }

                this.#openContent(this.#editRequestFor(copied ?? contentlet), { fromSdk });
            })
        );
    }

    /**
     * Opens a VTL file's source in the Edit Code side panel.
     *
     * `VTLFile` only carries `inode`/`name`, so the full contentlet is resolved by inode first:
     * `toSourceEditorRequest` needs its type, extension and name to decide whether the source
     * editor handles it, and the server's lock answer, fetched alongside, decides whether the user
     * may edit the source. A file the source editor does not handle, or one the user cannot edit,
     * opens in the content editor instead. If the file lookup fails (network/permissions), the
     * file opens in the content editor by its inode alone, and the opener fetches it.
     *
     * @param file the VTL file to edit
     * @returns the work the request does, completing when it is done
     */
    #editSource(file: VTLFile): Observable<unknown> {
        return forkJoin({
            contentlet: this.#contentletService.getContentletByInode(file.inode),
            // A failed lock check is an unknown answer, so it reads as "cannot edit the source".
            lock: this.#contentletService.canLock(file.inode).pipe(catchError(() => of(null)))
        }).pipe(
            take(1),
            catchError(() => of(null)),
            tap(
                (
                    result: {
                        contentlet: DotCMSContentlet;
                        lock: DotContentletCanLock | null;
                    } | null
                ) => {
                    if (!result) {
                        this.#openContent(
                            this.#buildRequest({
                                mode: 'edit',
                                inode: file.inode,
                                title: file.name
                            })
                        );

                        return;
                    }

                    const { contentlet, lock } = result;
                    const request = lock ? toSourceEditorRequest(contentlet, lock) : null;

                    if (!request) {
                        this.#openContent(this.#editRequestFor(contentlet));

                        return;
                    }

                    this.$sourceEditorPanel.set(request);
                }
            )
        );
    }

    /**
     * Opens a create of a content type in the page's language. The opener picks the editor from
     * the content type; what is saved goes into `into` when the request carries it, on the first
     * save a container takes.
     *
     * @param request what to create, and where it goes
     */
    #createContent({ contentType, into }: DotUveCreateContentRequest): void {
        this.#openContent(
            this.#buildRequest({
                mode: 'new',
                contentType: contentType.variable,
                languageId: this.uveStore.pageLanguageId(),
                title: this.#dotMessageService.get(
                    'contenttypes.content.create.contenttype',
                    contentType.name
                )
            }),
            into ? { actionPayload: into } : {}
        );
    }

    /**
     * A container's "add" menu (#38303): content and widgets open the legacy content selector in
     * the side panel, filtered by what the container accepts; forms open the form selector.
     *
     * @param request what to add, and where it goes
     */
    #addContent({ type, into }: DotUveAddContentRequest): void {
        if (type === 'form') {
            this.#openFormSelector(into);

            return;
        }

        const acceptTypes =
            type === 'widget'
                ? DotCMSBaseTypesContentTypes.WIDGET
                : (into.container.acceptTypes ?? '*');

        this.#openLegacyScreen(
            createAddContentletUrl(
                into.container.identifier,
                acceptTypes,
                into.language_id,
                this.uveStore.pageVariantId()
            ),
            this.#dotMessageService.get('edit.ema.page.dialog.header.search.content'),
            into
        );
    }

    /**
     * Opens a legacy admin screen in the generic legacy-screen side panel.
     *
     * @param url The screen's same-origin URL.
     * @param title The panel heading.
     * @param payload For the content selector, the container the selection goes in.
     */
    #openLegacyScreen(url: string, title: string, payload: ActionPayload | null = null): void {
        this.#legacyScreenPayload = payload;
        this.$legacyScreenRequest.set({ url, title });
    }

    /** Closes the legacy-screen panel and forgets the container it was adding to. */
    #closeLegacyScreen(): void {
        this.#legacyScreenPayload = null;
        this.$legacyScreenRequest.set(null);
    }

    /**
     * Reacts to what a legacy screen opened in the side panel reports: a selection or a "create
     * new" from the content selector, and the menu reorder screen's outcome. Any other event is
     * ignored.
     *
     * @param detail The `ng-event` detail the screen dispatched.
     */
    #onLegacyEvent(detail: DotLegacyScreenEvent): void {
        switch (detail?.name) {
            case NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT:
                this.#placeSelectedContentlet(String(detail.data?.['identifier'] ?? ''));
                break;

            case NG_CUSTOM_EVENTS.CREATE_CONTENTLET: {
                // "Create new" in the selector: the create opens in the Edit Content panel and
                // places the new contentlet in the container the selector was adding to.
                const actionPayload = this.#legacyScreenPayload;
                this.#closeLegacyScreen();
                this.#openContent(
                    this.#buildRequest({
                        mode: 'new',
                        contentType: String(detail.data?.['contentType'] ?? ''),
                        languageId: this.uveStore.pageLanguageId()
                    }),
                    actionPayload ? { actionPayload } : {}
                );
                break;
            }

            case NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER:
                this.#closeLegacyScreen();
                break;

            case NG_CUSTOM_EVENTS.SAVE_MENU_ORDER:
                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.#dotMessageService.get('message.menu.reordered'),
                    life: 2000
                });

                this.uveStore.pageReload();
                this.#closeLegacyScreen();

                // The page in the iframe renders the menu itself, so it reloads too. A stopgap
                // until the SDK has a reload strategy of its own.
                this.#iframeMessenger.reloadPage();
                break;

            case NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER:
                this.#messageService.add({
                    severity: 'error',
                    summary: this.#dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.#dotMessageService.get(
                        'error.menu.reorder.user_has_not_permission'
                    ),
                    life: 2000
                });
                break;
        }
    }

    /**
     * Places the contentlet chosen in the content selector in the container it was opened for,
     * through the store, and closes the selector. A duplicate or a full container is reported and
     * the selector stays open.
     *
     * @param identifier The chosen contentlet.
     */
    #placeSelectedContentlet(identifier: string): void {
        const actionPayload = this.#legacyScreenPayload;

        if (actionPayload && this.#insert(actionPayload, identifier)) {
            this.#closeLegacyScreen();
        }
    }

    /**
     * Opens the form selector as side panel content. A selected form is resolved to the
     * contentlet that renders it, placed in the container, and the page saved; the panel then
     * closes. A form that cannot be resolved reports the error and leaves the panel open.
     *
     * @param actionPayload The container the form goes in.
     */
    #openFormSelector(actionPayload: ActionPayload): void {
        const ref = this.#sidePanels.open(EmaFormSelectorComponent, {
            heading: this.#dotMessageService.get('edit.ema.page.dialog.header.search.form'),
            outputs: {
                selected: (formId: string) => this.#placeForm(actionPayload, formId, ref)
            },
            injector: this.#injector
        });
    }

    /**
     * Places a form chosen in the form selector in its container, through the store. A form goes
     * into a container under an identifier the container gives it, so that identifier is fetched
     * first, and the page shows it is loading meanwhile.
     *
     * @param actionPayload The container the form goes in.
     * @param formId The chosen form.
     * @param ref The form selector's panel, closed once the form is placed.
     */
    #placeForm(actionPayload: ActionPayload, formId: string, ref: DotSidePanelRef): void {
        this.#pageApiService
            .getFormIndetifier(actionPayload.container.identifier, formId)
            .pipe(
                tap(() => this.uveStore.setUveStatus(UVE_STATUS.LOADING)),
                take(1)
            )
            .subscribe({
                next: (formInContainerId: string) => {
                    if (!this.#insert(actionPayload, formInContainerId)) {
                        this.uveStore.setUveStatus(UVE_STATUS.LOADED);

                        return;
                    }

                    ref.close();
                },
                error: (error: HttpErrorResponse) => this.#httpErrorManager.handle(error)
            });
    }

    /**
     * Asks, in the shell's confirm dialog, whether to create the page's version in a language it
     * has none in. Accepting opens it in the editor the page's type uses; declining takes the user
     * out of the editor.
     *
     * @param language the language the page has no version in
     * @param page the page to translate
     */
    #askToTranslate(language: DotLanguage, page: DotCMSPage): void {
        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.header'
            ),
            message: this.#dotMessageService.get(
                'editpage.language-change-missing-lang-populate.confirm.message',
                language.language
            ),
            rejectIcon: 'hidden',
            acceptIcon: 'hidden',
            key: 'shell-confirm-dialog',
            accept: () => {
                this.#handleTranslatePageRequest({ page, newLanguage: language.id });
            },
            reject: () => {
                // The user declined creating the translation, so there is no page to show
                // in this language. Take them out of the dead-end instead of reloading the
                // same untranslated URL (which would re-open this dialog — see #36661).
                this.#redirectAfterTranslationRejected();
            }
        });
    }

    /**
     * Opens the page's translation into another language, in the editor the page's type uses
     * (#38303, T178): {@link #translateInNewEditor} or {@link #translateInLegacyEditor}. The
     * choice is handed to the opener, so it does not look the type up again.
     *
     * @param request the page, and the language to create its version in
     * @returns the editor lookup, completing once the translation opened
     */
    #translatePage({ page, newLanguage }: DotUveTranslatePageRequest): Observable<unknown> {
        return this.#editUrl.resolveEditor(page.contentType).pipe(
            take(1),
            tap((choice: DotContentEditorChoice) =>
                choice.editor === 'new'
                    ? this.#translateInNewEditor(page, newLanguage, choice)
                    : this.#translateInLegacyEditor(page, newLanguage, choice)
            )
        );
    }

    /**
     * Opens the page in the new editor, through the opener, asking for the language to translate
     * it into. The page has no version in it, so the editor opens the page as it is and offers the
     * translation: copied from the page, or from scratch; its first save creates it.
     *
     * A save loads the translation and keeps the panel open. Closing reloads the page only once
     * something was saved. Not written to the URL: the page editor keeps its panels out of it.
     *
     * @param page The page to translate.
     * @param newLanguage The language to translate it into.
     * @param resolved The page type's editor choice, handed to the opener.
     */
    #translateInNewEditor(
        page: DotCMSPage,
        newLanguage: number,
        resolved: DotContentEditorChoice
    ): void {
        const request = this.#buildRequest({
            mode: 'edit',
            inode: page.inode,
            identifier: page.identifier,
            contentType: page.contentType,
            languageId: newLanguage,
            title: page.title
        });
        let saved = false;

        this.#opener.open(request, {
            injector: this.#injector,
            resolved,
            saved: (contentlet) => {
                saved = true;
                this.#loadTranslation(contentlet);
            },
            closed: () => {
                if (saved) {
                    this.uveStore.pageReload();
                }
            }
        });
    }

    /**
     * Opens the page's translation in the legacy editor panel (#38303). The translate screen is
     * the legacy editor's create form pre-filled from the page, so it gets the workflow wizard,
     * compare and the unsaved-changes prompt that panel handles. It opens through the opener, with
     * the translate screen as its create screen and the editor already chosen, so the opener asks
     * the server nothing, and content a block editor in the form asks for stacks over it as over
     * any panel, at the right depth and with its saves notified.
     *
     * A save loads the translation and keeps the panel open; the editor's redirect after a first
     * save closes it and loads that page. Closing reloads the page only once something was saved.
     *
     * @param page The page to translate.
     * @param newLanguage The language to translate it into.
     * @param resolved The page type's editor choice, handed to the opener.
     */
    #translateInLegacyEditor(
        page: DotCMSPage,
        newLanguage: number,
        resolved: DotContentEditorChoice
    ): void {
        const context = pageRequestContext(this.uveStore);
        const { variantId, siteId } = context;
        const request = withPageContext(
            {
                mode: 'new',
                contentType: page.contentType,
                languageId: newLanguage,
                title: page.title
            },
            context
        );
        let saved = false;

        const ref = this.#opener.open(request, {
            injector: this.#injector,
            resolved,
            createUrl: createTranslatePageUrl(page, newLanguage, variantId, siteId),
            saved: (contentlet) => {
                saved = true;
                this.#loadTranslation(contentlet);
            },
            pageEditorRequested: ({ url, languageId }) => {
                // The page is loaded right here, so the close must not reload it again.
                saved = false;
                ref.close();
                this.uveStore.pageLoad({ url, language_id: String(languageId) });
            },
            // A delete is always followed by the panel closing, and only a saved translation can
            // be deleted, so this one reload covers it too.
            closed: () => {
                if (saved) {
                    this.uveStore.pageReload();
                }
            }
        });
    }

    /**
     * Loads the translation the editor saved: the saved page version, at its URL (which may
     * differ in the new language) and in its language.
     *
     * @param contentlet The saved page version.
     */
    #loadTranslation(contentlet: DotCMSContentlet): void {
        this.uveStore.pageLoad({
            language_id: String(contentlet.languageId),
            url:
                contentlet.url ||
                contentlet['URL_MAP_FOR_CONTENT'] ||
                this.uveStore.pageParams()?.url
        });
    }

    /**
     * Navigate the user away from an untranslated page after they decline creating a
     * translation for it.
     *
     * Redirects to the last different dotCMS URL the user was on before entering this
     * UVE session; if there is no usable previous URL, falls back to the Pages portlet.
     *
     * Why `DotRouterService.previousUrl` is the right source:
     * - Intra-UVE navigation (page/language/persona changes) is done with `pageLoad()` +
     *   a silent `Location.go()` in the shell, which does NOT emit a router `NavigationEnd`.
     *   So `previousUrl` is not polluted by same-page language switches — it holds the URL
     *   from before the editor opened (e.g. the Pages portlet, or another portlet).
     * - It is always an internal Angular route, never an external referrer, so the
     *   "only follow dotCMS URLs" requirement is satisfied by construction.
     *
     * We deliberately skip previous URLs that are:
     * - empty (no history — user opened the editor directly),
     * - public routes (e.g. the login page),
     * - any `/edit-page` route, which could resolve to the same untranslated page and
     *   re-open this dialog (the #36661 loop).
     *
     * In all skipped cases we fall back to the Pages portlet so the user always lands on
     * a valid page and is never left stuck on the untranslated one.
     */
    #redirectAfterTranslationRejected(): void {
        const previousUrl = this.#dotRouterService.previousUrl;

        const canUsePreviousUrl =
            !!previousUrl &&
            !this.#dotRouterService.isPublicUrl(previousUrl) &&
            !previousUrl.startsWith('/edit-page');

        if (canUsePreviousUrl) {
            this.#router.navigateByUrl(previousUrl);

            return;
        }

        this.#dotRouterService.gotoPortlet('/pages');
    }

    /**
     * Edits the content a URL-mapped page shows, as an ordinary edit request (#38303). It is the
     * one edit whose save follows the content to its new URL when the save moves it.
     *
     * @param contentlet The URL-mapped contentlet.
     */
    #editUrlContentMap(contentlet: DotCMSURLContentMap): void {
        this.#openContent(this.#editRequestFor(contentlet as unknown as DotCMSContentlet), {
            urlMap: true
        });
    }

    /**
     * Opens the menu reorder screen for the page's path and site in the legacy-screen panel.
     *
     * @param request the menu's start level and depth, as the SDK's `reorderMenu()` names them
     */
    #reorderMenu({ startLevel, depth }: ReorderMenuPayload): void {
        const pageParams = this.uveStore.pageParams();
        const site = this.uveStore.pageAsset()?.site;

        // The SDK asks from the page in the iframe, so a page and its site are always loaded.
        if (!pageParams || !site) {
            return;
        }

        const url = createReorderMenuURL({
            startLevel,
            depth,
            pagePath: pageParams.url,
            hostId: site.identifier
        });

        this.#openLegacyScreen(
            url,
            this.#dotMessageService.get('editpage.content.contentlet.menu.reorder.title')
        );
    }

    /**
     * The edit request for a contentlet on this page, completed with the page's context.
     *
     * @param contentlet The contentlet to edit.
     * @returns The request.
     */
    #editRequestFor(contentlet: DotUveEditableContentlet): DotEditContentRequest {
        return this.#buildRequest({
            mode: 'edit',
            inode: contentlet.inode,
            identifier: contentlet.identifier,
            contentType: contentlet.contentType,
            languageId: contentlet.languageId,
            title: contentlet.title
        });
    }

    /**
     * Completes a request with the page it is opened from: the site, the page editor as the
     * portlet, the page language when the content names none, and the experiment variant when the
     * page is not on the default one.
     *
     * @param request The content to open.
     * @returns The completed request.
     */
    #buildRequest(request: DotEditContentRequest): DotEditContentRequest {
        return withPageContext(request, pageRequestContext(this.uveStore));
    }

    /**
     * Opens a request in the Edit Content side panel.
     *
     * A request that arrives while a panel is open is ignored. The open panel already chose its
     * editor, and `injectEditContentPanel` does not reopen for a changed request, so the panel would
     * re-render with another contentlet in the wrong editor, and a pending placement would be lost.
     * The panel's mask covers the page, so only a headless page's SDK can ask then.
     *
     * @param request What to open.
     * @param options Where a create places its contentlet, and where the request came from.
     */
    #openContent(
        request: DotEditContentRequest,
        { actionPayload, fromSdk = false, urlMap = false }: OpenContentOptions = {}
    ): void {
        if (this.$editRequest()) {
            return;
        }

        this.#insertion = actionPayload ? { actionPayload, inserted: false } : null;
        this.#panelEditor = null;
        this.#fromSdkEdit = fromSdk && request.mode === 'edit';
        this.#urlMapEdit = urlMap && request.mode === 'edit';
        this.#pageEditorLoaded = false;
        this.$editRequest.set(request);
    }

    /**
     * A save in the Edit Content panel. The first save of a create opened from a drop or the
     * content selector places the new contentlet where it was asked for; any later save, and a save
     * of an SDK create, reloads the page. The URL-mapped content's edit loads the content's new URL
     * when it maps to another one; any other edit reloads (#38303, FR-021).
     *
     * @param contentlet The saved contentlet, as the editor reports it.
     */
    #onPanelSaved(contentlet: DotCMSContentlet): void {
        const insertion = this.#insertion;

        if (insertion && !insertion.inserted) {
            // Only once a container takes it: a refused placement leaves the panel open, and the
            // next save tries again.
            insertion.inserted = this.#insert(insertion.actionPayload, contentlet.identifier);

            return;
        }

        if (insertion || this.$editRequest()?.mode === 'new') {
            this.uveStore.pageReload();

            return;
        }

        if (this.#fromSdkEdit) {
            this.#iframeMessenger.reloadPage();
        }

        this.#reloadAfterEdit(contentlet);
    }

    /**
     * After an edit is saved: the URL-mapped content's edit loads the page at the content's new
     * URL when it moved; anything else reloads the page. The saved contentlet is complete, so
     * nothing is fetched.
     *
     * @param contentlet The saved contentlet.
     */
    #reloadAfterEdit(contentlet: DotCMSContentlet): void {
        const urlMap = contentlet?.['URL_MAP_FOR_CONTENT'];

        if (this.#urlMapEdit && shouldNavigate(urlMap, this.uveStore.pageParams()?.url ?? '')) {
            this.uveStore.pageLoad({ url: urlMap });

            return;
        }

        this.uveStore.pageReload();
    }

    /**
     * The legacy editor asked to open a page in the page editor (the first save of a new page):
     * close the panel and load that page. The legacy panel closes itself right after, and that
     * close must not reload the page just loaded.
     *
     * @param page The page's URL and language.
     */
    #onPageEditorRequested({ url, languageId }: DotPageEditorRequest): void {
        this.$editRequest.set(null);
        this.#insertion = null;
        this.#pageEditorLoaded = true;
        this.uveStore.pageLoad({ url, language_id: String(languageId) });
    }

    /**
     * The Edit Content panel closed itself: forget the request, then reload the page unless the new
     * editor showed. The reload is the safety net for workflow actions run in the legacy editor,
     * which report nothing the page can use (#38303, R4); the new editor reports every save, and
     * a delete reloads on its own. A panel closed before its editor was known reloads too. Not
     * after the panel asked for the page editor: that page is loading already.
     */
    #onPanelClosed(): void {
        const reload = this.#panelEditor !== 'new' && !this.#pageEditorLoaded;

        this.$editRequest.set(null);
        this.#insertion = null;
        this.#fromSdkEdit = false;
        this.#urlMapEdit = false;
        this.#panelEditor = null;
        this.#pageEditorLoaded = false;

        if (reload) {
            this.uveStore.pageReload();
        }
    }
}
