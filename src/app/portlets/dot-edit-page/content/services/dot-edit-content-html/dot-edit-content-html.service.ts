import { Injectable, ElementRef } from '@angular/core';

import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { Subscription } from 'rxjs/Subscription';
import { take } from 'rxjs/operators/take';

import * as _ from 'lodash';

import { DotContainerContentletService } from '../dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from '../html/dot-dom-html-util.service';
import { DotAlertConfirmService } from '../../../../../api/services/dot-alert-confirm/dot-alert-confirm.service';
import { DotDragDropAPIHtmlService } from '../html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from '../html/dot-edit-content-toolbar-html.service';
import { DotLayout } from '../../../shared/models/dot-layout.model';
import { DotLayoutColumn } from '../../../shared/models/dot-layout-column.model';
import { DotLayoutRow } from '../../../shared/models/dot-layout-row.model';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DotPageContainer } from '../../../shared/models/dot-page-container.model';
import { DotPageContent } from '../../../shared/models/dot-page-content.model';
import { getEditPageCss } from '../../shared/iframe-edit-mode.css';
import { GOOGLE_FONTS } from '../html/iframe-edit-mode.js';
import { MODEL_VAR_NAME } from '../html/iframe-edit-mode.js';
import { ContentType } from '../../../../content-types/shared/content-type.model';

export enum DotContentletAction {
    EDIT,
    ADD
}

interface RenderAddedItemParams {
    item: DotPageContent | ContentType;
    checkExistFunc: (item: DotPageContent | ContentType, containerEL: HTMLElement) => boolean;
    getContent: (container: DotPageContainer, form: DotPageContent | ContentType) => Observable<string>;
}

@Injectable()
export class DotEditContentHtmlService {
    contentletEvents$: Subject<any> = new Subject();
    currentContainer: DotPageContainer;
    currentContentlet: DotPageContent;
    iframe: ElementRef;
    iframeActions$: Subject<any> = new Subject();
    pageModel$: Subject<DotPageContainer[]> = new Subject();
    mutationConfig = { attributes: false, childList: true, characterData: false };

    private currentAction: DotContentletAction;
    private rowsMaxHeight: number[] = [];
    private docClickSubscription: Subscription;
    private updateContentletInode = false;

    private readonly docClickHandlers;

    constructor(
        private dotContainerContentletService: DotContainerContentletService,
        private dotDragDropAPIHtmlService: DotDragDropAPIHtmlService,
        private dotEditContentToolbarHtmlService: DotEditContentToolbarHtmlService,
        private dotDOMHtmlUtilService: DotDOMHtmlUtilService,
        private dotDialogService: DotAlertConfirmService,
        private dotMessageService: DotMessageService
    ) {
        this.contentletEvents$.subscribe((contentletEvent: any) => {
            this.handlerContentletEvents(contentletEvent.name)(contentletEvent);
        });

        this.dotMessageService.getMessages(['editpage.content.add.already.title', 'editpage.content.add.already.message']).subscribe();

        if (!this.docClickHandlers) {
            this.docClickHandlers = {};
            this.setGlobalClickHandlers();
        }
    }

    /**
     * Load code into iframe
     *
     * @param {string} editPageHTML
     * @param {ElementRef} iframeEl
     * @returns {Promise<any>}
     * @memberof DotEditContentHtmlService
     */
    renderPage(editPageHTML: string, iframeEl: ElementRef): Promise<any> {
        return new Promise((resolve, _reject) => {
            this.iframe = iframeEl;
            const iframeElement = this.getEditPageIframe();

            iframeElement.addEventListener('load', () => {
                iframeElement.contentWindow[MODEL_VAR_NAME] = this.pageModel$;
                iframeElement.contentWindow.contentletEvents = this.contentletEvents$;

                this.bindGlobalEvents();

                resolve(true);
            });
            // Load content after bind 'load' event.
            this.loadCodeIntoIframe(editPageHTML);
        });
    }

