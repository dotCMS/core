import { fromEvent, Observable, of, Subject, Subscription } from 'rxjs';

import { DOCUMENT } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ElementRef, Injectable, NgZone, inject } from '@angular/core';

import { catchError, filter, finalize, map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotAlertConfirmService,
    DotCopyContentService,
    DotEditPageService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotGlobalMessageService,
    DotSeoMetaTagsService,
    DotSeoMetaTagsUtilService
} from '@dotcms/data-access';
import {
    DotTreeNode,
    DotIframeEditEvent,
    DotPage,
    DotPageContainer,
    DotPageContainerPersonalized,
    DotPageRenderState,
    DotPersona,
    DotAddContentTypePayload,
    DotAssetPayload,
    DotContentletEvent,
    DotContentletEventDragAndDropDotAsset,
    DotContentletEventRelocate,
    DotContentletEventReorder,
    DotContentletEventSave,
    DotContentletEventSelect,
    DotInlineEditContent,
    DotPageContent,
    DotRelocatePayload,
    DotShowCopyModal,
    PageModelChangeEvent,
    PageModelChangeEventType,
    SeoMetaTags,
    SeoMetaTagsResult
} from '@dotcms/dotcms-models';
import { DotCopyContentModalService } from '@dotcms/ui';
import { DotLoadingIndicatorService } from '@dotcms/utils';

import { DotContainerContentletService } from '../dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from '../html/dot-dom-html-util.service';
import { DotDragDropAPIHtmlService } from '../html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from '../html/dot-edit-content-toolbar-html.service';
import { getEditPageCss } from '../html/libraries/iframe-edit-mode.css';
import { INLINE_TINYMCE_SCRIPTS } from '../html/libraries/inline-edit-mode.js';

export enum DotContentletAction {
    EDIT,
    ADD
}

export enum DotContentletMenuAction {
    add = 'ADD',
    code = 'CODE',
    edit = 'EDIT',
    remove = 'REMOVE'
}

export const CONTENTLET_PLACEHOLDER_SELECTOR = '#contentletPlaceholder';

export const MATERIAL_ICONS_PATH = '/dotAdmin/assets/material-icons.css';

@Injectable()
export class DotEditContentHtmlService {
    private dotEditPageService = inject(DotEditPageService);
    private dotContainerContentletService = inject(DotContainerContentletService);
    private dotDragDropAPIHtmlService = inject(DotDragDropAPIHtmlService);
    private dotEditContentToolbarHtmlService = inject(DotEditContentToolbarHtmlService);
    private dotDOMHtmlUtilService = inject(DotDOMHtmlUtilService);
    private dotDialogService = inject(DotAlertConfirmService);
    private dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private dotMessageService = inject(DotMessageService);
    private dotGlobalMessageService = inject(DotGlobalMessageService);
    private dotWorkflowActionsFireService = inject(DotWorkflowActionsFireService);
    private ngZone = inject(NgZone);
    private dotLicenseService = inject(DotLicenseService);
    private dotCopyContentModalService = inject(DotCopyContentModalService);
    private dotCopyContentService = inject(DotCopyContentService);
    private dotLoadingIndicatorService = inject(DotLoadingIndicatorService);
    private dotSeoMetaTagsService = inject(DotSeoMetaTagsService);
    private dotSeoMetaTagsUtilService = inject(DotSeoMetaTagsUtilService);
    private document = inject<Document>(DOCUMENT);

    contentletEvents$: Subject<
        | DotContentletEventDragAndDropDotAsset
        | DotContentletEventRelocate
        | DotContentletEventReorder
        | DotContentletEventSelect
        | DotContentletEventSave
        | DotContentletEvent<DotInlineEditContent | DotShowCopyModal>
    > = new Subject();
    currentContainer: DotPageContainer;
    currentContentlet: DotPageContent;
    iframe: ElementRef;
    iframeActions$: Subject<
        DotIframeEditEvent<Record<string, unknown> | DotAddContentTypePayload>
    > = new Subject();
    pageModel$: Subject<PageModelChangeEvent> = new Subject();
    mutationConfig = { attributes: false, childList: true, characterData: false };
    datasetMissing: string[];
    private currentPage: DotPage;
    private currentPersona: DotPersona;

    private inlineCurrentContent: { [key: string]: string } = {};
    private currentAction: DotContentletAction;
    private currentMenuAction: DotContentletMenuAction;
    private docClickSubscription: Subscription;
    private updateContentletInode = false;
    private remoteRendered: boolean;
    private askToCopy = true;

    private readonly origin: string = '';
    private readonly docClickHandlers;

    get pagePersonalization() {
        if (!this.currentPersona) {
            return `dot:default`;
        }

        return `dot:${this.currentPersona.contentType}:${this.currentPersona.keyTag}`;
    }

