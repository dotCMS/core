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

enum Action {
    EDIT,
    ADD
}
@Injectable()
export class DotEditContentHtmlService {
    contentletEvents: Subject<any> = new Subject();
    iframeActions: Subject<any> = new Subject();
    iframe: ElementRef;

    pageModelChange: Subject<any> = new Subject();

    currentContainer: DotPageContainer;

    private currentAction: Action;

    constructor(
        private dotContainerContentletService: DotContainerContentletService,
        private dotDragDropAPIHtmlService: DotDragDropAPIHtmlService,
        private dotEditContentToolbarHtmlService: DotEditContentToolbarHtmlService,
        private dotDOMHtmlUtilService: DotDOMHtmlUtilService,
        private loggerService: LoggerService,
        private dotDialogService: DotDialogService,
        private dotMessageService: DotMessageService
    ) {
        this.contentletEvents.subscribe((contentletEvent: any) => {
            this.handlerContentletEvents(contentletEvent.name)(contentletEvent);
        });

        this.dotMessageService
            .getMessages(['editpage.content.add.already.title', 'editpage.content.add.already.message'])
            .subscribe();
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
            iframeElement.contentWindow[MODEL_VAR_NAME] = this.pageModelChange;
            iframeElement.contentWindow.contentletEvents = this.contentletEvents;
            iframeElement.addEventListener('load', () => {
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
        // tslint:disable-next-line:max-line-length
        const contenletEl = doc.querySelector(
            `div[data-dot-object="container"][data-dot-identifier="${container.identifier}"][data-dot-uuid="${
                container.uuid
            }"] div[data-dot-inode="${content.inode}"]`
        );
        contenletEl.remove();
        this.pageModelChange.next(this.getContentModel());
    }

    /**
     * Render contentlet in the DOM after edition.
     *
     * @param {*} contentlet
     * @memberof DotEditContentHtmlService
     */
    renderEditedContentlet(contentlet: DotPageContent): void {
        const doc = this.getEditPageDocument();
        const currentContentlets = doc.querySelectorAll(`div[data-dot-identifier="${contentlet.identifier}"]`);

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
            `div[data-dot-object="container"][data-dot-identifier="${
                this.currentContainer.identifier
            }"][data-dot-uuid="${this.currentContainer.uuid}"]`
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
                    this.currentContainer = null;
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
        this.currentAction = Action.ADD;
    }

    /**
     * Set the container id where a contentlet will be update
     *
     * @param {string} identifier
     * @memberof DotEditContentHtmlService
     */
    setContainterToEditContentlet(pageContainer: DotPageContainer): void {
        this.currentContainer = pageContainer;
        this.currentAction = Action.EDIT;
    }

    /**
     * Return the page model
     *
     * @returns {*}
     * @memberof DotEditContentHtmlService
     */
    getContentModel(): any {
        return this.getEditPageIframe().contentWindow.getDotNgModel();
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
        const currentContentlets: HTMLElement[] = <HTMLElement[]>Array.from(
            containerEL.querySelectorAll(contentsSelector).values()
        );
        return currentContentlets.some(
            (contentElement) => contentElement.dataset.dotIdentifier === contentlet.identifier
        );
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
            const target = <HTMLElement>$event.target;
            const container = <HTMLElement>target.closest('div[data-dot-object="container"]');

            this.iframeActions.next({
                name: type,
                dataset: target.dataset,
                container: container ? container.dataset : null
            });
        });
    }

    private bindContainersEvents(): void {
        const addButtons = Array.from(this.getEditPageDocument().querySelectorAll('.dotedit-container__add'));
        addButtons.forEach((button: Node) => {
            const parent = button.parentElement;
            const menuItems = Array.from(parent.querySelectorAll('.dotedit-container__menu-item a'));

            this.bindEventToAddContentSubMenu(button);
            menuItems.forEach((menuItem: HTMLElement) => {
                this.bindButtonsEvent(menuItem, 'add');
            });
        });
    }

    private bindEventToAddContentSubMenu(button: Node): void {
        button.addEventListener('click', (_event) => {
            this.closeContainersToolBarMenu(button.parentElement);
            button.parentElement.classList.toggle('active');
        });
    }

    private bindWindowEvents(): void {
        const doc = this.getEditPageDocument();

        doc.addEventListener('click', ($event: MouseEvent) => {
            const target = <HTMLElement>$event.target;
            if (!target.classList.contains('dotedit-container__add')) {
                this.closeContainersToolBarMenu();
            }
        });
    }

    private closeContainersToolBarMenu(activeElement?: Node): void {
        const doc = this.getEditPageDocument();
        const activeToolBarMenus = Array.from(doc.querySelectorAll('.dotedit-container__toolbar.active'));
        activeToolBarMenus.forEach((toolbar: HTMLElement) => {
            if (activeElement !== toolbar) {
                toolbar.classList.remove('active');
            }
        });
    }

    private bindEditContentletEvents(): void {
        const editButtons = Array.from(this.getEditPageDocument().querySelectorAll('.dotedit-contentlet__edit'));
        editButtons.forEach((button: HTMLElement) => {
            this.bindButtonsEvent(button, 'edit');
        });
    }

    private bindRemoveContentletEvents(): void {
        const editButtons = Array.from(this.getEditPageDocument().querySelectorAll('.dotedit-contentlet__remove'));
        editButtons.forEach((button: HTMLElement) => {
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

        dotEditContentletEl.innerHTML = `<div class="dotedit-contentlet__toolbar">
                ${contenToolbarButtons}
            </div>
            <div class="dotedit-contentlet__content"><div class="loader__overlay"><div class="loader"></div></div></div>`;

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
                if (this.currentAction === Action.ADD) {
                    this.renderAddedContentlet(contentletEvent.data);
                } else if (this.currentAction === Action.EDIT) {
                    this.renderEditedContentlet(contentletEvent.data);
                }
                this.iframeActions.next({
                    name: 'close'
                });
            },
            // When a user select a content from the search jsp
            select: (contentletEvent: any) => {
                this.renderAddedContentlet(contentletEvent.data);
                this.iframeActions.next({
                    name: 'close'
                });
            },
            // When a user drang and drop a contentlet in the iframe
            relocate: (contentletEvent: any) => {
                this.renderRelocatedContentlet(contentletEvent.data);
            },
            // When user cancel the edition of a contentlet.
            cancel: (contentletEvent: any) => {
                this.iframeActions.next({
                    name: 'close'
                });
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
        this.pageModelChange.next(this.getContentModel());
    }

    private renderRelocatedContentlet(relocateInfo: any): void {
        const doc = this.getEditPageDocument();
        const contenletEl = doc.querySelector(
            `div[data-dot-object="contentlet"][data-dot-inode="${relocateInfo.contentlet.inode}"]`
        );
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
