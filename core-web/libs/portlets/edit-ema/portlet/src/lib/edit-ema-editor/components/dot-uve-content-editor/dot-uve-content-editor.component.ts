import { Events } from '@ngrx/signals/events';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, Observable, forkJoin, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal, viewChild } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { catchError, map, mergeMap, switchMap, take, tap } from 'rxjs/operators';

import {
    DotContentTypeService,
    DotContentletService,
    DotCopyContentService,
    DotHttpErrorManagerService
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

import { DotUveEditContentRequest, uveContentEditorEvents } from './dot-uve-content-editor.events';

import { DotEmaDialogComponent } from '../../../components/dot-ema-dialog/dot-ema-dialog.component';
import { NG_CUSTOM_EVENTS } from '../../../shared/enums';
import { DialogAction, VTLFile } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { WithPageApiMethods } from '../../../store/features/page-api/withPageApi';
import { getLanguageChangePageParams } from '../../../utils';

/**
 * The one place in the UVE editor that opens a content editor: the legacy JSP dialog, the new
 * editor in a centered dialog or in the side panel, and the VTL source editor. Any part of the
 * editor asks for one by dispatching a {@link uveContentEditorEvents} request; this component
 * decides which editor opens and reloads the page when it saves.
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
    readonly #dialogService = inject(DialogService);
    readonly #contentTypeService = inject(DotContentTypeService);
    readonly #contentletService = inject(DotContentletService);
    readonly #copyContentService = inject(DotCopyContentService);
    readonly #copyContentModalService = inject(DotCopyContentModalService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    /**
     * The legacy dialog. Always rendered, so a legacy edit opens in any mode and on any page, as
     * page Properties does on the layout, rules and experiments routes. TS-private because Angular
     * queries cannot target `#private` members.
     */
    private readonly $legacyDialog = viewChild<DotEmaDialogComponent>('dialog');

    /**
     * Drives the Edit Content side panel: the contentlet to edit, or `null` when closed. Only used
     * when the side-panel flag is on; the panel is open while it is set (see the constructor).
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
    }

    /**
     * Handles what the legacy dialog reports. An edit only needs the page to show the saved
     * content: a save reloads it, and a save in a new language loads that language, at the
     * page's new URL when the save moved it.
     *
     * @param action what the dialog reported
     */
    protected handleLegacyDialogAction({ event }: DialogAction): void {
        switch (event.detail.name) {
            case NG_CUSTOM_EVENTS.SAVE_PAGE: {
                this.uveStore.pageReload();
                break;
            }

            case NG_CUSTOM_EVENTS.LANGUAGE_IS_CHANGED: {
                const pageParams = this.uveStore.pageParams();

                // With no page loaded there is no page to show in the new language.
                if (pageParams) {
                    this.uveStore.pageLoad(
                        getLanguageChangePageParams(event.detail.payload?.htmlPageReferer, {
                            origin: this.#window.location.origin,
                            urlContentMap: this.uveStore.pageAsset()?.urlContentMap,
                            currentUrl: pageParams.url
                        })
                    );
                }

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
                if (
                    contentType?.metadata?.[FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED] ===
                    true
                ) {
                    this.#openNewEditor(contentlet);
                } else {
                    this.$legacyDialog()?.editContentlet(contentlet);
                }
            })
        );
    }

    /**
     * Opens the new editor on a contentlet: in the side panel when its flag is on, otherwise in a
     * centered dialog. A save reloads the page.
     *
     * @param contentlet the contentlet to edit
     */
    #openNewEditor(contentlet: DotCMSContentlet): void {
        const header = contentlet.title ?? '';
        const data: EditContentDialogData = {
            mode: 'edit',
            contentletInode: contentlet.inode,
            onContentSaved: () => {
                this.uveStore.pageReload();
            }
        };

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