    constructor() {
        this.contentletEvents$.subscribe(
            (
                contentletEvent:
                    | DotContentletEventRelocate
                    | DotContentletEventSelect
                    | DotContentletEventSave
            ) => {
                this.ngZone.run(() => {
                    this.handlerContentletEvents(contentletEvent.name)(contentletEvent.data);
                });
            }
        );

        if (!this.docClickHandlers) {
            this.docClickHandlers = {};
            this.setGlobalClickHandlers();
        }

        this.origin = this.document.location.origin;
    }

    /**
     * Set the current page
     *
     * @param DotPage page
     */
    setCurrentPage(page: DotPage) {
        this.currentPage = page;
    }

    /**
     * Set the current Persona
     *
     * @param DotPersona persona
     */
    setCurrentPersona(persona: DotPersona) {
        this.currentPersona = persona;
    }

    /**
     * Load code into iframe
     *
     * @param string editPageHTML
     * @param ElementRef iframeEl
     * @returns Promise<boolean>
     * @memberof DotEditContentHtmlService
     */
    renderPage(pageState: DotPageRenderState, iframeEl: ElementRef): Promise<boolean> {
        this.remoteRendered = pageState.page.remoteRendered;

        return new Promise((resolve, _reject) => {
            this.iframe = iframeEl;
            const iframeElement = this.getEditPageIframe();

            iframeElement.addEventListener('load', () => {
                iframeElement.contentWindow['contentletEvents'] = this.contentletEvents$;

                this.bindGlobalEvents();
                this.setMaterialIcons();

                resolve(true);
            });

            // Load content after bind 'load' event.
            this.loadCodeIntoIframe(pageState);
        });
    }

    /**
     * Initalize edit content mode
     *
     * @param string editPageHTML
     * @param ElementRef iframeEl
     * @memberof DotEditContentHtmlService
     */
    initEditMode(pageState: DotPageRenderState, iframeEl: ElementRef): void {
        this.renderPage(pageState, iframeEl).then(() => {
            this.setEditMode();
        });
    }

    /**
     * Remove a contentlet from the DOM by inode and update the page model
     *
     * @param string inode
     * @memberof DotEditContentHtmlService
     */
    removeContentlet(container: DotPageContainer, content: DotPageContent): void {
        const doc = this.getEditPageDocument();

        const selector = [
            `[data-dot-object="container"][data-dot-identifier="${container.identifier}"][data-dot-uuid="${container.uuid}"] `,
            `[data-dot-object="contentlet"][data-dot-inode="${content.inode}"]`
        ].join('');

        const contenletEl = doc.querySelector(selector);

        contenletEl.remove();

        this.savePage(this.getContentModel()).subscribe();

        this.pageModel$.next({
            model: this.getContentModel(),
            type: PageModelChangeEventType.REMOVE_CONTENT
        });
        this.updateContainerToolbar(container.identifier);
    }

    /**
     * Render contentlet in the DOM after edition.
     *
     * @param * contentlet
     * @memberof DotEditContentHtmlService
     */
    renderEditedContentlet(contentlet: DotPageContent): void {
        if (this.remoteRendered || !contentlet) {
            this.iframeActions$.next({
                name: 'save'
            });
        } else {
            const doc = this.getEditPageDocument();
            const currentContentlets: HTMLElement[] = Array.from(
                doc.querySelectorAll(
                    `[data-dot-object="contentlet"][data-dot-identifier="${contentlet.identifier}"]`
                )
            );

            currentContentlets.forEach((currentContentlet: HTMLElement) => {
                contentlet.type = currentContentlet.dataset.dotType;
                const containerEl = <HTMLElement>currentContentlet.parentNode;

                const container: DotPageContainer = this.getDotPageContainer(containerEl);

                this.dotContainerContentletService
                    .getContentletToContainer(container, contentlet, this.currentPage)
                    .pipe(take(1))
                    .subscribe((contentletHtml: string) =>
                        this.replaceHTMLContentlet(contentletHtml, currentContentlet)
                    );
            });
        }
    }

    /**
     * Removes placeholder when closing the dialog.
     *
     * @memberof DotEditContentHtmlService
     */
    removeContentletPlaceholder(): void {
        const doc = this.getEditPageDocument();
        const placeholder = doc.querySelector(CONTENTLET_PLACEHOLDER_SELECTOR);
        if (placeholder) {
            placeholder.remove();
        }
    }

