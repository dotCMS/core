/* eslint-disable @typescript-eslint/no-explicit-any */

import { Observable, of, throwError } from 'rxjs';

import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Injectable } from '@angular/core';
import { TestBed, waitForAsync } from '@angular/core/testing';

import { ConfirmationService } from 'primeng/api';

import {
    DotAlertConfirmService,
    DotEditPageService,
    DotEventsService,
    DotHttpErrorManagerService,
    DotLicenseService,
    DotMessageService,
    DotWorkflowActionsFireService,
    DotGlobalMessageService
} from '@dotcms/data-access';
import { CoreWebService, HttpCode, LoggerService, StringUtils } from '@dotcms/dotcms-js';
import {
    DotCMSClazzes,
    DotCMSContentType,
    DotPageContainer,
    DotPageContent,
    DotPageRender,
    DotPageRenderState,
    PageModelChangeEventType
} from '@dotcms/dotcms-models';
import {
    CoreWebServiceMock,
    dotcmsContentTypeBasicMock,
    mockDotLayout,
    MockDotMessageService,
    mockDotPage,
    mockDotRenderedPage,
    mockResponseView,
    mockUser
} from '@dotcms/utils-testing';

import {
    CONTENTLET_PLACEHOLDER_SELECTOR,
    DotEditContentHtmlService
} from './dot-edit-content-html.service';

import { DotContainerContentletService } from '../dot-container-contentlet.service';
import { DotDOMHtmlUtilService } from '../html/dot-dom-html-util.service';
import { DotDragDropAPIHtmlService } from '../html/dot-drag-drop-api-html.service';
import { DotEditContentToolbarHtmlService } from '../html/dot-edit-content-toolbar-html.service';

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(false);
    }
}

const mouseoverEvent = new MouseEvent('mouseover', {
    view: window,
    bubbles: true,
    cancelable: true
});

