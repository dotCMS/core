import { describe, it, expect } from '@jest/globals';
import {
    Spectator,
    createComponentFactory,
    SpyObject,
    byTestId,
    mockProvider
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';

import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import {
    DotAlertConfirmService,
    DotContentTypeService,
    DotHttpErrorManagerService,
    DotIframeService,
    DotMessageService,
    DotWorkflowActionsFireService,
    PushPublishService
} from '@dotcms/data-access';
import { CoreWebService, DotcmsConfigService, DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotCMSBaseTypesContentTypes, DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotContentCompareComponent } from '@dotcms/portlets/dot-ema/ui';
import {
    DotcmsConfigServiceMock,
    DotcmsEventsServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotEmaDialogComponent } from './dot-ema-dialog.component';
import { DotEmaDialogStore } from './store/dot-ema-dialog.store';

import { DotActionUrlService } from '../../services/dot-action-url/dot-action-url.service';
import { DotEmaWorkflowActionsService } from '../../services/dot-ema-workflow-actions/dot-ema-workflow-actions.service';
import { FormStatus, NG_CUSTOM_EVENTS } from '../../shared/enums';
import { MOCK_RESPONSE_HEADLESS, PAYLOAD_MOCK } from '../../shared/mocks';
import { DotPage } from '../../shared/models';

describe('DotEmaDialogComponent', () => {
    let spectator: Spectator<DotEmaDialogComponent>;
    let component: DotEmaDialogComponent;
    let storeSpy: SpyObject<DotEmaDialogStore>;
    let workflowActionEventHandler: SpyObject<DotEmaWorkflowActionsService>;

    const triggerIframeCustomEvent = (
        customEvent: {
            detail: {
                name: string;
                payload: unknown;
                data?: unknown;
            };
        } = {
            detail: {
                name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                payload: {
                    htmlPageReferer: '/my-awesome-page'
                }
            }
        }
    ) => {
        const dialogIframe = spectator.debugElement.query(By.css('[data-testId="dialog-iframe"]'));

        spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

        dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
            new CustomEvent('ng-event', {
                ...customEvent
            })
        );
        spectator.detectChanges();
    };

    const createComponent = createComponentFactory({
        component: DotEmaDialogComponent,
        imports: [HttpClientTestingModule],
        providers: [
            DotEmaDialogStore,
            HttpClient,
            DotWorkflowActionsFireService,
            MessageService,
            {
                provide: DotcmsConfigService,
                useValue: new DotcmsConfigServiceMock()
            },
            {
                provide: DotcmsEventsService,
                useValue: new DotcmsEventsServiceMock()
            },
            {
                provide: PushPublishService,
                useValue: {
                    getEnvironments() {
                        return of([
                            {
                                id: '123',
                                name: 'Environment 1'
                            },
                            {
                                id: '456',
                                name: 'Environment 2'
                            }
                        ]);
                    }
                }
            },
            {
                provide: DotActionUrlService,
                useValue: {
                    getCreateContentletUrl: jest
                        .fn()
                        .mockReturnValue(of('https://demo.dotcms.com/jsp.jsp'))
                }
            },
            {
                provide: CoreWebService,
                useValue: {
                    requestView: jest.fn().mockReturnValue(of({}))
                }
            },
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({})
            },
            mockProvider(DotContentTypeService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotAlertConfirmService),
            mockProvider(DotIframeService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;
        storeSpy = spectator.inject(DotEmaDialogStore, true);
        workflowActionEventHandler = spectator.inject(DotEmaWorkflowActionsService, true);

        jest.spyOn(workflowActionEventHandler, 'handleWorkflowAction').mockImplementation(() =>
            of({})
        );
    });

    describe('DOM', () => {
        it('should make dialog visible', () => {
            component.addContentlet(PAYLOAD_MOCK);
            spectator.detectChanges();
            expect(spectator.query(byTestId('dialog'))).not.toBeNull();
        });

        it("should make the form selector visible when it's a form", () => {
            component.addForm(PAYLOAD_MOCK);
            spectator.detectChanges();
            expect(spectator.query(byTestId('form-selector'))).not.toBeNull();
        });

        it("should make the iframe visible when it's not a form", () => {
            component.addContentlet(PAYLOAD_MOCK);
            spectator.detectChanges();
            expect(spectator.query(byTestId('dialog-iframe'))).not.toBeNull();
        });
    });

    describe('outputs', () => {
        it('should dispatch custom events', () => {
            const customEventSpy = jest.spyOn(component.action, 'emit');

            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent();

            expect(customEventSpy).toHaveBeenCalledWith({
                event: expect.objectContaining({
                    isTrusted: false
                }),
                payload: PAYLOAD_MOCK,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
        });

        it('should dispatch onHide when p-dialog hide', () => {
            const actionSpy = jest.spyOn(component.action, 'emit');

            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            spectator.triggerEventHandler(Dialog, 'visibleChange', false);

            expect(actionSpy).toHaveBeenCalledWith({
                event: new CustomEvent('ng-event', {
                    detail: {
                        name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
                    }
                }),
                payload: PAYLOAD_MOCK,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                }
            });
        });
    });

    describe('component methods', () => {
        it("should trigger handleWorkflowEvent when the iframe's custom event is 'workflow-wizard'", () => {
            const handleWorkflowEventSpy = jest.spyOn(component, 'handleWorkflowEvent');

            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent({
                detail: {
                    name: NG_CUSTOM_EVENTS.OPEN_WIZARD,
                    data: {},
                    payload: {}
                }
            });

            expect(handleWorkflowEventSpy).toHaveBeenCalledWith({});
        });

        it("should trigger setDirty in the store when the iframe's custom event is 'edit-contentlet-data-updated' and is not a translation", () => {
            const setDirtySpy = jest.spyOn(storeSpy, 'setDirty');

            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent({
                detail: {
                    name: NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED,
                    data: {},
                    payload: {}
                }
            });

            expect(setDirtySpy).toHaveBeenCalled();
        });

        it("should trigger setSaved in the store when the iframe's custom event is 'edit-contentlet-data-updated' and is a translation", () => {
            const setSavedSpy = jest.spyOn(storeSpy, 'setSaved');

            component.translatePage({
                page: MOCK_RESPONSE_HEADLESS.page,
                newLanguage: '3'
            }); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent({
                detail: {
                    name: NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED,
                    data: {},
                    payload: {}
                }
            });

            expect(setSavedSpy).toHaveBeenCalled();
        });

        it("should trigger setSaved in the store when the iframe's custom event is 'edit-contentlet-data-updated', is a translation and payload is move action", () => {
            const reloadIframeSpy = jest.spyOn(component, 'reloadIframe');

            component.translatePage({
                page: MOCK_RESPONSE_HEADLESS.page,
                newLanguage: '3'
            }); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent({
                detail: {
                    name: NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED,
                    data: {},
                    payload: {
                        isMoveAction: true
                    }
                }
            });

            expect(reloadIframeSpy).toHaveBeenCalled();
        });

        it("should trigger setSaved when the iframe's custom event is 'save-page'", () => {
            const setSavedSpy = jest.spyOn(storeSpy, 'setSaved');

            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent({
                detail: {
                    name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                    payload: {
                        isMoveAction: false
                    }
                }
            });

            expect(setSavedSpy).toHaveBeenCalled();
        });

        it("should reload the iframe when the iframe's custom event is 'save-page' and the payload is a move action", () => {
            const reloadIframeSpy = jest.spyOn(component, 'reloadIframe');

            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent({
                detail: {
                    name: NG_CUSTOM_EVENTS.SAVE_PAGE,
                    payload: {
                        isMoveAction: true
                    }
                }
            });

            expect(reloadIframeSpy).toHaveBeenCalled();
        });

        it('should trigger addContentlet in the store', () => {
            const addContentletSpy = jest.spyOn(storeSpy, 'addContentlet');

            component.addContentlet(PAYLOAD_MOCK);

            expect(addContentletSpy).toHaveBeenCalledWith({
                containerId: PAYLOAD_MOCK.container.identifier,
                acceptTypes: PAYLOAD_MOCK.container.acceptTypes,
                language_id: PAYLOAD_MOCK.language_id,
                payload: PAYLOAD_MOCK
            });
        });

        it('should trigger addFormContentlet in the store', () => {
            const addFormContentletSpy = jest.spyOn(storeSpy, 'addFormContentlet');

            component.addForm(PAYLOAD_MOCK);

            expect(addFormContentletSpy).toHaveBeenCalledWith(PAYLOAD_MOCK);
        });

        it('should trigger addContentletSpy in the store for widget', () => {
            const addContentletSpy = jest.spyOn(storeSpy, 'addContentlet');

            component.addWidget(PAYLOAD_MOCK);

            expect(addContentletSpy).toHaveBeenCalledWith({
                containerId: PAYLOAD_MOCK.container.identifier,
                acceptTypes: DotCMSBaseTypesContentTypes.WIDGET,
                language_id: PAYLOAD_MOCK.language_id,
                payload: PAYLOAD_MOCK
            });
        });

        it('should trigger translatePage from the store', () => {
            const translatePageSpy = jest.spyOn(storeSpy, 'translatePage');

            component.translatePage({
                page: {
                    title: 'test'
                } as DotPage,
                newLanguage: '1'
            });

            expect(translatePageSpy).toHaveBeenCalledWith({
                page: {
                    title: 'test'
                },
                newLanguage: '1'
            });
        });

        it('should trigger editContentlet in the store', () => {
            const editContentletSpy = jest.spyOn(storeSpy, 'editContentlet');

            component.editContentlet(PAYLOAD_MOCK.contentlet);

            expect(editContentletSpy).toHaveBeenCalledWith({
                inode: PAYLOAD_MOCK.contentlet.inode,
                title: PAYLOAD_MOCK.contentlet.title
            });
        });

        it('should trigger editVTLContentlet in the store', () => {
            const editVTLContentletSpy = jest.spyOn(storeSpy, 'editContentlet');

            const vtlFile = {
                inode: '123',
                name: 'test.vtl'
            };

            component.editVTLContentlet(vtlFile);

            expect(editVTLContentletSpy).toHaveBeenCalledWith({
                inode: vtlFile.inode,
                title: vtlFile.name
            });
        });

        it('should trigger editContentlet in the store for url Map', () => {
            const editContentletSpy = jest.spyOn(storeSpy, 'editUrlContentMapContentlet');

            component.editUrlContentMapContentlet(PAYLOAD_MOCK.contentlet as DotCMSContentlet);

            expect(editContentletSpy).toHaveBeenCalledWith({
                inode: PAYLOAD_MOCK.contentlet.inode,
                title: PAYLOAD_MOCK.contentlet.title
            });
        });

        it('should trigger createContentlet in the store', () => {
            const createContentletSpy = jest.spyOn(storeSpy, 'createContentlet');

            component.createContentlet({
                url: 'https://demo.dotcms.com/jsp.jsp',
                contentType: 'test',
                payload: PAYLOAD_MOCK
            });

            expect(createContentletSpy).toHaveBeenCalledWith({
                contentType: 'test',
                url: 'https://demo.dotcms.com/jsp.jsp',
                payload: PAYLOAD_MOCK
            });
        });

        it('should trigger createContentletFromPalette in the store', () => {
            const createContentletFromPalletSpy = jest.spyOn(
                storeSpy,
                'createContentletFromPalette'
            );

            component.createContentletFromPalette({
                variable: 'test',
                name: 'test',
                payload: PAYLOAD_MOCK
            });

            expect(createContentletFromPalletSpy).toHaveBeenCalledWith({
                name: 'test',
                payload: PAYLOAD_MOCK,
                variable: 'test'
            });
        });

        it('should trigger a reset in the store', () => {
            const resetSpy = jest.spyOn(storeSpy, 'resetDialog');

            component.resetDialog();

            expect(resetSpy).toHaveBeenCalled();
        });

        it('should trigger a loading iframe in the store', () => {
            const resetSpy = jest.spyOn(storeSpy, 'loadingIframe');

            component.showLoadingIframe();

            expect(resetSpy).toHaveBeenCalled();
        });

        it("should trigger openDialogOnURL in the store when it's a URL", () => {
            const openDialogOnURLSpy = jest.spyOn(storeSpy, 'openDialogOnURL');

            component.openDialogOnUrl('https://demo.dotcms.com/jsp.jsp', 'test');

            expect(openDialogOnURLSpy).toHaveBeenCalledWith({
                title: 'test',
                url: 'https://demo.dotcms.com/jsp.jsp'
            });
        });
    });

    describe('Compare dialog', () => {
        const renderCompareDialog = () => {
            component.addContentlet(PAYLOAD_MOCK); // This is to make the dialog open
            spectator.detectChanges();

            triggerIframeCustomEvent();

            const dialogIframe = spectator.debugElement.query(
                By.css('[data-testId="dialog-iframe"]')
            );

            spectator.triggerEventHandler(dialogIframe, 'load', {}); // There's no way we can load the iframe, because we are setting a real src and will not load

            dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
                new CustomEvent('ng-event', {
                    detail: {
                        name: NG_CUSTOM_EVENTS.COMPARE_CONTENTLET,
                        data: {
                            inode: '123',
                            identifier: 'identifier',
                            language: '1'
                        }
                    }
                })
            );
            spectator.detectChanges();
        };

        it('should render a compare dialog', () => {
            renderCompareDialog();
            expect(spectator.query(byTestId('dialog-compare'))).toBeDefined();
            expect(spectator.query(DotContentCompareComponent)).toBeDefined();

            expect(spectator.component.$compareData()).toEqual({
                inode: '123',
                identifier: 'identifier',
                language: '1'
            });
        });

        it('should trigger a bring back action', () => {
            const bringBackSpy = jest.spyOn(component, 'bringBack');

            renderCompareDialog();

            spectator.triggerEventHandler(DotContentCompareComponent, 'letMeBringBack', {
                name: 'getVersionBack',
                args: ['123']
            });

            expect(bringBackSpy).toHaveBeenCalledWith({
                name: 'getVersionBack',
                args: ['123']
            });
        });
    });
});