    /**
     * Render a contentlet in the DOM after add it
     *
     * @param DotPageContent contentlet
     * @param boolean isDroppedAsset
     * @memberof DotEditContentHtmlService
     */
    renderAddedContentlet(contentlet: DotPageContent, isDroppedContentlet = false): void {
        const doc = this.getEditPageDocument();
        if (isDroppedContentlet) {
            this.setCurrentContainerOnContentDrop(doc);
        }

        const containerEl: HTMLElement = doc.querySelector(
            `[data-dot-object="container"][data-dot-identifier="${this.currentContainer.identifier}"][data-dot-uuid="${this.currentContainer.uuid}"]`
        );

        if (this.isContentExistInContainer(contentlet, containerEl)) {
            this.showContentAlreadyAddedError();
            this.removeContentletPlaceholder();
        } else {
            let contentletPlaceholder = <HTMLElement>(
                doc.querySelector(CONTENTLET_PLACEHOLDER_SELECTOR)
            );
            if (!contentletPlaceholder) {
                contentletPlaceholder = this.getContentletPlaceholder();
                containerEl.appendChild(contentletPlaceholder);
            }

            const contentletHTML$ = this.dotContainerContentletService.getContentletToContainer(
                this.currentContainer,
                contentlet,
                this.currentPage
            );

            this.savePage(this.getContentModel(contentlet.identifier))
                .pipe(
                    switchMap(() => contentletHTML$),
                    take(1)
                )
                .subscribe((contentletHtml: string) => {
                    if (contentletHtml) {
                        this.replaceHTMLContentlet(contentletHtml, contentletPlaceholder);
                        // Update the model with the recently added contentlet
                        this.pageModel$.next({
                            model: this.getContentModel(),
                            type: PageModelChangeEventType.ADD_CONTENT
                        });

                        this.currentAction = DotContentletAction.EDIT;
                        this.updateContainerToolbar(containerEl.dataset.dotIdentifier);
                    }
                });
        }
    }

    /**
     * Render a form in the DOM after add it
     *
     * @param string formId
     * @param booblean isDroppedAsset
     * @memberof DotEditContentHtmlService
     */
    renderAddedForm(formId: string, isDroppedForm = false): void {
        const doc = this.getEditPageDocument();

        if (isDroppedForm) {
            this.setCurrentContainerOnContentDrop(doc);
        }

        const containerEl: HTMLElement = doc.querySelector(
            [
                '[data-dot-object="container"]',
                `[data-dot-identifier="${this.currentContainer.identifier}"]`,
                `[data-dot-uuid="${this.currentContainer.uuid}"]`
            ].join('')
        );

        if (this.isFormExistInContainer(formId, containerEl)) {
            this.showContentAlreadyAddedError();
            this.removeContentletPlaceholder();
        } else {
            let contentletPlaceholder = doc.querySelector(CONTENTLET_PLACEHOLDER_SELECTOR);

            if (!contentletPlaceholder) {
                contentletPlaceholder = this.getContentletPlaceholder();
                containerEl.appendChild(contentletPlaceholder);
            }

            this.dotContainerContentletService
                .getFormToContainer(this.currentContainer, formId)
                .pipe(
                    tap(({ content }: { content: { [key: string]: string } }) => {
                        const { identifier, inode } = content;
                        const formContentlet = this.renderFormContentlet(identifier, inode);
                        containerEl.replaceChild(formContentlet, contentletPlaceholder);
                    }),
                    switchMap(() => this.savePage(this.getContentModel()))
                )
                .subscribe(() => {
                    const model = this.getContentModel();
                    if (model) {
                        // Update the model with the recently added contentlet
                        this.pageModel$.next({
                            model: model,
                            type: PageModelChangeEventType.ADD_CONTENT
                        });

                        this.iframeActions$.next({
                            name: 'save'
                        });
                    }
                });
        }
    }

    /**
     * Set the container id where a contentlet will be added
     *
     * @param string identifier
     * @memberof DotEditContentHtmlService
     */
    setContainterToAppendContentlet(pageContainer: DotPageContainer): void {
        this.currentContainer = pageContainer;
        this.currentAction = DotContentletAction.ADD;
    }

    /**
     * Return the page model
     *
     * @returns *
     * @memberof DotEditContentHtmlService
     */
    getContentModel(addedContentId = ''): DotPageContainer[] {
        const { uuid, identifier } = this.currentContainer || {};

        return this.getEditPageIframe().contentWindow['getDotNgModel']({
            uuid,
            identifier,
            addedContentId
        });
    }

    /**
     *  Returns the meta tags results
     *
     * @returns SeoMetaTagsResult[]
     */
    getMetaTagsResults(): Observable<SeoMetaTagsResult[]> {
        const pageDocument = this.getEditPageDocument();

        return this.dotSeoMetaTagsService.getMetaTagsResults(pageDocument);
    }

    /**
     * Returns the meta tags
     *
     * @returns SeoMetaTags
     */
    getMetaTags(): SeoMetaTags {
        const pageDocument = this.getEditPageDocument();

        return this.dotSeoMetaTagsUtilService.getMetaTags(pageDocument);
    }

    private setMaterialIcons(): void {
        const doc = this.getEditPageDocument();
        const link = this.dotDOMHtmlUtilService.createLinkElement(
            this.origin + MATERIAL_ICONS_PATH
        );
        doc.head.appendChild(link);
    }

