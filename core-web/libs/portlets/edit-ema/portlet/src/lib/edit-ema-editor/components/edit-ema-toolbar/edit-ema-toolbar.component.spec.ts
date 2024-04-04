import { expect, describe, it } from '@jest/globals';
import { SpectatorRouting, createRoutingFactory } from '@ngneat/spectator/jest';
import { MockComponent, MockProvider, MockProviders } from 'ng-mocks';
import { of } from 'rxjs';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';

import { DotMessageService, DotPersonalizeService } from '@dotcms/data-access';
import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotDeviceSelectorSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe, mockDotDevices } from '@dotcms/utils-testing';

import { EditEmaToolbarComponent } from './edit-ema-toolbar.component';

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { EDITOR_MODE, EDITOR_STATE } from '../../../shared/enums';
import { DotEditEmaWorkflowActionsComponent } from '../dot-edit-ema-workflow-actions/dot-edit-ema-workflow-actions.component';
import { DotEmaBookmarksComponent } from '../dot-ema-bookmarks/dot-ema-bookmarks.component';
import { DotEmaInfoDisplayComponent } from '../dot-ema-info-display/dot-ema-info-display.component';
import { DotEmaRunningExperimentComponent } from '../dot-ema-running-experiment/dot-ema-running-experiment.component';
import { EditEmaLanguageSelectorComponent } from '../edit-ema-language-selector/edit-ema-language-selector.component';
import { EditEmaPersonaSelectorComponent } from '../edit-ema-persona-selector/edit-ema-persona-selector.component';

