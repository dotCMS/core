import { signalMethod } from '@ngrx/signals';
import { injectDispatch } from '@ngrx/signals/events';
import { Observable, fromEvent, of } from 'rxjs';

import {
    AfterViewInit,
    Component,
    DestroyRef,
    ElementRef,
    OnDestroy,
    ViewChild,
    computed,
    effect,
    inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ProgressBarModule } from 'primeng/progressbar';

import { filter, switchMap, tap } from 'rxjs/operators';

import {
    DotCopyContentService,
    DotMessageService,
    DotTempFileUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSTempFile, DotTreeNode } from '@dotcms/dotcms-models';
import { DotCMSUVEAction, UVE_MODE } from '@dotcms/types';
import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { DotCopyContentModalService } from '@dotcms/ui';
import { WINDOW, isEqual } from '@dotcms/utils';

import { DotUveActionsHandlerService } from '../../../services/dot-uve-actions-handler/dot-uve-actions-handler.service';
import { DotUveDragDropService } from '../../../services/dot-uve-drag-drop/dot-uve-drag-drop.service';
import { UveIframeMessengerService } from '../../../services/iframe-messenger/uve-iframe-messenger.service';
import { InlineEditService } from '../../../services/inline-edit/inline-edit.service';
import { notifyContainerInsertRefused } from '../../../shared/container-insert';
import {
    canEditOwningContentlet,
    notifyNoEditPermission
} from '../../../shared/contentlet-permission';
import { EDITOR_STATE, UVE_STATUS } from '../../../shared/enums';
import {
    ActionPayload,
    ClientData,
    ContainerPayload,
    DeletePayload,
    InsertPayloadFromDelete,
    PageContainer,
    PositionPayload,
    PostMessage,
    SelectedContentlet
} from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { ReloadEditorContent } from '../../../store/features/editor/models';
import { WithPageApiMethods } from '../../../store/features/page-api/withPageApi';
import { IframeAccessMode, PageType } from '../../../store/models';
import {
    TEMPORAL_DRAG_ITEM,
    deleteContentletFromContainer,
    getPageOwnedParamKeys,
    isAssetPath,
    isNavigableHref,
    isPageSiteHost,
    isSamePageNavigation,
    isSelectedContentletGone,
    resolveClickedAnchor,
    scrollIframeToFragment
} from '../../../utils';
import { DotUveCanvasSizingDirective } from '../../directives/dot-uve-canvas-sizing/dot-uve-canvas-sizing.directive';
import { uveContentEditorEvents } from '../dot-uve-content-editor/dot-uve-content-editor.events';
import { DotUveContentletToolsComponent } from '../dot-uve-contentlet-tools/dot-uve-contentlet-tools.component';
import { DotUveIframeComponent } from '../dot-uve-iframe/dot-uve-iframe.component';
import { DotUveIframeResizeHandlesComponent } from '../dot-uve-iframe-resize-handles/dot-uve-iframe-resize-handles.component';
import { DotUveLockOverlayComponent } from '../dot-uve-lock-overlay/dot-uve-lock-overlay.component';
import { DotUvePageVersionNotFoundComponent } from '../dot-uve-page-version-not-found/dot-uve-page-version-not-found.component';
import { EmaPageDropzoneComponent } from '../ema-page-dropzone/ema-page-dropzone.component';
import { EmaDragItem } from '../ema-page-dropzone/types';

/**
 * The canvas: the page in its iframe and everything drawn over it (contentlet tools, dropzone,
 * lock overlay, page-not-found state and resize handles). It owns both directions of the talk
 * with the page: it hands the page's messages to the actions handler and posts to the page, and
 * it handles drops, deletes, link clicks and inline-editing clicks. It reads and writes the store
 * and asks the content editor for editors; it takes no inputs and sends no outputs.
 */
