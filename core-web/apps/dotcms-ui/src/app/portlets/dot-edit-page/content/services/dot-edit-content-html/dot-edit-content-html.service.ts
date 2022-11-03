import { fromEvent, of, Observable, Subject, Subscription } from 'rxjs';

import { filter, map, take } from 'rxjs/operators';
import { Injectable, ElementRef, NgZone } from '@angular/core';

import { DotContainerContentletService } from '../dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from '../html/dot-dom-html-util.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotDragDropAPIHtmlService } from '../html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from '../html/dot-edit-content-toolbar-html.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPageContent, DotPageRenderState } from '@portlets/dot-edit-page/shared/models';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotWorkflowActionsFireService } from '@services/dot-workflow-actions-fire/dot-workflow-actions-fire.service';
import { getEditPageCss } from '../html/libraries/iframe-edit-mode.css';
import { MODEL_VAR_NAME } from '@dotcms/app/portlets/dot-edit-page/content/services/html/libraries/iframe-edit-mode.js';
import { PageModelChangeEvent, PageModelChangeEventType } from './models';
import {
    DotAssetPayload,
    DotContentletEvent,
    DotContentletEventDragAndDropDotAsset,
    DotContentletEventRelocate,
    DotContentletEventSave,
    DotContentletEventSelect,
    DotInlineEditContent,
    DotRelocatePayload
} from './models/dot-contentlets-events.model';
import { DotPageContainer } from '@models/dot-page-container/dot-page-container.model';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { INLINE_TINYMCE_SCRIPTS } from '@dotcms/app/portlets/dot-edit-page/content/services/html/libraries/inline-edit-mode.js';
import { HttpErrorResponse } from '@angular/common/http';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotPage } from '@dotcms/app/shared/models/dot-page/dot-page.model';
import { DotAddContentTypePayload } from './models/dot-contentlets-events.model';
import { DotIframeEditEvent } from '@dotcms/dotcms-models';
export enum DotContentletAction {
    EDIT,
    ADD
}

export const CONTENTLET_PLACEHOLDER_SELECTOR = '#contentletPlaceholder';

@Injectable()
export class DotEditContentHtmlService {
    contentletEvents$: Subject<
        | DotContentletEventDragAndDropDotAsset
        | DotContentletEventRelocate
        | DotContentletEventSelect
        | DotContentletEventSave
        | DotContentletEvent<DotInlineEditContent>
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

    private inlineCurrentContent: { [key: string]: string } = {};
    private currentAction: DotContentletAction;
    private docClickSubscription: Subscription;
    private updateContentletInode = false;
    private remoteRendered: boolean;

    private readonly docClickHandlers;

