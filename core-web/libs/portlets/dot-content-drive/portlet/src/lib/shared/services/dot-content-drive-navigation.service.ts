import { EMPTY, Observable, of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, Provider, inject, signal } from '@angular/core';

import { catchError, map, switchMap, take } from 'rxjs/operators';

import {
    DotContentSearchService,
    DotHttpErrorManagerService,
    DotRouterService
} from '@dotcms/data-access';
import { DotCMSBaseTypesContentTypes, DotCMSContentlet } from '@dotcms/dotcms-models';
import {
    DotEditContentRequest,
    EDIT_CONTENT_NAVIGATION_OVERRIDE,
    EditContentIdentity,
    EditContentNavigationOverride
} from '@dotcms/edit-content';
import { DotFolderTreeNodeContentData } from '@dotcms/portlets/content-drive/ui';

import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { SYSTEM_HOST } from '../constants';
import { DotContentDrivePanelLocation } from '../models';

/** Shape of the `/api/content/_search` entity we read the resolved contentlet from. */
interface ContentSearchEntity {
    jsonObjectView: { contentlets: DotCMSContentlet[] };
}

/** Sent to the legacy editor as `angularCurrentPortlet`, so it knows which portlet hosts it. */
const CONTENT_DRIVE_PORTLET_ID = 'content-drive';

// Provided at the Content Drive shell level (not `root`) so it can inject the shell-scoped
// DotContentDriveStore and read the list's language filter and default language from it.
// `EditContentNavigationOverride` and its methods: Remove with the legacy editor.
@Injectable()
export class DotContentDriveNavigationService implements EditContentNavigationOverride {
    readonly #dotRouterService = inject(DotRouterService);
    readonly #httpErrorManager = inject(DotHttpErrorManagerService);
    readonly #contentSearch = inject(DotContentSearchService);
    readonly #store = inject(DotContentDriveStore);

    readonly #panelRequest = signal<DotEditContentRequest | null>(null);

    /**
     * What the side panel was asked to open, or `null` when it is closed. One request for either
     * editor: it names the content, and the opener picks the editor from its content type
     * (#38303). The shell keeps the panel open while this is set.
     */
    readonly $editPanelRequest = this.#panelRequest.asReadonly();

    readonly #forceLegacyEditor = signal(false);

    /**
     * Whether the open request must show the legacy editor whatever its content type says. Set
     * only by "switch to the old editor" ({@link switchToLegacyEditor}), whose type was just set
     * back to the legacy editor while the opener still remembers its old choice; cleared by any
     * other open and by a close. Remove with the legacy editor.
     */
    readonly $forceLegacyEditor = this.#forceLegacyEditor.asReadonly();

    readonly #reopeningPanel = signal(false);

    /**
     * True for the one tick in which the open panel must close so it can reopen on a fresh
     * request. The panel helper keeps a panel open while its request changes, so the shell binds
     * `null` while this is set and the panel closes first. {@link $editPanelRequest} never goes
     * `null` meanwhile, so the URL keeps naming the content. Remove with the legacy editor.
     */
    readonly $reopeningPanel = this.#reopeningPanel.asReadonly();

    readonly #panelLocation = signal<DotContentDrivePanelLocation | null>(null);

    /**
     * What the URL must say about the open panel (#37759, FR-020). Set from the request when a
     * panel opens, then free to follow the panel without touching the request, which would
     * remount it.
     */
    readonly $panelLocation = this.#panelLocation.asReadonly();
    /**
     * Navigates to the appropriate editor based on the content type.
     * Routes to the page editor for HTML pages, or opens the side panel for other content.
     *
     * @param contentlet - The content item to edit
     */
    editContent(contentlet: DotCMSContentlet) {
        if (contentlet.baseType === DotCMSBaseTypesContentTypes.HTMLPAGE) {
            this.editPage(contentlet);
        } else {
            this.#openEdit(contentlet, contentlet.identifier);
        }
    }

    /**
     * Navigates to the edit page editor for a page contentlet.
     * Uses the contentlet's URL map or URL along with the language ID for routing.
     *
     * @param contentlet - The page content item to edit
     */
    editPage(contentlet: DotCMSContentlet) {
        const url = contentlet.urlMap || contentlet.url;

        this.#dotRouterService.goToEditPage({ url, language_id: contentlet.languageId });
    }

    /**
     * Opens a page's content form (its properties) in the side panel, the same form Edit Content
     * opens for any other content. {@link editContent} sends pages to the page editor instead, so
     * this is the way into a page's fields (#37903, FR-010).
     *
     * @param contentlet - The page whose properties to open
     */
    editPageProperties(contentlet: DotCMSContentlet) {
        this.#openEdit(contentlet, contentlet.identifier);
    }