    private setCurrentContainerOnContentDrop(doc: Document): void {
        const container: HTMLElement = doc
            .querySelector(CONTENTLET_PLACEHOLDER_SELECTOR)
            .closest('[data-dot-object="container"]');
        this.setContainterToAppendContentlet({
            identifier: container.dataset['dotIdentifier'],
            uuid: container.dataset['dotUuid']
        });
    }

    private updateContainerToolbar(dotIdentifier: string) {
        const doc = this.getEditPageDocument();
        const target = <HTMLElement>(
            doc.querySelector(
                `[data-dot-object="container"][data-dot-identifier="${dotIdentifier}"]`
            )
        );
        this.dotEditContentToolbarHtmlService.updateContainerToolbar(target);
    }

    private getContentletPlaceholder(): HTMLDivElement {
        const doc = this.getEditPageDocument();
        const placeholder = doc.createElement('div');

        placeholder.setAttribute('data-dot-object', 'contentlet');
        placeholder.appendChild(this.getLoadingIndicator());

        return placeholder;
    }

    private renderFormContentlet(identifier: string, inode: string): HTMLElement {
        return this.createEmptyContentletElement({
            identifier,
            inode,
            baseType: 'FORM',
            type: 'forms'
        });
    }

    private bindGlobalEvents(): void {
        const doc = this.getEditPageDocument();

        if (this.docClickSubscription) {
            this.docClickSubscription.unsubscribe();
        }

        this.docClickSubscription = fromEvent(doc, 'click').subscribe(($event: MouseEvent) => {
            const target = <HTMLElement>$event.target;
            const method = this.docClickHandlers[target.dataset.dotObject];

            if (method) {
                this.ngZone.run(() => method(target));
            }

            if (!target.classList.contains('dotedit-menu__button')) {
                this.closeContainersToolBarMenu();
            }
        });
    }

    private getCurrentContentlet(target: HTMLElement): DotPageContent {
        try {
            const contentlet = <HTMLElement>target.closest('[data-dot-object="contentlet"]');

            return {
                identifier: contentlet.dataset.dotIdentifier,
                inode: contentlet.dataset.dotInode,
                type: contentlet.dataset.dotType,
                baseType: contentlet.dataset.dotBasetype
            };
        } catch {
            return null;
        }
    }

    private setGlobalClickHandlers(): void {
        this.docClickHandlers['edit-content'] = (target: HTMLElement) => {
            this.currentContentlet = this.getCurrentContentlet(target);
            this.buttonClickHandler(target, 'edit');
        };

        this.docClickHandlers['remove-content'] = (target: HTMLElement) => {
            this.buttonClickHandler(target, 'remove');
        };

        this.docClickHandlers['popup-button'] = (target: HTMLElement) => {
            target.nextElementSibling.classList.toggle('active');
        };

        this.docClickHandlers['popup-menu-item'] = (target: HTMLElement) => {
            if (target.dataset.dotAction === 'code') {
                this.currentContentlet = this.getCurrentContentlet(target);
            }

            this.buttonClickHandler(target, target.dataset.dotAction);
        };
    }

    private showContentAlreadyAddedError(): void {
        this.currentContainer = null;
        this.dotDialogService.alert({
            header: this.dotMessageService.get('editpage.content.add.already.title'),
            message: this.dotMessageService.get('editpage.content.add.already.message'),
            footerLabel: {
                accept: 'Ok'
            }
        });
    }

    private isContentExistInContainer(
        contentlet: DotPageContent,
        containerEL: HTMLElement
    ): boolean {
        const contentsSelector = `[data-dot-object="contentlet"]`;
        const currentContentlets: HTMLElement[] = <HTMLElement[]>(
            Array.from(containerEL.querySelectorAll(contentsSelector).values())
        );

        return currentContentlets.some(
            (contentElement) => contentElement.dataset.dotIdentifier === contentlet.identifier
        );
    }

    private isFormExistInContainer(formId: string, containerEL: HTMLElement): boolean {
        const contentsSelector = `[data-dot-object="contentlet"]`;
        const currentContentlets: HTMLElement[] = <HTMLElement[]>(
            Array.from(containerEL.querySelectorAll(contentsSelector).values())
        );

        return currentContentlets.some(
            (contentElement) => contentElement.dataset.dotContentTypeId === formId
        );
    }

    private addContentToolBars(): void {
        const doc = this.getEditPageDocument();
        this.dotEditContentToolbarHtmlService.bindContentletEvents(doc);
        this.dotEditContentToolbarHtmlService.addContainerToolbar(doc);
    }

