import { of, Observable } from 'rxjs';
import { async, TestBed } from '@angular/core/testing';
import { DotEditContentHtmlService, DotContentletAction } from './dot-edit-content-html.service';
import { DotEditContentToolbarHtmlService } from '../html/dot-edit-content-toolbar-html.service';
import { DotContainerContentletService } from '../dot-container-contentlet.service';
import { DotDragDropAPIHtmlService } from '../html/dot-drag-drop-api-html.service';
import { DotDOMHtmlUtilService } from '../html/dot-dom-html-util.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { LoggerService, StringUtils, CoreWebService } from 'dotcms-js';
import { DotAlertConfirmService } from '@services/dot-alert-confirm/dot-alert-confirm.service';
import { DotPageContent } from '../../../../dot-edit-page/shared/models/dot-page-content.model';
import {
    mockDotLayout,
    mockDotRenderedPage,
    mockDotPage
} from '../../../../../test/dot-page-render.mock';
import { DotCMSContentType } from 'dotcms-models';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { Injectable } from '@angular/core';
import { DotPageRenderState } from '@portlets/dot-edit-page/shared/models/dot-rendered-page-state.model';
import { mockUser } from '../../../../../test/login-service.mock';
import { PageModelChangeEventType } from './models';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';
import { DotPageRender, DotPageContainer } from '@portlets/dot-edit-page/shared/models';
import { CoreWebServiceMock } from 'projects/dotcms-js/src/lib/core/core-web.service.mock';
import { Http, ConnectionBackend, RequestOptions, BaseRequestOptions } from '@angular/http';
import { MockBackend } from '@angular/http/testing';
import { ConfirmationService } from 'primeng/api';

@Injectable()
class MockDotLicenseService {
    isEnterprise(): Observable<boolean> {
        return of(false);
    }
}