    /**
     * Opens the create form for a content type in the side panel. The opener picks the editor and,
     * for a legacy type, resolves its create screen, so a type the server refuses shows the
     * standard error and opens no editor (#38303). Either way the author stays in Content Drive.
     *
     * The form starts in the language the list is filtered by, or Content Drive's default language
     * when there is no language filter, so the new content shows in the list once saved (FR-003).
     * It goes in the folder Content Drive is showing ({@link #currentFolder}). The request carries
     * no title: the opener heads a create with the type's name.
     *
     * @param contentTypeVariable - The variable name of the content type to create
     */
    createContent(contentTypeVariable: string): void {
        this.#openPanel(this.#createRequest(contentTypeVariable, this.#createLanguageId()));
    }

    /**
     * A create request for a content type, in the folder Content Drive is showing.
     *
     * @param contentTypeVariable The type to create.
     * @param languageId The language the content starts in, when one is known.
     * @returns The request.
     */
    #createRequest(contentTypeVariable: string, languageId: number | undefined) {
        const { folderPath, folderInode } = this.#currentFolder();

        return withoutUnset({
            mode: 'new',
            contentType: contentTypeVariable,
            folderPath,
            folderInode,
            languageId,
            portletId: CONTENT_DRIVE_PORTLET_ID
        });
    }

    /**
     * An edit request for a contentlet, named by the identifier the URL uses.
     *
     * @param contentlet The working version to edit.
     * @param identifier The identifier the URL names it by.
     * @param contentType The content type variable, when it is not the contentlet's own.
     * @returns The request.
     */
    #editRequest(
        contentlet: DotCMSContentlet,
        identifier: string,
        contentType = contentlet.contentType
    ): DotEditContentRequest {
        return withoutUnset({
            mode: 'edit',
            inode: contentlet.inode,
            identifier,
            contentType,
            languageId: contentlet.languageId,
            title: contentlet.title,
            portletId: CONTENT_DRIVE_PORTLET_ID
        });
    }

    /**
     * The folder Content Drive is showing, as a create needs it, so new content lands where the
     * author is looking: a create from the create action or a `createContent` link, and the create
     * that "switch to the old editor" reopens.
     *
     * At the site root both fall back to the current site (empty path / no inode). System Host is a
     * destination in its own right, and the site in the switcher is only context while it is
     * selected: pasting the location onto the hostname would produce `demo.dotcms.comSYSTEM_HOST`,
     * which resolves to nothing.
     *
     * @returns `folderPath` (`hostname/path`) for the new editor, `folderInode` for the legacy one.
     */
    #currentFolder(): { folderPath?: string; folderInode?: string } {
        if (this.#store.$systemHostSelected()) {
            return { folderInode: SYSTEM_HOST.identifier };
        }

        const hostname = this.#store.currentSite()?.hostname;
        const path = this.#store.path();
        const data = this.#store.selectedNode()?.data;
        const inode =
            data?.type === 'folder' || data?.type === 'site'
                ? (data as DotFolderTreeNodeContentData).inode
                : undefined;

        return {
            folderPath: hostname ? `${hostname}${path ?? ''}` : undefined,
            folderInode: inode || undefined
        };
    }

    /**
     * "Switch to the old editor" in Content Drive's new-editor panel: the type was just set back to
     * the legacy editor, so the same content reopens, in the same language, in the legacy editor
     * (#37759, FR-028). The URL already names this content, unless the panel moved in place to
     * related content, in which case it follows the switched content. From a create there is no
     * content yet: a create for the same type opens instead, in the folder being browsed and the
     * language the create started in.
     *
     * The request names no editor (#38303), and the opener remembers the type's old choice, so the
     * reopen forces the legacy editor ({@link $forceLegacyEditor}) and closes the open panel for
     * one tick first ({@link $reopeningPanel}).
     *
     * Content Drive answers only for the panel it opened. Any other editor that inherits the
     * override (a related content opened from a relationship field) is declined, and navigates as
     * it does outside Content Drive. Remove with the legacy editor.
     *
     * @param opened What the editor was opened with.
     * @param contentlet The content being edited, or `null` for a create that was never saved.
     * @param contentTypeVariable The type that was just set back to the legacy editor.
     * @returns Whether Content Drive reopened it.
     */
    switchToLegacyEditor(
        opened: EditContentIdentity,
        contentlet: DotCMSContentlet | null,
        contentTypeVariable: string
    ): boolean {
        const open = this.#ownEditor(opened);

        if (!open) {
            return false;
        }

        const request = contentlet
            ? this.#editRequest(contentlet, contentlet.identifier, contentTypeVariable)
            : this.#createRequest(contentTypeVariable, open.languageId ?? this.#createLanguageId());

        this.#forceLegacyEditor.set(true);
        this.#panelRequest.set(request);
        this.#reopenPanel();

        // Inside the panel, related content opens in place, so the content switched can differ
        // from the one the URL names. The URL follows what reopens (FR-020).
        const location = this.#panelLocation();
        if (
            contentlet &&
            location?.kind === 'edit' &&
            (location.editContent !== contentlet.identifier ||
                location.editContentLang !== contentlet.languageId)
        ) {
            this.#panelLocation.set({
                kind: 'edit',
                editContent: contentlet.identifier,
                editContentLang: contentlet.languageId
            });
        }

        return true;
    }

    /**
     * Has the shell close the open panel and reopen it on the current request. The panel closes
     * on this tick and reopens on the next, once its helper has seen it cleared. A timer, not a
     * microtask: change detection runs on a timer, so a microtask would release the panel before
     * it ever saw the hold. Remove with the legacy editor.
     */
    #reopenPanel(): void {
        this.#reopeningPanel.set(true);
        setTimeout(() => this.#reopeningPanel.set(false));
    }

    /**
     * The content of Content Drive's new-editor panel failed to load, after the standard error
     * was shown: close the panel, keeping the folder, filters and page (#37759, FR-029). Any other
     * editor is declined, as for {@link switchToLegacyEditor}. Remove with the legacy editor.
     *
     * @param opened What the editor was opened with.
     * @returns Whether Content Drive closed its panel.
     */
    leaveOnLoadError(opened: EditContentIdentity): boolean {
        if (!this.#ownEditor(opened)) {
            return false;
        }

        this.closeEditPanel();

        return true;
    }

    /**
     * The panel Content Drive has open, when it is the editor that was opened with `opened`: the
     * same content for an edit, the same type for a create. What the editor was opened with comes
     * from the request Content Drive sent, and stays the same across the editor's in-place reloads
     * (a language switch). Remove with the legacy editor.
     *
     * @param opened What the editor was opened with.
     * @returns The open request, or `null` for any other editor.
     */
    #ownEditor(opened: EditContentIdentity): DotEditContentRequest | null {
        const request = this.#panelRequest();

        if (!request) {
            return null;
        }

        const isOwn =
            request.mode === 'edit'
                ? !!opened.inode && opened.inode === request.inode
                : !opened.inode && opened.contentTypeId === request.contentType;

        return isOwn ? request : null;
    }

    /**
     * A panel saved. The first save of a create switches the URL to the saved content, so a refresh
     * reopens it instead of a second empty form (#37759, FR-025). A later save only follows a
     * language the saved version names, such as a new translation (FR-020). The request is left
     * alone, so the panel is not remounted.
     *
     * @param saved The saved content's identifier and language.
     */
    panelSaved({ identifier, languageId }: { identifier: string; languageId: number }): void {
        const location = this.#panelLocation();

        if (location?.kind === 'create') {
            this.#panelLocation.set({
                kind: 'edit',
                editContent: identifier,
                editContentLang: languageId
            });

            return;
        }

        // A new translation saved in the new editor: the saved version names the language. A save
        // of related content reached in place names another content, so it is left alone.
        if (
            location?.kind === 'edit' &&
            location.editContent === identifier &&
            location.editContentLang !== languageId
        ) {
            this.#panelLocation.set({ ...location, editContentLang: languageId });
        }
    }

    /**
     * The author switched language inside the panel: the URL follows it, so a refresh reopens that
     * language (#37759, FR-020). A create names no content yet, so it is left alone.
     *
     * @param languageId The language the editor now shows.
     */
    panelLanguageChanged(languageId: number): void {
        const location = this.#panelLocation();

        if (location?.kind === 'edit') {
            this.#panelLocation.set({ ...location, editContentLang: languageId });
        }
    }

    /** Closes the side panel, clears what the URL says about it and drops any forced editor. */
    closeEditPanel(): void {
        this.#forceLegacyEditor.set(false);
        this.#panelRequest.set(null);
        this.#panelLocation.set(null);
    }

    /**
     * Opens the side panel and names it in the URL: the identifier and language for an edit, the
     * content type variable for a create. The opener picks the editor from the content type.
     *
     * @param request What to open.
     */
    #openPanel(request: DotEditContentRequest): void {
        this.#forceLegacyEditor.set(false);
        this.#panelRequest.set(request);
        this.#panelLocation.set(locationOf(request));
    }

    /**
     * Opens a contentlet for edit in the side panel. The one rule every edit path shares, so a link
     * and a double-click never disagree (#37759, FR-007, FR-021).
     *
     * @param contentlet The working version to edit.
     * @param identifier The identifier the URL names it by.
     */
    #openEdit(contentlet: DotCMSContentlet, identifier: string): void {
        this.#openPanel(this.#editRequest(contentlet, identifier));
    }

    /**
     * Opens the editor for a content addressed by its stable `identifier` (e.g. from a shared
     * `?editContent=<identifier>` URL, or Part 1's redirect from `c/content/<inode>`). Resolves the
     * identifier to its current working inode (the editor loads by inode), then opens it in the
     * side panel; the opener picks the editor from its type. No-op when the content can't be
     * resolved (deleted, no permission, bad id).
     *
     * @param identifier The content's identifier.
     * @param languageId The language the URL asked for, when it carried one.
     */
    openEditByIdentifier(identifier: string, languageId?: number): void {
        const anyVersion = `+identifier:${identifier} +working:true`;
        const preferred = this.#preferredLanguageId(languageId);

        this.#resolveWorkingVersion(
            preferred ? `${anyVersion} +languageId:${preferred}` : anyVersion
        )
            .pipe(
                // A link to content with no version in the preferred language must still open, so
                // "nothing" falls back to any version rather than being read as "do not open". Only
                // reached when that language is genuinely missing: the link carries the language that
                // was open, so the first lookup normally hits.
                switchMap((version) =>
                    version || !preferred ? of(version) : this.#resolveWorkingVersion(anyVersion)
                ),
                take(1),
                catchError((error: HttpErrorResponse) => {
                    this.#httpErrorManager.handle(error);

                    return EMPTY;
                })
            )
            .subscribe((contentlet) => {
                if (contentlet?.inode) {
                    this.#openEdit(contentlet, identifier);
                }
            });
    }

    /**
     * Resolves an identifier query to the single working contentlet it matches, or `undefined`.
     *
     * @param query The Lucene query to run.
     *
     * @return {*} {Observable<DotCMSContentlet | undefined>} The matched contentlet, if any.
     */
    #resolveWorkingVersion(query: string): Observable<DotCMSContentlet | undefined> {
        return this.#contentSearch
            .get<ContentSearchEntity>({ query, limit: 1 })
            .pipe(map((entity) => entity?.jsonObjectView?.contentlets?.[0]));
    }

    /**
     * Which language version the deep link should open, most authoritative first.
     *
     * One identifier has one inode PER LANGUAGE, so the identifier alone does not name a version.
     * `languageId` comes from the URL that opened the panel and names the exact one, so it wins. It is
     * only absent on a link written before the language was recorded; the rest is a best guess for that
     * case — the drive's active Locale filter (its first language, if several are selected), then the
     * environment default. Note both are usually still unresolved here: this runs from the shell's
     * constructor while the store's languages request is in flight, which is exactly why the URL
     * carrying the language matters.
     *
     * @param languageId The language the URL asked for, when it carried one.
     *
     * @return {*} {number | undefined} The language to look for, or `undefined` when none is known.
     */
    #preferredLanguageId(languageId?: number): number | undefined {
        return languageId || this.#createLanguageId();
    }

    /**
     * The language a create starts in: the list's language filter (its first language, if several
     * are selected), else Content Drive's default language (#37759, FR-003). Never the language of
     * the URL, which names an open edit, not the list.
     *
     * @returns The language id, or `undefined` while neither is known.
     */
    #createLanguageId(): number | undefined {
        const [selected] = (this.#store.getFilterValue('languageId') as string[]) ?? [];

        return Number(selected) || this.#store.defaultLanguageId() || undefined;
    }
}