    private injectInlineEditingScripts(): void {
        const doc = this.getEditPageDocument();
        const editModeNodes = doc.querySelectorAll('[data-mode]');

        if (editModeNodes.length) {
            const TINYMCE = `${this.origin}/html/js/tinymce/js/tinymce/tinymce.min.js`;
            const tinyMceScript = this.dotDOMHtmlUtilService.creatExternalScriptElement(TINYMCE);
            const tinyMceInitScript: HTMLScriptElement =
                this.dotDOMHtmlUtilService.createInlineScriptElement(INLINE_TINYMCE_SCRIPTS);

            this.dotLicenseService
                .isEnterprise()
                .pipe(
                    take(1),
                    filter((isEnterprise: boolean) => isEnterprise === true)
                )
                .subscribe(() => {
                    // We have elements in the DOM and we're on enterprise plan

                    doc.body.append(tinyMceInitScript);
                    doc.body.append(tinyMceScript);

                    editModeNodes.forEach((node) => {
                        node.classList.add('dotcms__inline-edit-field');
                    });
                });
        }
    }

    // Inject Block Editor
    private injectInlineBlockEditor(): void {
        const doc = this.getEditPageDocument();
        const editBlockEditorNodes = doc.querySelectorAll('[data-block-editor-content]');
        if (editBlockEditorNodes.length) {
            this.dotLicenseService
                .isEnterprise()
                .pipe(
                    take(1),
                    filter((isEnterprise: boolean) => isEnterprise === true)
                )
                .subscribe(() => {
                    editBlockEditorNodes.forEach((node) => {
                        node.classList.add('dotcms__inline-edit-field');
                        node.addEventListener('click', (event) => {
                            this.ngZone.run(() => this.onEditBlockEditor(event));
                        });
                    });
                });
        }
    }

    private createScriptTag(node: HTMLScriptElement): HTMLScriptElement {
        const doc = this.getEditPageDocument();
        const script = doc.createElement('script');
        script.type = 'text/javascript';

        if (node.src) {
            script.src = node.src;
        } else {
            script.text = node.textContent;
        }

        return script;
    }

    private getScriptTags(
        scriptTags: HTMLScriptElement[],
        contentlet: HTMLElement
    ): HTMLScriptElement[] {
        Array.from(contentlet.children).forEach((node: HTMLElement) => {
            if (node.tagName === 'SCRIPT') {
                const script = this.createScriptTag(<HTMLScriptElement>node);
                scriptTags.push(script);
                node.parentElement.removeChild(node);
            } else if (node.children.length) {
                this.getScriptTags(scriptTags, node);
            }
        });

        return scriptTags;
    }

    private getContentletElementFromHtml(html: string): HTMLElement {
        const doc = this.getEditPageDocument();
        // Add innerHTML to a plain so we can get the HTML nodes later
        const div = doc.createElement('div');
        div.innerHTML = html;

        return <HTMLElement>div.children[0];
    }

    private generateNewContentlet(html: string): HTMLElement {
        const newContentlet = this.getContentletElementFromHtml(html);

        let scriptTags: HTMLScriptElement[] = [];
        scriptTags = this.getScriptTags(scriptTags, newContentlet);

        scriptTags.forEach((script: HTMLScriptElement) => {
            newContentlet.appendChild(script);
        });

        return newContentlet;
    }

    private buttonClickHandler(target: HTMLElement, type: string) {
        this.updateContentletInode = this.shouldUpdateContentletInode(target);
        this.currentMenuAction = DotContentletMenuAction[type];

        const container = <HTMLElement>target.closest('[data-dot-object="container"]');
        const contentlet = <HTMLElement>target.closest('[data-dot-object="contentlet"]');
        const isInMultiplePages = this.isContentInMultiplePages(contentlet);

        const eventData = {
            name: type,
            dataset: target.dataset,
            container: container ? container.dataset : null
        };

        // If we are editing a contentlet that is in multiple pages
        // we need to show the copy modal and then update the contentlet if needed
        if (type === 'edit' && isInMultiplePages) {
            this.showCopyModal(contentlet, container).subscribe(({ dataset }) => {
                const dotInode = dataset.dotInode;
                this.iframeActions$.next({
                    ...eventData,
                    dataset: {
                        dotInode
                    }
                });
            });

            return;
        }

        this.iframeActions$.next(eventData);
    }

    private closeContainersToolBarMenu(activeElement?: Node): void {
        const doc = this.getEditPageDocument();
        const activeToolBarMenus = Array.from(doc.querySelectorAll('.dotedit-menu__list.active'));
        activeToolBarMenus.forEach((toolbar: HTMLElement) => {
            if (activeElement !== toolbar) {
                toolbar.classList.remove('active');
            }
        });
    }

    private createEmptyContentletElement(dotPageContent: DotPageContent): HTMLElement {
        const doc = this.getEditPageDocument();

        const dotEditContentletEl: HTMLElement = doc.createElement('div');
        dotEditContentletEl.setAttribute('data-dot-object', 'contentlet');

        for (const attr in dotPageContent) {
            // eslint-disable-next-line no-prototype-builtins
            if (dotPageContent.hasOwnProperty(attr)) {
                dotEditContentletEl.setAttribute(`data-dot-${attr}`, dotPageContent[attr]);
            }
        }

        return dotEditContentletEl;
    }