    /**
     * Initalize edit content mode
     *
     * @param {string} editPageHTML
     * @param {ElementRef} iframeEl
     * @memberof DotEditContentHtmlService
     */
    initEditMode(editPageHTML: string, iframeEl: ElementRef): void {
        this.renderPage(editPageHTML, iframeEl).then(() => {
            this.setEditMode();
        });
    }

    /**
     * Remove a contentlet from the DOM by inode and update the page model
     *
     * @param {string} inode
     * @memberof DotEditContentHtmlService
     */
    removeContentlet(container: DotPageContainer, content: DotPageContent): void {
        const doc = this.getEditPageDocument();
        const selector = [
            `div[data-dot-object="container"][data-dot-identifier="${container.identifier}"][data-dot-uuid="${container.uuid}"] `,
            `div[data-dot-object="contentlet"][data-dot-inode="${content.inode}"]`
        ].join('');
        const contenletEl = doc.querySelector(selector);
        contenletEl.remove();
        this.pageModel$.next(this.getContentModel());
    }

    /**
     * Render contentlet in the DOM after edition.
     *
     * @param {*} contentlet
     * @memberof DotEditContentHtmlService
     */
    renderEditedContentlet(contentlet: DotPageContent): void {
        const doc = this.getEditPageDocument();
        const currentContentlets = doc.querySelectorAll(
            `div[data-dot-object="contentlet"][data-dot-identifier="${contentlet.identifier}"]`
        );

        currentContentlets.forEach((currentContentlet) => {
            contentlet.type = currentContentlet.dataset.dotType;

            const containerEl = currentContentlet.parentNode;

            const container: DotPageContainer = {
                identifier: containerEl.dataset.dotIdentifier,
                uuid: containerEl.dataset.dotUuid
            };

            this.dotContainerContentletService
                .getContentletToContainer(container, contentlet)
                .pipe(take(1))
                .subscribe((contentletHtml: string) => {
                    const contentletEl: HTMLElement = this.createNewContentletFromString(contentletHtml);
                    this.renderHTMLToContentlet(contentletEl, contentletHtml);
                    containerEl.replaceChild(contentletEl, currentContentlet);
                });
        });
    }

    /**
     * Render a contrentlet in the DOM after add it
     *
     * @param {*} contentlet
     * @memberof DotEditContentHtmlService
     */
    renderAddedContentlet(contentlet: DotPageContent): void {
        this.renderAddedItem({
            item: contentlet,
            checkExistFunc: this.isContentExistInContainer.bind(this),
            getContent: this.dotContainerContentletService.getContentletToContainer.bind(this.dotContainerContentletService)
        });
    }

    /**
     * Render a form in the DOM after add it
     *
     * @param {ContentType} form
     * @memberof DotEditContentHtmlService
     */
    renderAddedForm(form: ContentType): void {
        this.renderAddedItem({
            item: form,
            checkExistFunc: this.isFormExistInContainer.bind(this),
            getContent: this.dotContainerContentletService.getFormToContainer.bind(this.dotContainerContentletService)
        });
    }

    /**
     * Set the container id where a contentlet will be added
     *
     * @param {string} identifier
     * @memberof DotEditContentHtmlService
     */
    setContainterToAppendContentlet(pageContainer: DotPageContainer): void {
        this.currentContainer = pageContainer;
        this.currentAction = DotContentletAction.ADD;
    }

    /**
     * Set listener for Iframe body changes to change container's height
     *
     * @param {DotLayout} pageLayout
     * @memberof DotEditContentHtmlService
     */
    setContaintersChangeHeightListener(pageLayout: DotLayout): void {
        const doc = this.getEditPageDocument();
        const target = doc.querySelector('body');
        const debounceContainersHeightChange = _.debounce((layout: DotLayout) => this.setContaintersSameHeight(layout), 500, {
            leading: true
        });
        const observer = new MutationObserver(() => {
            debounceContainersHeightChange(pageLayout);
        });
        observer.observe(target, this.mutationConfig);
    }

