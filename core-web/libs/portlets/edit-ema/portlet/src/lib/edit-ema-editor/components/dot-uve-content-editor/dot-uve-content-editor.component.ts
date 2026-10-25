import { Events } from '@ngrx/signals/events';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, Observable, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectorRef, Component, inject, signal, viewChild } from '@angular/core';

import { MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { catchError, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSContentType, DotCMSContentlet, FeaturedFlags } from '@dotcms/dotcms-models';
import {
    DotEditContentDialogComponent,
    DotSourceEditorRequest,
    EditContentDialogData,
    loadDotEditContentSidePanel,
    loadDotSourceEditorSidePanel,
    toSourceEditorRequest
} from '@dotcms/edit-content';
import { DotCopyContentModalService, injectSidePanel } from '@dotcms/ui';
import { WINDOW } from '@dotcms/utils';

import {
    DotUveAddContentRequest,
    DotUveCreateContentRequest,
    DotUveEditContentRequest,
    uveContentEditorEvents
} from './dot-uve-content-editor.events';

import { DotEmaDialogComponent } from '../../../components/dot-ema-dialog/dot-ema-dialog.component';
import { DotPageApiService } from '../../../services/dot-page-api/dot-page-api.service';
import { notifyContainerInsertRefused } from '../../../shared/container-insert';
import { NG_CUSTOM_EVENTS, UVE_STATUS } from '../../../shared/enums';
import {
    ActionPayload,
    CreateContentletAction,
    DialogAction,
    VTLFile
} from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { WithPageApiMethods } from '../../../store/features/page-api/withPageApi';
import { getLanguageChangePageParams } from '../../../utils';

/**
 * The one place in the UVE editor that opens a content editor: the legacy JSP dialog, the new
 * editor in a centered dialog or in the side panel, and the VTL source editor. Any part of the
 * editor asks for one by dispatching a {@link uveContentEditorEvents} request; this component
 * decides which editor opens and reloads the page when it saves. When a request carries a
 * container position, it also inserts the new or picked contentlet there, through the store.
 */
@Component({
    selector: 'dot-uve-content-editor',
    imports: [DotEmaDialogComponent],
    templateUrl: './dot-uve-content-editor.component.html',
    styleUrls: ['./dot-uve-content-editor.component.scss'],
    providers: [DotCopyContentModalService]
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

    /**
     * The legacy dialog. Always rendered, so a legacy edit opens in any mode and on any page, as
     * page Properties does on the layout, rules and experiments routes. TS-private because Angular
     * queries cannot target `#private` members.
     */
    private readonly $legacyDialog = viewChild<DotEmaDialogComponent>('dialog');

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
    }

    /**
     * Handles what the legacy dialog reports.
     *
     * An edit only needs the page to show the saved content: a save reloads it, and a save in a
     * new language loads that language, at the page's new URL when the save moved it. A dialog
     * opened for a container position sends that position back with every event
     * (`actionPayload`): the content or form picked in its search, and a contentlet saved in its
     * create form, go into that position, and a content type picked to create opens the editor it
     * asks for.
     *
     * @param action what the dialog reported
     */
    protected handleLegacyDialogAction({ event, actionPayload }: DialogAction): void {
        const { detail } = event;

        switch (detail.name) {
            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                if (actionPayload) {
                    this.#insertInto(actionPayload, detail.payload?.contentletIdentifier);
                } else {
                    this.uveStore.pageReload();
                }

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
            appendTo: 'body',
            baseZIndex: 10000,
            closable: true,
            closeOnEscape: true,
            draggable: false,
            keepInViewport: true,
            modal: true,
            resizable: true,
            position: 'center',
            width: '95%',
            height: '95%',
            maskStyleClass: 'p-dialog-mask-dynamic p-dialog-create-content',
            style: { 'max-width': '1400px', 'max-height': '900px' },
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
