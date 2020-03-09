import { of as observableOf, Observable } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { DotEditContentToolbarHtmlService } from './dot-edit-content-toolbar-html.service';
import { DotMessageService } from '@services/dot-messages-service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DotDOMHtmlUtilService } from './dot-dom-html-util.service';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { Injectable } from '@angular/core';

@Injectable()
class DotLicenseServiceMock {
    isEnterprise(): Observable<boolean> {
        return observableOf(true);
    }
}

describe('DotEditContentToolbarHtmlService', () => {
    let dotEditContentToolbarHtmlService: DotEditContentToolbarHtmlService;
    let testDoc: Document;
    let dummyContainer: HTMLDivElement;

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
        dotEditContentToolbarHtmlService = TestBed.get(DotEditContentToolbarHtmlService);
    });

    describe('container toolbar', () => {
        let containerEl: HTMLElement;
        let addButtonEl: Element;
        let menuItems: NodeListOf<Element>;

        describe('default', () => {
            beforeEach(() => {
                testDoc = document.implementation.createDocument(
                    'http://www.w3.org/1999/xhtml',
                    'html',
                    null
                );
                dummyContainer = testDoc.createElement('div');
            });

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
                    dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);
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
                    dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);
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
                    dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);
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
                    dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);
                    menuItems = testDoc.querySelectorAll('.dotedit-menu__item a');
                    const menuItemsLabels = Array.from(menuItems).map((item) =>
                        item.textContent.replace(/\s/g, '')
                    );

                    expect(menuItemsLabels).toEqual(['Widget']);
                    expect(menuItems.length).toEqual(1);
                });

                describe('without license', () => {
                    beforeEach(() => {
                        const dotLicenseService = TestBed.get(DotLicenseService);
                        spyOn(dotLicenseService, 'isEnterprise').and.returnValue(
                            observableOf(false)
                        );
                    });

                    it('should have content, widget and form', () => {
                        dummyContainer.innerHTML =
                            '<div data-dot-object="container" data-dot-can-add="CONTENT,WIDGET,FORM"></div>';
                        const htmlElement: HTMLHtmlElement = testDoc.getElementsByTagName(
                            'html'
                        )[0];
                        htmlElement.appendChild(dummyContainer);
                        dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);
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
                        const htmlElement: HTMLHtmlElement = testDoc.getElementsByTagName('html')[0];
                        htmlElement.appendChild(dummyContainer);
                        dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);
                    });

                    it('should create container toolbar', () => {
                        containerEl = <HTMLElement>testDoc.querySelector('[data-dot-object="container"]');
                        containerEl.innerHTML = `
                        <div data-dot-object="contentlet">
                            <div class="large-column"></div>
                        </div>
                        <div data-dot-object="contentlet">
                            <div class="large-column"></div>
                        </div>
                        `;
                        dotEditContentToolbarHtmlService.updateContainerToolbar(containerEl);
                        menuItems = testDoc.querySelectorAll('.dotedit-menu__item ');

                        expect(menuItems.length).toEqual(3);
                        expect(
                            menuItems[0].classList.contains('dotedit-menu__item--disabled')
                        ).toBeTruthy();
                        expect(
                            menuItems[1].classList.contains('dotedit-menu__item--disabled')
                        ).toBeTruthy();
                        expect(
                            menuItems[2].classList.contains('dotedit-menu__item--disabled')
                        ).toBeTruthy();
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
                dotEditContentToolbarHtmlService.addContainerToolbar(testDoc);

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
        });

        describe('default', () => {
            beforeEach(() => {
                dummyContainer.innerHTML = `
                    <div data-dot-object="container">
                        <div data-dot-object="contentlet" data-dot-can-edit="false" data-dot-has-page-lang-version="true">
                            <div class="large-column"></div>
                        </div>
                    </div>
                `;
                htmlElement.appendChild(dummyContainer);
                dotEditContentToolbarHtmlService.addContentletMarkup(testDoc);
            });

            it('should create buttons', () => {
                expect(testDoc.querySelectorAll('.dotedit-contentlet__drag').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__edit').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__remove').length).toEqual(1);
                expect(testDoc.querySelectorAll('.dotedit-contentlet__code').length).toEqual(0);
            });

            it('should have edit button disabled', () => {
                expect(
                    testDoc
                        .querySelector('.dotedit-contentlet__edit')
                        .classList.contains('dotedit-contentlet__disabled')
                ).toBe(true);
            });

            xit('should bind events');
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
                dotEditContentToolbarHtmlService.addContentletMarkup(testDoc);
            });

            it('should create buttons for only one contentlet', () => {
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
                dotEditContentToolbarHtmlService.addContentletMarkup(testDoc);
            });

            it('should have edit button disabled', () => {
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
                    dotEditContentToolbarHtmlService.addContentletMarkup(testDoc);
                });

                it('should have button', () => {
                    expect(testDoc.querySelectorAll('.dotedit-contentlet__code').length).toEqual(1);
                });

                it('should have submenu link', () => {
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
                    dotEditContentToolbarHtmlService.addContentletMarkup(testDoc);
                });

                it('should have submenu link', () => {
                    const links = testDoc.querySelectorAll('.dotedit-menu__item');
                    expect(links.length).toEqual(1);
                    expect(links[0].classList.contains('dotedit-menu__item--disabled')).toBe(true);
                });
            });
        });
    });
});
