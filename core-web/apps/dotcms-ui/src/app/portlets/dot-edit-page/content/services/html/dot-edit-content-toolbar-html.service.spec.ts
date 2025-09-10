import { of as observableOf } from 'rxjs';

import { TestBed } from '@angular/core/testing';

import { DotLicenseService, DotMessageService } from '@dotcms/data-access';
import { DotLicenseServiceMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';
import { DotEditContentToolbarHtmlService } from './dot-edit-content-toolbar-html.service';

const mouseoverEvent = new MouseEvent('mouseover', {
    view: window,
    bubbles: true,
    cancelable: true
});

describe('DotEditContentToolbarHtmlService', () => {
    let service: DotEditContentToolbarHtmlService;
    let testDoc: Document;
    let dummyContainer: HTMLDivElement;

    function dispatchMouseOver() {
        const el = testDoc.querySelector('.large-column');
        el.dispatchEvent(mouseoverEvent);
    }

    const messageServiceMock = new MockDotMessageService({
        'editpage.content.contentlet.menu.drag': 'Drag',
        'editpage.content.contentlet.menu.edit': 'Edit',
        'editpage.content.contentlet.menu.remove': 'Remove',
        'editpage.content.container.action.add': 'Add',
        'editpage.content.container.menu.content': 'Content',
        'editpage.content.container.menu.widget': 'Widget',
        'editpage.content.container.menu.form': 'Form',
        'dot.common.license.enterprise.only.error': 'Enterprise Only',
        'dot.common.contentlet.max.limit.error': 'Max contentlets limit reached'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotEditContentToolbarHtmlService,
                { provide: DotLicenseService, useClass: DotLicenseServiceMock },
                DotDOMHtmlUtilService,
                { provide: DotMessageService, useValue: messageServiceMock }
            ]
        });
        service = TestBed.inject(DotEditContentToolbarHtmlService);
    });

    describe('container toolbar', () => {
        let containerEl: HTMLElement;
        let addButtonEl: Element;
        let menuItems: NodeListOf<Element>;

        beforeEach(() => {
            testDoc = document.implementation.createDocument(
                'http://www.w3.org/1999/xhtml',
                'html',
                null
            );
            dummyContainer = testDoc.createElement('div');
        });

        describe('default', () => {
            describe('button', () => {
                beforeEach(() => {
                    dummyContainer.innerHTML = `
                        <div data-dot-object="container" data-dot-can-add="CONTENT,WIDGET,FORM">
                            <div data-dot-object="contentlet">
                                <div class="large-column"></div>
                            </div>
                        </div>
                    `;
                    const htmlElement: HTMLHtmlElement = testDoc.getElementsByTagName('html')[0];
                    htmlElement.appendChild(dummyContainer);
                    service.addContainerToolbar(testDoc);
                });

                it('should create container toolbar', () => {
                    containerEl = testDoc.querySelector('[data-dot-object="container"]');
                    expect(containerEl).not.toBe(null);
                    expect(containerEl.classList.contains('disabled')).toBe(false);
                });

                it('should have add button', () => {
                    addButtonEl = testDoc.querySelector('.dotedit-container__add');
                    expect(addButtonEl).not.toBe(null);
                    expect(addButtonEl.attributes.getNamedItem('disabled')).toEqual(null);
                });
            });

            describe('actions', () => {
                it('should have content, widget and form', () => {
                    dummyContainer.innerHTML =
                        '<div data-dot-object="container" data-dot-can-add="CONTENT,WIDGET,FORM"></div>';
                    const htmlElement: HTMLHtmlElement = testDoc.getElementsByTagName('html')[0];
                    htmlElement.appendChild(dummyContainer);
                    service.addContainerToolbar(testDoc);
                    menuItems = testDoc.querySelectorAll('.dotedit-menu__item a');
                    const menuItemsLabels = Array.from(menuItems).map((item) =>
                        item.textContent.replace(/\s/g, '')
                    );

                    expect(menuItemsLabels).toEqual(['Content', 'Widget', 'Form']);
                    expect(menuItems.length).toEqual(3);
                });

                it('should have widget and form', () => {
                    dummyContainer.innerHTML =
                        '<div data-dot-object="container" data-dot-can-add="WIDGET,FORM"></div>';
                    const htmlElement: HTMLHtmlElement = testDoc.getElementsByTagName('html')[0];
                    htmlElement.appendChild(dummyContainer);
                    service.addContainerToolbar(testDoc);
                    menuItems = testDoc.querySelectorAll('.dotedit-menu__item a');
                    const menuItemsLabels = Array.from(menuItems).map((item) =>
                        item.textContent.replace(/\s/g, '')
                    );

                    expect(menuItemsLabels).toEqual(['Widget', 'Form']);
                    expect(menuItems.length).toEqual(2);
                });

                it('should have widget', () => {
                    dummyContainer.innerHTML =
                        '<div data-dot-object="container" data-dot-can-add="WIDGET"></div>';
                    const htmlElement: HTMLHtmlElement = testDoc.getElementsByTagName('html')[0];
                    htmlElement.appendChild(dummyContainer);
                    service.addContainerToolbar(testDoc);
                    menuItems = testDoc.querySelectorAll('.dotedit-menu__item a');
                    const menuItemsLabels = Array.from(menuItems).map((item) =>
                        item.textContent.replace(/\s/g, '')
                    );

                    expect(menuItemsLabels).toEqual(['Widget']);
                    expect(menuItems.length).toEqual(1);
                });

                describe('without license', () => {
                    beforeEach(() => {
                        const dotLicenseService = TestBed.inject(DotLicenseService);
                        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(
                            observableOf(false)
                        );
                    });

                    it('should have content, widget and form', () => {
                        dummyContainer.innerHTML =
                            '<div data-dot-object="container" data-dot-can-add="CONTENT,WIDGET,FORM"></div>';
                        const htmlElement: HTMLHtmlElement =
                            testDoc.getElementsByTagName('html')[0];
                        htmlElement.appendChild(dummyContainer);
                        service.addContainerToolbar(testDoc);
                        menuItems = testDoc.querySelectorAll('.dotedit-menu__item ');

                        expect(menuItems.length).toEqual(3);
                        expect(
                            menuItems[0].classList.contains('dotedit-menu__item--disabled')
                        ).toBeFalsy();
                        expect(
                            menuItems[1].classList.contains('dotedit-menu__item--disabled')
                        ).toBeFalsy();
                        expect(
                            menuItems[2].classList.contains('dotedit-menu__item--disabled')
                        ).toBeTruthy();
                        expect(menuItems[2].getAttribute('dot-title')).toBe('Enterprise Only');
                    });
                });

                describe('should update container toolbar with disabled actions due to max contentlets limit', () => {
                    beforeEach(() => {
                        dummyContainer.innerHTML = `
                            <div data-dot-object="container" data-max-contentlets="2" data-dot-can-add="CONTENT,WIDGET,FORM">
                                <div data-dot-object="contentlet">
                                    <div class="large-column"></div>
                                </div>
                            </div>
                        `;
                        const htmlElement: HTMLHtmlElement =
                            testDoc.getElementsByTagName('html')[0];
                        htmlElement.appendChild(dummyContainer);
                        service.addContainerToolbar(testDoc);
                    });

                    it('should create container toolbar', () => {
                        containerEl = <HTMLElement>(
                            testDoc.querySelector('[data-dot-object="container"]')
                        );
                        containerEl.innerHTML = `
                        <div data-dot-object="contentlet">
                            <div class="large-column"></div>
                        </div>
                        <div data-dot-object="contentlet">
                            <div class="large-column"></div>
                        </div>
                        `;
                        service.updateContainerToolbar(containerEl);
                        menuItems = testDoc.querySelectorAll('.dotedit-menu__item ');

                        expect(menuItems.length).toEqual(3);
                        expect(
                            menuItems[0].classList.contains('dotedit-menu__item--disabled')
                        ).toBeTruthy();
                        expect(menuItems[0].getAttribute('dot-title')).toBe(
                            'Max contentlets limit reached'
                        );
                        expect(
                            menuItems[1].classList.contains('dotedit-menu__item--disabled')
                        ).toBeTruthy();
                        expect(menuItems[1].getAttribute('dot-title')).toBe(
                            'Max contentlets limit reached'
                        );
                        expect(
                            menuItems[2].classList.contains('dotedit-menu__item--disabled')
                        ).toBeTruthy();
                        expect(menuItems[2].getAttribute('dot-title')).toBe(
                            'Max contentlets limit reached'
                        );
                    });
                });
            });
        });

        describe('disabled', () => {
            beforeEach(() => {
                const htmlElement: HTMLHtmlElement = testDoc.getElementsByTagName('html')[0];
                dummyContainer.innerHTML = `
                    <div data-dot-object="container" data-dot-can-add="">
                        <div data-dot-object="contentlet">
                            <div class="large-column"></div>
                        </div>
                    </div>
                `;
                htmlElement.appendChild(dummyContainer);
                service.addContainerToolbar(testDoc);

                containerEl = testDoc.querySelector('[data-dot-object="container"]');
                addButtonEl = testDoc.querySelector('.dotedit-container__add');
                menuItems = testDoc.querySelectorAll('.dotedit-menu__item');
            });

            it('should create container toolbar disabled', () => {
                expect(containerEl.classList.contains('disabled')).toBe(true);
            });

            it('should have add button disabled', () => {
                expect(addButtonEl.attributes.getNamedItem('disabled')).not.toEqual(null);
            });

            it('should not have add actions', () => {
                expect(menuItems.length).toEqual(0);
            });

            xit('should bind events');
        });
    });

    describe('contentlet toolbar', () => {
        let htmlElement: HTMLHtmlElement;

        beforeEach(() => {
            testDoc = document.implementation.createDocument(
                'http://www.w3.org/1999/xhtml',
                'html',
                null
            );
            dummyContainer = testDoc.createElement('div');
            htmlElement = testDoc.getElementsByTagName('html')[0];
            service.bindContentletEvents(testDoc);
        });

        describe('default', () => {
            beforeEach(() => {
                dummyContainer.innerHTML = `
                    <div data-dot-object="container" data-dot-inode="854ac983-9a18-4a9a-874b-dd18d8be91f5">
                        <div data-dot-object="contentlet" data-dot-can-edit="false" data-dot-has-page-lang-version="true">
                            <div class="large-column"></div>
                        </div>
                    </div>
                `;
                htmlElement.appendChild(dummyContainer);
            });

            it('should create buttons', () => {
                dispatchMouseOver();

                expect(testDoc.querySelectorAll('.dotedit-contentlet__drag').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__edit').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__remove').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__code').length).toEqual(0);
            });

            it('should create toolbar with dotInode asociated with the container', () => {
                const toolbar: HTMLElement = testDoc.querySelector(`[data-dot-object="container"]`);
                expect(toolbar.dataset['dotInode']).toEqual('854ac983-9a18-4a9a-874b-dd18d8be91f5');
            });

            it('should have edit button disabled', () => {
                dispatchMouseOver();

                expect(
                    testDoc
                        .querySelector('.dotedit-contentlet__edit')
                        .classList.contains('dotedit-contentlet__disabled')
                ).toBe(true);
            });
        });

        describe('not show', () => {
            beforeEach(() => {
                dummyContainer.innerHTML = `
                    <div data-dot-object="container">
                        <div data-dot-object="contentlet" data-dot-can-edit="false" data-dot-has-page-lang-version="true">
                            <div class="large-column"></div>
                        </div>
                        <div data-dot-object="contentlet" data-dot-can-edit="false" data-dot-has-page-lang-version="false">
                            <div class="large-column"></div>
                        </div>
                    </div>
                `;
                htmlElement.appendChild(dummyContainer);
            });

            it('should create buttons for only one contentlet', () => {
                dispatchMouseOver();

                expect(testDoc.querySelectorAll('.dotedit-contentlet__drag').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__edit').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__remove').length).toEqual(1);
            });
        });

        describe('form', () => {
            beforeEach(() => {
                dummyContainer.innerHTML = `
                    <div data-dot-object="container">
                        <div data-dot-object="contentlet" data-dot-basetype="FORM" data-dot-has-page-lang-version="true">
                            <div class="large-column"></div>
                        </div>
                    </div>
                `;
                htmlElement.appendChild(dummyContainer);
            });

            it('should have edit button disabled', () => {
                dispatchMouseOver();

                expect(
                    testDoc
                        .querySelector('.dotedit-contentlet__edit')
                        .classList.contains('dotedit-contentlet__disabled')
                ).toBe(true);
            });
        });

        describe('with vtl files', () => {
            describe('enabled', () => {
                beforeEach(() => {
                    dummyContainer.innerHTML = `
                        <div data-dot-object="container">
                            <div data-dot-object="contentlet" data-dot-can-edit="false" data-dot-has-page-lang-version="true">
                                <div
                                    data-dot-object="vtl-file"
                                    data-dot-inode="123"
                                    data-dot-url="/news/personalized-news-listing.vtl"
                                    data-dot-can-edit="true"></div>
                                <div class="large-column"></div>
                            </div>
                        </div>
                    `;
                    htmlElement.appendChild(dummyContainer);
                });

                it('should have button', () => {
                    const el = testDoc.querySelector('.large-column');
                    el.dispatchEvent(mouseoverEvent);
                    expect(testDoc.querySelectorAll('.dotedit-contentlet__code').length).toEqual(1);
                });

                it('should have submenu link', () => {
                    const el = testDoc.querySelector('.large-column');
                    el.dispatchEvent(mouseoverEvent);

                    const links = testDoc.querySelectorAll('.dotedit-menu__item a');
                    expect(links.length).toEqual(1);
                    expect(links[0].textContent.trim()).toEqual('personalized-news-listing.vtl');
                });
            });

            describe('disabled', () => {
                beforeEach(() => {
                    dummyContainer.innerHTML = `
                        <div data-dot-object="container">
                            <div data-dot-object="contentlet" data-dot-can-edit="false" data-dot-has-page-lang-version="true">
                                <div data-dot-object="vtl-file"
                                    data-dot-inode="123"
                                    data-dot-url="/news/personalized-news-listing.vtl"
                                    data-dot-can-edit="false">
                                </div>
                                <div class="large-column"></div>
                            </div>
                        </div>
                    `;
                    htmlElement.appendChild(dummyContainer);
                });

                it('should have submenu link', () => {
                    const el = testDoc.querySelector('.large-column');
                    el.dispatchEvent(mouseoverEvent);

                    const links = testDoc.querySelectorAll('.dotedit-menu__item');
                    expect(links.length).toEqual(1);
                    expect(links[0].classList.contains('dotedit-menu__item--disabled')).toBe(true);
                });
            });
        });
    });
});