    private getEditPageIframe(): HTMLIFrameElement {
        return this.iframe.nativeElement;
    }

    private getEditPageDocument(): Document {
        return (
            this.getEditPageIframe().contentDocument ||
            this.getEditPageIframe().contentWindow.document
        );
    }

    private handleTinyMCEOnFocusEvent(contentlet: DotInlineEditContent) {
        this.inlineCurrentContent = {
            ...this.inlineCurrentContent,
            [contentlet.element.id]: contentlet.innerHTML
        };
    }

    private handleTinyMCEOnBlurEvent(content: DotInlineEditContent) {
        // TODO: Remove it from here and add it to the TinyMCE component
        this.askToCopy = true;
        // If editor is dirty then we continue making the request
        if (!content.isNotDirty) {
            // Add the loading indicator to the field
            content.element.classList.add('inline-editing--saving');

            // All good, initiate the request
            this.dotWorkflowActionsFireService
                .saveContentlet({
                    [content.dataset.fieldName]: content.innerHTML,
                    inode: content.dataset.inode
                })
                .pipe(take(1))
                .subscribe(
                    () => {
                        // onSuccess
                        content.element.classList.remove('inline-editing--saving');
                        delete this.inlineCurrentContent[content.element.id];
                        this.dotGlobalMessageService.success(
                            this.dotMessageService.get('dot.common.message.saved')
                        );
                    },
                    (e: HttpErrorResponse) => {
                        // onError
                        content.element.innerHTML = this.inlineCurrentContent[content.element.id];
                        const message =
                            e.error.errors[0].message ||
                            this.dotMessageService.get('editpage.inline.error');
                        this.dotGlobalMessageService.error(message);
                        content.element.classList.remove('inline-editing--saving');
                        delete this.inlineCurrentContent[content.element.id];
                    }
                );
        } else {
            delete this.inlineCurrentContent[content.element.id];
        }
    }

    private handlerContentletEvents(
        event: string
    ): (contentletEvent: DotPageContent | DotRelocatePayload) => void {
        const contentletEventsMap = {
            // When an user create or edit a contentlet from the jsp
            save: (contentlet: DotPageContent) => {
                /*
                 * The Save event is triggered when the user edit a contentlet or edit the vtl code from the jsp.
                 * When the user edit the vtl code from the jsp the data sent is the vtl code information.
                 */
                const contentletEdited = this.isEditAction() ? contentlet : this.currentContentlet;

                if (this.currentAction === DotContentletAction.ADD) {
                    this.renderAddedContentlet(contentlet);
                } else {
                    if (this.updateContentletInode) {
                        this.currentContentlet.inode = contentlet.inode;
                    }
                    // because: https://github.com/dotCMS/core/issues/21818

                    setTimeout(() => {
                        this.renderEditedContentlet(contentletEdited || this.currentContentlet);
                    }, 1800);
                }
            },
            showCopyModal: (data: DotShowCopyModal) => {
                const { contentlet, container, initEdit, selector } = data;
                this.showCopyModal(contentlet, container).subscribe((contentlet) => {
                    const element = (
                        selector ? contentlet.querySelector(selector) : contentlet
                    ) as HTMLElement;
                    initEdit(element);
                });
            },
            inlineEdit: (data: DotInlineEditContent) => {
                const { eventType: type } = data;

                if (type === 'focus') {
                    this.handleTinyMCEOnFocusEvent(data);
                }

                if (type === 'blur') {
                    this.handleTinyMCEOnBlurEvent(data);
                }
            },
            // When a user select a content from the search jsp
            select: (contentlet: DotPageContent) => {
                this.renderAddedContentlet(contentlet);
                this.iframeActions$.next({
                    name: 'select'
                });
            },
            // When a user drag and drop a contentlet in the anohter container in the iframe
            relocate: (relocateInfo: DotRelocatePayload) => {
                if (!this.remoteRendered) {
                    this.renderRelocatedContentlet(relocateInfo);
                }
            },
            // When a user drag and drop a contentlet in the same container in the iframe
            reorder: (model: DotPageContainer[]) => {
                this.savePage(model)
                    .pipe(take(1))
                    .subscribe(() => {
                        this.pageModel$.next({
                            type: PageModelChangeEventType.MOVE_CONTENT,
                            model
                        });
                    });
            },
            'deleted-contenlet': () => {
                this.removeCurrentContentlet();
            },
            'add-uploaded-dotAsset': (dotAssetData: DotAssetPayload) => {
                this.renderAddedContentlet(dotAssetData.contentlet, true);
            },
            'add-content': (data: DotAddContentTypePayload) => {
                this.iframeActions$.next({
                    name: 'add-content',
                    data: data
                });
            },
            'add-contentlet': (dotAssetData: DotAssetPayload) => {
                this.renderAddedContentlet(dotAssetData.contentlet, true);
            },
            'add-form': (formId: string) => {
                this.renderAddedForm(formId, true);
            },
            'handle-http-error': (err: HttpErrorResponse) => {
                this.dotHttpErrorManagerService.handle(err).pipe(take(1)).subscribe();
            }
        };

        return contentletEventsMap[event];
    }