@Component({
    selector: 'dot-uve-canvas',
    templateUrl: './dot-uve-canvas.component.html',
    imports: [
        ProgressBarModule,
        DotUveCanvasSizingDirective,
        DotUveIframeComponent,
        DotUveIframeResizeHandlesComponent,
        DotUvePageVersionNotFoundComponent,
        DotUveContentletToolsComponent,
        EmaPageDropzoneComponent,
        DotUveLockOverlayComponent
    ],
    providers: [
        DotCopyContentModalService,
        DotTempFileUploadService,
        DotUveActionsHandlerService,
        DotUveDragDropService
    ],
    // `contents` keeps the progress bar and the viewport in the editor's content grid.
    host: { class: 'contents' }
})
export class DotUveCanvasComponent implements AfterViewInit, OnDestroy {
    @ViewChild('iframe') iframeComponent!: DotUveIframeComponent;
    @ViewChild('customDragImage') customDragImage!: ElementRef<HTMLDivElement>;

    get iframe(): ElementRef<HTMLIFrameElement> | undefined {
        return this.iframeComponent?.iframe;
    }

    /**
     * The UVE store. `withPageApi`'s methods (`pageLoad`, `editorSave`…) reach the store's type
     * only through an index signature, as plain `Function`, so they are typed through that
     * feature's own interface.
     */
    protected readonly uveStore = inject(UVEStore) as InstanceType<typeof UVEStore> &
        WithPageApiMethods;

    readonly #dotMessageService = inject(DotMessageService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(MessageService);
    readonly #window = inject(WINDOW);
    readonly #dotCopyContentService = inject(DotCopyContentService);
    readonly #tempFileUploadService = inject(DotTempFileUploadService);
    readonly #dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    readonly #inlineEditingService = inject(InlineEditService);
    readonly #destroyRef = inject(DestroyRef);
    readonly #actionsHandler = inject(DotUveActionsHandlerService);
    readonly #dragDropService = inject(DotUveDragDropService);
    readonly #iframeMessenger = inject(UveIframeMessengerService);
    readonly #contentEditor = injectDispatch(uveContentEditorEvents);

    protected readonly host = '*';
    protected readonly UVE_STATUS = UVE_STATUS;

    protected readonly $dropzone = computed(() => {
        const canEditPage = this.uveStore.editorCanEditContent();
        const state = this.uveStore.editorState();
        const bounds = this.uveStore.editorBounds();
        const dragItem = this.uveStore.editorDragItem();

        const showDropzone = canEditPage && state === EDITOR_STATE.DRAGGING;

        return showDropzone
            ? {
                  bounds,
                  dragItem
              }
            : null;
    });

    protected readonly $showLockOverlay = computed(() => {
        const mode = this.uveStore.viewMode();
        if (mode !== UVE_MODE.EDIT) {
            return false;
        }

        const lockOptions = this.uveStore.$lockOptions();

        const lockFeatureEnabled = this.uveStore.$lockFeatureEnabled();
        const isLocked = lockOptions?.isLocked;
        const isLockedByCurrentUser = lockOptions?.isLockedByCurrentUser;
        const canLock = lockOptions?.canLock;

        // For feature flag, we force the user to lock pages to edit, so the
        // overlay stays up unless the current user is the one holding the lock —
        // a page locked by someone else must be unlocked and re-locked by this
        // user first, not just "any" lock.
        if (lockFeatureEnabled) {
            return !isLockedByCurrentUser;
        }

        // Without feature flag, we show the lock overlay if the page is locked
        // And is not locked by the current user or the user has no permission to lock/unlock
        if (isLocked && (!isLockedByCurrentUser || !canLock)) {
            return true;
        }

        return false;
    });

    protected readonly $iframeSrc = computed((): string => {
        const url = this.uveStore.$iframeURL();
        return (typeof url === 'string' ? url : '') || '';
    });
    protected readonly $iframePointerEvents = computed((): string => {
        // Block iframe pointer events while a drag is in flight (with or
        // without iframe scroll) so the dropzone reads drag events instead
        // of the page underneath.
        const state = this.uveStore.editorState();
        const dragIsActive = state === EDITOR_STATE.DRAGGING || state === EDITOR_STATE.SCROLL_DRAG;
        return dragIsActive ? 'none' : 'auto';
    });
    protected readonly $iframeOpacity = computed((): number => {
        return this.uveStore.$editorIsIframeLoading() ? 0.5 : 1;
    });