    constructor(
        private dotContainerContentletService: DotContainerContentletService,
        private dotDragDropAPIHtmlService: DotDragDropAPIHtmlService,
        private dotEditContentToolbarHtmlService: DotEditContentToolbarHtmlService,
        private dotDOMHtmlUtilService: DotDOMHtmlUtilService,
        private dotDialogService: DotAlertConfirmService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotMessageService: DotMessageService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotWorkflowActionsFireService: DotWorkflowActionsFireService,
        private ngZone: NgZone,
        private dotLicenseService: DotLicenseService
    ) {
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
                iframeElement.contentWindow[MODEL_VAR_NAME] = this.pageModel$;
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

                const container: DotPageContainer = {
                    identifier: containerEl.dataset.dotIdentifier,
                    uuid: containerEl.dataset.dotUuid
                };

                this.dotContainerContentletService
                    .getContentletToContainer(container, contentlet, this.currentPage)
                    .pipe(take(1))
                    .subscribe((contentletHtml: string) => {
                        const contentletEl: HTMLElement =
                            this.generateNewContentlet(contentletHtml);
                        containerEl.replaceChild(contentletEl, currentContentlet);
                    });
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
            let contentletPlaceholder = doc.querySelector(CONTENTLET_PLACEHOLDER_SELECTOR);
            if (!contentletPlaceholder) {
                contentletPlaceholder = this.getContentletPlaceholder();
                containerEl.appendChild(contentletPlaceholder);
            }

            this.dotContainerContentletService
                .getContentletToContainer(this.currentContainer, contentlet, this.currentPage)
                .pipe(take(1))
                .subscribe((contentletHtml: string) => {
                    const contentletEl: HTMLElement = this.generateNewContentlet(contentletHtml);
                    containerEl.replaceChild(contentletEl, contentletPlaceholder);
                    // Update the model with the recently added contentlet
                    this.pageModel$.next({
                        model: this.getContentModel(),
                        type: PageModelChangeEventType.ADD_CONTENT
                    });
                    this.currentAction = DotContentletAction.EDIT;
                    this.updateContainerToolbar(containerEl.dataset.dotIdentifier);
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
    renderAddedForm(formId: string, isDroppedForm = false): Observable<DotPageContainer[]> {
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

            return of(null);
        } else {
            let contentletPlaceholder = doc.querySelector(CONTENTLET_PLACEHOLDER_SELECTOR);

            if (!contentletPlaceholder) {
                contentletPlaceholder = this.getContentletPlaceholder();
                containerEl.appendChild(contentletPlaceholder);
            }

            return this.dotContainerContentletService
                .getFormToContainer(this.currentContainer, formId)
                .pipe(
                    map(({ content }: { content: { [key: string]: string } }) => {
                        const { identifier, inode } = content;

                        containerEl.replaceChild(
                            this.renderFormContentlet(identifier, inode),
                            contentletPlaceholder
                        );

                        return this.getContentModel();
                    })
                );
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
    getContentModel(): DotPageContainer[] {
        return this.getEditPageIframe().contentWindow['getDotNgModel']();
    }

    private setMaterialIcons(): void {
        const doc = this.getEditPageDocument();
        const link = this.dotDOMHtmlUtilService.createLinkElement(
            'dotAdmin/assets/material-icons.css'
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
                method(target);
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
            const TINYMCE = `/html/js/tinymce/js/tinymce/tinymce.min.js`;
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
                            const customEvent = new CustomEvent('ng-event', {
                                detail: { name: 'edit-block-editor', data: event.target }
                            });
                            window.top.document.dispatchEvent(customEvent);
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

        const container = <HTMLElement>target.closest('[data-dot-object="container"]');
        this.iframeActions$.next({
            name: type,
            dataset: target.dataset,
            container: container ? container.dataset : null
        });
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
                if (this.currentAction === DotContentletAction.ADD) {
                    this.renderAddedContentlet(contentlet);
                } else {
                    if (this.updateContentletInode) {
                        this.currentContentlet.inode = contentlet.inode;
                    }
                    // because: https://github.com/dotCMS/core/issues/21818

                    setTimeout(() => {
                        this.renderEditedContentlet(this.currentContentlet);
                    }, 1800);
                }
            },
            inlineEdit: (contentlet: DotInlineEditContent) => {
                if (contentlet.eventType === 'focus') {
                    this.handleTinyMCEOnFocusEvent(contentlet);
                }

                if (contentlet.eventType === 'blur') {
                    this.handleTinyMCEOnBlurEvent(contentlet);
                }
            },
            // When a user select a content from the search jsp
            select: (contentlet: DotPageContent) => {
                this.renderAddedContentlet(contentlet);
                this.iframeActions$.next({
                    name: 'select'
                });
            },
            // When a user drag and drop a contentlet in the iframe
            relocate: (relocateInfo: DotRelocatePayload) => {
                if (!this.remoteRendered) {
                    this.renderRelocatedContentlet(relocateInfo);
                }
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
                this.renderAddedForm(formId, true)
                    .pipe(take(1))
                    .subscribe((model: DotPageContainer[]) => {
                        if (model) {
                            this.pageModel$.next({
                                model: model,
                                type: PageModelChangeEventType.ADD_CONTENT
                            });
                            this.iframeActions$.next({
                                name: 'save'
                            });
                        }
                    });
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

        base.href = href.join('/') + '/';

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
            getEditPageCss(`#${timeStampId}`)
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
            .subscribe((contentletHtml: string) => {
                const newContentletEl: HTMLElement = this.generateNewContentlet(contentletHtml);
                container.replaceChild(newContentletEl, contenletEl);
            });
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
}