    private shouldUpdateContentletInode(target: HTMLElement) {
        return target.dataset.dotObject === 'edit-content' && target.tagName === 'BUTTON';
    }

    private updateHtml(pageState: DotPageRenderState): string {
        const fakeHtml = document.createElement('html');
        fakeHtml.innerHTML = pageState.html;

        const head = fakeHtml.querySelector('head');

        if (fakeHtml.querySelector('base')) {
            return pageState.html;
        } else {
            const base = this.getBaseTag(pageState.page.pageURI);
            head.appendChild(base);
        }

        return fakeHtml.innerHTML;
    }

    private getBaseTag(url: string): HTMLBaseElement {
        const base = document.createElement('base');
        const href = url.split('/');
        href.pop();

        base.href = this.origin + href.join('/') + '/';

        return base;
    }

    private loadCodeIntoIframe(pageState: DotPageRenderState): void {
        const doc = this.getEditPageDocument();
        const html = this.updateHtml(pageState);
        doc.open();
        doc.write(html);
        doc.close();
    }

    private setEditContentletStyles(): void {
        const timeStampId = `iframeId_${Math.floor(Date.now() / 100).toString()}`;
        const style = this.dotDOMHtmlUtilService.createStyleElement(
            getEditPageCss(`#${timeStampId}`, this.origin)
        );

        const doc = this.getEditPageDocument();

        doc.documentElement.id = timeStampId;
        doc.head.appendChild(style);
    }

    private setEditMode(): void {
        this.setEditContentletStyles();
        this.addContentToolBars();
        this.injectInlineEditingScripts();
        this.injectInlineBlockEditor();
        this.dotDragDropAPIHtmlService.initDragAndDropContext(this.getEditPageIframe());
    }

    private removeCurrentContentlet(): void {
        const doc = this.getEditPageDocument();
        const contentlets = doc.querySelectorAll(
            `[data-dot-object="contentlet"][data-dot-inode="${this.currentContentlet.inode}"]`
        );

        contentlets.forEach((contentlet) => {
            contentlet.remove();
        });
    }

    private renderRelocatedContentlet(relocateInfo: DotRelocatePayload): void {
        const doc = this.getEditPageDocument();
        const contenletEl: HTMLElement = doc.querySelector(
            `[data-dot-object="contentlet"][data-dot-inode="${relocateInfo.contentlet.inode}"]`
        );
        contenletEl.insertAdjacentElement('afterbegin', this.getLoadingIndicator());

        const container: HTMLElement = <HTMLElement>contenletEl.parentNode;

        relocateInfo.container = relocateInfo.container || {
            identifier: container.dataset.dotIdentifier,
            uuid: container.dataset.dotUuid
        };

        this.dotContainerContentletService
            .getContentletToContainer(
                relocateInfo.container,
                relocateInfo.contentlet,
                this.currentPage
            )
            .pipe(take(1))
            .subscribe((contentletHtml: string) =>
                this.replaceHTMLContentlet(contentletHtml, contenletEl)
            );
    }

    private getLoadingIndicator(): HTMLElement {
        const div = document.createElement('div');
        div.innerHTML = `
            <div class="loader__overlay">
                <div class="loader"></div>
            </div>
        `;

        return <HTMLElement>div.children[0];
    }

    /**
     * Get DotPageContainer from the container element
     *
     * @private
     * @param {HTMLElement} container
     * @return {*}  {DotPageContainer}
     * @memberof DotEditContentHtmlService
     */
    private getDotPageContainer(container: HTMLElement): DotPageContainer {
        const { dotIdentifier, dotUuid } = container.dataset;

        return {
            identifier: dotIdentifier,
            uuid: dotUuid
        };
    }

    /**
     * Replace the contentlet with the new contentlet html
     *
     * @private
     * @param {string} html
     * @param {HTMLElement} contentlet
     * @return {*}  {HTMLElement}
     * @memberof DotEditContentHtmlService
     */
    private replaceHTMLContentlet(html: string, contentlet: HTMLElement): HTMLElement {
        const contentletEl: HTMLElement = this.generateNewContentlet(html);
        contentlet.replaceWith(contentletEl);

        return contentletEl;
    }

    private getTreeNodeData(contentlet: HTMLElement, container: HTMLElement): DotTreeNode {
        try {
            /* Get Copy content Data from the contentlet and Container*/
            const { dotIdentifier: contentId, dotVariant: variantId } = contentlet.dataset;
            const { dotUuid: relationType, dotIdentifier: containerId } = container.dataset;

            return {
                pageId: this.currentPage.identifier,
                treeOrder: this.getTreeOrder(contentlet).toString(),
                containerId,
                contentId,
                relationType,
                variantId,
                personalization: this.pagePersonalization
            };
        } catch {
            return null;
        }
    }