describe('EditEmaToolbarComponent', () => {
    let spectator: SpectatorRouting<EditEmaToolbarComponent>;
    let store: EditEmaStore;
    let messageService: MessageService;
    let router: Router;
    let confirmationService: ConfirmationService;

    const createComponent = createRoutingFactory({
        component: EditEmaToolbarComponent,
        declarations: [
            DotMessagePipe,
            MockComponent(DotDeviceSelectorSeoComponent),
            MockComponent(DotEditEmaWorkflowActionsComponent),
            MockComponent(DotEmaBookmarksComponent),
            MockComponent(DotEmaInfoDisplayComponent),
            MockComponent(DotEmaRunningExperimentComponent),
            MockComponent(EditEmaLanguageSelectorComponent),
            MockComponent(EditEmaPersonaSelectorComponent)
        ],
        imports: [MenuModule, ButtonModule, ToolbarModule, ClipboardModule],
        providers: [
            MockProviders(Router),
            MockProvider(ConfirmationService, {
                confirm: jest.fn()
            }),
            MockProvider(MessageService, {
                add: jest.fn()
            }),
            MockProvider(DotMessageService, {
                get: (key) => {
                    const data = {
                        Copied: 'Copied',
                        'editpage.personalization.confirm.header': 'Personalize',
                        'editpage.personalization.confirm.message': 'Confirm personalization?',
                        'editpage.personalization.delete.confirm.header': 'Despersonalization?',
                        'editpage.personalization.delete.confirm.message':
                            'Confirm despersonalization?',
                        'dot.common.dialog.accept': 'Accept',
                        'dot.common.dialog.reject': 'Reject'
                    };

                    return data[key];
                }
            }),
            {
                provide: EditEmaStore,
                useValue: {
                    editorState$: of({
                        iframeURL: 'http://localhost:8080/index',
                        clientHost: 'http://localhost:3000',
                        apiURL: 'http://localhost/api/v1/page/json/page-one',
                        currentExperiment: {
                            status: DotExperimentStatus.RUNNING
                        },
                        editorData: {
                            mode: EDITOR_MODE.EDIT
                        },
                        editor: {
                            page: {
                                identifier: '123',
                                inode: '456'
                            },
                            viewAs: {
                                persona: {}
                            }
                        }
                    }),
                    load: jest.fn(),
                    setDevice: jest.fn(),
                    setSocialMedia: jest.fn(),
                    updateEditorState: jest.fn()
                }
            }
        ],
        componentProviders: [
            {
                provide: DotPersonalizeService,
                useValue: {
                    getPersonalize: jest.fn()
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        store = spectator.inject(EditEmaStore);
        messageService = spectator.inject(MessageService);
        router = spectator.inject(Router);
        confirmationService = spectator.inject(ConfirmationService);
    });

    describe('events', () => {
        describe('dot-device-selector-seo', () => {
            it('should call store.setDevice', () => {
                jest.spyOn(store, 'setDevice');

                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

                spectator.triggerEventHandler(deviceSelector, 'selected', iphone);
                spectator.detectChanges();

                expect(store.setDevice).toHaveBeenCalledWith(iphone);
            });

            it('should call store.setSocialMedia', () => {
                jest.spyOn(store, 'setSocialMedia');

                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                spectator.triggerEventHandler(deviceSelector, 'changeSeoMedia', 'facebook');
                spectator.detectChanges();

                expect(store.setSocialMedia).toHaveBeenCalledWith('facebook');
            });
        });

        describe('ema-preview', () => {
            it('should call deviceSelector.openMenu', () => {
                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                jest.spyOn(deviceSelector.componentInstance, 'openMenu');

                const emaPreviewButton = spectator.debugElement.query(
                    By.css('[data-testId="ema-preview"]')
                );

                spectator.triggerEventHandler(emaPreviewButton, 'onClick', { hello: 'world' });
                spectator.detectChanges();

                expect(deviceSelector.componentInstance.openMenu).toHaveBeenCalledWith({
                    hello: 'world'
                });
            });
        });

        describe('ema-copy-url', () => {
            it('should call messageService.add', () => {
                const button = spectator.debugElement.query(By.css('[data-testId="ema-copy-url"]'));

                spectator.triggerEventHandler(button, 'cdkCopyToClipboardCopied', {});

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'Copied',
                    life: 3000
                });
            });
        });

        describe('dot-edit-ema-language-selector', () => {
            it('should set language', () => {
                spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 2);
                spectator.detectChanges();

                expect(store.updateEditorState).toHaveBeenCalledWith(EDITOR_STATE.LOADING);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: 2 },
                    queryParamsHandling: 'merge'
                });
            });
        });

        describe('dot-edit-ema-persona-selector', () => {
            it('should personalize - no confirmation', () => {
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    identifier: '123',
                    pageId: '123',
                    personalized: true
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);
                spectator.detectChanges();

                expect(store.updateEditorState).toHaveBeenCalledWith(EDITOR_STATE.LOADING);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { 'com.dotmarketing.persona.id': '123' },
                    queryParamsHandling: 'merge'
                });
            });

            it('should personalize - confirmation', () => {
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
                    identifier: '123',
                    pageId: '123',
                    personalized: false
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);
                spectator.detectChanges();

                expect(confirmationService.confirm).toHaveBeenCalledWith({
                    accept: expect.any(Function),
                    acceptLabel: 'Accept',
                    header: 'Personalize',
                    message: 'Confirm personalization?',
                    reject: expect.any(Function),
                    rejectLabel: 'Reject'
                });
            });

            xit('should personalize - call service', () => {
                expect(true).toBe(true);
            });

            it('should despersonalize', () => {
                spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
                    identifier: '123',
                    pageId: '123',
                    personalized: true
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);

                spectator.detectChanges();

                expect(confirmationService.confirm).toHaveBeenCalledWith({
                    accept: expect.any(Function),
                    acceptLabel: 'Accept',
                    header: 'Despersonalization?',
                    message: 'Confirm despersonalization?',
                    rejectLabel: 'Reject'
                });
            });

            xit('should dpersonalize - call service', () => {
                expect(true).toBe(true);
            });
        });

        describe('dot-edit-ema-workflow-actions', () => {
            it('should update page', () => {
                spectator.triggerEventHandler(DotEditEmaWorkflowActionsComponent, 'newPage', {
                    pageURI: '/path-and-stuff',
                    url: 'path',
                    languageId: 1
                    // eslint-disable-next-line @typescript-eslint/no-explicit-any
                } as any);

                spectator.detectChanges();

                expect(store.updateEditorState).toHaveBeenCalledWith(EDITOR_STATE.LOADING);

                expect(router.navigate).toHaveBeenCalledWith([], {
                    queryParams: { language_id: '1', url: '/path-and-stuff' },
                    queryParamsHandling: 'merge'
                });
            });
        });
    });

    // it('should open seo results when clicking on a social media tile', () => {
    //     const setSocialMediaMock = jest.spyOn(store, 'setSocialMedia');

    //     const deviceSelector = spectator.debugElement.query(
    //         By.css('[data-testId="dot-device-selector"]')
    //     );

    //     spectator.triggerEventHandler(deviceSelector, 'changeSeoMedia', 'Facebook');

    //     // expect(spectator.query(byTestId('results-seo-tool'))).not.toBeNull(); // This components share the same logic as the preview by device

    //     expect(setSocialMediaMock).toHaveBeenCalledWith('Facebook');
    // });

    // it('should trigger messageService when clicking on ema-copy-url', () => {
    //     const messageService = spectator.inject(MessageService, true);
    //     const messageServiceSpy = jest.spyOn(messageService, 'add');
    //     spectator.detectChanges();

    //     const button = spectator.debugElement.query(By.css('[data-testId="ema-copy-url"]'));

    //     spectator.triggerEventHandler(button, 'cdkCopyToClipboardCopied', {});

    //     expect(messageServiceSpy).toHaveBeenCalledWith({
    //         severity: 'success',
    //         summary: 'Copied',
    //         life: 3000
    //     });
    // });

    // it('should call navigate when selecting a language', () => {
    //     spectator.detectChanges();
    //     const router = spectator.inject(Router);

    //     jest.spyOn(router, 'navigate');

    //     spectator.triggerEventHandler(EditEmaLanguageSelectorComponent, 'selected', 2);
    //     spectator.detectChanges();

    //     expect(router.navigate).toHaveBeenCalledWith([], {
    //         queryParams: { language_id: 2 },
    //         queryParamsHandling: 'merge'
    //     });
    // });

    // describe('persona selector', () => {
    //     it('should have a persona selector', () => {
    //         spectator.detectChanges();
    //         expect(spectator.query(byTestId('persona-selector'))).not.toBeNull();
    //     });

    //     it("should open a confirmation dialog when selecting a persona that it's not personalized", () => {
    //         const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
    //         spectator.detectChanges();

    //         spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
    //             ...DEFAULT_PERSONA,
    //             identifier: '123',
    //             pageId: '123',
    //             personalized: false
    //         });
    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();
    //     });

    //     it('should fetchPersonas and navigate when confirming the personalization', () => {
    //         const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
    //         spectator.detectChanges();

    //         spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
    //             ...DEFAULT_PERSONA,
    //             identifier: '123',
    //             pageId: '123',
    //             personalized: false
    //         });
    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         const confirmDialog = spectator.query(byTestId('confirm-dialog'));
    //         const personaSelector = spectator.debugElement.query(
    //             By.css('[data-testId="persona-selector"]')
    //         ).componentInstance;
    //         const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
    //         const fetchPersonasSpy = jest.spyOn(personaSelector, 'fetchPersonas');

    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         confirmDialog
    //             .querySelector('.p-confirm-dialog-accept')
    //             .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

    //         spectator.detectChanges();

    //         expect(routerSpy).toBeCalledWith([], {
    //             queryParams: { 'com.dotmarketing.persona.id': '123' },
    //             queryParamsHandling: 'merge'
    //         });
    //         expect(fetchPersonasSpy).toHaveBeenCalled();
    //     });

    //     it('should reset the value on personalization rejection', () => {
    //         const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
    //         spectator.detectChanges();

    //         spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'selected', {
    //             ...DEFAULT_PERSONA,
    //             identifier: '123',
    //             pageId: '123',
    //             personalized: false
    //         });
    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         const confirmDialog = spectator.query(byTestId('confirm-dialog'));
    //         const personaSelector = spectator.debugElement.query(
    //             By.css('[data-testId="persona-selector"]')
    //         ).componentInstance;

    //         const resetValueSpy = jest.spyOn(personaSelector, 'resetValue');

    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         confirmDialog
    //             .querySelector('.p-confirm-dialog-reject')
    //             .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

    //         spectator.detectChanges();

    //         expect(resetValueSpy).toHaveBeenCalled();
    //     });

    //     it('should open a confirmation dialog when despersonalize is triggered', () => {
    //         const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
    //         spectator.detectChanges();

    //         spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
    //             ...DEFAULT_PERSONA,
    //             pageId: '123',
    //             selected: false
    //         });
    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();
    //     });

    //     it('should fetchPersonas when confirming the despersonalization', () => {
    //         const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
    //         spectator.detectChanges();

    //         spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
    //             ...DEFAULT_PERSONA,
    //             pageId: '123',
    //             selected: false
    //         });
    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         const confirmDialog = spectator.query(byTestId('confirm-dialog'));
    //         const personaSelector = spectator.debugElement.query(
    //             By.css('[data-testId="persona-selector"]')
    //         ).componentInstance;

    //         const fetchPersonasSpy = jest.spyOn(personaSelector, 'fetchPersonas');

    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         confirmDialog
    //             .querySelector('.p-confirm-dialog-accept')
    //             .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

    //         spectator.detectChanges();

    //         expect(fetchPersonasSpy).toHaveBeenCalled();
    //     });

    //     it('should navigate with default persona as current persona when the selected is the same as the despersonalized', () => {
    //         const confirmDialogOpen = jest.spyOn(confirmationService, 'confirm');
    //         spectator.detectChanges();

    //         spectator.triggerEventHandler(EditEmaPersonaSelectorComponent, 'despersonalize', {
    //             ...CUSTOM_PERSONA,
    //             pageId: '123',
    //             selected: true
    //         });
    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         const confirmDialog = spectator.query(byTestId('confirm-dialog'));

    //         const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');

    //         spectator.detectChanges();

    //         expect(confirmDialogOpen).toHaveBeenCalled();

    //         confirmDialog
    //             .querySelector('.p-confirm-dialog-accept')
    //             .dispatchEvent(new Event('click')); // This is the internal button, coudln't find a better way to test it

    //         spectator.detectChanges();

    //         expect(routerSpy).toHaveBeenCalledWith([], {
    //             queryParams: {
    //                 'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
    //             },
    //             queryParamsHandling: 'merge'
    //         });
    //     });
    // });

    // it('should show the info display when you cannot edit the page', () => {
    //     store.load({
    //         url: 'index',
    //         language_id: '6',
    //         'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
    //     });

    //     spectator.detectChanges();

    //     const infoDisplay = spectator.query(byTestId('info-display'));

    //     expect(infoDisplay).not.toBeNull();
    // });

    // describe('Workflow actions', () => {
    //     it('should set the inputs correctly', () => {
    //         const component = spectator.query(DotEditEmaWorkflowActionsComponent);

    //         expect(component.inode).toBe(PAGE_INODE_MOCK);
    //     });

    //     it('should update reload if the page url changes', () => {
    //         const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
    //         const component = spectator.query(DotEditEmaWorkflowActionsComponent);

    //         component.newPage.emit({
    //             ...dotcmsContentletMock,
    //             url: 'new-page'
    //         });

    //         spectator.detectChanges();

    //         expect(routerSpy).toHaveBeenCalledWith([], {
    //             queryParams: {
    //                 ...QUERY_PARAMS_MOCK,
    //                 url: 'new-page',
    //                 language_id: '1'
    //             },
    //             queryParamsHandling: 'merge'
    //         });
    //     });

    //     it('should update reload if the language changes', () => {
    //         const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
    //         const component = spectator.query(DotEditEmaWorkflowActionsComponent);

    //         component.newPage.emit({
    //             ...dotcmsContentletMock,
    //             url: 'index',
    //             languageId: 2
    //         });

    //         spectator.detectChanges();

    //         expect(routerSpy).toHaveBeenCalledWith([], {
    //             queryParams: {
    //                 ...QUERY_PARAMS_MOCK,
    //                 url: 'index',
    //                 language_id: '2'
    //             },
    //             queryParamsHandling: 'merge'
    //         });
    //     });

    //     it('should not reload if neither the url or language changes ', () => {
    //         const routerSpy = jest.spyOn(spectator.inject(Router), 'navigate');
    //         const component = spectator.query(DotEditEmaWorkflowActionsComponent);

    //         component.newPage.emit({
    //             ...dotcmsContentletMock,
    //             url: QUERY_PARAMS_MOCK.url,
    //             languageId: QUERY_PARAMS_MOCK.language_id
    //         });

    //         spectator.detectChanges();

    //         expect(routerSpy).not.toHaveBeenCalled();
    //     });
    // });

    // it('should show the info display when trying to edit a variant of a running experiment', () => {
    //     store.load({
    //         url: 'index',
    //         language_id: '6',
    //         'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
    //         experimentId: 'i-have-a-running-experiment'
    //     }); // This will load a page with a running experiment

    //     spectator.detectChanges();

    //     const infoDisplay = spectator.query(byTestId('info-display'));

    //     expect(infoDisplay).not.toBeNull();
    // });

    // it('should show the info display when trying to edit a variant of an scheduled experiment', () => {
    //     store.load({
    //         url: 'index',
    //         language_id: '6',
    //         'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
    //         experimentId: 'i-have-a-scheduled-experiment'
    //     }); // This will load a page with a scheduled experiment

    //     spectator.detectChanges();

    //     const infoDisplay = spectator.query(byTestId('info-display'));

    //     expect(infoDisplay).not.toBeNull();
    // });

    // it('should render the running experiment component', () => {
    //     store.load({
    //         url: 'index',
    //         language_id: '5',
    //         'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
    //         experimentId: 'i-have-a-running-experiment'
    //     }); // This will load a page with a running experiment

    //     spectator.detectChanges();

    //     const runningExperiment = spectator.query(byTestId('ema-running-experiment'));

    //     expect(runningExperiment).not.toBeNull();
    // });

    // it('should show the components that need showed on preview mode', () => {
    //     const componentsToShow = ['info-display']; // Test id of components that should show when entering preview modes

    //     spectator.detectChanges();

    //     const iphone = { ...mockDotDevices[0], icon: 'someIcon' };

    //     store.setDevice(iphone);
    //     spectator.detectChanges();

    //     componentsToShow.forEach((testId) => {
    //         expect(spectator.query(byTestId(testId))).not.toBeNull();
    //     });
    // });

    // describe('API URL', () => {
    //     it('should have the url setted with the current language and persona', () => {
    //         spectator.detectChanges();

    //         const button = spectator.debugElement.query(By.css('[data-testId="ema-api-link"]'));

    //         expect(button.nativeElement.href).toBe(
    //             'http://localhost/api/v1/page/json/page-one?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona&variantName=DEFAULT&mode=EDIT_MODE'
    //         );
    //     });

    //     it('should open a new tab', () => {
    //         spectator.detectChanges();

    //         const button = spectator.debugElement.query(By.css('[data-testId="ema-api-link"]'));

    //         expect(button.nativeElement.target).toBe('_blank');
    //     });
    // });

    // describe('language selector', () => {
    //     it('should have a language selector', () => {
    //         spectator.detectChanges();
    //         expect(spectator.query(byTestId('language-selector'))).not.toBeNull();
    //     });

    //     it("should have the current language as label in the language selector's button", () => {
    //         spectator.detectChanges();
    //         expect(spectator.query(byTestId('language-button')).textContent).toBe('English - US');
    //     });
    // });

    // describe('DOM', () => {
    //     it('should have left-content on left', () => {
    //         const leftContent = spectator.query(byTestId('toolbar-left-content'));

    //         expect(leftContent.querySelector('[data-testId="left-content"]')).not.toBeNull();
    //     });

    //     it('should have right-content on right', () => {
    //         const rightContent = spectator.query(byTestId('toolbar-right-content'));

    //         expect(rightContent.querySelector('[data-testId="right-content"]')).not.toBeNull();
    //     });

    //     it('should have title-content on title', () => {
    //         const titleContent = spectator.query(byTestId('title-content'));

    //         expect(titleContent).not.toBeNull();
    //     });
    // });
});