describe('DotEditContentHtmlService', () => {
    let dotLicenseService: DotLicenseService;
    let fakeDocument: Document;

    const fakeHTML = `
        <html>
        <head>
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
        'editpage.content.container.menu.form': 'Form'
    });

    beforeEach(async(() => {
        this.injector = TestBed.configureTestingModule({
            providers: [
                DotEditContentHtmlService,
                DotContainerContentletService,
                DotEditContentToolbarHtmlService,
                DotDragDropAPIHtmlService,
                DotDOMHtmlUtilService,
                LoggerService,
                StringUtils,
                DotAlertConfirmService,
                Http,
                DotAlertConfirmService,
                ConfirmationService,
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: ConnectionBackend, useClass: MockBackend },
                { provide: RequestOptions, useClass: BaseRequestOptions },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: DotLicenseService, useClass: MockDotLicenseService }
            ]
        });
        this.dotEditContentHtmlService = <DotEditContentHtmlService>(
            this.injector.get(DotEditContentHtmlService)
        );
        this.dotEditContentToolbarHtmlService = this.injector.get(DotEditContentToolbarHtmlService);

        fakeIframeEl = document.createElement('iframe');
        document.body.appendChild(fakeIframeEl);
        fakeIframeEl.contentWindow.document.open();
        fakeIframeEl.contentWindow.document.write('');
        fakeIframeEl.contentWindow.document.close();
        /*
            TODO: in the refactor we need to make this service just to generate and return stuff, pass the iframe
            is not a good architecture.
        */

        const pageState: DotPageRenderState = new DotPageRenderState(
            mockUser,
            new DotPageRender({
                ...mockDotRenderedPage,
                page: {
                    ...mockDotPage,
                    rendered: fakeHTML
                }
            })
        );

        this.dotEditContentHtmlService.initEditMode(pageState, { nativeElement: fakeIframeEl });
        dotLicenseService = this.injector.get(DotLicenseService);
        fakeDocument = fakeIframeEl.contentWindow.document;
    }));

    describe('same height containers', () => {
        let mockLayout;
        const querySelector1 = [
            `div[data-dot-object="container"]`,
            `[data-dot-identifier="123"]`,
            `[data-dot-uuid="456"]`
        ].join('');
        const querySelector2 = [
            `div[data-dot-object="container"]`,
            `[data-dot-identifier="321"]`,
            `[data-dot-uuid="654"]`
        ].join('');
        const querySelector3 = [
            `div[data-dot-object="container"]`,
            `[data-dot-identifier="976"]`,
            `[data-dot-uuid="156"]`
        ].join('');

        beforeEach(() => {
            mockLayout = JSON.parse(JSON.stringify(mockDotLayout));
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

        it('should set same height to containers when more than one per row', () => {
            this.dotEditContentHtmlService.setContaintersSameHeight(mockLayout);

            const firstContainer = this.dotEditContentHtmlService
                .getEditPageDocument()
                .querySelector(querySelector1);
            const secondContainer = this.dotEditContentHtmlService
                .getEditPageDocument()
                .querySelector(querySelector2);
            const thirdContainer = this.dotEditContentHtmlService
                .getEditPageDocument()
                .querySelector(querySelector3);
            expect(firstContainer.style.height).not.toBe('');
            expect(firstContainer.offsetHeight).toEqual(secondContainer.offsetHeight);
            expect(thirdContainer.style.height).toEqual('');
        });

        it('should not set same height to containers when only one row of containers', () => {
            mockLayout.body.rows[0].columns.pop();
            this.dotEditContentHtmlService.setContaintersSameHeight(mockLayout);

            const firstContainer = this.dotEditContentHtmlService
                .getEditPageDocument()
                .querySelector(querySelector1);
            const secondContainer = this.dotEditContentHtmlService
                .getEditPageDocument()
                .querySelector(querySelector2);
            expect(firstContainer.style.height).toBe('');
            expect(secondContainer.style.height).toBe('');
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
        spyOn(this.dotEditContentHtmlService, 'renderAddedContentlet');
        this.dotEditContentHtmlService.setContainterToAppendContentlet({
            identifier: '123',
            uuid: '456'
        });

        this.dotEditContentHtmlService.contentletEvents$.next({
            name: 'save',
            data: {
                identifier: '123'
            }
        });

        expect(this.dotEditContentHtmlService.renderAddedContentlet).toHaveBeenCalledWith(
            {
                identifier: '123'
            },
            PageModelChangeEventType.ADD_CONTENT
        );
    });

    it('should render relocated contentlet', () => {
        const dotContainerContentletService = this.injector.get(DotContainerContentletService);
        spyOn(dotContainerContentletService, 'getContentletToContainer').and.callThrough();
        spyOn(this.dotEditContentHtmlService, 'renderRelocatedContentlet').and.callThrough();

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

        this.dotEditContentHtmlService.contentletEvents$.next({
            name: 'relocate',
            data: dataObj
        });

        expect(this.dotEditContentHtmlService.renderRelocatedContentlet).toHaveBeenCalledWith(
            dataObj
        );
        expect(dotContainerContentletService.getContentletToContainer).toHaveBeenCalledWith(
            dataObj.container,
            dataObj.contentlet
        );
    });

    it('should show loading indicator on relocate contentlet', () => {
        const dotContainerContentletService = this.injector.get(DotContainerContentletService);
        spyOn(dotContainerContentletService, 'getContentletToContainer').and.returnValue(
            of('<div></div>')
        );

        const contentlet = this.dotEditContentHtmlService.iframe.nativeElement.contentDocument.querySelector(
            'div[data-dot-object="contentlet"][data-dot-inode="456"]'
        );

        this.dotEditContentHtmlService.contentletEvents$.next({
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
        spyOn(this.dotEditContentHtmlService, 'renderRelocatedContentlet').and.callThrough();

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

        const pageState: DotPageRenderState = new DotPageRenderState(
            mockUser,
            new DotPageRender({
                ...mockDotRenderedPage,
                page: {
                    ...mockDotPage,
                    rendered: fakeHTML,
                    remoteRendered: true
                }
            })
        );
        this.dotEditContentHtmlService.initEditMode(pageState, { nativeElement: fakeIframeEl });

        this.dotEditContentHtmlService.contentletEvents$.next({
            name: 'relocate',
            data: dataObj
        });

        expect(this.dotEditContentHtmlService.renderRelocatedContentlet).not.toHaveBeenCalled();
    });

    it('should emit save when edit a piece of content outside a contentlet div', (done) => {
        this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
            expect(res).toEqual({
                name: 'save'
            });
            done();
        });

        this.dotEditContentHtmlService.renderEditedContentlet(null);
    });

    it('should render added contentlet', () => {
        const modelExpected = [
            {
                identifier: '123',
                uuid: '456',
                contentletsId: ['3']
            }
        ];

        let currentModel;
        const currentContainer = {
            identifier: '123',
            uuid: '456'
        };

        this.dotEditContentHtmlService.currentContainer = currentContainer;
        this.dotEditContentHtmlService.currentAction = DotContentletAction.ADD;

        const dotEditContentToolbarHtmlService = this.injector.get(DotContainerContentletService);
        spyOn(dotEditContentToolbarHtmlService, 'getContentletToContainer').and.returnValue(
            of('<i>testing</i>')
        );
        spyOn(this.dotEditContentToolbarHtmlService, 'updateContainerToolbar');

        const contentlet: DotPageContent = {
            identifier: '67',
            inode: '89',
            type: 'type',
            baseType: 'CONTENT'
        };

        this.dotEditContentHtmlService.pageModel$.subscribe((model) => (currentModel = model));

        this.dotEditContentHtmlService.renderAddedContentlet(
            contentlet,
            PageModelChangeEventType.ADD_CONTENT
        );

        expect(this.dotEditContentHtmlService.currentAction === DotContentletAction.EDIT).toBe(
            true,
            'update the action after content creation'
        );

        expect(dotEditContentToolbarHtmlService.getContentletToContainer).toHaveBeenCalledWith(
            currentContainer,
            contentlet
        );

        expect(this.dotEditContentHtmlService.currentContainer).toEqual(
            {
                identifier: '123',
                uuid: '456'
            },
            'currentContainer must be the same after add content'
        );

        expect(currentModel).toEqual(
            {
                model: modelExpected,
                type: PageModelChangeEventType.ADD_CONTENT
            },
            'should tigger model change event'
        );

        expect(this.dotEditContentToolbarHtmlService.updateContainerToolbar).toHaveBeenCalledTimes(
            1
        );
    });

    it('should remove contentlet and update container toolbar', () => {
        spyOn(this.dotEditContentToolbarHtmlService, 'updateContainerToolbar');
        let currentModel;
        const currentContainer = [
            {
                identifier: '123',
                uuid: '456',
                contentletsId: ['3']
            }
        ];

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

        this.dotEditContentHtmlService.pageModel$.subscribe((model) => (currentModel = model));

        this.dotEditContentHtmlService.removeContentlet(container, contentlet);

        expect(currentModel).toEqual(
            {
                model: currentContainer,
                type: PageModelChangeEventType.REMOVE_CONTENT
            },
            'should tigger model change event'
        );

        expect(this.dotEditContentToolbarHtmlService.updateContainerToolbar).toHaveBeenCalledTimes(
            1
        );
    });

    it('should remove contentlet', () => {
        const remove = jasmine.createSpy('deleted');

        spyOn(fakeDocument, 'querySelectorAll').and.returnValue([
            {
                remove: remove
            },
            {
                remove: remove
            }
        ]);

        this.dotEditContentHtmlService.currentContentlet = {
            inode: '123'
        };

        this.dotEditContentHtmlService.contentletEvents$.next({
            name: 'deleted-contenlet'
        });

        expect(remove).toHaveBeenCalledTimes(2);
    });

    it('should show error message when the content already exists', () => {
        let currentModel = null;
        const currentContainer = {
            identifier: '123',
            uuid: '456'
        };

        this.dotEditContentHtmlService.currentContainer = currentContainer;

        const dotEditContentToolbarHtmlService = this.injector.get(DotContainerContentletService);
        spyOn(dotEditContentToolbarHtmlService, 'getContentletToContainer').and.returnValue(
            of('<i>testing</i>')
        );

        const dotDialogService = this.injector.get(DotAlertConfirmService);
        spyOn(dotDialogService, 'alert');

        const contentlet: DotPageContent = {
            identifier: '456',
            inode: '456',
            type: 'type',
            baseType: 'CONTENT'
        };

        this.dotEditContentHtmlService.pageModel$.subscribe((model) => (currentModel = model));

        this.dotEditContentHtmlService.renderAddedContentlet(contentlet);

        const doc = this.dotEditContentHtmlService.iframe.nativeElement.contentDocument;

        expect(doc.querySelector('.loader__overlay')).toBeNull();

        expect(dotEditContentToolbarHtmlService.getContentletToContainer).not.toHaveBeenCalled();
        expect(this.dotEditContentHtmlService.currentContainer).toBeNull(
            'The current container must be null'
        );
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

        this.dotEditContentHtmlService.currentContainer = currentContainer;
        const contentlet: DotPageContent = {
            identifier: '456',
            inode: '456',
            type: 'type',
            baseType: 'CONTENT'
        };

        const dotEditContentToolbarHtmlService = this.injector.get(DotContainerContentletService);
        spyOn(dotEditContentToolbarHtmlService, 'getContentletToContainer').and.returnValue(
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

        this.dotEditContentHtmlService.renderEditedContentlet(contentlet);

        expect(dotEditContentToolbarHtmlService.getContentletToContainer).toHaveBeenCalledWith(
            currentContainer,
            contentlet
        );
        expect(dotEditContentToolbarHtmlService.getContentletToContainer).toHaveBeenCalledWith(
            anotherContainer,
            contentlet
        );
        expect(window.top['changed']).toEqual(true);
    });

    it('should emit "save" event when remote rendered edit contentlet', (done) => {
        this.dotEditContentHtmlService.remoteRendered = true;

        const contentlet: DotPageContent = {
            identifier: '456',
            inode: '456',
            type: 'type',
            baseType: 'CONTENT'
        };

        this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
            expect(res).toEqual({
                name: 'save'
            });
            done();
        });

        this.dotEditContentHtmlService.renderEditedContentlet(contentlet);
    });

    describe('document click', () => {
        beforeEach(() => {
            spyOn(dotLicenseService, 'isEnterprise').and.returnValue(of(true));
        });

        it('should open sub menu', () => {
            const button: HTMLButtonElement = <HTMLButtonElement>(
                fakeDocument.querySelector('[data-dot-object="popup-button"]')
            );
            button.click();
            expect(button.nextElementSibling.classList.contains('active')).toBe(true);
        });

        it('should emit iframe action to add content', () => {
            this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
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
            this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
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
            this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
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
            this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
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

            expect(this.dotEditContentHtmlService.currentContentlet).toEqual({
                identifier: '456',
                inode: '456',
                type: 'NewsWidgets',
                baseType: 'CONTENT'
            });
        });
    });

    describe('edit contentlets', () => {
        beforeEach(() => {
            spyOn(this.dotEditContentHtmlService, 'renderEditedContentlet');
        });

        it('should render main contentlet edit', () => {
            this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
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

            this.dotEditContentHtmlService.contentletEvents$.next({
                name: 'save',
                data: {
                    identifier: '456',
                    inode: '999'
                }
            });

            const doc: HTMLElement = <HTMLElement>fakeDocument.querySelector('html');
            expect(doc.id).toContain('iframeId');
            expect(this.dotEditContentHtmlService.renderEditedContentlet).toHaveBeenCalledWith({
                identifier: '456',
                inode: '999',
                type: 'NewsWidgets',
                baseType: 'CONTENT'
            });
        });

        it('should render edit vtl', () => {
            this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
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

            this.dotEditContentHtmlService.contentletEvents$.next({
                name: 'save',
                data: {
                    identifier: '456',
                    inode: '888'
                }
            });

            expect(this.dotEditContentHtmlService.renderEditedContentlet).toHaveBeenCalledWith({
                identifier: '456',
                inode: '456',
                type: 'NewsWidgets',
                baseType: 'CONTENT'
            });
        });

        it('should render internal contentlet edit', () => {
            this.dotEditContentHtmlService.iframeActions$.subscribe((res) => {
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

            const button = fakeIframeEl.contentWindow.document.querySelector(
                '[data-dot-object="edit-content"]'
            );

            button.click();

            this.dotEditContentHtmlService.contentletEvents$.next({
                name: 'save',
                data: {
                    identifier: '34345',
                    inode: '67789'
                }
            });

            expect(this.dotEditContentHtmlService.renderEditedContentlet).toHaveBeenCalledWith({
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
            clazz: 'clazz',
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

        beforeEach(() => {
            spyOn(this.dotEditContentHtmlService, 'renderEditedContentlet');

            this.dotEditContentHtmlService.currentContainer = currentContainer;
        });

        it('should render added form', () => {
            const modelExpected = [
                {
                    identifier: '123',
                    uuid: '456',
                    contentletsId: ['3']
                }
            ];

            const dotEditContentToolbarHtmlService = this.injector.get(
                DotContainerContentletService
            );

            spyOn(dotEditContentToolbarHtmlService, 'getFormToContainer').and.returnValue(
                of({
                    render: '<i>testing</i>',
                    content: {
                        identifier: '2',
                        inode: '123'
                    }
                })
            );

            this.dotEditContentHtmlService
                .renderAddedForm({ ...form, id: 4 })
                .subscribe((model) => {
                    expect(model).toEqual(modelExpected, 'should tigger model change event');
                });

            expect(dotEditContentToolbarHtmlService.getFormToContainer).toHaveBeenCalledWith(
                currentContainer,
                {
                    ...form,
                    id: 4
                }
            );

            expect(this.dotEditContentHtmlService.currentContainer).toEqual(
                {
                    identifier: '123',
                    uuid: '456'
                },
                'currentContainer must be the same after add form'
            );
        });

        it('should show content added message', () => {
            const dotEditContentToolbarHtmlService = this.injector.get(
                DotContainerContentletService
            );

            spyOn(dotEditContentToolbarHtmlService, 'getFormToContainer').and.returnValue(
                of({
                    render: '<i>testing</i>',
                    content: {
                        identifier: '4',
                        inode: '123'
                    }
                })
            );

            this.dotEditContentHtmlService.renderAddedForm(form).subscribe((model) => {
                expect(model).toBeNull();
            });

            const doc = this.dotEditContentHtmlService.iframe.nativeElement.contentDocument;
            expect(doc.querySelector('.loader__overlay')).toBeNull();
        });
    });
    afterEach(() => {
        document.body.removeChild(fakeIframeEl);
    });
});
