import { Observable, of } from 'rxjs';

import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import {
    Injectable,
    Injector,
    Signal,
    Type,
    computed,
    inject,
    isSignal,
    signal,
    untracked
} from '@angular/core';

import { map, switchMap, takeUntil } from 'rxjs/operators';

import {
    DotActionUrlService,
    DotContentEditor,
    DotContentletEditUrlService,
    DotContentletService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { DotCMSContentlet, DotCMSContentType } from '@dotcms/dotcms-models';
import { DotSidePanelRef, DotSidePanelService } from '@dotcms/ui';

import { loadDotEditContentSidePanel } from '../components/dot-edit-content-side-panel/load-dot-edit-content-side-panel';
import { loadDotLegacyEditorSidePanel } from '../components/dot-legacy-editor-side-panel/load-dot-legacy-editor-side-panel';
import { DotEditContentPanelHandlers } from '../models/dot-edit-content-panel-events.model';
import { DotEditContentRequest } from '../models/dot-edit-content-request.model';
import { DotOpenContentEvent } from '../models/dot-open-content-event.model';

/** How to open a panel: the opener's injector, and handlers for any of the panel events. */
export interface DotEditContentOpenOptions extends DotEditContentPanelHandlers {
    /**
     * The opener's injector. The panel resolves its dependencies through it (MessageService for
     * toasts, route-level providers) and closes when it is destroyed. Required.
     */
    injector: Injector;
}

/** What the server said about a request before any editor shows. */
interface DotResolvedRequest {
    editor: DotContentEditor;
    /** The content type the editor decision was read from; `null` when the lookup failed. */
    contentType: DotCMSContentType | null;
    /** The contentlet fetched for an edit that named no content type. */
    contentlet: DotCMSContentlet | null;
    /** The create screen for a legacy create. */
    createUrl: string | null;
}

/** The panel events a surface can handle, by the output name both panels use for each. */
const HANDLED_OUTPUTS = ['saved', 'languageChanged', 'pageEditorRequested', 'deleted'] as const;

/**
 * Opens content in the side panel, in whichever editor its content type uses (#38303). Every
 * surface that edits or creates content goes through here, with one request shape, and hears back
 * through the same five events whichever editor showed. Surfaces that keep a panel open for as long
 * as a request is set use {@link injectEditContentPanel} on top of it.
 */
@Injectable({ providedIn: 'root' })
export class DotEditContentOpenerService {
    readonly #sidePanels = inject(DotSidePanelService);
    readonly #editUrl = inject(DotContentletEditUrlService);
    readonly #actionUrl = inject(DotActionUrlService);
    readonly #contentlets = inject(DotContentletService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);

    /**
     * Opens the side panel for a request. Everything server-side is resolved first (the content type
     * of an edit that named none, the editor, the create screen of a legacy create), while the
     * panel's spinner shows; a refused request shows the standard error and no editor. An invalid
     * request opens nothing.
     *
     * Pass a signal to keep the heading and the panel's request following it while the panel stays
     * open; the editor itself is chosen once, from the request as it is now.
     *
     * @param request What to open, or a signal of it.
     * @param options The opener's injector and the events it handles.
     * @returns The panel, so the caller can route browser Back through `requestClose()`. Already
     * closed for an invalid request.
     */
    open(
        request: DotEditContentRequest | Signal<DotEditContentRequest>,
        options: DotEditContentOpenOptions
    ): DotSidePanelRef {
        const $source = isSignal(request) ? request : signal(request).asReadonly();
        const initial = untracked($source);

        if (!isOpenable(initial)) {
            return this.#refuse(options);
        }

        const $contentlet = signal<DotCMSContentlet | null>(null);
        const $typeName = signal<string | null>(null);
        const $request = computed(() => withContentlet($source(), $contentlet()));

        // Read by the outlet only once the content has loaded, so the inputs a given editor takes
        // are added below, before its component is handed over.
        const inputs: Record<string, unknown> = { request: $request };

        let showEditor: (component: Promise<Type<unknown>>) => void = () => undefined;
        const component = new Promise<Type<unknown>>((resolve) => (showEditor = resolve));

        const ref = this.#sidePanels.open(component, {
            heading: computed(
                () => $request().title || $typeName() || $request().contentType || ''
            ),
            inputs,
            outputs: this.#outputs(options),
            injector: options.injector
        });

        ref.closed.subscribe(() => options.closed?.());

        this.#resolve(initial)
            .pipe(takeUntil(ref.closed))
            .subscribe({
                next: ({ editor, contentType, contentlet, createUrl }) => {
                    $contentlet.set(contentlet);
                    $typeName.set(contentType?.name ?? null);

                    if (editor === 'new') {
                        showEditor(loadDotEditContentSidePanel());

                        return;
                    }

                    inputs['createUrl'] = signal(createUrl).asReadonly();
                    showEditor(loadDotLegacyEditorSidePanel());
                },
                // The component never resolves: the outlet would skip it on a closed panel anyway.
                error: (error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);
                    ref.close();
                }
            });

        return ref;
    }

    /**
     * Turns down a request that names nothing to open: the standard error, and a panel that is
     * already closed, so the caller's `closed` still fires once.
     *
     * @param options The caller's handlers.
     * @returns A closed panel ref.
     */
    #refuse(options: DotEditContentOpenOptions): DotSidePanelRef {
        this.#httpErrorManager.handle(new HttpErrorResponse({ status: HttpStatusCode.BadRequest }));

        const ref = new DotSidePanelRef(() => undefined);
        ref.closed.subscribe(() => options.closed?.());
        ref.close();

        return ref;
    }

    /**
     * Asks the server what the request needs before an editor shows: the contentlet, when an edit
     * names no content type; which editor the type uses; and the create screen for a legacy create.
     *
     * @param request A request that passed {@link isOpenable}.
     * @returns The answers, once; errors when a fetch or the create screen is refused.
     */
    #resolve(request: DotEditContentRequest): Observable<DotResolvedRequest> {
        const contentlet$: Observable<DotCMSContentlet | null> = request.contentType
            ? of(null)
            : this.#contentlets.getContentletByInode(request.inode as string);

        return contentlet$.pipe(
            switchMap((contentlet) => {
                const variable = (request.contentType ?? contentlet?.contentType) as string;

                return this.#editUrl.resolveEditor(variable).pipe(
                    switchMap(({ editor, contentType }) => {
                        const createUrl$ =
                            editor === 'legacy' && request.mode === 'new'
                                ? this.#actionUrl.getCreateContentletUrl(
                                      variable,
                                      request.languageId
                                  )
                                : of(null);

                        return createUrl$.pipe(
                            map((createUrl) => ({ editor, contentType, contentlet, createUrl }))
                        );
                    })
                );
            })
        );
    }

    /**
     * The panel's output handlers: the caller's, by the output name of each event, and
     * `openContent`, which stacks a panel for the content a panel asked for, with the same opener
     * injector, routing that panel's saves to the asker's reply. `closed` is not an output here: it
     * is taken from the panel ref, so it fires even when no editor loaded.
     *
     * @param options The caller's injector and handlers.
     * @returns The handlers, by output name.
     */
    #outputs(options: DotEditContentOpenOptions): Record<string, (value: never) => void> {
        const outputs: Record<string, (value: never) => void> = {
            openContent: (event: DotOpenContentEvent) =>
                this.open(event.request, { injector: options.injector, saved: event.onSaved })
        };

        for (const name of HANDLED_OUTPUTS) {
            const handler = options[name];

            if (handler) {
                outputs[name] = handler;
            }
        }

        return outputs;
    }
}

/**
 * Whether a request names something to open: an inode to edit, or a content type to create.
 *
 * @param request The request.
 */
function isOpenable(request: DotEditContentRequest): boolean {
    return request.mode === 'edit' ? !!request.inode : !!request.contentType;
}

/**
 * Fills what an edit request left out from the contentlet fetched for it, keeping everything the
 * request did say.
 *
 * @param request The request.
 * @param contentlet The contentlet fetched for it, if one was.
 * @returns The request, completed.
 */
function withContentlet(
    request: DotEditContentRequest,
    contentlet: DotCMSContentlet | null
): DotEditContentRequest {
    if (!contentlet) {
        return request;
    }

    return {
        ...request,
        contentType: request.contentType ?? contentlet.contentType,
        identifier: request.identifier ?? contentlet.identifier,
        title: request.title ?? contentlet.title,
        languageId: request.languageId ?? contentlet.languageId
    };
}