    /**
     * Set the same height to containers in the same row
     *
     * @param {DotLayout} pageLayout
     * @memberof DotEditContentHtmlService
     */
    setContaintersSameHeight(pageLayout: DotLayout): void {
        const containersLayoutIds = this.getContainersLayoutIds(pageLayout);
        const containerDomElements = this.getContainerDomElements(containersLayoutIds);

        containerDomElements.forEach((containerRow: Array<HTMLElement>, index: number) => {
            if (containerRow.length > 1) {
                containerRow.forEach((container: HTMLElement) => {
                    container.style.height = `${this.rowsMaxHeight[index]}px`;
                });
            }
        });

        const body = this.getEditPageDocument().querySelector('body');
        body.style.display = 'none';
        body.style.display = '';
    }

    /**
     * Return the page model
     *
     * @returns {*}
     * @memberof DotEditContentHtmlService
     */
    getContentModel(): DotPageContainer[] {
        return this.getEditPageIframe().contentWindow.getDotNgModel();
    }

    private renderAddedItem(params: RenderAddedItemParams): void {
        const doc = this.getEditPageDocument();
        const containerEl = doc.querySelector(
            // tslint:disable-next-line:max-line-length
            `div[data-dot-object="container"][data-dot-identifier="${this.currentContainer.identifier}"][data-dot-uuid="${
                this.currentContainer.uuid
            }"]`
        );

        if (params.checkExistFunc(params.item, containerEl)) {
            this.showContentAlreadyAddedError();
        } else {
            params.getContent(this.currentContainer, params.item).subscribe((contentletHtml: string) => {
                const contentletEl: HTMLElement = this.createNewContentletFromString(contentletHtml);
                containerEl.insertAdjacentElement('beforeend', contentletEl);

                this.renderHTMLToContentlet(contentletEl, contentletHtml);
                // Update the model with the recently added contentlet
                this.pageModel$.next(this.getContentModel());
                this.currentAction = DotContentletAction.EDIT;
            });
        }
    }

