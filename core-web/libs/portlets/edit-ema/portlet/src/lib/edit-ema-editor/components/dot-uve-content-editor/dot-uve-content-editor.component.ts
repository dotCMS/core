import { signalMethod } from '@ngrx/signals';
import { Events } from '@ngrx/signals/events';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, Observable, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, computed, inject, signal, viewChild } from '@angular/core';
import { Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { catchError, filter, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService
} from '@dotcms/data-access';
import {
    DotCMSContentType,
    DotCMSContentlet,
    DotLanguage,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import {
    DotEditContentDialogComponent,
    DotSourceEditorRequest,
    EDIT_CONTENT_DIALOG_CONFIG,
    EditContentDialogData,
    loadDotEditContentSidePanel,
    loadDotSourceEditorSidePanel,
    toSourceEditorRequest
} from '@dotcms/edit-content';
import { DotCMSInlineEditingPayload, DotCMSPage, DotCMSURLContentMap } from '@dotcms/types';
import { DotCopyContentModalService, injectSidePanel } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

import {
    DotUveAddContentRequest,
    DotUveCreateContentRequest,
    DotUveEditContentRequest,
    DotUveTranslatePageRequest,
    uveContentEditorEvents
} from './dot-uve-content-editor.events';

import { DotBlockEditorSidebarComponent } from '../../../components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';
import { DotEmaDialogComponent } from '../../../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { notifyContainerInsertRefused } from '../../../shared/container-insert';
import { EDITOR_STATE, NG_CUSTOM_EVENTS, UVE_STATUS } from '../../../shared/enums';
import {
    ActionPayload,
    CreateContentletAction,
    DialogAction,
    ReorderMenuPayload,
    VTLFile
} from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { ReloadEditorContent } from '../../../store/features/editor/models';
import { WithPageApiMethods } from '../../../store/features/page-api/withPageApi';
import { TranslateProps } from '../../../store/models';
import { createReorderMenuURL, getLanguageChangePageParams } from '../../../utils';

/**
 * The one place in the UVE editor that opens a content editor: the legacy JSP dialog, the new
 * editor in a centered dialog or in the side panel, the VTL source editor and the block editor
 * sidebar. Any part of the editor asks for one by dispatching a {@link uveContentEditorEvents}
 * request; this component decides which editor opens and reloads the page when it saves. When a
 * request carries a container position, it also inserts the new or picked contentlet there,
 * through the store. It also offers to create the page's version in a language the page loaded
 * in without one.
 */
@Component({
    selector: 'dot-uve-content-editor',
    imports: [DotEmaDialogComponent, DotBlockEditorSidebarComponent],
    templateUrl: './dot-uve-content-editor.component.html',
    providers: [DotCopyContentModalService],
    host: { class: 'contents' }
})
export class DotUveContentEditorComponent {
    /**
     * The UVE store. `withPageApi`'s methods (`pageLoad`, `pageReload`…) reach the store's type
     * only through an index signature, as plain `Function`, so they are typed through that
     * feature's own interface.
     */
    protected readonly uveStore = inject(UVEStore) as InstanceType<typeof UVEStore> &
        WithPageApiMethods;
    readonly #events = inject(Events);
    readonly #window = inject(WINDOW);
    readonly #cd = inject(ChangeDetectorRef);
    readonly #dialogService = inject(DialogService);
    readonly #messageService = inject(MessageService);
    readonly #dotMessageService = inject(DotMessageService);
    readonly #contentTypeService = inject(DotContentTypeService);
    readonly #contentletService = inject(DotContentletService);
    readonly #copyContentService = inject(DotCopyContentService);
    readonly #copyContentModalService = inject(DotCopyContentModalService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #pageApiService = inject(DotPageApiService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #dotRouterService = inject(DotRouterService);
    readonly #router = inject(Router);
    readonly #iframeMessenger = inject(UveIframeMessengerService);

    /**
     * The legacy dialog. Always rendered, so a legacy edit opens in any mode and on any page, as
     * page Properties does on the layout, rules and experiments routes. TS-private because Angular
     * queries cannot target `#private` members.
     */
    private readonly $legacyDialog = viewChild<DotEmaDialogComponent>('dialog');

    /**
     * The block editor sidebar. Only rendered while the user can edit the page's content.
     * TS-private because Angular queries cannot target `#private` members.
     */
    private readonly $blockSidebar = viewChild<DotBlockEditorSidebarComponent>('blockSidebar');

    /**
     * Drives the Edit Content side panel: the contentlet to edit or create, or `null` when closed.
     * Only used when the side-panel flag is on; the panel is open while it is set (see the
     * constructor).
     */
    protected readonly $editContentPanel = signal<EditContentDialogData | null>(null);

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

    /** Opens the editor each create request asks for. */
    readonly #handleCreateContentRequest = rxMethod<DotUveCreateContentRequest>(
        pipe(mergeMap((request) => this.#createContent(request)))
    );

    /** Opens the legacy dialog's search for each add request. */
    readonly #handleAddContentRequest = rxMethod<DotUveAddContentRequest>(
        pipe(tap((request: DotUveAddContentRequest) => this.#addContent(request)))
    );

    /** Opens the editor for each content type the legacy dialog's content search asks to create. */
    readonly #handleCreateFromSearch = rxMethod<CreateContentletAction>(
        pipe(mergeMap((action) => this.#createFromSearch(action)))
    );

    /** Inserts each form picked in the legacy dialog. */
    readonly #handleFormSelected = rxMethod<{ into: ActionPayload; formId: string }>(
        pipe(mergeMap(({ into, formId }) => this.#insertForm(into, formId)))
    );

    /** Opens each new language version of the page a translate request asks for. */
    readonly #handleTranslatePageRequest = rxMethod<DotUveTranslatePageRequest>(
        pipe(tap((request: DotUveTranslatePageRequest) => this.#translatePage(request)))
    );

    /** Opens the legacy dialog on each URL-mapped page's contentlet a request names. */
    readonly #handleEditUrlContentMapRequest = rxMethod<DotCMSURLContentMap>(
        pipe(
            tap((contentlet: DotCMSURLContentMap) =>
                this.$legacyDialog()?.editUrlContentMapContentlet(contentlet)
            )
        )
    );

    /** Shows the URL-mapped page again each time the legacy dialog saves its contentlet. */
    readonly #handleUrlContentMapSaved = rxMethod<string>(
        pipe(mergeMap((inodeOrIdentifier) => this.#reloadUrlContentMapPage(inodeOrIdentifier)))
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
     * The legacy dialog keeps the container position a create or add opened it for, and sends it
     * back with every event. Once the page content reloads, what was saved is in, so the position
     * is forgotten: a second save in the same create form then only reloads, instead of inserting
     * again.
     */
    readonly #handlePageContentReload = signalMethod<ReloadEditorContent>(() => {
        this.$legacyDialog()?.resetActionPayload();
    });

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
        // The Edit Content side panel, open while `$editContentPanel` is set. Post-save work runs
        // through the request's `onContentSaved` callback; closing clears the request. Loaded on
        // its first open, so the editor stays out of this portlet's chunk.
        injectSidePanel(this.$editContentPanel, {
            component: loadDotEditContentSidePanel,
            heading: (data) => data.title ?? '',
            inputs: (data) => ({ data }),
            closed: () => this.$editContentPanel.set(null)
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

        this.#handlePageContentReload(this.uveStore.$reloadEditorContent);
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
     * Handles what the legacy dialog reports.
     *
     * An edit only needs the page to show the saved content: a save reloads it, and a save in a
     * new language loads that language, at the page's new URL when the save moved it. A saved
     * URL-mapped contentlet may have moved the page, so the page follows the contentlet's URL. A
     * dialog opened for a container position sends that position back with every event
     * (`actionPayload`): the content or form picked in its search, and a contentlet saved in its
     * create form, go into that position, and a content type picked to create opens the editor it
     * asks for. The menu reorder screen reports whether the new order was saved.
     *
     * @param action what the dialog reported
     */
    protected handleLegacyDialogAction({ event, actionPayload }: DialogAction): void {
        const { detail } = event;

        switch (detail.name) {
            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                if (actionPayload) {
                    this.#insertInto(actionPayload, detail.payload?.contentletIdentifier);
                } else if (detail.payload?.shouldReloadPage) {
                    // Only a URL-mapped contentlet's save sets it.
                    this.#handleUrlContentMapSaved(detail.payload.contentletIdentifier);
                } else {
                    this.uveStore.pageReload();
                }

                break;
            }

            case NG_CUSTOM_EVENTS.SAVE_MENU_ORDER: {
                this.#messageService.add({
                    severity: 'success',
                    summary: this.#dotMessageService.get(
                        'editpage.content.contentlet.menu.reorder.title'
                    ),
                    detail: this.#dotMessageService.get('message.menu.reordered'),
                    life: 2000
                });

                this.uveStore.pageReload();
                this.$legacyDialog()?.resetDialog();

                // The page in the iframe renders the menu itself, so it reloads too. A stopgap
                // until the SDK has a reload strategy of its own.
                this.#iframeMessenger.reloadPage();

                break;
            }

            case NG_CUSTOM_EVENTS.ERROR_SAVING_MENU_ORDER: {
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

            case NG_CUSTOM_EVENTS.CANCEL_SAVING_MENU_ORDER: {
                this.$legacyDialog()?.resetDialog();
                // The JSP inside the legacy dialog reports it outside Angular's zone.
                this.#cd.detectChanges();

                break;
            }

            case NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED: {
                const pageParams = this.uveStore.pageParams();

                // With no page loaded there is no page to show in the new language.
                if (pageParams) {
                    this.uveStore.pageLoad(
                        getLanguageChangePageParams(detail.payload?.htmlPageReferer, {
                            origin: this.#window.location.origin,
                            urlContentMap: this.uveStore.pageAsset()?.urlContentMap,
                            currentUrl: pageParams.url
                        })
                    );
                }

                break;
            }

            // Only a dialog opened for a container position offers a content or form search.
            case NG_CUSTOM_EVENTS.CONTENT_SEARCH_SELECT: {
                if (actionPayload && this.#insertInto(actionPayload, detail.data.identifier)) {
                    this.$legacyDialog()?.resetDialog();
                }

                break;
            }

            case NG_CUSTOM_EVENTS.FORM_SELECTED: {
                if (actionPayload) {
                    this.#handleFormSelected({
                        into: actionPayload,
                        formId: detail.data.identifier
                    });
                }

                break;
            }

            case NG_CUSTOM_EVENTS.CREATE_CONTENTLET: {
                const { contentType, url } = detail.data;

                this.#handleCreateFromSearch({ contentType, url, actionPayload });
                break;
            }
        }
    }

    /**
     * Opens the editor a request asks for. A contentlet picked from a container that is on more
     * than one page first asks whether to edit it on every page or a copy on this page only; the
     * copy is made in place, the page reloads, and the copy opens. Then the content type decides
     * the editor.
     *
     * @param request what to edit, and how
     * @returns the work the request does, completing when it is done
     */
    #editContent({ contentlet, forkFrom }: DotUveEditContentRequest): Observable<unknown> {
        const onMultiplePages = Number(contentlet['onNumberOfPages'] ?? 1) > 1;

        if (!forkFrom || !onMultiplePages) {
            return this.#openContentEditor(contentlet);
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
            switchMap((copied) => {
                if (copied) {
                    this.uveStore.pageReload();
                }

                return this.#openContentEditor(copied ?? contentlet);
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
     * opens in the content editor instead. A failed lookup falls back to the legacy dialog.
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
            switchMap((result) => {
                if (!result) {
                    this.$legacyDialog()?.editVTLContentlet(file);

                    return EMPTY;
                }

                const { contentlet, lock } = result;
                const request = lock ? toSourceEditorRequest(contentlet, lock) : null;

                if (!request) {
                    return this.#openContentEditor(contentlet);
                }

                this.$sourceEditorPanel.set(request);

                return EMPTY;
            })
        );
    }

    /**
     * Opens the editor to create a contentlet of a content type, in the page's language. The
     * content type decides between the new editor and the legacy create form; what is saved goes
     * into `into` when the request carries it.
     *
     * @param request what to create, and where it goes
     * @returns the content type lookup, completing once an editor opened
     */
    #createContent({ contentType, into }: DotUveCreateContentRequest): Observable<unknown> {
        return this.#openCreateEditor(contentType, into, () =>
            this.$legacyDialog()?.createContentletFromPalette({
                ...contentType,
                actionPayload: into,
                language_id: this.uveStore.pageLanguageId()
            })
        );
    }

    /**
     * Opens the editor to create a contentlet of the content type the legacy dialog's content
     * search picked. The JSP already resolved the legacy create form's URL, so the legacy form
     * opens on it.
     *
     * @param action the content type, the legacy form's URL and the position the dialog was
     * opened for
     * @returns the content type lookup, completing once an editor opened
     */
    #createFromSearch({
        contentType,
        url,
        actionPayload
    }: CreateContentletAction): Observable<unknown> {
        return this.#openCreateEditor(
            { variable: contentType, name: contentType },
            actionPayload,
            () => {
                this.$legacyDialog()?.createContentlet({ contentType, url, actionPayload });
                // The request comes from the JSP inside the legacy dialog, outside Angular's zone.
                this.#cd.detectChanges();
            }
        );
    }

    /**
     * Opens the legacy dialog's search for a container's "add" menu: existing content, a widget or
     * a form. What the user picks comes back as a dialog event, with `into` attached.
     *
     * @param request what to add, and where it goes
     */
    #addContent({ type, into }: DotUveAddContentRequest): void {
        const dialog = this.$legacyDialog();

        switch (type) {
            case 'content':
                dialog?.addContentlet(into);
                break;
            case 'widget':
                dialog?.addWidget(into);
                break;
            case 'form':
                dialog?.addForm(into);
                break;
        }
    }

    /**
     * Inserts a form picked in the legacy dialog. A form goes into a container under an identifier
     * the container gives it, so that identifier is fetched first, and the page shows it is
     * loading meanwhile. A form the container cannot resolve is dropped.
     *
     * @param into the container position the dialog was opened for
     * @param formId the form the user picked
     * @returns the identifier lookup, completing once the form went in or was refused
     */
    #insertForm(into: ActionPayload, formId: string): Observable<unknown> {
        return this.#pageApiService.getFormIndetifier(into.container.identifier, formId).pipe(
            take(1),
            tap((formInContainerId: string) => {
                this.uveStore.setUveStatus(UVE_STATUS.LOADING);

                if (this.#insertInto(into, formInContainerId)) {
                    this.$legacyDialog()?.resetDialog();
                } else {
                    this.uveStore.setUveStatus(UVE_STATUS.LOADED);
                }
            }),
            catchError(() => EMPTY)
        );
    }

    /**
     * Inserts a contentlet into a container position through the store. When the container
     * refuses it, tells the user why and closes the legacy dialog.
     *
     * @param into the container position
     * @param contentletId the contentlet to insert
     * @returns whether it went in
     */
    #insertInto(into: ActionPayload, contentletId: string): boolean {
        const result = this.uveStore.editorInsertContentlet(into, contentletId);

        // `=== false`: without strictNullChecks, as this library compiles, `!` does not narrow the
        // result to a refusal.
        if (result.didInsert === false) {
            notifyContainerInsertRefused(result, this.#messageService, this.#dotMessageService);
            this.$legacyDialog()?.resetDialog();
        }

        return result.didInsert;
    }

    /**
     * Asks, in the shell's confirm dialog, whether to create the page's version in a language it
     * has none in. Accepting opens it in the legacy dialog; declining takes the user out of the
     * editor.
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
                this.#translatePage({ page, newLanguage: language.id });
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
     * Opens the legacy dialog on the page's new language version.
     *
     * @param request the page, and the language to create its version in
     */
    #translatePage(request: DotUveTranslatePageRequest): void {
        this.$legacyDialog()?.translatePage(request);
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
     * Shows a URL-mapped page again once the legacy dialog saved its contentlet. The save may
     * have changed the contentlet's URL, which is the page's: a new URL loads, the same one
     * reloads. The page shows it is loading meanwhile; a failed read is reported and puts the
     * editor in its error state.
     *
     * @param inodeOrIdentifier the saved contentlet
     * @returns the contentlet read, completing once the page was asked to show it
     */
    #reloadUrlContentMapPage(inodeOrIdentifier: string): Observable<unknown> {
        this.uveStore.setUveStatus(UVE_STATUS.LOADING);

        return this.#contentletService.getContentletByInode(inodeOrIdentifier).pipe(
            catchError((error: HttpErrorResponse) => {
                this.uveStore.setEditorState(EDITOR_STATE.ERROR);

                return this.#httpErrorManager.handle(error).pipe(map(() => null));
            }),
            filter((contentlet: DotCMSContentlet | null) => !!contentlet),
            tap(({ URL_MAP_FOR_CONTENT }: DotCMSContentlet) => {
                if (URL_MAP_FOR_CONTENT != this.uveStore.pageParams()?.url) {
                    this.uveStore.pageLoad({ url: URL_MAP_FOR_CONTENT });

                    return;
                }

                this.uveStore.pageReload();
            })
        );
    }

    /**
     * Opens the legacy dialog on the menu reorder screen for the page's path and site.
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

        this.$legacyDialog()?.openDialogOnUrl(
            url,
            this.#dotMessageService.get('editpage.content.contentlet.menu.reorder.title')
        );
    }

    /**
     * Opens the new editor when the contentlet's content type enables it, and the legacy dialog
     * otherwise, including when the content type cannot be read or the contentlet has none.
     *
     * @param contentlet the contentlet to edit
     * @returns the content type lookup, completing once an editor opened
     */
    #openContentEditor(contentlet: DotCMSContentlet): Observable<unknown> {
        if (!contentlet.contentType) {
            this.$legacyDialog()?.editContentlet(contentlet);

            return EMPTY;
        }

        return this.#contentTypeService.getContentType(contentlet.contentType).pipe(
            take(1),
            catchError(() => of(null)),
            tap((contentType: DotCMSContentType | null) => {
                if (isOnNewEditor(contentType)) {
                    this.#openNewEditorToEdit(contentlet);
                } else {
                    this.$legacyDialog()?.editContentlet(contentlet);
                }
            })
        );
    }

    /**
     * Opens the new editor to create a contentlet when the content type enables it, and
     * `openLegacyForm` otherwise, including when the content type cannot be read.
     *
     * @param contentType the content type to create, and the name to fall back to
     * @param into the container position what is saved goes into, if any
     * @param openLegacyForm opens the legacy create form instead
     * @returns the content type lookup, completing once an editor opened
     */
    #openCreateEditor(
        contentType: DotUveCreateContentRequest['contentType'],
        into: ActionPayload | undefined,
        openLegacyForm: () => void
    ): Observable<unknown> {
        return this.#contentTypeService.getContentType(contentType.variable).pipe(
            take(1),
            catchError(() => of(null)),
            tap((found: DotCMSContentType | null) => {
                if (isOnNewEditor(found)) {
                    this.#openNewEditorToCreate(
                        contentType.variable,
                        found?.name ?? contentType.name,
                        into
                    );
                } else {
                    openLegacyForm();
                }
            })
        );
    }

    /**
     * Opens the new editor on a contentlet. A save reloads the page.
     *
     * @param contentlet the contentlet to edit
     */
    #openNewEditorToEdit(contentlet: DotCMSContentlet): void {
        this.#openNewEditor(contentlet.title ?? '', {
            mode: 'edit',
            contentletInode: contentlet.inode,
            onContentSaved: () => {
                this.uveStore.pageReload();
            }
        });
    }

    /**
     * Closes the legacy dialog and opens the new editor to create a contentlet. What is saved goes
     * into `into`; without a position, the page reloads instead.
     *
     * @param contentTypeId the content type to create
     * @param contentTypeName the name shown in the editor's header
     * @param into the container position what is saved goes into, if any
     */
    #openNewEditorToCreate(
        contentTypeId: string,
        contentTypeName: string,
        into: ActionPayload | undefined
    ): void {
        this.$legacyDialog()?.resetDialog();

        this.#openNewEditor(
            this.#dotMessageService.get('contenttypes.content.create.contenttype', contentTypeName),
            {
                mode: 'new',
                contentTypeId,
                onContentSaved: (contentlet) => {
                    if (!into) {
                        this.uveStore.pageReload();

                        return;
                    }

                    if (contentlet?.identifier) {
                        this.#insertInto(into, contentlet.identifier);
                    }
                }
            }
        );
    }

    /**
     * Opens the new editor: in the side panel when its flag is on, otherwise in a centered dialog.
     *
     * @param header the title shown above the editor
     * @param data what the editor opens, and what to do once it saves
     */
    #openNewEditor(header: string, data: EditContentDialogData): void {
        if (this.uveStore.flags()[FeaturedFlags.FEATURE_FLAG_EDIT_CONTENT_SIDE_PANEL]) {
            // The side panel shows `title` in its header, as the dialog shows `header`.
            this.$editContentPanel.set({ ...data, title: header });

            return;
        }

        this.#dialogService.open(DotEditContentDialogComponent, {
            ...EDIT_CONTENT_DIALOG_CONFIG,
            contentStyle: { padding: '0' },
            data,
            header
        });
    }
}

/**
 * Whether a content type opens in the new editor: only when its metadata turns it on explicitly.
 *
 * @param contentType the content type, or `null` when it could not be read
 * @returns whether the new editor opens it
 */
function isOnNewEditor(contentType: DotCMSContentType | null): boolean {
    return contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] === true;
}
