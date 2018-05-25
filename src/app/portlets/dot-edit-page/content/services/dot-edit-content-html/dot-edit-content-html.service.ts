import * as _ from 'lodash';
import { LoggerService } from 'dotcms-js/dotcms-js';
import { Injectable, ElementRef } from '@angular/core';
import { EDIT_PAGE_CSS } from '../../shared/iframe-edit-mode.css';
import { DotContainerContentletService } from '../dot-container-contentlet.service';
import { DotDragDropAPIHtmlService } from '../html/dot-drag-drop-api-html.service';
import { GOOGLE_FONTS } from '../html/iframe-edit-mode.js';
import { DotEditContentToolbarHtmlService } from '../html/dot-edit-content-toolbar-html.service';
import { DotDOMHtmlUtilService } from '../html/dot-dom-html-util.service';
import { MODEL_VAR_NAME } from '../html/iframe-edit-mode.js';
import { Subject } from 'rxjs/Subject';
import { DotPageContainer } from '../../../shared/models/dot-page-container.model';
import { DotPageContent } from '../../../shared/models/dot-page-content.model';
import { DotDialogService } from '../../../../../api/services/dot-dialog/dot-dialog.service';
import { DotMessageService } from '../../../../../api/services/dot-messages-service';
import { DotLayout } from '../../../shared/models/dot-layout.model';
import { DotLayoutColumn } from '../../../shared/models/dot-layout-column.model';
import { DotLayoutRow } from '../../../shared/models/dot-layout-row.model';
import { take } from 'rxjs/operators/take';

export enum DotContentletAction {
    EDIT,
    ADD
}

@Injectable()
export class DotEditContentHtmlService {
    contentletEvents$: Subject<any> = new Subject();
    currentContainer: DotPageContainer;
    currentContentlet: DotPageContent;
    iframe: ElementRef;
    iframeActions$: Subject<any> = new Subject();
    pageModel$: Subject<DotPageContainer[]> = new Subject();

    private currentAction: DotContentletAction;
    private rowsMaxHeight: number[] = [];

