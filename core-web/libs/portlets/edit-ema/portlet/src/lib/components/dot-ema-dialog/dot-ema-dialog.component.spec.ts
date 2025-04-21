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
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { MessageService } from 'primeng/api';
import { Dialog } from 'primeng/dialog';

import { CLIENT_ACTIONS } from '@dotcms/client';
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
import { DotContentCompareComponent } from '@dotcms/portlets/dot-ema/ui';
import {
    DotcmsConfigServiceMock,
    DotcmsEventsServiceMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotEmaDialogComponent } from './dot-ema-dialog.component';
import { DotEditorDialogService } from './services/dot-editor-dialog.service';

import { DotActionUrlService } from '../../services/dot-action-url/dot-action-url.service';
import { DotEmaWorkflowActionsService } from '../../services/dot-ema-workflow-actions/dot-ema-workflow-actions.service';
import { DialogStatus, FormStatus, NG_CUSTOM_EVENTS } from '../../shared/enums';
import { PAYLOAD_MOCK } from '../../shared/mocks';
import { UVEStore } from '../../store/dot-uve.store';

const CUSTOM_EVENT_MOCK = {
    detail: {
        name: NG_CUSTOM_EVENTS.SAVE_PAGE,
        payload: {
            htmlPageReferer: '/my-awesome-page'
        }
    }
};

interface CustomEvent {
    detail: {
        name: string;
        payload: unknown;
        data?: unknown;
    };
}