    private getTreeOrder(element: HTMLElement): number {
        return Array.from(element.parentElement.children).indexOf(element);
    }

    private isContentInMultiplePages(contentlet: HTMLElement): boolean {
        return Number(contentlet?.dataset?.dotOnNumberOfPages || 0) > 1;
    }

    private isEditAction() {
        return this.currentMenuAction === DotContentletMenuAction.edit;
    }

    private savePage(model: DotPageContainer[]) {
        this.dotGlobalMessageService.loading(
            this.dotMessageService.get('dot.common.message.saving')
        );

        return this.dotEditPageService
            .save(this.currentPage.identifier, this.getPersonalizedModel(model) || model)
            .pipe(
                take(1),
                tap(() => {
                    this.dotGlobalMessageService.success();
                }),
                catchError((error: HttpErrorResponse) => {
                    this.pageModel$.next({
                        model: this.getContentModel(),
                        type: PageModelChangeEventType.SAVE_ERROR
                    });

                    return this.dotHttpErrorManagerService.handle(error);
                })
            );
    }

    private getPersonalizedModel(model: DotPageContainer[]): DotPageContainerPersonalized[] {
        if (this.currentPersona && this.currentPersona.personalized) {
            return model.map((container: DotPageContainer) => {
                return {
                    ...container,
                    personaTag: this.currentPersona.keyTag
                };
            });
        }

        return null;
    }

    /**
     * Get the content model from the iframe
     *
     * @private
     * @param {*} contentlet
     * @param {*} container
     * @return {*}  {Observable<HTMLElement>}
     * @memberof DotEditContentHtmlService
     */
    private showCopyModal(
        contentlet: HTMLElement,
        container: HTMLElement
    ): Observable<HTMLElement> {
        return this.dotCopyContentModalService.open().pipe(
            switchMap(({ shouldCopy }) => {
                // If shouldCopy is true, we need to copy the contentlet
                // otherwise we just return the contentlet
                return shouldCopy ? this.copyContent(contentlet, container) : of(contentlet);
            })
        );
    }

    /**
     * Copy content
     *
     * @param {DotCopyContent} content
     * @param {*} inode
     * @return {*}  {Observable<ModelCopyContentResponse>}
     * @memberof DotCopyContentModalService
     */
    private copyContent(contentlet: HTMLElement, container: HTMLElement): Observable<HTMLElement> {
        const content = this.getTreeNodeData(contentlet, container);
        const dotPageContainer = this.getDotPageContainer(container);

        return this.dotCopyContentService.copyInPage(content).pipe(
            tap(() => this.dotLoadingIndicatorService.show()),
            switchMap((dotContentlet) => {
                // After copy the contentlet, we need to get the new contentletHTML
                return this.dotContainerContentletService.getContentletToContainer(
                    dotPageContainer,
                    dotContentlet,
                    this.currentPage
                );
            }),
            // After replace the contentlet, we need to update the tree
            map((html: string) => this.replaceHTMLContentlet(html, contentlet)),
            catchError((error: HttpErrorResponse) => {
                throw this.dotHttpErrorManagerService.handle(error);
            }),
            finalize(() => this.dotLoadingIndicatorService.hide())
        );
    }

    /**
     * Dispatch event to notify that a block editor was clicked
     *
     * @private
     * @param {Event} event
     * @memberof DotEditContentHtmlService
     */
    private onEditBlockEditor(event: Event): void {
        const target = event.target as HTMLElement;
        const contentlet = target.closest('[data-dot-object="contentlet"]') as HTMLElement;
        const container = target.closest('[data-dot-object="container"]') as HTMLElement;
        const isInMultiplePages = this.isContentInMultiplePages(contentlet);

        if (isInMultiplePages) {
            this.showCopyModal(contentlet, container).subscribe((contentlet) => {
                const editor = contentlet.querySelector(
                    '[data-block-editor-content]'
                ) as HTMLElement;
                editor.classList.add('dotcms__inline-edit-field');
                // Add click event to the new block editor in Page
                editor.addEventListener('click', this.onEditBlockEditor.bind(this));
                this.dispatchEditorEvent(editor);
            });

            return;
        }

        this.dispatchEditorEvent(target);
    }

    /**
     * Dispatch event to notify that a block editor was clicked
     *
     * @private
     * @param {*} target
     * @memberof DotEditContentHtmlService
     */
    private dispatchEditorEvent(target: HTMLElement) {
        const customEvent = new CustomEvent('ng-event', {
            detail: { name: 'edit-block-editor', data: target }
        });
        window.top.document.dispatchEvent(customEvent);
    }
}