xdescribe('DotEditContentHtmlService', () => {
    let dotLicenseService: DotLicenseService;
    let fakeDocument: Document;

    const fakeHTML = `
        <html>
        <head>
            <!-- <base href="/" /> -->
            <script>
                function getDotNgModel() {
                    return [
                        {
                            identifier: '123',
                            uuid: '456',
                            contentletsId: ['3']
                        }
                    ];
                }
            </script>
        </head>
        <body>
            <div class="row-1">

                <div data-dot-object="container" data-dot-identifier="123" data-dot-uuid="456" data-dot-can-add="CONTENT">
                    <div
                        data-dot-object="contentlet"
                        data-dot-identifier="456"
                        data-dot-inode="456"
                        data-dot-type="NewsWidgets"
                        data-dot-content-type-id="2"
                        data-dot-basetype="CONTENT"
                        data-dot-has-page-lang-version="true">
                        <div class="large-column">
                            <div
                                data-dot-object="vtl-file"
                                data-dot-inode="345274e0-3bbb-41f1-912c-b398d5745b9a"
                                data-dot-url="/application/vtl/widgets/news/personalized-news-listing.vtl"
                                data-dot-can-read="true"
                                data-dot-can-edit="true">
                            </div>
                            <h3>This is a title</h3>
                            <p>this is a paragraph</p>
                            <div
                                data-dot-object="edit-content"
                                data-dot-inode="bdf24784-fbea-478d-ad04-71159052037b"
                                data-dot-can-edit="true">
                            </div>
                        </div>
                    </div>
                    <div data-dot-object="contentlet" data-dot-identifier="tmpPlaceholder" id="contentletPlaceholder"></div>
                </div>


                <div data-dot-object="contentlet" data-dot-type="Banner" data-field-name="title">
                    <div class="dotedit-contentlet__toolbar">
                        <div data-dot-inode="123" data-dot-url="banner.vtl" data-dot-can-read="true" data-dot-can-edit="true">
                            <div>
                                <h1 data-test-id="inline-edit-element-title" data-mode="minimal" data-inode="123" data-field-name="title" data-language="1">Hello World</h1>
                                <h2 data-test-id="inline-edit-element-subtitle" data-mode="minimal" data-inode="123" data-field-name="caption" data-language="1">Hello Subtitle</h2>
                            </div>
                        </div>
                    </div>
                </div>


                <div data-dot-object="container" data-dot-identifier="321" data-dot-uuid="654" data-dot-can-add="CONTENT">
                    <div
                        data-dot-object="contentlet"
                        data-dot-identifier="456"
                        data-dot-inode="456"
                        data-dot-type="NewsWidgets"
                        data-dot-basetype="CONTENT"
                        data-dot-has-page-lang-version="true">
                        <div class="large-column">
                            <h3>This is a title</h3>
                            <p>this is a paragraph</p>
                            <p>this is other paragraph</p>
                            <p>this is another paragraph</p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row-2">
                <div data-dot-object="container" data-dot-identifier="976" data-dot-uuid="156" data-dot-can-add="CONTENT">
                    <div
                        data-dot-object="contentlet"
                        data-dot-identifier="367"
                        data-dot-inode="908"
                        data-dot-type="NewsWidgets"
                        data-dot-basetype="CONTENT"
                        data-dot-has-page-lang-version="true">
                        <div class="large-column">
                            <h3>This is a title</h3>
                            <p>this is a paragraph</p>
                            <p>this is other paragraph</p>
                            <p>this is another paragraph</p>
                        </div>
                    </div>
                </div>
            </div>
        </body>
        </html>
    `;
    let fakeIframeEl;

    const messageServiceMock = new MockDotMessageService({
        'editpage.content.contentlet.menu.drag': 'Drag',
        'editpage.content.contentlet.menu.edit': 'Edit',
        'editpage.content.contentlet.menu.remove': 'Remove',
        'editpage.content.container.action.add': 'Add',
        'editpage.content.container.menu.content': 'Content',
        'editpage.content.container.menu.widget': 'Widget',
        'editpage.content.container.menu.form': 'Form',
        'editpage.inline.error': 'An error ocurred',
        'dot.common.message.saved': 'All changes Saved'
    });

    let service: DotEditContentHtmlService;
    let dotEditContentToolbarHtmlService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;
    let mouseOverContentlet;
    let dotContainerContentletService: DotContainerContentletService;
    let dotEditPageService: DotEditPageService;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                DotEditContentHtmlService,
                DotContainerContentletService,
                DotEditContentToolbarHtmlService,
                DotDragDropAPIHtmlService,
                DotDOMHtmlUtilService,
                LoggerService,
                StringUtils,
                DotAlertConfirmService,
                ConfirmationService,
                DotGlobalMessageService,
                DotEventsService,
                DotEditPageService,
                {
                    provide: DotHttpErrorManagerService,
                    useValue: {
                        handle: jest.fn().mockReturnValue(of({}))
                    }
                },
                DotWorkflowActionsFireService,
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotLicenseService, useClass: MockDotLicenseService }
            ]
        });
        service = TestBed.inject(DotEditContentHtmlService);
        dotEditContentToolbarHtmlService = TestBed.inject(DotEditContentToolbarHtmlService);
        dotLicenseService = TestBed.inject(DotLicenseService);
        dotEditPageService = TestBed.inject(DotEditPageService);
        dotContainerContentletService = TestBed.inject(DotContainerContentletService);
        dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);

        fakeIframeEl = document.createElement('iframe');
        document.body.appendChild(fakeIframeEl);

        /*
                TODO: in the refactor we need to make this service just to generate and return stuff, pass the iframe
                is not a good architecture.
            */

        const pageState: DotPageRenderState = new DotPageRenderState(
            mockUser(),
            new DotPageRender({
                ...mockDotRenderedPage(),
                page: {
                    ...mockDotPage(),
                    rendered: fakeHTML
                }
            })
        );

        service.initEditMode(pageState, { nativeElement: fakeIframeEl });
        fakeDocument = fakeIframeEl.contentWindow.document;

        mouseOverContentlet = () => {
            const doc = service.iframe.nativeElement.contentDocument;
            doc.querySelector('[data-dot-object="contentlet"] h3').dispatchEvent(mouseoverEvent);
        };
    }));

    describe('same height containers', () => {
        let mockLayout;

        beforeEach(() => {
            mockLayout = mockDotLayout();
            mockLayout.body.rows = [
                {
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '123',
                                    uuid: '456'
                                }
                            ],
                            leftOffset: 1,
                            width: 8
                        },
                        {
                            containers: [
                                {
                                    identifier: '321',
                                    uuid: '654'
                                }
                            ],
                            leftOffset: 1,
                            width: 8
                        }
                    ]
                },
                {
                    columns: [
                        {
                            containers: [
                                {
                                    identifier: '976',
                                    uuid: '156'
                                }
                            ],
                            leftOffset: 1,
                            width: 8
                        }
                    ]
                }
            ];
        });

        xit('should redraw the body', () => {
            // TODO need to test the change of the body.style.style but right now not sure how.
        });
    });

    it('should add base tag', () => {
        const base = fakeDocument.querySelector('base');
        expect(base.outerHTML).toEqual('<base href="/an/url/">');
    });

    it('should add contentlet', () => {
        jest.spyOn(service, 'renderAddedContentlet');
        service.setContainterToAppendContentlet({
            identifier: '123',
            uuid: '456'
        });

        service.contentletEvents$.next({
            name: 'save',
            data: {
                identifier: '123',
                inode: ''
            }
        });

        expect(service.renderAddedContentlet).toHaveBeenCalledWith({
            identifier: '123',
            inode: ''
        });
    });

    it('should add uploaded DotAsset', () => {
        jest.spyOn(service, 'renderAddedContentlet');
        service.setContainterToAppendContentlet({
            identifier: '123',
            uuid: '456'
        });

        const dataObj = {
            contentlet: {
                identifier: '456',
                inode: '456'
            },
            placeholderId: 'id1'
        };

        service.contentletEvents$.next({
            name: 'add-uploaded-dotAsset',
            data: dataObj
        });

        expect(service.renderAddedContentlet).toHaveBeenCalledWith(
            {
                identifier: '456',
                inode: '456'
            },
            true
        );
    });

    it('should remove placeholder', () => {
        service.removeContentletPlaceholder();
        expect(fakeDocument.querySelector(CONTENTLET_PLACEHOLDER_SELECTOR)).toBeNull();
    });

    it('should handle http error', () => {
        const errorResponse = new HttpErrorResponse(
            new HttpResponse({
                body: null,
                status: HttpCode.FORBIDDEN,
                headers: null,
                url: ''
            })
        );

        service.contentletEvents$.next({
            name: 'handle-http-error',
            data: <any>errorResponse
        });

        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledWith(errorResponse);
        expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
    });

    it('should render relocated contentlet', () => {
        jest.spyOn(dotContainerContentletService, 'getContentletToContainer').mockReturnValue(
            of('<h1>new container</h1>')
        );
        const insertAdjacentElement = jest.fn();
        const replaceChild = jest.fn();

        const pageState: DotPageRenderState = new DotPageRenderState(
            mockUser(),
            new DotPageRender({
                ...mockDotRenderedPage(),
                page: {
                    ...mockDotPage(),
                    rendered: fakeHTML,
                    remoteRendered: false
                }
            })
        );

        service.setCurrentPage(pageState.page);

        service.initEditMode(pageState, {
            nativeElement: {
                ...fakeIframeEl,
                addEventListener: () => {
                    //
                },
                contentDocument: {
                    createElement: () => {
                        const el = document.createElement('div');
                        el.innerHTML = '<h1>new container</h1>';

                        return el;
                    },
                    open: () => {
                        //
                    },
                    close: () => {
                        //
                    },
                    write: () => {
                        //
                    },
                    querySelector: () => {
                        return {
                            tagName: 'DIV',
                            dataset: {
                                dotIdentifier: '888',
                                dotUuid: '999'
                            },
                            insertAdjacentElement,
                            parentNode: {
                                replaceChild,
                                dataset: {
                                    dotIdentifier: '123',
                                    dotUuid: '456'
                                }
                            }
                        };
                    }
                }
            }
        });

        const dataObj = {
            container: {
                identifier: '123',
                uuid: '456'
            },
            contentlet: {
                identifier: '456',
                inode: '456'
            }
        };

        service.contentletEvents$.next({
            name: 'relocate',
            data: dataObj
        });

        expect(insertAdjacentElement).toHaveBeenCalledWith(
            'afterbegin',
            expect.objectContaining({
                tagName: 'DIV',
                className: 'loader__overlay'
            })
        );

        expect(dotContainerContentletService.getContentletToContainer).toHaveBeenCalledWith(
            { identifier: '123', uuid: '456' },
            { identifier: '456', inode: '456' },
            pageState.page
        );

        expect(replaceChild).toHaveBeenCalledWith(
            expect.objectContaining({
                tagName: 'H1',
                innerHTML: 'new container'
            }),
            expect.objectContaining({
                tagName: 'DIV',
                dataset: {
                    dotIdentifier: '888',
                    dotUuid: '999'
                }
            })
        );
    });

    it('should show loading indicator on relocate contentlet', () => {
        jest.spyOn(dotContainerContentletService, 'getContentletToContainer').mockReturnValue(
            of('<div></div>')
        );

        const contentlet = service.iframe.nativeElement.contentDocument.querySelector(
            'div[data-dot-object="contentlet"][data-dot-inode="456"]'
        );

        service.contentletEvents$.next({
            name: 'relocate',
            data: {
                container: {
                    identifier: '123',
                    uuid: '456'
                },
                contentlet: {
                    identifier: '456',
                    inode: '456'
                }
            }
        });

        expect(contentlet.querySelector('.loader__overlay').innerHTML.trim()).toBe(
            '<div class="loader"></div>'
        );
    });

    it('should not render relocated contentlet', () => {
        jest.spyOn(dotContainerContentletService, 'getContentletToContainer').mockReturnValue(
            of('<h1>new container</h1>')
        );

        const pageState: DotPageRenderState = new DotPageRenderState(
            mockUser(),
            new DotPageRender({
                ...mockDotRenderedPage(),
                page: {
                    ...mockDotPage(),
                    rendered: fakeHTML,
                    remoteRendered: true
                }
            })
        );
        service.initEditMode(pageState, {
            nativeElement: fakeIframeEl
        });

        const dataObj = {
            container: {
                identifier: '123',
                uuid: '456'
            },
            contentlet: {
                identifier: '456',
                inode: '456'
            }
        };

        service.contentletEvents$.next({
            name: 'relocate',
            data: dataObj
        });

        expect(dotContainerContentletService.getContentletToContainer).not.toHaveBeenCalled();
    });

    it('should emit save when edit a piece of content outside a contentlet div', (done) => {
        service.iframeActions$.subscribe((res) => {
            expect(res).toEqual({
                name: 'save'
            });
            done();
        });

        service.renderEditedContentlet(null);
    });

    it('should render added contentlet', () => {
        let currentModel;

        const currentContainer = {
            identifier: '123',
            uuid: '456'
        };

        service.currentContainer = currentContainer;

        jest.spyOn(dotContainerContentletService, 'getContentletToContainer').mockReturnValue(
            of('<i>testing</i>')
        );

        const contentlet: DotPageContent = {
            identifier: '67',
            inode: '89',
            type: 'type',
            baseType: 'CONTENT'
        };

        service.pageModel$.subscribe((model) => (currentModel = model));

        service.renderAddedContentlet(contentlet);

        expect(dotContainerContentletService.getContentletToContainer).toHaveBeenCalledWith(
            currentContainer,
            contentlet,
            null
        );

        expect(service.currentContainer).toEqual(
            {
                identifier: '123',
                uuid: '456'
            },
            'currentContainer must be the same after add content'
        );
        expect(currentModel).toEqual(
            {
                model: [
                    { identifier: '123', uuid: '456', contentletsId: ['456'] },
                    { identifier: '321', uuid: '654', contentletsId: ['456'] },
                    { identifier: '976', uuid: '156', contentletsId: ['367'] }
                ],
                type: PageModelChangeEventType.ADD_CONTENT
            },
            'should tigger model change event'
        );
    });

    it('should render added Dot Asset', () => {
        let currentModel;

        const currentContainer = {
            identifier: '123',
            uuid: '456'
        };

        jest.spyOn(dotContainerContentletService, 'getContentletToContainer').mockReturnValue(
            of(
                '<div id="newContent" data-dot-object="contentlet" data-dot-identifier="zxc"><i>replaced contentlet</i></div>'
            )
        );

        const contentlet: DotPageContent = {
            identifier: '67',
            inode: '89',
            type: 'type',
            baseType: 'CONTENT'
        };

        service.pageModel$.subscribe((model) => (currentModel = model));

        service.renderAddedContentlet(contentlet, true);

        expect(dotContainerContentletService.getContentletToContainer).toHaveBeenCalledWith(
            currentContainer,
            contentlet,
            null
        );

        expect(service.currentContainer).toEqual(
            {
                identifier: '123',
                uuid: '456'
            },
            'currentContainer must be the same after add content'
        );

        expect(currentModel).toEqual(
            {
                model: [
                    { identifier: '123', uuid: '456', contentletsId: ['456', 'zxc'] },
                    { identifier: '321', uuid: '654', contentletsId: ['456'] },
                    { identifier: '976', uuid: '156', contentletsId: ['367'] }
                ],
                type: PageModelChangeEventType.ADD_CONTENT
            },
            'should tigger model change event'
        );
    });

    it('should remove contentlet and update container toolbar', () => {
        jest.spyOn(dotEditContentToolbarHtmlService, 'updateContainerToolbar');

        let currentModel;

        const contentlet: DotPageContent = {
            identifier: '367',
            inode: '908',
            type: 'NewsWidgets',
            baseType: 'CONTENT'
        };

        const container: DotPageContainer = {
            identifier: '976',
            uuid: '156'
        };

        service.pageModel$.subscribe((model) => (currentModel = model));

        service.removeContentlet(container, contentlet);

        expect(currentModel).toEqual(
            {
                model: [
                    {
                        identifier: '123',
                        uuid: '456',
                        contentletsId: ['456', 'tmpPlaceholder']
                    },
                    { identifier: '321', uuid: '654', contentletsId: ['456'] },
                    { identifier: '976', uuid: '156', contentletsId: [] }
                ],
                type: PageModelChangeEventType.REMOVE_CONTENT
            },
            'should tigger model change event'
        );

        expect(dotEditContentToolbarHtmlService.updateContainerToolbar).toHaveBeenCalledTimes(1);
    });

    it('should remove contentlet', () => {
        const remove = jest.fn();

        jest.spyOn<any>(fakeDocument, 'querySelectorAll').mockReturnValue([
            {
                remove: remove
            },
            {
                remove: remove
            }
        ]);

        service.currentContentlet = {
            identifier: '',
            inode: '123'
        };

        service.contentletEvents$.next({
            name: 'deleted-contenlet',
            data: null
        });

        expect(remove).toHaveBeenCalledTimes(2);
    });

    it('should show error message when the content already exists', () => {
        let currentModel = null;
        const currentContainer = {
            identifier: '123',
            uuid: '456'
        };

        service.currentContainer = currentContainer;

        jest.spyOn(dotContainerContentletService, 'getContentletToContainer').mockReturnValue(
            of('<i>testing</i>')
        );

        const dotDialogService = TestBed.inject(DotAlertConfirmService);
        jest.spyOn(dotDialogService, 'alert');

        const contentlet: DotPageContent = {
            identifier: '456',
            inode: '456',
            type: 'type',
            baseType: 'CONTENT'
        };

        service.pageModel$.subscribe((model) => (currentModel = model));

        service.renderAddedContentlet(contentlet);

        const doc = service.iframe.nativeElement.contentDocument;

        expect(doc.querySelector('.loader__overlay')).toBeNull();

        expect(dotContainerContentletService.getContentletToContainer).not.toHaveBeenCalled();
        expect(service.currentContainer).toBeNull('The current container must be null');
        expect(currentModel).toBeNull('should not tigger model change event');
        expect(dotDialogService.alert).toHaveBeenCalled();
    });

    it('should render edit contentlet', () => {
        window.top['changed'] = false;

        const currentContainer = {
            identifier: '123',
            uuid: '456'
        };

        const anotherContainer = {
            identifier: '321',
            uuid: '654'
        };

        service.currentContainer = currentContainer;
        const contentlet: DotPageContent = {
            identifier: '456',
            inode: '456',
            type: 'type',
            baseType: 'CONTENT'
        };

        jest.spyOn(dotContainerContentletService, 'getContentletToContainer').mockReturnValue(
            of(`
        <div data-dot-object="contentlet" data-dot-identifier="456">
            <script>
                console.log('First');
            </script>
            <div>
                <div>
                    <p>
                        <div>
                            <ul>
                                <li>
                                    <p>
                                        Text test
                                        <script>
                                            window.top['changed'] = true;
                                        </script>
                                    </p>
                                </li>
                            </ul>
                        </div>
                    </p>
                </div>
            </div>
        </div>`)
        );

        service.renderEditedContentlet(contentlet);

        expect(dotContainerContentletService.getContentletToContainer).toHaveBeenCalledWith(
            currentContainer,
            contentlet,
            null
        );
        expect(dotContainerContentletService.getContentletToContainer).toHaveBeenCalledWith(
            anotherContainer,
            contentlet,
            null
        );
        expect(window.top['changed']).toEqual(true);
    });

    // TODO needs to move this to a new describe to pass pageState.page.remoteRendered as true
    xit('should emit "save" event when remote rendered edit contentlet', (done) => {
        // service.remoteRendered = true;

        const contentlet: DotPageContent = {
            identifier: '456',
            inode: '456',
            type: 'type',
            baseType: 'CONTENT'
        };

        service.iframeActions$.subscribe((res) => {
            expect(res).toEqual({
                name: 'save'
            });
            done();
        });

        service.renderEditedContentlet(contentlet);
    });

    describe('document click', () => {
        beforeEach(() => {
            jest.spyOn(dotLicenseService, 'isEnterprise').mockReturnValue(of(true));
        });

        it('should open sub menu', () => {
            const button: HTMLButtonElement = <HTMLButtonElement>(
                fakeDocument.querySelector('[data-dot-object="popup-button"]')
            );
            button.click();
            expect(button.nextElementSibling.classList.contains('active')).toBe(true);
        });

        it('should emit iframe action to add content', () => {
            service.iframeActions$.subscribe((res) => {
                expect(res).toEqual({
                    name: 'add',
                    container: null,
                    dataset: button.dataset
                });
            });

            const button: HTMLButtonElement = <HTMLButtonElement>(
                fakeDocument.querySelector('[data-dot-object="popup-menu-item"]')
            );
            button.click();
        });

        it('should emit iframe action to edit content', () => {
            mouseOverContentlet();

            service.iframeActions$.subscribe((res) => {
                expect(res).toEqual({
                    name: 'edit',
                    container: container.dataset,
                    dataset: button.dataset
                });
            });

            const button: HTMLButtonElement = <HTMLButtonElement>(
                fakeDocument.querySelector('.dotedit-contentlet__edit')
            );
            const container = <HTMLElement>button.closest('div[data-dot-object="container"]');
            button.click();
        });

        it('should emit iframe action to remove content', () => {
            mouseOverContentlet();

            service.iframeActions$.subscribe((res) => {
                expect(res).toEqual({
                    name: 'remove',
                    container: container.dataset,
                    dataset: button.dataset
                });
            });
            const button: HTMLButtonElement = <HTMLButtonElement>(
                fakeDocument.querySelector('.dotedit-contentlet__remove')
            );
            const container = <HTMLElement>button.closest('div[data-dot-object="container"]');
            button.click();
        });

        it('should emit iframe action to edit vtl', () => {
            mouseOverContentlet();

            service.iframeActions$.subscribe((res) => {
                expect(res).toEqual({
                    name: 'code',
                    container: container.dataset,
                    dataset: button.dataset
                });
            });
            const button: HTMLButtonElement = <HTMLButtonElement>(
                fakeDocument.querySelector(
                    '.dotedit-contentlet__toolbar [data-dot-object="popup-menu-item"]'
                )
            );
            const container = <HTMLElement>button.closest('div[data-dot-object="container"]');
            button.click();

            expect(service.currentContentlet).toEqual({
                identifier: '456',
                inode: '456',
                type: 'NewsWidgets',
                baseType: 'CONTENT'
            });
        });
    });

    describe('inline editing', () => {
        let dotWorkflowActionsFireService: DotWorkflowActionsFireService;
        let dotGlobalMessageService: DotGlobalMessageService;

        beforeEach(() => {
            dotWorkflowActionsFireService = TestBed.inject(DotWorkflowActionsFireService);
            dotGlobalMessageService = TestBed.inject(DotGlobalMessageService);
        });

        it('should return the content if an error occurs', () => {
            const fakeElem: HTMLElement = fakeDocument.querySelector(
                '[data-test-id="inline-edit-element-title"]'
            );

            const error404 = mockResponseView(404);
            jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(
                throwError(error404)
            );

            const events = ['focus', 'blur'];
            events.forEach((event) => {
                service.contentletEvents$.next({
                    name: 'inlineEdit',
                    data: {
                        eventType: event,
                        innerHTML: event === 'focus' ? fakeElem.innerHTML : '<div>hello</div>',
                        isNotDirty: false,
                        dataset: {
                            fieldName: 'title',
                            inode: '999',
                            language: '1',
                            mode: 'full'
                        },
                        element: fakeElem
                    }
                });
            });

            expect(fakeElem.innerHTML).toBe('Hello World');
        });

        it('should call saveContentlet and save the content', () => {
            jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(of({}));
            jest.spyOn(dotGlobalMessageService, 'success');
            const fakeElem: HTMLElement = fakeDocument.querySelector(
                '[data-test-id="inline-edit-element-title"]'
            );

            service.contentletEvents$.next({
                name: 'inlineEdit',
                data: {
                    eventType: 'blur',
                    innerHTML: '<div>hello</div>',
                    isNotDirty: false,
                    dataset: {
                        fieldName: 'title',
                        inode: '999',
                        language: '1',
                        mode: 'full'
                    },
                    element: fakeElem
                }
            });

            expect(dotWorkflowActionsFireService.saveContentlet).toHaveBeenCalledWith({
                title: '<div>hello</div>',
                inode: '999'
            });

            expect(dotGlobalMessageService.success).toHaveBeenCalledWith('All changes Saved');
            expect(dotGlobalMessageService.success).toHaveBeenCalledTimes(1);
        });

        it('should not call saveContentlet if isNotDirty is true', () => {
            jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(of({}));
            const fakeElem: HTMLElement = fakeDocument.querySelector(
                '[data-test-id="inline-edit-element-title"]'
            );

            service.contentletEvents$.next({
                name: 'inlineEdit',
                data: {
                    eventType: 'blur',
                    innerHTML: '<div>hello</div>',
                    isNotDirty: true,
                    dataset: {
                        fieldName: 'title',
                        inode: '999',
                        language: '1',
                        mode: 'full'
                    },
                    element: fakeElem
                }
            });

            expect(dotWorkflowActionsFireService.saveContentlet).not.toHaveBeenCalled();
        });

        it('should display a toast on error', () => {
            const error404 = mockResponseView(404, '', null, {
                errors: [{ message: 'An error ocurred' }]
            });
            jest.spyOn(dotWorkflowActionsFireService, 'saveContentlet').mockReturnValue(
                throwError(error404)
            );
            jest.spyOn(dotGlobalMessageService, 'error');

            const fakeElem: HTMLElement = fakeDocument.querySelector(
                '[data-test-id="inline-edit-element-title"]'
            );

            service.contentletEvents$.next({
                name: 'inlineEdit',
                data: {
                    eventType: 'blur',
                    innerHTML: '<div>hello</div>',
                    isNotDirty: false,
                    dataset: {
                        fieldName: 'title',
                        inode: '999',
                        language: '1',
                        mode: 'full'
                    },
                    element: fakeElem
                }
            });

            expect(dotGlobalMessageService.error).toHaveBeenCalledWith('An error ocurred');
            expect(dotGlobalMessageService.error).toHaveBeenCalledTimes(1);
        });
    });

    describe('edit contentlets', () => {
        beforeEach(() => {
            jest.spyOn(service, 'renderEditedContentlet');
        });

        it('should render main contentlet edit', () => {
            mouseOverContentlet();

            service.iframeActions$.subscribe((res) => {
                expect(JSON.parse(JSON.stringify(res))).toEqual({
                    name: 'edit',
                    dataset: {
                        dotIdentifier: '456',
                        dotInode: '456',
                        dotObject: 'edit-content'
                    },
                    container: {
                        dotObject: 'container',
                        dotIdentifier: '123',
                        dotUuid: '456',
                        dotCanAdd: 'CONTENT'
                    }
                });
            });

            const button: HTMLButtonElement = <HTMLButtonElement>(
                fakeDocument.querySelector('.dotedit-contentlet__edit')
            );
            button.click();

            service.contentletEvents$.next({
                name: 'save',
                data: {
                    identifier: '456',
                    inode: '999'
                }
            });

            const doc: HTMLElement = <HTMLElement>fakeDocument.querySelector('html');
            expect(doc.id).toContain('iframeId');
            expect(service.renderEditedContentlet).toHaveBeenCalledWith({
                identifier: '456',
                inode: '999',
                type: 'NewsWidgets',
                baseType: 'CONTENT'
            });
        });

        it('should render edit vtl', () => {
            mouseOverContentlet();

            service.iframeActions$.subscribe((res) => {
                expect(JSON.parse(JSON.stringify(res))).toEqual({
                    name: 'code',
                    dataset: {
                        dotObject: 'popup-menu-item',
                        dotAction: 'code',
                        dotInode: '345274e0-3bbb-41f1-912c-b398d5745b9a'
                    },
                    container: {
                        dotObject: 'container',
                        dotIdentifier: '123',
                        dotUuid: '456',
                        dotCanAdd: 'CONTENT'
                    }
                });
            });

            const button = fakeIframeEl.contentWindow.document.querySelector(
                '[data-dot-action="code"][data-dot-object="popup-menu-item"]'
            );
            button.click();

            service.contentletEvents$.next({
                name: 'save',
                data: {
                    identifier: '456',
                    inode: '888'
                }
            });

            expect(service.renderEditedContentlet).toHaveBeenCalledWith({
                identifier: '456',
                inode: '456',
                type: 'NewsWidgets',
                baseType: 'CONTENT'
            });
        });

        it('should render internal contentlet edit', () => {
            mouseOverContentlet();
            service.iframeActions$.subscribe((res) => {
                expect(JSON.stringify(res)).toEqual(
                    JSON.stringify({
                        name: 'edit',
                        dataset: {
                            dotIdentifier: '456',
                            dotInode: '456',
                            dotObject: 'edit-content'
                        },
                        container: {
                            dotObject: 'container',
                            dotIdentifier: '123',
                            dotUuid: '456',
                            dotCanAdd: 'CONTENT'
                        }
                    })
                );
            });

            const button = fakeIframeEl.contentWindow.document.querySelector(
                '[data-dot-object="edit-content"]'
            );

            button.click();

            service.contentletEvents$.next({
                name: 'save',
                data: {
                    identifier: '34345',
                    inode: '67789'
                }
            });

            expect(service.renderEditedContentlet).toHaveBeenCalledWith({
                identifier: '456',
                inode: '67789',
                type: 'NewsWidgets',
                baseType: 'CONTENT'
            });
        });
    });

    describe('render Form', () => {
        const form: DotCMSContentType = {
            ...dotcmsContentTypeBasicMock,
            clazz: DotCMSClazzes.TEXT,
            defaultType: true,
            fixed: true,
            folder: 'folder',
            host: 'host',
            name: 'name',
            owner: 'owner',
            system: false,
            baseType: 'form',
            id: '2',
            variable: 'test123'
        };

        const currentContainer = {
            identifier: '123',
            uuid: '456'
        };

        let dotGlobalMessageService: DotGlobalMessageService;

        beforeEach(() => {
            jest.spyOn(service, 'renderEditedContentlet');

            service.currentContainer = currentContainer;
            dotGlobalMessageService = TestBed.inject(DotGlobalMessageService);
        });

        it('should render added form', () => {
            jest.spyOn(dotContainerContentletService, 'getFormToContainer').mockReturnValue(
                of({
                    render: '<i>testing</i>',
                    content: {
                        identifier: '2',
                        inode: '123'
                    }
                })
            );

            service.renderAddedForm('4');

            expect(dotGlobalMessageService.success).toHaveBeenCalledTimes(1);
            expect(dotEditPageService.save).toHaveBeenCalledTimes(1);

            expect<any>(dotContainerContentletService.getFormToContainer).toHaveBeenCalledWith(
                currentContainer,
                {
                    ...form,
                    id: '4'
                }
            );

            expect(service.currentContainer).toEqual(
                {
                    identifier: '123',
                    uuid: '456'
                },
                'currentContainer must be the same after add form'
            );
        });

        it('should show content added message', () => {
            jest.spyOn(dotContainerContentletService, 'getFormToContainer').mockReturnValue(
                of({
                    render: '<i>testing</i>',
                    content: {
                        identifier: '4',
                        inode: '123'
                    }
                })
            );

            service.renderAddedForm(form.id);

            const doc = service.iframe.nativeElement.contentDocument;
            expect(doc.querySelector('.loader__overlay')).toBeNull();
        });
    });

    describe('errors', () => {
        let httpErrorManagerService: DotHttpErrorManagerService;

        describe('Error on save', () => {
            it('should handle error on save and emit SAVE_ERROR Event Type', (done) => {
                const errorResponse = {
                    error: { message: 'error' }
                } as HttpErrorResponse;
                jest.spyOn(dotEditPageService, 'save').mockReturnValue(throwError(errorResponse));
                jest.spyOn(httpErrorManagerService, 'handle');

                service.pageModel$.subscribe((model) => {
                    expect(model.type).toEqual(PageModelChangeEventType.SAVE_ERROR);
                    done();
                });

                service.renderAddedContentlet({ identifier: '123', inode: '' });

                expect(httpErrorManagerService.handle).toHaveBeenCalledWith(errorResponse);
                expect(httpErrorManagerService.handle).toHaveBeenCalledTimes(1);
            });
        });
    });

    afterEach(() => {
        document.body.removeChild(fakeIframeEl);
    });
});
