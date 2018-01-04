import { LoggerService } from 'dotcms-js/dotcms-js';
import { Injectable, ElementRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { EDIT_PAGE_CSS } from '../shared/iframe-edit-mode.css';
import { DotContainerContentletService } from './dot-container-contentlet.service';
import { DotDragDropAPIHtmlService } from './html/dot-drag-drop-api-html.service';
import { GOOGLE_FONTS } from './html/iframe-edit-mode.js';
import { DotEditContentToolbarHtmlService } from './html/dot-edit-content-toolbar-html.service';
import { DotDOMHtmlUtilService } from './html/dot-dom-html-util.service';

@Injectable()
export class DotEditContentHtmlService {
    contentletEvents: BehaviorSubject<any> = new BehaviorSubject({});
    iframe: ElementRef;

    private addContentContainer: string;

    constructor(
        private dotContainerContentletService: DotContainerContentletService,
        private dotDragDropAPIHtmlService: DotDragDropAPIHtmlService,
        private dotEditContentToolbarHtmlService: DotEditContentToolbarHtmlService,
        private dotDOMHtmlUtilService: DotDOMHtmlUtilService,
        private loggerService: LoggerService
    ) {
        this.contentletEvents.subscribe(res => {
            if (res.event === 'save') {
                this.renderRelocatedContentlet({
                    contentlet: {
                        inode: 'e0e31ce27719' || res.data.inode
                    }
                });
            }

            if (res.event === 'select') {
                this.renderAddedContentlet({
                    identifier: res.data.identifier
                });
            }

            if (res.event === 'relocate') {
                this.renderRelocatedContentlet(res.data);
            }
        });
    }

    initEditMode(editPageHTML: string, iframeEl: ElementRef): void {
        this.iframe = iframeEl;
        this.loadCodeIntoIframe(editPageHTML);

        const iframeElement = this.getEditPageIframe();

        iframeElement.contentWindow.contentletEvents = this.contentletEvents;

        iframeElement.addEventListener('load', () => {
            this.setEditMode();
        });
    }

    removeContentlet(inode: string): void {
        const doc = this.getEditPageDocument();
        const contenletEl = doc.querySelector(`div[data-dot-inode="${inode}"]`);
        contenletEl.remove();
    }

    renderAddedContentlet(contentlet: any): void {
        const doc = this.getEditPageDocument();
        const containerEl = doc.querySelector(
            `div[data-dot-object="container"][data-dot-identifier="${this.addContentContainer}"]`
        );
        const contentletEl: HTMLElement = this.createNewContentlet(contentlet);

        containerEl.insertAdjacentElement('afterbegin', contentletEl);

        this.dotContainerContentletService
            .getContentletToContainer(this.addContentContainer, contentlet.identifier)
            .subscribe((contentletHtml: string) => {
                const contentletContentEl = contentletEl.querySelector('.dotedit-contentlet__content');

                // Removing the loading indicator
                contentletContentEl.innerHTML = '';
                this.appendNewContentlets(contentletContentEl, contentletHtml);

                this.addContentContainer = null;
            });
    }

    setContainterToAppendContentlet(identifier: string): void {
        this.addContentContainer = identifier;
    }

    private addContentToolBars(): void {
        const doc = this.getEditPageDocument();
        this.dotEditContentToolbarHtmlService.addContainerToolbar(doc).then(() => {
            this.bindContainersEvents();
        }).catch(error => {
            this.loggerService.debug(error);
        });

        this.dotEditContentToolbarHtmlService.addContentletMarkup(doc).then(() => {
            this.bindContenletsEvents();
        }).catch(error => {
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
            this.contentletEvents.next({
                event: type,
                dataset: target.dataset,
                contentletEvents: this.contentletEvents
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
        button.addEventListener('click', $event => {
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

    private createNewContentlet(contentlet: any): HTMLElement {
        const doc = this.getEditPageDocument();
        const dotEditContentletEl: HTMLElement = doc.createElement('div');
        dotEditContentletEl.dataset.dotObject = 'contentlet';
        dotEditContentletEl.dataset.dotIdentifier = contentlet.identifier;
        dotEditContentletEl.dataset.dotInode = contentlet.inode;
        dotEditContentletEl.dataset.dotType = contentlet.type;

        dotEditContentletEl.innerHTML = `<div class="dotedit-contentlet__toolbar">
                <button type="button" data-dot-identifier="${contentlet.identifier}"
                    data-dot-inode="${contentlet.inode}"
                    class="dotedit-contentlet__drag">
                    Drag
                </button>
                <button type="button" data-dot-identifier="${contentlet.identifier}"
                    data-dot-inode="${contentlet.inode}"
                    class="dotedit-contentlet__edit">
                    Edit
                </button>
                <button type="button" data-dot-identifier="${contentlet.identifier}"
                    data-dot-inode="${contentlet.inode}"
                    class="dotedit-contentlet__remove">
                    Remove
                </button>
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

        this.dotDragDropAPIHtmlService.initDragAndDropContext(this.getEditPageDocument());
        this.setEditContentletStyles();
        this.bindWindowEvents();
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
            .getContentletToContainer(relocateInfo.container.identifier, relocateInfo.contentlet.identifier)
            .subscribe((contentletHtml: string) => {
                // Removing the loading indicator
                contentletContentEl.innerHTML = '';
                this.appendNewContentlets(contentletContentEl, contentletHtml);
            });
    }
}