    constructor(
        private dotContainerContentletService: DotContainerContentletService,
        private dotDragDropAPIHtmlService: DotDragDropAPIHtmlService,
        private dotEditContentToolbarHtmlService: DotEditContentToolbarHtmlService,
        private dotDOMHtmlUtilService: DotDOMHtmlUtilService,
        private loggerService: LoggerService,
        private dotDialogService: DotDialogService,
        private dotMessageService: DotMessageService
    ) {
        this.contentletEvents$.subscribe((contentletEvent: any) => {
            this.handlerContentletEvents(contentletEvent.name)(contentletEvent);
        });

        this.dotMessageService.getMessages(['editpage.content.add.already.title', 'editpage.content.add.already.message']).subscribe();
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

                iframeElement.contentWindow.addEventListener('click', (event: MouseEvent) => {
                    this.handleIframeClicks(<HTMLElement>event.target);
                });

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
            const contentletEl: HTMLElement = this.createNewContentlet(contentlet);

            containerEl.replaceChild(contentletEl, currentContentlet);

            const container: DotPageContainer = {
                identifier: containerEl.dataset.dotIdentifier,
                uuid: containerEl.dataset.dotUuid
            };

            this.dotContainerContentletService
                .getContentletToContainer(container, contentlet)
                .pipe(take(1))
                .subscribe((contentletHtml: string) => {
                    this.renderHTMLToContentlet(contentletEl, contentletHtml);
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
        const doc = this.getEditPageDocument();
        const containerEl = doc.querySelector(
            // tslint:disable-next-line:max-line-length
            `div[data-dot-object="container"][data-dot-identifier="${this.currentContainer.identifier}"][data-dot-uuid="${
                this.currentContainer.uuid
            }"]`
        );

        if (this.isContentExistInContainer(contentlet, containerEl)) {
            this.showContentAlreadyAddedError();
        } else {
            const contentletEl: HTMLElement = this.createNewContentlet(contentlet);
            containerEl.insertAdjacentElement('beforeend', contentletEl);
            this.dotContainerContentletService
                .getContentletToContainer(this.currentContainer, contentlet)
                .subscribe((contentletHtml: string) => {
                    this.renderHTMLToContentlet(contentletEl, contentletHtml);
                    this.currentAction = DotContentletAction.EDIT;
                });
        }
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
     * Set the container id where a contentlet will be update
     *
     * @param {string} identifier
     * @memberof DotEditContentHtmlService
     */
    setContainterToEditContentlet(pageContainer: DotPageContainer): void {
        this.currentContainer = pageContainer;
        this.currentAction = DotContentletAction.EDIT;
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
        const config = { attributes: true, childList: true, characterData: true };
        const debounceContainersHeightChange = _.debounce((layout: DotLayout) => this.setContaintersSameHeight(layout), 500, {
            leading: true
        });
        const observer = new MutationObserver((mutations) => {
            debounceContainersHeightChange(pageLayout);
        });
        observer.observe(target, config);
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
            containerRow.forEach((container: HTMLElement) => {
                container.style.height = `${this.rowsMaxHeight[index]}px`;
            });
        });
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

    private handleIframeClicks(target: HTMLElement) {
        if (target.dataset.dotObject === 'edit-content') {
            this.editInnerContentlet(target);
        }
    }

    private editInnerContentlet(target: HTMLElement) {
        const parentContentlet = <HTMLElement>target.closest('div[data-dot-object="contentlet"]');
        this.currentContentlet = {
            identifier: parentContentlet.dataset.dotIdentifier,
            inode: parentContentlet.dataset.dotInode
        };

        this.buttonClickHandler(target, 'edit');
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

    private addContentToolBars(): void {
        const doc = this.getEditPageDocument();

        this.dotEditContentToolbarHtmlService
            .addContainerToolbar(doc)
            .then(() => {
                this.bindContainersEvents();
            })
            .catch((error) => {
                this.loggerService.debug(error);
            });

        this.dotEditContentToolbarHtmlService
            .addContentletMarkup(doc)
            .then(() => {
                this.bindContenletsEvents();
            })
            .catch((error) => {
                this.loggerService.debug(error);
            });
    }

    private appendNewContentlets(contentletContentEl: any, renderedContentet: string): void {
        const doc = this.getEditPageDocument();

        // Add innerHTML to a plain so we can get the HTML nodes later
        const div = doc.createElement('div');
        div.innerHTML = renderedContentet;
        // TODO: need to come up with a more efficient way to do this
        Array.from(div.children).forEach((node: any) => {
            if (node.tagName === 'SCRIPT') {
                const script = doc.createElement('script');
                script.type = 'text/javascript';

                if (node.src) {
                    script.src = node.src;
                } else {
                    script.text = node.textContent;
                }

                contentletContentEl.appendChild(script);
            } else {
                node.removeAttribute('data-dot-object');
                contentletContentEl.appendChild(node);
            }
        });
    }

    private bindContenletsEvents(): void {
        this.bindEditContentletEvents();
        this.bindRemoveContentletEvents();
    }

    private bindButtonsEvent(button: HTMLElement, type: string): void {
        button.addEventListener('click', ($event: MouseEvent) => {
            this.buttonClickHandler(<HTMLElement>$event.target, type);
        });
    }

    private buttonClickHandler(target: HTMLElement, type: string) {
        const container = <HTMLElement>target.closest('div[data-dot-object="container"]');

        this.iframeActions$.next({
            name: type,
            dataset: target.dataset,
            container: container ? container.dataset : null
        });
    }

    private bindContainersEvents(): void {
        const addButtons = Array.from(this.getEditPageDocument().querySelectorAll('.dotedit-menu__button:not([disabled])'));

        addButtons.forEach((button: HTMLElement) => {
            const menuList = button.nextElementSibling;
            const menuItems = Array.from(menuList.querySelectorAll('.dotedit-menu__item a'));

            this.bindEventToSubMenu(button);

            menuItems.forEach((menuItem: HTMLElement) => {
                this.bindButtonsEvent(menuItem, menuItem.dataset.dotAction);
            });
        });
    }

    private bindEventToSubMenu(button: HTMLElement): void {
        button.addEventListener('click', (_event) => {
            this.closeContainersToolBarMenu(button.nextElementSibling);
            button.nextElementSibling.classList.toggle('active');
        });
    }

    private bindWindowEvents(): void {
        const doc = this.getEditPageDocument();

        doc.addEventListener('click', ($event: MouseEvent) => {
            const target = <HTMLElement>$event.target;
            if (!target.classList.contains('dotedit-menu__button')) {
                this.closeContainersToolBarMenu();
            }
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

    private bindEditContentletEvents(): void {
        const editButtons = Array.from(
            this.getEditPageDocument().querySelectorAll('.dotedit-contentlet__edit:not(.dotedit-contentlet__disabled)')
        );

        editButtons.forEach((button: HTMLElement) => {
            this.bindButtonsEvent(button, 'edit');
        });
    }

    private bindRemoveContentletEvents(): void {
        const removeButtons = Array.from(this.getEditPageDocument().querySelectorAll('.dotedit-contentlet__remove'));
        removeButtons.forEach((button: HTMLElement) => {
            this.bindButtonsEvent(button, 'remove');
        });
    }

    private createNewContentlet(contentlet: DotPageContent): HTMLElement {
        const doc = this.getEditPageDocument();
        const dotEditContentletEl: HTMLElement = doc.createElement('div');
        dotEditContentletEl.dataset.dotObject = 'contentlet';
        dotEditContentletEl.dataset.dotIdentifier = contentlet.identifier;
        dotEditContentletEl.dataset.dotInode = contentlet.inode;
        dotEditContentletEl.dataset.dotType = contentlet.type;
        dotEditContentletEl.dataset.dotBasetype = contentlet.baseType;
        dotEditContentletEl.dataset.dotCanEdit = 'true';

        /*
            TODO: we have the method: DotEditContentToolbarHtmlService.addContentletMarkup that does this, we need
            to consolidate this.
        */
        const contenToolbarButtons = this.dotEditContentToolbarHtmlService.getContentButton(
            contentlet.identifier,
            contentlet.inode,
            dotEditContentletEl.dataset.dotCanEdit === 'true'
        );

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

        this.bindButtonsEvent(<HTMLElement>dotEditContentletEl.querySelector('.dotedit-contentlet__edit'), 'edit');
        this.bindButtonsEvent(<HTMLElement>dotEditContentletEl.querySelector('.dotedit-contentlet__remove'), 'remove');
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
            save: (contentletEvent: any) => {
                if (this.currentAction === DotContentletAction.ADD) {
                    this.renderAddedContentlet(contentletEvent.data);
                } else {
                    this.renderEditedContentlet(this.currentContentlet || contentletEvent.data);
                }
            },
            // When a user select a content from the search jsp
            select: (contentletEvent: any) => {
                this.renderAddedContentlet(contentletEvent.data);
                this.iframeActions$.next({
                    name: 'select'
                });
            },
            // When a user drang and drop a contentlet in the iframe
            relocate: (contentletEvent: any) => {
                this.renderRelocatedContentlet(contentletEvent.data);
            }
        };

        return contentletEventsMap[event];
    }

    private loadCodeIntoIframe(editPageHTML: string): void {
        const doc = this.getEditPageDocument();
        doc.open();
        doc.write(editPageHTML);
        doc.close();
    }

    private setEditContentletStyles(): void {
        const style = this.dotDOMHtmlUtilService.createStyleElement(EDIT_PAGE_CSS);
        const robotoFontElement = this.dotDOMHtmlUtilService.createLinkElement(GOOGLE_FONTS);

        const doc = this.getEditPageDocument();
        doc.head.appendChild(style);
        doc.head.appendChild(robotoFontElement);
    }

    private setEditMode(): void {
        this.addContentToolBars();

        this.dotDragDropAPIHtmlService.initDragAndDropContext(this.getEditPageIframe());
        this.setEditContentletStyles();
        this.bindWindowEvents();
    }

    private renderHTMLToContentlet(contentletEl: HTMLElement, contentletHtml: string): void {
        const contentletContentEl = contentletEl.querySelector('.dotedit-contentlet__content');

        // Removing the loading indicator
        contentletContentEl.innerHTML = '';
        this.appendNewContentlets(contentletContentEl, contentletHtml);

        // Update the model with the recently added contentlet
        this.pageModel$.next(this.getContentModel());
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
                // Removing the loading indicator
                contentletContentEl.innerHTML = '';
                this.appendNewContentlets(contentletContentEl, contentletHtml);
            });
    }
}