/**
 * The URL location a freshly opened panel starts from: the identifier and language for an edit,
 * the content type variable for a create.
 *
 * @param request The request the panel was opened with.
 * @returns The location for that request.
 */
function locationOf(request: DotEditContentRequest): DotContentDrivePanelLocation {
    return request.mode === 'edit'
        ? {
              kind: 'edit',
              editContent: request.identifier ?? '',
              editContentLang: request.languageId
          }
        : { kind: 'create', createContent: request.contentType ?? '' };
}

/**
 * Drops the keys a request left unset, so it names only what Content Drive knows.
 *
 * @param request The request, possibly with `undefined` values.
 * @returns The same request without them.
 */
function withoutUnset(request: DotEditContentRequest): DotEditContentRequest {
    return Object.fromEntries(
        Object.entries(request).filter(([, value]) => value !== undefined)
    ) as unknown as DotEditContentRequest;
}

/**
 * Hands the new editor's "switch to the old editor" and load error to Content Drive's navigation
 * service, for the panel Content Drive opened (#37759, FR-028, FR-029). The shell provides it next
 * to the service. Remove with the legacy editor.
 *
 * @returns The provider for `EDIT_CONTENT_NAVIGATION_OVERRIDE`.
 */
export function provideContentDriveNavigationOverride(): Provider {
    return {
        provide: EDIT_CONTENT_NAVIGATION_OVERRIDE,
        useExisting: DotContentDriveNavigationService
    };
}