    get contentWindow(): Window | null {
        return this.iframeComponent?.contentWindow || null;
    }

    /**
     * We should not depend on this `$reloadEditorContent` computed to `resetEditorProperties` or `resetDialog`
     * This depends on the `code` with each the page renders code. This reset should be done in `widthLoad` signal feature but we can't do it yet
     */
    readonly #handleReloadContent = signalMethod<ReloadEditorContent>(({ pageType }) => {
        const isClientReady = this.uveStore.isClientReady();
        const isGraphQLSourced = this.uveStore.pageAsset()?.source === 'graphql';

        this.uveStore.resetEditorProperties();

        if (pageType === PageType.TRADITIONAL || !isClientReady) {
            return;
        }

        // Headless pages are driven entirely by the client's own GraphQL query. Never push a
        // REST-sourced pageAsset into the iframe — it never carries the relationships that
        // query defines. Provenance is tracked explicitly on the stored asset (`source`)
        // rather than inferred from whether a query was merely registered
        // (`requestMetadata`): a registered-but-unresolved or aborted/failed GraphQL fetch
        // leaves a stale REST asset in place while `requestMetadata` stays set, which the old
        // `hasClientQuery` check could not distinguish from the resolved case (dotCMS/core#37097).
        // Tell the client to reload itself from its own GraphQL source instead of pushing
        // anything REST-shaped — but only ONCE per non-resolved streak. `$reloadEditorContent`
        // re-emits on every CLIENT_READY re-announcement (setCustomClient unconditionally
        // re-patches requestMetadata, which changes pageAsset()'s reference), including the one
        // the reload we're about to send will itself trigger. Without this guard, a GraphQL
        // fetch that stays unresolved (slow, aborted, or permanently failing) causes an
        // uncapped reload → CLIENT_READY → reload loop, driving the client's iframe into
        // continuous full navigations (regression found in #37097's post-merge QA).
        if (pageType === PageType.HEADLESS && !isGraphQLSourced) {
            const alreadyRequested = this.uveStore.pageReloadPending();

            if (!alreadyRequested) {
                this.uveStore.setPageReloadPending(true);
                this.#sendMessageToIframe(
                    {
                        name: __DOTCMS_UVE_EVENT__.UVE_RELOAD_PAGE
                    },
                    this.host
                );
            }