    private bindGlobalEvents(): void {
        const doc = this.getEditPageDocument();

        if (this.docClickSubscription) {
            this.docClickSubscription.unsubscribe();
        }

        this.docClickSubscription = Observable.fromEvent(doc, 'click').subscribe(($event: MouseEvent) => {
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
        const contentlet = <HTMLElement>target.closest('div[data-dot-object="contentlet"]');
        return {
            identifier: contentlet.dataset.dotIdentifier,
            inode: contentlet.dataset.dotInode,
            type: contentlet.dataset.dotType,
            baseType: contentlet.dataset.dotBasetype
        };
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

    private getContainersLayoutIds(pageLayout: DotLayout): Array<Array<DotPageContainer>> {
        return pageLayout.body.rows.map((row: DotLayoutRow) => {
            return row.columns.map((column: DotLayoutColumn) => {
                return {
                    identifier: column.containers[0].identifier,
                    uuid: column.containers[0].uuid
                };
            });
        });
    }

    private getContainerDomElements(containersLayoutIds: Array<Array<DotPageContainer>>): Array<Array<HTMLElement>> {
        const doc = this.getEditPageDocument();

        return containersLayoutIds.map((containerRow: Array<DotPageContainer>, index: number) => {
            this.rowsMaxHeight[index] = 0;
            return containerRow.map((container: DotPageContainer) => {
                const querySelector = [
                    `div[data-dot-object="container"]`,
                    `[data-dot-identifier="${container.identifier}"]`,
                    `[data-dot-uuid="${container.uuid}"]`
                ].join('');
                const containerElement = doc.querySelector(querySelector);
                containerElement.style.height = 'auto';
                this.rowsMaxHeight[index] =
                    containerElement.offsetHeight > this.rowsMaxHeight[index] ? containerElement.offsetHeight : this.rowsMaxHeight[index];
                return containerElement;
            });
        });
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

    private isContentExistInContainer(contentlet: DotPageContent, containerEL: HTMLElement): boolean {
        const contentsSelector = `div[data-dot-object="contentlet"]`;
        const currentContentlets: HTMLElement[] = <HTMLElement[]>Array.from(containerEL.querySelectorAll(contentsSelector).values());
        return currentContentlets.some((contentElement) => contentElement.dataset.dotIdentifier === contentlet.identifier);
    }

    private isFormExistInContainer(form: ContentType, containerEL: HTMLElement): boolean {
        const contentsSelector = `div[data-dot-object="contentlet"]`;
        const currentContentlets: HTMLElement[] = <HTMLElement[]>Array.from(containerEL.querySelectorAll(contentsSelector).values());
        return currentContentlets.some((contentElement) => contentElement.dataset.dotContentTypeId === form.id);
    }

    private addContentToolBars(): void {
        const doc = this.getEditPageDocument();
        this.dotEditContentToolbarHtmlService.addContainerToolbar(doc);
        this.dotEditContentToolbarHtmlService.addContentletMarkup(doc);
    }

    private createScriptTag(node: any): HTMLScriptElement {
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

    private getScriptTags(scriptTags, contentDivWrapper: HTMLElement): HTMLScriptElement[] {
        Array.from(contentDivWrapper.children).forEach((node: any) => {
            if (node.tagName === 'SCRIPT') {
                const script = this.createScriptTag(node);
                scriptTags.push(script);
            } else if (node.children.length) {
                this.getScriptTags(scriptTags, node);
            }
        });

        return scriptTags;
    }

    private appendNewContentlets(contentletEl: any, html: string): void {

        const contentletContentEl = contentletEl.querySelector('.dotedit-contentlet__content');
        contentletContentEl.innerHTML = ''; // Removing the loading indicator
        let scriptTags: HTMLScriptElement[] = [];
        const doc = this.getEditPageDocument();

        // Add innerHTML to a plain so we can get the HTML nodes later
        const div = doc.createElement('div');
        div.innerHTML = html;

        const contentDivWrapper = div.children[0];

        scriptTags = this.getScriptTags(scriptTags, contentDivWrapper);

        scriptTags.forEach((script: HTMLScriptElement) => {
            contentletContentEl.appendChild(script);
        });

        // TODO: need to come up with a more efficient way to do this
        Array.from(contentDivWrapper.children).forEach((node: any) => {
            if (node.tagName !== 'SCRIPT') {
                contentletContentEl.appendChild(node);
            }
        });
    }

    private buttonClickHandler(target: HTMLElement, type: string) {
        this.updateContentletInode = this.shouldUpdateContentletInode(target);

        const container = <HTMLElement>target.closest('div[data-dot-object="container"]');

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

    private createNewContentletFromString(contentletHTML: string): HTMLElement {
        const doc = this.getEditPageDocument();

        const div = doc.createElement('div');
        div.innerHTML = contentletHTML;

        return this.createNewContentlet(div.children[0].dataset);
    }

    private createNewContentlet(dotPageContent: DotPageContent): HTMLElement {
        const doc = this.getEditPageDocument();

        const dotEditContentletEl: HTMLElement = doc.createElement('div');
        Object.assign(dotEditContentletEl.dataset, dotPageContent);

        /*
            TODO: we have the method: DotEditContentToolbarHtmlService.addContentletMarkup that does this, we need
            to consolidate this.
        */
        const contenToolbarButtons = this.dotEditContentToolbarHtmlService.getContentButton(dotPageContent);

        dotEditContentletEl.innerHTML = `
            <div class="dotedit-contentlet__toolbar">
                ${contenToolbarButtons}
            </div>
            <div class="dotedit-contentlet__content">
                <div class="loader__overlay">
                    <div class="loader"></div>
                </div>
            </div>
        `;

        return dotEditContentletEl;
    }

    private getEditPageIframe(): any {
        return this.iframe.nativeElement;
    }

    private getEditPageDocument(): any {
        return this.getEditPageIframe().contentDocument || this.getEditPageIframe().contentWindow.document;
    }

    private handlerContentletEvents(event: string): (contentletEvent: any) => void {
        const contentletEventsMap = {
            // When an user create or edit a contentlet from the jsp
            'save': (contentletEvent: any) => {
                if (this.currentAction === DotContentletAction.ADD) {
                    this.renderAddedContentlet(contentletEvent.data);
                } else {
                    if (this.updateContentletInode) {
                        this.currentContentlet.inode = contentletEvent.data.inode;
                    }
                    this.renderEditedContentlet(this.currentContentlet);
                }
            },
            // When a user select a content from the search jsp
            'select': (contentletEvent: any) => {
                this.renderAddedContentlet(contentletEvent.data);
                this.iframeActions$.next({
                    name: 'select'
                });
            },
            // When a user drag and drop a contentlet in the iframe
            'relocate': (contentletEvent: any) => {
                this.renderRelocatedContentlet(contentletEvent.data);
            },
            'deleted-contenlet': (contentletEvent: any) => {
                this.removeCurrentContentlet();
            }
        };

        return contentletEventsMap[event];
    }

    private shouldUpdateContentletInode(target: HTMLElement) {
        return target.dataset.dotObject === 'edit-content' && target.tagName === 'BUTTON';
    }

    private loadCodeIntoIframe(editPageHTML: string): void {
        const doc = this.getEditPageDocument();
        doc.open();
        doc.write(editPageHTML);
        doc.close();
    }

    private setEditContentletStyles(): void {
        const timeStampId = `iframeId_${Math.floor(Date.now() / 100).toString()}`;
        const style = this.dotDOMHtmlUtilService.createStyleElement(getEditPageCss(`#${timeStampId}`));
        const robotoFontElement = this.dotDOMHtmlUtilService.createLinkElement(GOOGLE_FONTS);

        const doc = this.getEditPageDocument();
        doc.documentElement.id = timeStampId;
        doc.head.appendChild(style);
        doc.head.appendChild(robotoFontElement);
    }

    private setEditMode(): void {
        this.addContentToolBars();

        this.dotDragDropAPIHtmlService.initDragAndDropContext(this.getEditPageIframe());
        this.setEditContentletStyles();
    }

    private addVtlEditMenu(contentletEl: HTMLElement): void {
        const contentletContentEl = contentletEl.querySelector('.dotedit-contentlet__content');
        const contentletToolbarEl = contentletEl.querySelector('.dotedit-contentlet__toolbar');

        const vtls = Array.from(contentletContentEl.querySelectorAll('div[data-dot-object="vtl-file"]'));

        if (vtls.length) {
            contentletToolbarEl.innerHTML = `
                ${this.dotEditContentToolbarHtmlService.getEditVtlButtons(vtls)}
                ${contentletToolbarEl.innerHTML}
            `;
        }
    }

    private removeCurrentContentlet(): void {
        const doc = this.getEditPageDocument();
        const contentlets = doc.querySelectorAll(`div[data-dot-object="contentlet"][data-dot-inode="${this.currentContentlet.inode}"]`);

        contentlets.forEach(contentlet => {
            contentlet.remove();
        });
    }

    private renderHTMLToContentlet(contentletEl: HTMLElement, contentletHtml: string): void {
        this.appendNewContentlets(contentletEl, contentletHtml);

        this.addVtlEditMenu(contentletEl);
    }

    private renderRelocatedContentlet(relocateInfo: any): void {
        const doc = this.getEditPageDocument();
        const contenletEl = doc.querySelector(`div[data-dot-object="contentlet"][data-dot-inode="${relocateInfo.contentlet.inode}"]`);
        const contentletContentEl = contenletEl.querySelector('.dotedit-contentlet__content');
        contentletContentEl.innerHTML += '<div class="loader__overlay"><div class="loader"></div></div>';
        relocateInfo.container = relocateInfo.container || contenletEl.parentNode.dataset.dotIdentifier;

        this.dotContainerContentletService
            .getContentletToContainer(relocateInfo.container, relocateInfo.contentlet)
            .subscribe((contentletHtml: string) => {
                this.appendNewContentlets(contenletEl, contentletHtml);
            });
    }
}
