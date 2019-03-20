import {
    fromEvent as observableFromEvent,
    of as observableOf,
    Observable,
    Subject,
    Subscription
} from 'rxjs';

import { map, take } from 'rxjs/operators';
import { Injectable, ElementRef } from '@angular/core';

import * as _ from 'lodash';

import { DotContainerContentletService } from '../dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from '../html/dot-dom-html-util.service';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotDragDropAPIHtmlService } from '../html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from '../html/dot-edit-content-toolbar-html.service';
import { DotLayout } from '../../../shared/models/dot-layout.model';
import { DotLayoutColumn } from '../../../shared/models/dot-layout-column.model';
import { DotLayoutRow } from '../../../shared/models/dot-layout-row.model';
import { DotMessageService } from '@services/dot-messages-service';
import { DotPageContainer } from '../../../shared/models/dot-page-container.model';
import { DotPageContent } from '../../../shared/models/dot-page-content.model';
import { getEditPageCss } from '../../shared/iframe-edit-mode.css';
import { GOOGLE_FONTS } from '../html/iframe-edit-mode.js';
import { MODEL_VAR_NAME } from '../html/iframe-edit-mode.js';
import { ContentType } from '../../../../content-types/shared/content-type.model';
import { DotRenderedPageState } from '../../../shared/models/dot-rendered-page-state.model';
import { PageModelChangeEvent, PageModelChangeEventType } from './models';
import {
    DotContentletEventRelocate,
    DotContentletEventSave,
    DotContentletEventSelect,
    DotRelocatePayload
} from './models/dot-contentlets-events.model';

export enum DotContentletAction {
    EDIT,
    ADD
}

interface RenderAddedItemParams {
    event: PageModelChangeEventType;
    item: DotPageContent | ContentType | DotRelocatePayload;
    checkExistFunc: (
        item: DotPageContent | ContentType | DotRelocatePayload,
        containerEL: HTMLElement
    ) => boolean;
    getContent: (
        container: DotPageContainer,
        form: DotPageContent | ContentType | DotRelocatePayload
    ) => Observable<string>;
}

@Injectable()
export class DotEditContentHtmlService {
    contentletEvents$: Subject<
        DotContentletEventRelocate | DotContentletEventSelect | DotContentletEventSave
    > = new Subject();
    currentContainer: DotPageContainer;
    currentContentlet: DotPageContent;
    iframe: ElementRef;
    iframeActions$: Subject<any> = new Subject();
    pageModel$: Subject<PageModelChangeEvent> = new Subject();
    mutationConfig = { attributes: false, childList: true, characterData: false };

