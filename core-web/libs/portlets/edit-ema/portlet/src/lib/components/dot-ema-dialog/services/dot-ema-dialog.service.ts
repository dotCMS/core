import { Injectable, inject, signal } from '@angular/core';

import { CLIENT_ACTIONS } from '@dotcms/client';
import { DotMessageService } from '@dotcms/data-access';
import { DEFAULT_VARIANT_ID, DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { DotActionUrlService } from '../../../services/dot-action-url/dot-action-url.service';
import { LAYOUT_URL, CONTENTLET_SELECTOR_URL } from '../../../shared/consts';
import { DialogStatus, FormStatus } from '../../../shared/enums';
import {
    ActionPayload,
    AddContentletAction,
    CreateContentletAction,
    CreateFromPaletteAction,
    DotPage,
    EditContentletPayload,
    EditEmaDialogState
} from '../../../shared/models';

@Injectable({
    providedIn: 'root'
})
export class DotEditorDialogService {
    private readonly dotActionUrlService = inject(DotActionUrlService);
    private readonly dotMessageService = inject(DotMessageService);

    readonly state = signal<EditEmaDialogState>({
        header: '',
        url: '',
        type: null,
        status: DialogStatus.IDLE,
        form: {
            status: FormStatus.PRISTINE,
            isTranslation: false
        },
        clientAction: CLIENT_ACTIONS.NOOP
    });

    /**
     * Create a contentlet from the palette
     */
    createContentletFromPalette(params: CreateFromPaletteAction): void {
        const { name, variable, actionPayload, language_id = 1 } = params;

        this.dotActionUrlService.getCreateContentletUrl(variable, language_id).subscribe((url) => {
            this.createContentlet({
                url,
                contentType: name,
                actionPayload
            });
        });
    }

    /**
     * Open a dialog with a specific URL
     */
    openDialogOnURL({ url, title }: { url: string; title: string }): void {
        this.state.update((state) => ({
            ...state,
            header: title,
            status: DialogStatus.LOADING,
            url,
            type: 'content'
        }));
    }

    /**
     * Create a contentlet
     */
    createContentlet({ url, contentType, actionPayload }: CreateContentletAction): void {
        const completeURL = new URL(url, window.location.origin);
        // completeURL.searchParams.set('variantName', this.uveStore.pageParams().variantName);

        this.state.update((state) => ({
            ...state,
            url: completeURL.toString(),
            actionPayload,
            header: this.dotMessageService.get(
                'contenttypes.content.create.contenttype',
                contentType
            ),
            status: DialogStatus.LOADING,
            type: 'content'
        }));
    }

    /**
     * Set loading state for iframe
     */
    loadingIframe(title: string): void {
        this.state.update((state) => ({
            ...state,
            header: title,
            status: DialogStatus.LOADING,
            url: '',
            type: 'content'
        }));
    }

    /**
     * Edit a contentlet
     */
    editContentlet({
        inode,
        title,
        clientAction = CLIENT_ACTIONS.NOOP,
        angularCurrentPortlet
    }: EditContentletPayload): void {
        this.state.update((state) => ({
            ...state,
            clientAction,
            header: title,
            status: DialogStatus.LOADING,
            type: 'content',
            url: this.createEditContentletUrl(inode, angularCurrentPortlet)
        }));
    }

    /**
     * Edit a URL content map contentlet
     */
    editUrlContentMapContentlet({ inode, title }: EditContentletPayload): void {
        const url = this.createEditContentletUrl(inode, null) + '&isURLMap=true';

        this.state.update((state) => ({
            ...state,
            header: title,
            status: DialogStatus.LOADING,
            type: 'content',
            url
        }));
    }

    /**
     * Translate a page
     */
    translatePage({ page, newLanguage }: { page: DotPage; newLanguage: number | string }): void {
        this.state.update((state) => ({
            ...state,
            header: page.title,
            status: DialogStatus.LOADING,
            type: 'content',
            url: this.createTranslatePageUrl(page, newLanguage),
            form: {
                status: FormStatus.PRISTINE,
                isTranslation: true
            }
        }));
    }

    addWidget(actionPayload: ActionPayload): void {
        const { container, language_id } = actionPayload;
        this.openContentlet({
            containerId: container.identifier,
            acceptTypes: DotCMSBaseTypesContentTypes.WIDGET,
            language_id,
            actionPayload
        });
    }

    /**
     * Add a contentlet
     */
    addContentlet(actionPayload: ActionPayload): void {
        const { container, language_id } = actionPayload;
        this.openContentlet({
            containerId: container.identifier,
            acceptTypes: container.acceptTypes ?? '*',
            language_id,
            actionPayload
        });
    }

    /**
     * Add a form contentlet
     */
    addForm(actionPayload: ActionPayload): void {
        this.state.update((state) => ({
            ...state,
            header: this.dotMessageService.get('edit.ema.page.dialog.header.search.form'),
            status: DialogStatus.LOADING,
            url: null,
            type: 'form',
            actionPayload
        }));
    }

    private openContentlet(data: AddContentletAction): void {
        const { actionPayload, ...contentData } = data;
        this.state.update((state) => ({
            ...state,
            header: this.dotMessageService.get('edit.ema.page.dialog.header.search.content'),
            status: DialogStatus.LOADING,
            url: this.createAddContentletUrl(contentData),
            type: 'content',
            actionPayload
        }));
    }

    /**
     * Set dirty state
     */
    setDirty(): void {
        this.state.update((state) => ({
            ...state,
            form: {
                ...state.form,
                status: FormStatus.DIRTY
            }
        }));
    }

    /**
     * Set saved state
     */
    setSaved(): void {
        this.state.update((state) => ({
            ...state,
            form: {
                ...state.form,
                status: FormStatus.SAVED
            }
        }));
    }

    /**
     * Reset dialog state
     */
    resetDialog(): void {
        this.state.update((state) => ({
            ...state,
            url: '',
            header: '',
            status: DialogStatus.IDLE,
            type: null,
            actionPayload: undefined,
            form: {
                status: FormStatus.PRISTINE,
                isTranslation: false
            },
            clientAction: CLIENT_ACTIONS.NOOP
        }));
    }

    /**
     * Set dialog status
     */
    setStatus(status: DialogStatus): void {
        this.state.update((state) => ({
            ...state,
            status
        }));
    }

    /**
     * Create the url to edit a contentlet
     */
    private createEditContentletUrl(inode: string, angularCurrentPortlet: string): string {
        const queryParams = new URLSearchParams({
            p_p_id: 'content',
            p_p_action: '1',
            p_p_state: 'maximized',
            p_p_mode: 'view',
            _content_struts_action: '/ext/contentlet/edit_contentlet',
            _content_cmd: 'edit',
            inode: inode,
            angularCurrentPortlet: angularCurrentPortlet,
            variantName: this.getVariantName()
        });

        return `${LAYOUT_URL}?${queryParams.toString()}`;
    }

    /**
     * Create the url to add a contentlet
     */
    private createAddContentletUrl({
        containerId,
        acceptTypes,
        language_id
    }: {
        containerId: string;
        acceptTypes: string;
        language_id: string;
    }): string {
        const queryParams = new URLSearchParams({
            ng: 'true',
            container_id: containerId,
            add: acceptTypes,
            language_id,
            variantName: this.getVariantName()
        });

        return `${CONTENTLET_SELECTOR_URL}?${queryParams.toString()}`;
    }

    /**
     * Create the url to translate a page
     */
    private createTranslatePageUrl(page: DotPage, newLanguage: number | string): string {
        const { working, workingInode, inode } = page;
        const pageInode = working ? workingInode : inode;
        const queryParams = new URLSearchParams({
            p_p_id: 'content',
            p_p_action: '1',
            p_p_state: 'maximized',
            angularCurrentPortlet: 'edit-page',
            _content_sibbling: pageInode,
            _content_cmd: 'edit',
            p_p_mode: 'view',
            _content_sibblingStructure: pageInode,
            _content_struts_action: '/ext/contentlet/edit_contentlet',
            inode: '',
            lang: newLanguage.toString(),
            populateaccept: 'true',
            reuseLastLang: 'true',
            variantName: this.getVariantName()
        });

        return `${LAYOUT_URL}?${queryParams.toString()}`;
    }

    getVariantName(): string {
        const url = new URL(window.location.href);

        return url.searchParams.get('variantName') || DEFAULT_VARIANT_ID;
    }
}