describe('DotEmaDialogComponent', () => {
    let spectator: Spectator<DotEmaDialogComponent>;
    let component: DotEmaDialogComponent;
    let dialogService: DotEditorDialogService;
    let workflowActionService: SpyObject<DotEmaWorkflowActionsService>;

    const triggerIframeCustomEvent = (customEvent: CustomEvent = CUSTOM_EVENT_MOCK) => {
        const dialogIframe = spectator.debugElement.query(By.css('[data-testId="dialog-iframe"]'));

        spectator.triggerEventHandler(dialogIframe, 'load', {});

        dialogIframe.nativeElement.contentWindow.document.dispatchEvent(
            new CustomEvent('ng-event', {
                ...customEvent
            })
        );
        spectator.detectChanges();
    };

    const spyIframeReload = () => {
        const reloadIframeSpy = jest.fn();
        const iframe = component.iframe.nativeElement;
        Object.defineProperty(iframe.contentWindow, 'location', {
            configurable: true,
            value: { reload: reloadIframeSpy }
        });

        return reloadIframeSpy;
    };

    const createComponent = createComponentFactory({
        component: DotEmaDialogComponent,
        imports: [HttpClientTestingModule],
        providers: [
            HttpClient,
            MessageService,
            DotEditorDialogService,
            mockProvider(DotEmaWorkflowActionsService),
            mockProvider(DotWorkflowActionsFireService),
            {
                provide: UVEStore,
                useValue: {
                    pageParams: signal({
                        variantName: 'DEFAULT'
                    })
                }
            },
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
        dialogService = spectator.inject(DotEditorDialogService, true);
        workflowActionService = spectator.inject(DotEmaWorkflowActionsService, true);
    });

    describe('DOM', () => {
        it('should make dialog visible', () => {
            dialogService.state.update((state) => ({
                ...state,
                status: DialogStatus.LOADING,
                type: 'content',
                url: 'http://example.com',
                actionPayload: PAYLOAD_MOCK
            }));
            spectator.detectChanges();
            expect(spectator.query(byTestId('dialog'))).not.toBeNull();
        });

        it("should make the form selector visible when it's a form", () => {
            dialogService.state.update((state) => ({
                ...state,
                status: DialogStatus.LOADING,
                type: 'form',
                actionPayload: PAYLOAD_MOCK
            }));
            spectator.detectChanges();
            expect(spectator.query(byTestId('form-selector'))).not.toBeNull();
        });

        it("should make the iframe visible when it's not a form", () => {
            dialogService.state.update((state) => ({
                ...state,
                status: DialogStatus.LOADING,
                type: 'content',
                url: 'http://example.com',
                actionPayload: PAYLOAD_MOCK
            }));
            spectator.detectChanges();
            expect(spectator.query(byTestId('dialog-iframe'))).not.toBeNull();
        });
    });

    describe('outputs', () => {
        it('should dispatch custom events', () => {
            const customEventSpy = jest.spyOn(component.action, 'emit');

            // Set dialog state
            dialogService.state.update((state) => ({
                ...state,
                status: DialogStatus.LOADING,
                type: 'content',
                url: 'http://example.com',
                actionPayload: PAYLOAD_MOCK
            }));
            spectator.detectChanges();

            triggerIframeCustomEvent();

            expect(customEventSpy).toHaveBeenCalledWith({
                event: expect.objectContaining({
                    isTrusted: false
                }),
                actionPayload: PAYLOAD_MOCK,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: CLIENT_ACTIONS.NOOP
            });
        });

        it('should dispatch onHide when p-dialog hide', () => {
            const actionSpy = jest.spyOn(component.action, 'emit');

            // Set dialog state
            dialogService.state.update((state) => ({
                ...state,
                status: DialogStatus.LOADING,
                type: 'content',
                url: 'http://example.com',
                actionPayload: PAYLOAD_MOCK
            }));
            spectator.detectChanges();

            spectator.triggerEventHandler(Dialog, 'visibleChange', false);

            expect(actionSpy).toHaveBeenCalledWith({
                event: new CustomEvent('ng-event', {
                    detail: {
                        name: NG_CUSTOM_EVENTS.DIALOG_CLOSED
                    }
                }),
                actionPayload: PAYLOAD_MOCK,
                form: {
                    status: FormStatus.PRISTINE,
                    isTranslation: false
                },
                clientAction: CLIENT_ACTIONS.NOOP
            });
        });
    });

    describe('NG EVENTS HANDLERS', () => {
        beforeEach(() => {
            // Set dialog state first
            dialogService.state.update((state) => ({
                ...state,
                status: DialogStatus.LOADING,
                type: 'content',
                url: 'http://example.com',
                actionPayload: PAYLOAD_MOCK
            }));
            spectator.detectChanges();
        });

        describe('NG_CUSTOM_EVENTS.OPEN_WIZARD', () => {
            it('should call workflowActionService.handleWorkflowAction', () => {
                const workflowActionSpy = jest
                    .spyOn(workflowActionService, 'handleWorkflowAction')
                    .mockReturnValue(of({}));

                triggerIframeCustomEvent({
                    detail: {
                        name: NG_CUSTOM_EVENTS.OPEN_WIZARD,
                        data: {},
                        payload: {}
                    }
                });

                expect(workflowActionSpy).toHaveBeenCalled();
            });
        });

        describe('NG_CUSTOM_EVENTS.SAVE_PAGE', () => {
            it('should call dialogService.setSaved', () => {
                const setSavedSpy = jest.spyOn(dialogService, 'setSaved');

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
        });

        describe('NG_CUSTOM_EVENTS.SAVE_PAGE', () => {
            it('should reload the iframe when payload is a move action', () => {
                const reloadIframeSpy = spyIframeReload();

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
        });

        describe('NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED', () => {
            it('should call dialogService.setDirty when is not a translation', () => {
                const setDirtySpy = jest.spyOn(dialogService, 'setDirty');

                dialogService.state.update((state) => ({
                    ...state,
                    form: {
                        ...state.form,
                        isTranslation: false
                    }
                }));

                triggerIframeCustomEvent({
                    detail: {
                        name: NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED,
                        data: {},
                        payload: {}
                    }
                });

                expect(setDirtySpy).toHaveBeenCalled();
            });

            it('should call dialogService.setSaved when is a translation', () => {
                const setSavedSpy = jest.spyOn(dialogService, 'setSaved');

                // Update dialog state to be a translation
                dialogService.state.update((state) => ({
                    ...state,
                    form: {
                        ...state.form,
                        isTranslation: true
                    }
                }));
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

            it('should call iframe reload when is a translation and payload is move action', () => {
                dialogService.state.update((state) => ({
                    ...state,
                    form: {
                        ...state.form,
                        isTranslation: true
                    }
                }));

                spectator.detectChanges();

                const reloadIframeSpy = spyIframeReload();

                triggerIframeCustomEvent({
                    detail: {
                        name: NG_CUSTOM_EVENTS.EDIT_CONTENTLET_UPDATED,
                        data: {},
                        payload: {
                            isMoveAction: true
                        }
                    }
                });

                // Check if reload was called
                expect(reloadIframeSpy).toHaveBeenCalled();
            });
        });
    });

    describe('Compare dialog', () => {
        const renderCompareDialog = () => {
            // Set dialog state first
            dialogService.state.update((state) => ({
                ...state,
                status: DialogStatus.LOADING,
                type: 'content',
                url: 'http://example.com',
                actionPayload: PAYLOAD_MOCK
            }));
            spectator.detectChanges();

            triggerIframeCustomEvent(CUSTOM_EVENT_MOCK);

            const dialogIframe = spectator.debugElement.query(
                By.css('[data-testId="dialog-iframe"]')
            );

            spectator.triggerEventHandler(dialogIframe, 'load', {});

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

        it('should call bringBack method when letMeBringBack event is emitted', () => {
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