    private currentAction: DotContentletAction;
    private rowsMaxHeight: number[] = [];
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
        private dotMessageService: DotMessageService
    ) {
        this.contentletEvents$.subscribe(
            (
                contentletEvent:
                    | DotContentletEventRelocate
                    | DotContentletEventSelect
                    | DotContentletEventSave
            ) => {
                this.handlerContentletEvents(contentletEvent.name)(contentletEvent);
            }
        );

        this.dotMessageService
            .getMessages([
                'editpage.content.add.already.title',
                'editpage.content.add.already.message'
            ])
            .subscribe();

        if (!this.docClickHandlers) {
            this.docClickHandlers = {};
            this.setGlobalClickHandlers();
        }
    }

    /**
     * Load code into iframe
     *
     * @param string editPageHTML
     * @param ElementRef iframeEl
     * @returns Promise<boolean>
     * @memberof DotEditContentHtmlService
     */
    renderPage(pageState: DotRenderedPageState, iframeEl: ElementRef): Promise<boolean> {
        this.remoteRendered = pageState.page.remoteRendered;
        return new Promise((resolve, _reject) => {
            this.iframe = iframeEl;
            const iframeElement = this.getEditPageIframe();

            iframeElement.addEventListener('load', () => {
                iframeElement.contentWindow[MODEL_VAR_NAME] = this.pageModel$;
                iframeElement.contentWindow['contentletEvents'] = this.contentletEvents$;

                this.bindGlobalEvents();

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
    initEditMode(pageState: DotRenderedPageState, iframeEl: ElementRef): void {
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
            `div[data-dot-object="container"][data-dot-identifier="${
                container.identifier
            }"][data-dot-uuid="${container.uuid}"] `,
            `div[data-dot-object="contentlet"][data-dot-inode="${content.inode}"]`
        ].join('');
        const contenletEl = doc.querySelector(selector);
        contenletEl.remove();
        this.pageModel$.next({
            model: this.getContentModel(),
            type: PageModelChangeEventType.REMOVE_CONTENT
        });
    }

    /**
     * Render contentlet in the DOM after edition.
     *
     * @param * contentlet
     * @memberof DotEditContentHtmlService
     */
    renderEditedContentlet(contentlet: DotPageContent): void {
        const doc = this.getEditPageDocument();
        const currentContentlets: HTMLElement[] = Array.from(
            doc.querySelectorAll(
                `div[data-dot-object="contentlet"][data-dot-identifier="${contentlet.identifier}"]`
            )
        );
        if (this.remoteRendered) {
            this.iframeActions$.next({
                name: 'save'
            });
        } else {
            currentContentlets.forEach((currentContentlet: HTMLElement) => {
                contentlet.type = currentContentlet.dataset.dotType;
                const containerEl = <HTMLElement>currentContentlet.parentNode;

                const container: DotPageContainer = {
                    identifier: containerEl.dataset.dotIdentifier,
                    uuid: containerEl.dataset.dotUuid
                };

                this.dotContainerContentletService
                    .getContentletToContainer(container, contentlet)
                    .pipe(take(1))
                    .subscribe((contentletHtml: string) => {
                        const contentletEl: HTMLElement = this.createNewContentletFromString(
                            contentletHtml
                        );
                        containerEl.replaceChild(contentletEl, currentContentlet);
                        this.renderHTMLToContentlet(contentletEl, contentletHtml);
                    });
            });
        }
    }

    /**
     * Render a contrentlet in the DOM after add it
     *
     * @param * contentlet
     * @memberof DotEditContentHtmlService
     */
    renderAddedContentlet(
        contentlet: DotPageContent | DotRelocatePayload,
        eventType: PageModelChangeEventType
    ): void {
        this.renderAddedItem({
            event: eventType,
            item: contentlet,
            checkExistFunc: this.isContentExistInContainer.bind(this),
            getContent: this.dotContainerContentletService.getContentletToContainer.bind(
                this.dotContainerContentletService
            )
        });
    }

    /**
     * Render a form in the DOM after add it
     *
     * @param ContentType form
     * @memberof DotEditContentHtmlService
     */
    renderAddedForm(form: ContentType): Observable<DotPageContainer[]> {
        const doc = this.getEditPageDocument();
        const containerEl: HTMLElement = doc.querySelector(
            [
                'div[data-dot-object="container"]',
                `[data-dot-identifier="${this.currentContainer.identifier}"]`,
                `[data-dot-uuid="${this.currentContainer.uuid}"]`
            ].join()
        );

        if (this.isFormExistInContainer(form, containerEl)) {
            this.showContentAlreadyAddedError();
            return observableOf(null);
        } else {
            return this.dotContainerContentletService
                .getFormToContainer(this.currentContainer, form)
                .pipe(
                    map((response) => {
                        const containers: DotPageContainer[] = this.getContentModel();

                        containers
                            .filter(
                                (container) =>
                                    container.identifier === this.currentContainer.identifier &&
                                    container.uuid === this.currentContainer.uuid
                            )
                            .forEach((container) => {
                                if (!container.contentletsId) {
                                    container.contentletsId = [];
                                }

                                container.contentletsId.push(response.content.identifier);
                            });

                        return containers;
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
     * Set listener for Iframe body changes to change container's height
     *
     * @param DotLayout pageLayout
     * @memberof DotEditContentHtmlService
     */
    setContaintersChangeHeightListener(pageLayout: DotLayout): void {
        const doc = this.getEditPageDocument();
        const target = doc.querySelector('body');
        const debounceContainersHeightChange = _.debounce(
            (layout: DotLayout) => this.setContaintersSameHeight(layout),
            500,
            {
                leading: true
            }
        );
        const observer = new MutationObserver(() => {
            debounceContainersHeightChange(pageLayout);
        });
        observer.observe(target, this.mutationConfig);
    }

    /**
     * Set the same height to containers in the same row
     *
     * @param DotLayout pageLayout
     * @memberof DotEditContentHtmlService
     */
    setContaintersSameHeight(pageLayout: DotLayout): void {
        try {
            const containersLayoutIds = this.getContainersLayoutIds(pageLayout);
            const containerDomElements = this.getContainerDomElements(containersLayoutIds);
            containerDomElements.forEach((containerRow: Array<HTMLElement>) => {
                if (containerRow.length > 1) {
                    let maxHeight = 0;
                    containerRow.forEach((container: HTMLElement) => {
                        maxHeight = maxHeight < container.offsetHeight ? container.offsetHeight : maxHeight;
                    });
                    containerRow.forEach((container: HTMLElement) => {
                        container.style.height = `${maxHeight}px`;
                    });
                }
            });
        } catch (err) {
            console.error(err);
        }

        const body = this.getEditPageDocument().querySelector('body');
        body.style.display = 'none';
        body.style.display = '';
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

    private renderAddedItem(params: RenderAddedItemParams): void {
        const doc = this.getEditPageDocument();
        const containerEl: HTMLElement = doc.querySelector(
            `div[data-dot-object="container"][data-dot-identifier="${
                this.currentContainer.identifier
            }"][data-dot-uuid="${this.currentContainer.uuid}"]`
        );

        if (params.checkExistFunc(params.item, containerEl)) {
            this.showContentAlreadyAddedError();
        } else {
            params
                .getContent(this.currentContainer, params.item)
                .subscribe((contentletHtml: string) => {
                    const contentletEl: HTMLElement = this.createNewContentletFromString(
                        contentletHtml
                    );
                    containerEl.insertAdjacentElement('beforeend', contentletEl);

                    this.renderHTMLToContentlet(contentletEl, contentletHtml);
                    // Update the model with the recently added contentlet
                    this.pageModel$.next({
                        model: this.getContentModel(),
                        type: params.event
                    });
                    this.currentAction = DotContentletAction.EDIT;
                });
        }
    }

    private bindGlobalEvents(): void {
        const doc = this.getEditPageDocument();

        if (this.docClickSubscription) {
            this.docClickSubscription.unsubscribe();
        }

        this.docClickSubscription = observableFromEvent(doc, 'click').subscribe(
            ($event: MouseEvent) => {
                const target = <HTMLElement>$event.target;
                const method = this.docClickHandlers[target.dataset.dotObject];
                if (method) {
                    method(target);
                }

                if (!target.classList.contains('dotedit-menu__button')) {
                    this.closeContainersToolBarMenu();
                }
            }
        );
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

    private getContainerDomElements(
        containersLayoutIds: Array<Array<DotPageContainer>>
    ): HTMLElement[][] {
        const doc = this.getEditPageDocument();

        return containersLayoutIds.map((containerRow: Array<DotPageContainer>, index: number) => {
            this.rowsMaxHeight[index] = 0;
            return containerRow.map((container: DotPageContainer) => {
                const querySelector = [
                    `div[data-dot-object="container"]`,
                    `[data-dot-identifier="${container.identifier}"]`,
                    `[data-dot-uuid="${container.uuid}"]`
                ].join('');
                return doc.querySelector(querySelector);
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

    private isContentExistInContainer(
        contentlet: DotPageContent,
        containerEL: HTMLElement
    ): boolean {
        const contentsSelector = `div[data-dot-object="contentlet"]`;
        const currentContentlets: HTMLElement[] = <HTMLElement[]>(
            Array.from(containerEL.querySelectorAll(contentsSelector).values())
        );
        return currentContentlets.some(
            (contentElement) => contentElement.dataset.dotIdentifier === contentlet.identifier
        );
    }

    private isFormExistInContainer(form: ContentType, containerEL: HTMLElement): boolean {
        const contentsSelector = `div[data-dot-object="contentlet"]`;
        const currentContentlets: HTMLElement[] = <HTMLElement[]>(
            Array.from(containerEL.querySelectorAll(contentsSelector).values())
        );
        return currentContentlets.some(
            (contentElement) => contentElement.dataset.dotContentTypeId === form.id
        );
    }

    private addContentToolBars(): void {
        const doc = this.getEditPageDocument();
        this.dotEditContentToolbarHtmlService.addContainerToolbar(doc);
        this.dotEditContentToolbarHtmlService.addContentletMarkup(doc);
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

    private getNewContentlet(html: string): HTMLElement {
        const doc = this.getEditPageDocument();
        // Add innerHTML to a plain so we can get the HTML nodes later
        const div = doc.createElement('div');
        div.innerHTML = html;

        return <HTMLElement>div.children[0];
    }

    private appendNewContentlets(contentletEl: HTMLElement, html: string): void {
        this.removeLoadingIndicator(contentletEl);

        const newContentlet = this.getNewContentlet(html);

        this.dotEditContentToolbarHtmlService.addToolbarToContentlet(newContentlet);

        let scriptTags: HTMLScriptElement[] = [];
        scriptTags = this.getScriptTags(scriptTags, newContentlet);

        scriptTags.forEach((script: HTMLScriptElement) => {
            newContentlet.appendChild(script);
        });

        contentletEl.parentNode.replaceChild(newContentlet, contentletEl);
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
        const newContentlet = this.getNewContentlet(contentletHTML);
        const { identifier, inode, type, baseType } = newContentlet.dataset;

        return this.createNewContentlet({
            identifier,
            inode,
            type,
            baseType
        });
    }

    private createNewContentlet(dotPageContent: DotPageContent): HTMLElement {
        const doc = this.getEditPageDocument();

        const dotEditContentletEl: HTMLElement = doc.createElement('div');
        Object.assign(dotEditContentletEl.dataset, dotPageContent);

        /*
            TODO: we have the method: DotEditContentToolbarHtmlService.addContentletMarkup that does this, we need
            to consolidate this.
        */
        const contenToolbarButtons = this.dotEditContentToolbarHtmlService.getContentButton(
            dotPageContent
        );

        dotEditContentletEl.innerHTML = `
            <div class="dotedit-contentlet__toolbar">
                ${contenToolbarButtons}
            </div>
            <div class="loader__overlay">
                <div class="loader"></div>
            </div>
        `;

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

    private handlerContentletEvents(event: string): (contentletEvent: any) => void {
        const contentletEventsMap = {
            // When an user create or edit a contentlet from the jsp
            save: (contentletEvent: DotContentletEventSave) => {
                if (this.currentAction === DotContentletAction.ADD) {
                    this.renderAddedContentlet(
                        contentletEvent.data,
                        PageModelChangeEventType.ADD_CONTENT
                    );
                } else {
                    if (this.updateContentletInode) {
                        this.currentContentlet.inode = contentletEvent.data.inode;
                    }
                    this.renderEditedContentlet(this.currentContentlet);
                }
            },
            // When a user select a content from the search jsp
            select: (contentletEvent: DotContentletEventSelect) => {
                this.renderAddedContentlet(
                    contentletEvent.data,
                    PageModelChangeEventType.EDIT_CONTENT
                );
                this.iframeActions$.next({
                    name: 'select'
                });
            },
            // When a user drag and drop a contentlet in the iframe
            relocate: (contentletEvent: DotContentletEventRelocate) => {
                if (!this.remoteRendered) {
                    this.renderRelocatedContentlet(contentletEvent.data);
                }
            },
            'deleted-contenlet': () => {
                this.removeCurrentContentlet();
            }
        };

        return contentletEventsMap[event];
    }

    private shouldUpdateContentletInode(target: HTMLElement) {
        return target.dataset.dotObject === 'edit-content' && target.tagName === 'BUTTON';
    }

    private updateHtml(pageState: DotRenderedPageState): string {
        const fakeHtml = document.createElement('html');
        fakeHtml.innerHTML = pageState.html;

        if (fakeHtml.querySelector('base')) {
            return pageState.html;
        } else {
            const head = fakeHtml.querySelector('head');
            head.insertBefore(this.getBaseTag(pageState.page.pageURI), head.childNodes[0]);
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

    private loadCodeIntoIframe(pageState: DotRenderedPageState): void {
        const doc = this.getEditPageDocument();
        doc.open();
        doc.write(this.updateHtml(pageState));
        doc.close();
    }

    private setEditContentletStyles(): void {
        const timeStampId = `iframeId_${Math.floor(Date.now() / 100).toString()}`;
        const style = this.dotDOMHtmlUtilService.createStyleElement(
            getEditPageCss(`#${timeStampId}`)
        );
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
        const contentletToolbarEl = contentletEl.querySelector('.dotedit-contentlet__toolbar');

        const vtls: HTMLElement[] = Array.from(
            contentletEl.querySelectorAll('div[data-dot-object="vtl-file"]')
        );

        if (vtls.length) {
            contentletToolbarEl.innerHTML = `
                ${this.dotEditContentToolbarHtmlService.getEditVtlButtons(vtls)}
                ${contentletToolbarEl.innerHTML}
            `;
        }
    }

    private removeCurrentContentlet(): void {
        const doc = this.getEditPageDocument();
        const contentlets = doc.querySelectorAll(
            `div[data-dot-object="contentlet"][data-dot-inode="${this.currentContentlet.inode}"]`
        );

        contentlets.forEach((contentlet) => {
            contentlet.remove();
        });
    }

    private renderHTMLToContentlet(contentletEl: HTMLElement, contentletHtml: string): void {
        this.appendNewContentlets(contentletEl, contentletHtml);

        this.addVtlEditMenu(contentletEl);
    }

    private renderRelocatedContentlet(relocateInfo: DotRelocatePayload): void {
        const doc = this.getEditPageDocument();
        const contenletEl: HTMLElement = doc.querySelector(
            `div[data-dot-object="contentlet"][data-dot-inode="${relocateInfo.contentlet.inode}"]`
        );

        this.addLoadingIndicator(contenletEl);

        const container: HTMLElement = <HTMLElement>contenletEl.parentNode;

        relocateInfo.container = relocateInfo.container || {
            identifier: container.dataset.dotIdentifier,
            uuid: container.dataset.dotUuid
        };

        this.dotContainerContentletService
            .getContentletToContainer(relocateInfo.container, relocateInfo.contentlet)
            .subscribe((contentletHtml: string) => {
                this.appendNewContentlets(contenletEl, contentletHtml);
            });
    }

    private addLoadingIndicator(contentlet: HTMLElement): void {
        contentlet.insertAdjacentElement('afterbegin', this.getLoadingIndicator());
    }

    private getLoadingIndicator(): HTMLElement {
        const div = document.createElement('div');
        div.innerHTML = `
            <div class="loader__overlay">
                <div class="loader"></div>
            </div>
        `;

        return div;
    }

    private removeLoadingIndicator(contentlet: HTMLElement): void {
        contentlet.querySelector('.loader__overlay').remove();
    }
}