            return;
        }

        this.#reloadIframeContent();
    });

    /**
     * Notify the iframe whenever the editor clears its contentlet selection
     * (canvas resize, scroll, device switch, etc.). The SDK keeps a
     * `lastSelectedInode` tracker that gates click→passthrough behavior; if
     * the editor drops the selection without telling the SDK, a follow-up
     * click on the same contentlet would be silently treated as a passthrough
     * (page click) and the toolbar wouldn't reappear.
     */
    #lastSelectedAreaWasSet = false;
    readonly #notifySelectionCleared = signalMethod<SelectedContentlet | null>((selected) => {
        const hasSelection = !!selected;

        if (this.#lastSelectedAreaWasSet && !hasSelection) {
            this.#iframeMessenger.selectionCleared();
        }
        this.#lastSelectedAreaWasSet = hasSelection;
    });

    readonly $handleIsDraggingEffect = effect(() => {
        const isDragging = this.uveStore.$editorIsInDraggingState();

        if (!isDragging) {
            return;
        }

        // Drag needs bounds NOW so the dropzone can compute targets
        // before the user moves another pixel — bypass the auto-bounds
        // debounce.
        this.#iframeMessenger.flushBounds();
    });

    // Reflow re-anchoring (sidebar open/close, scroll-end, device switch,
    // zoom change, image/font load shifts, media-query reflows) flows
    // through the SDK's debounced ResizeObserver in onAutoBounds, which
    // emits SET_BOUNDS automatically when the iframe layout settles. The
    // editor only needs to flip editorState back to IDLE so the overlay
    // un-hides; SET_BOUNDS lands ~100ms later with fresh coords.

    // Device-switch / zoom-change recovery is handled by the SET_BOUNDS
    // handler in DotUveActionsHandlerService: when the SDK's auto-bounds
    // channel pushes fresh bounds after the layout settles, the handler
    // flips editorState back to IDLE. Flipping IDLE earlier (on rAF after
    // the trigger) used to race the bounds round-trip and caused the
    // selected overlay to flash at stale coordinates before snapping to
    // the new ones.

    constructor() {
        this.#handleReloadContent(this.uveStore.$reloadEditorContent);
        this.#notifySelectionCleared(this.uveStore.editorSelected);
    }

    ngAfterViewInit(): void {
        if (!this.iframe) {
            return;
        }

        fromEvent<MessageEvent>(this.#window, 'message')
            .pipe(
                takeUntilDestroyed(this.#destroyRef),
                filter((event) => this.#isMessageFromUvePreviewFrame(event))
            )
            .subscribe((event) => {
                const data = event.data;
                if (this.#isUvePostMessage(data)) {
                    this.#handleUveMessage(data);
                }
            });

        this.#setupDragDrop();
    }

    ngOnDestroy(): void {
        if (this.uveStore.pageType() === PageType.TRADITIONAL) {
            this.uveStore.setIsClientReady(true);
        }
    }

    #setupDragDrop(): void {
        if (!this.iframe) {
            return;
        }

        this.#dragDropService.setupDragEvents(this.uveStore, this.iframe, this.customDragImage, {
            onDrop: (event) => this.#handleDrop(event),
            onDragEnter: () => {
                // Handled in dragDropService
            },
            onDragOver: () => {
                // Handled in dragDropService
            },
            onDragLeave: () => {
                this.uveStore.resetEditorProperties();
            },
            onDragEnd: () => {
                this.uveStore.resetEditorProperties();
            },
            onDragStart: () => {
                // Handled in dragDropService
            }
        });
    }

    #handleUveMessage(message: PostMessage): void {
        if (
            !this.#isUvePostMessage(message) ||
            (message.action === DotCMSUVEAction.IFRAME_HEIGHT &&
                this.uveStore.iframeAccessMode() === IframeAccessMode.LOCAL)
        ) {
            return;
        }

        this.#actionsHandler.handleAction(message, {
            uveStore: this.uveStore,
            inlineEditingService: this.#inlineEditingService,
            contentWindow: this.contentWindow,
            host: this.host,
            onCopyContent: (currentTreeNode) => this.#handleCopyContent(currentTreeNode)
        });
    }

    #handleDrop(event: DragEvent): void {
        event.preventDefault();
        const target = event.target as HTMLDivElement;
        const { position, payload, dropzone } = target.dataset;

        if (dropzone !== 'true') {
            this.uveStore.resetEditorProperties();
            return;
        }

        const data: ClientData = JSON.parse(payload || '{}');
        const file = event.dataTransfer?.files[0];
        const dragItem = this.uveStore.editorDragItem();

        if (file) {
            this.#handleFileUpload({
                file,
                data,
                position,
                // A file dragged in from outside the page carries the temporary drag item the
                // drag-drop service sets on dragenter.
                dragItem: dragItem as EmaDragItem
            });
            return;
        }

        if (!isEqual(dragItem, TEMPORAL_DRAG_ITEM) && dragItem) {
            const positionPayload = <PositionPayload>{
                position,
                ...data
            };

            this.#placeItem(positionPayload, dragItem);
            return;
        }

        this.uveStore.resetEditorProperties();
    }

    /**
     * Handles internal navigation by preventing the default behavior of the click event,
     * updating the query parameters, and opening external links in a new tab.
     *
     * @param e - The MouseEvent object representing the click event.
     */
    protected handleInternalNav(e: MouseEvent) {
        const target = e.target as HTMLElement;
        const anchor = resolveClickedAnchor(target);
        const rawHref = anchor?.getAttribute('href') ?? '';

        // Same-page anchors (#section) are browser-handled scrolls, and
        // `javascript:…` hrefs never navigate at all — both are a page's own
        // link-shaped UI (toggles, accordions), not a link to follow. Bail
        // before any URL parsing: for traditional VTL pages the iframe sits
        // at about:blank, so anchor.href would resolve `#section` to
        // "about:blank#section" (hostname "", incorrectly tripping the
        // external-link branch below) and `javascript:…` to itself (hostname
        // also "", same false trip — previously opened as an "external"
        // link in a new tab, dotCMS/core#37961's second QA regression).
        if (!isNavigableHref(rawHref)) {
            return;
        }

        const href = anchor?.href || rawHref;
        const isInlineEditing = this.uveStore.editorState() === EDITOR_STATE.INLINE_EDITING;

        // If the link is not valid or we are in inline editing mode, we do nothing
        if (!href || isInlineEditing) {
            return;
        }

        const url = new URL(href, this.#window.location.origin);
        // Get the query parameters from the URL
        const urlQueryParams: Record<string, string | undefined> = Object.fromEntries(
            url.searchParams.entries()
        );

        if (
            !isPageSiteHost(
                url.hostname,
                this.uveStore.pageAsset()?.site,
                this.#window.location.hostname
            )
        ) {
            // Cancel first: without it the anchor also navigates the iframe to the
            // external site, which usually refuses to be framed and blanks the canvas.
            e.preventDefault();
            this.#openInNewTab(url.href);

            return;
        }

        // Files (PDFs, images, docs…) are not pages: the Page API cannot resolve
        // them and the editor would show "Page not found". Open them in a new tab
        // so the author can verify the link without leaving the editor.
        if (isAssetPath(url.pathname)) {
            // Cancel before opening, so the page under edit stays put whatever the
            // open does. `url` is the origin-resolved form of `href`, which can
            // still be a raw relative attribute when the click lands on a child of
            // the anchor.
            e.preventDefault();
            this.#openInNewTab(url.href);

            return;
        }

        // A same-page hash with no query is an in-page anchor. Scroll the iframe
        // ourselves: left to the browser, the link would load outside the editor
        // instead of scrolling.
        const isSamePageAnchor =
            !!url.hash &&
            !url.search &&
            isSamePageNavigation(href, this.uveStore.pageParams()?.url ?? '');

        if (isSamePageAnchor) {
            e.preventDefault();
            scrollIframeToFragment(this.contentWindow, url.hash);

            return;
        }

        // Anything else loads through the Page API below. Left to the browser, the
        // iframe of a traditional page navigates on its own and comes back blank,
        // and the new query never reaches the page render (#36999, #37327).
        //
        // The previous link's query is cleared first, so the link's own query is
        // the whole query of the next page instead of inheriting the old one through
        // the `pageLoad` merge. That holds for any link, not only same-path ones:
        // `/folder/` and `/folder/index` are the same page under two paths, and a
        // filter must not follow the editor to another page either.
        for (const key of getPageOwnedParamKeys(this.uveStore.pageParams())) {
            if (!(key in urlQueryParams)) {
                urlQueryParams[key] = undefined;
            }
        }

        this.uveStore.pageLoad({ url: url.pathname, ...urlQueryParams });
        e.preventDefault();
    }

    /**
     * Opens a URL in a new tab with the opener severed.
     *
     * Deliberately not `window.open(url, '_blank', 'noopener')`. A windowFeatures
     * string makes Firefox classify the call as a popup request, and the iframe
     * raising the gesture is sandboxed without `allow-popups`, so Firefox throws
     * "DOMException: The operation is insecure". A `rel="noopener"` anchor is an
     * ordinary tab navigation, which the sandbox permits, and carries the same
     * opener guarantee, including for cross-origin targets where assigning
     * `opener = null` on the returned window would not be allowed.
     *
     * @param {string} href - Absolute URL to open
     * @memberof DotUveCanvasComponent
     */
    #openInNewTab(href: string): void {
        let link: HTMLAnchorElement | null = null;

        try {
            const doc = this.#window.document;

            link = doc.createElement('a');
            link.href = href;
            link.target = '_blank';
            link.rel = 'noopener';

            doc.body.appendChild(link);
            link.click();
        } catch {
            // Swallow. This runs inside the RxJS subscriber that feeds the iframe
            // click handler, so an escaping throw would complete the subscription
            // and kill link handling for the rest of the session.
        } finally {
            // `click()` is the call that throws when the open is refused, so
            // cleanup has to be unconditional or every failed attempt strands an
            // anchor in the admin document.
            link?.remove();
        }
    }

    /**
     * Handles the inline editing functionality triggered by a mouse event.
     * @param e - The mouse event that triggered the inline editing.
     */
    protected handleInlineEditing(e: MouseEvent): void {
        const target = e.target as HTMLElement;
        const element = target.dataset?.['mode']
            ? target
            : target.closest<HTMLElement>('[data-mode]');

        if (!element?.dataset['mode']) {
            return;
        }

        // Inline editing writes contentlet fields, so it is gated like the
        // pencil and Quick Edit. Unlike them there is no control to disable, so
        // the refusal has to be announced — a click that does nothing reads as
        // a broken editor.
        if (!canEditOwningContentlet(element)) {
            notifyNoEditPermission(this.#messageService, this.#dotMessageService);

            return;
        }

        this.#inlineEditingService.handleInlineEdit({
            ...element.dataset
        } as unknown as { language: string; mode: string; inode: string; fieldName: string });
    }

    protected onIframePageLoad(): void {
        this.#iframeMessenger.setIframeWindow(this.iframe?.nativeElement.contentWindow ?? null);

        if (this.uveStore.editorState() === EDITOR_STATE.INLINE_EDITING) {
            this.#inlineEditingService.initEditor();
        }
    }

    /**
     * Check if the active contentlet still exists in the page containers
     * and reset it if it was removed
     *
     * @param {PageContainer[]} pageContainers
     * @memberof DotUveCanvasComponent
     */
    #checkAndResetActiveContentlet(pageContainers: PageContainer[]): void {
        if (isSelectedContentletGone(this.uveStore.editorSelected(), pageContainers)) {
            this.uveStore.resetSelected();
        }
    }

    /**
     * When the user drop a palette item in the dropzone
     *
     * @param {PositionPayload} positionPayload
     * @return {*}  {void}
     * @memberof DotUveCanvasComponent
     */
    #placeItem(positionPayload: PositionPayload, dragItem: EmaDragItem): void {
        let payload = this.uveStore.getPageSavePayload(positionPayload);

        const destinationContainer = payload.container;
        const pivotContentlet = payload.contentlet;
        const positionToInsert = positionPayload.position;

        if (dragItem.draggedPayload.type === 'contentlet') {
            const draggedPayload = dragItem.draggedPayload;
            const originContainer = draggedPayload.item.container;
            const contentletToMove = draggedPayload.item.contentlet;

            if (draggedPayload.move) {
                const deletePayload = this.#createDeletePayload({
                    payload,
                    // A move starts from the container the contentlet sits in, which only a
                    // palette contentlet lacks.
                    originContainer: originContainer as ContainerPayload,
                    contentletToMove
                });

                const { pageContainers, contentletsId } =
                    deleteContentletFromContainer(deletePayload); // Delete from the original position

                // Update the payload to handle the data to insert the contentlet in the new position
                payload = this.#createInsertPayloadFromDelete({
                    payload,
                    pageContainers,
                    contentletsId,
                    destinationContainer,
                    pivotContentlet,
                    positionToInsert
                });
            }

            this.#insertDropped(payload, draggedPayload.item.contentlet.identifier);

            return;
        } else if (dragItem.draggedPayload.type === 'content-type') {
            this.uveStore.resetEditorProperties(); // In case the user cancels the creation of the contentlet, we already have the editor in idle state

            const { variable, name } = dragItem.draggedPayload.item;

            this.#contentEditor.uveCreateContentRequested({
                contentType: { variable, name },
                into: payload
            });
        } else if (dragItem.draggedPayload.type === 'temp' && payload.newContentlet) {
            // A temp drop is a file uploaded on drop, which always carries the new contentlet.
            this.#insertDropped(payload, payload.newContentlet.identifier);
        }
    }

    /**
     * Inserts a dropped contentlet into its container position through the store, and tells the
     * user why when the container refuses it.
     *
     * @param into the drop position
     * @param contentletId the dropped contentlet
     */
    #insertDropped(into: ActionPayload, contentletId: string): void {
        const result = this.uveStore.editorInsertContentlet(into, contentletId);

        // `=== false`: without strictNullChecks, as this library compiles, `!` does not narrow the
        // result to a refusal.
        if (result.didInsert === false) {
            notifyContainerInsertRefused(result, this.#messageService, this.#dotMessageService);
        }
    }

    /**
     * Delete contentlet
     *
     * @param {ActionPayload} payload
     * @memberof DotUveCanvasComponent
     */
    protected deleteContent(payload: ActionPayload) {
        const { pageContainers } = deleteContentletFromContainer(payload);

        this.#confirmationService.confirm({
            header: this.#dotMessageService.get(
                'editpage.content.contentlet.remove.confirmation_message.header'
            ),
            message: this.#dotMessageService.get(
                'editpage.content.contentlet.remove.confirmation_message.message'
            ),
            acceptLabel: this.#dotMessageService.get('dot.common.dialog.accept'),
            rejectLabel: this.#dotMessageService.get('dot.common.dialog.reject'),
            accept: () => {
                // Check if active contentlet was removed during delete operation
                this.#checkAndResetActiveContentlet(pageContainers);

                this.uveStore.editorSave(pageContainers);
            }
        });
    }

    /**
     * Notify the user to reload the iframe
     *
     * @memberof DotUveCanvasComponent
     */
    #reloadIframeContent(): void {
        this.#sendMessageToIframe(
            {
                name: __DOTCMS_UVE_EVENT__.UVE_SET_PAGE_DATA,
                payload: this.#clientPayload()
            },
            this.host
        );
    }

    #sendMessageToIframe(message: unknown, host = '*'): void {
        this.iframe?.nativeElement.contentWindow?.postMessage(message, host);
    }

    /**
     * Only handle postMessage events sent from the UVE preview iframe (or a nested frame
     * inside it). Ignores same-window messages (e.g. dot-html-to-image thumbnail iframes).
     */
    #isMessageFromUvePreviewFrame(event: MessageEvent): boolean {
        const previewWindow = this.iframe?.nativeElement?.contentWindow;
        const source = event.source as Window;
        if (!previewWindow || !source) {
            return false;
        }
        return source === previewWindow || source === previewWindow.parent;
    }

    #isUvePostMessage(data: unknown): data is PostMessage {
        return !!data && typeof data === 'object' && 'action' in data;
    }

    #handleCopyContent(treeNode: DotTreeNode): Observable<DotCMSContentlet> {
        return this.#dotCopyContentService.copyInPage(treeNode);
    }

    /**
     * Create the payload to delete a contentlet
     *
     * @param {DeletePayload} {
     *         payload,
     *         originContainer,
     *         contentletToMove
     *     }
     * @return {*}  {ActionPayload}
     * @memberof DotUveCanvasComponent
     */
    #createDeletePayload({
        payload,
        originContainer,
        contentletToMove
    }: DeletePayload): ActionPayload {
        return {
            ...payload,
            container: {
                ...originContainer // The container where the contentlet was before
            },
            contentlet: {
                ...contentletToMove // The contentlet that was dragged
            }
        };
    }

    /**
     * Create the payload to insert a contentlet after deleting it
     *
     * @param {InsertPayloadFromDelete} {
     *         payload,
     *         pageContainers,
     *         contentletsId,
     *         destinationContainer,
     *         pivotContentlet,
     *         positionToInsert
     *     }
     * @return {*}  {ActionPayload}
     * @memberof DotUveCanvasComponent
     */
    #createInsertPayloadFromDelete({
        payload,
        pageContainers,
        contentletsId,
        destinationContainer,
        pivotContentlet,
        positionToInsert
    }: InsertPayloadFromDelete): ActionPayload {
        return {
            ...payload,
            pageContainers,
            container: {
                ...payload.container,
                ...destinationContainer,
                contentletsId // Contentlets id after deleting the contentlet
            },
            contentlet: pivotContentlet,
            position: positionToInsert
        };
    }

    /**
     * Handle the file upload
     *
     * @param {{
     *         data: ClientData;
     *         position?: string;
     *         file: File;
     *         dragItem: EmaDragItem;
     *     }} {
     *         data,
     *         position,
     *         file,
     *         dragItem
     *     }
     * @return {*}
     * @memberof DotUveCanvasComponent
     */
    #handleFileUpload({
        data,
        position,
        file,
        dragItem
    }: {
        data: ClientData;
        position?: string;
        file: File;
        dragItem: EmaDragItem;
    }): void {
        this.uveStore.resetEditorProperties();

        if (!/image.*/.exec(file.type)) {
            this.#messageService.add({
                severity: 'error',
                summary: this.#dotMessageService.get('file-upload'),
                detail: this.#dotMessageService.get('editpage.file.upload.not.image'),
                life: 3000
            });

            return;
        }

        this.#tempFileUploadService
            .upload(file)
            .pipe(
                tap(() => {
                    this.#messageService.add({
                        severity: 'info',
                        summary: this.#dotMessageService.get('upload-image'),
                        detail: this.#dotMessageService.get('editpage.file.uploading', file.name),
                        life: 3000
                    });
                }),
                switchMap((response: DotCMSTempFile[] | string) => {
                    // A failed upload answers with its HTTP status instead, which has no image.
                    const [{ id, image }] = response as DotCMSTempFile[];

                    if (!image) {
                        this.#messageService.add({
                            severity: 'error',
                            summary: this.#dotMessageService.get('upload-image'),
                            detail: this.#dotMessageService.get('editpage.file.upload.error'),
                            life: 3000
                        });

                        return of(undefined);
                    }

                    return this.#dotWorkflowActionsFireService
                        .publishContentletAndWaitForIndex<DotCMSContentlet>('dotAsset', {
                            asset: id
                        })
                        .pipe(
                            tap(() => {
                                this.#messageService.add({
                                    severity: 'info',
                                    summary: this.#dotMessageService.get('Workflow-Action'),
                                    detail: this.#dotMessageService.get(
                                        'editpage.file.publishing',
                                        file.name
                                    ),
                                    life: 3000
                                });
                            })
                        );
                })
            )
            .subscribe((contentlet) => {
                if (!contentlet) {
                    this.uveStore.resetEditorProperties();

                    return;
                }

                const payload = {
                    ...data,
                    position,
                    newContentlet: {
                        identifier: contentlet.identifier,
                        inode: contentlet.inode,
                        title: contentlet.title,
                        contentType: contentlet.contentType,
                        baseType: contentlet.baseType
                    }
                } as ActionPayload;

                this.#placeItem(payload, dragItem);
            });
    }

    #clientPayload() {
        const clientResponse = this.uveStore.pageAsset()?.clientResponse;

        if (clientResponse) {
            return clientResponse;
        }

        return {
            // Removed pageAPIResponse spread
            params: this.uveStore.pageParams()
        };
    }
}
