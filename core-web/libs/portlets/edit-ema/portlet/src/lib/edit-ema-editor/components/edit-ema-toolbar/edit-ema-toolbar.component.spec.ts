import { expect, describe, it } from '@jest/globals';
import { SpectatorRouting, byTestId, createRoutingFactory } from '@ngneat/spectator/jest';
import { MockComponent, MockProvider, MockProviders } from 'ng-mocks';
import { of } from 'rxjs';

import { ClipboardModule } from '@angular/cdk/clipboard';
import { DebugElement } from '@angular/core';
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
            })
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

    describe('edit', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    {
                        provide: EditEmaStore,
                        useValue: {
                            editorToolbarData$: of({
                                previewURL: 'http://localhost:8080/index',
                                favoritePageURL: 'http://localhost:8080/fav',
                                iframeURL: 'http://localhost:8080/index',
                                clientHost: 'http://localhost:3000',
                                apiURL: 'http://localhost/api/v1/page/json/page-one',
                                pageURI: 'http://localhost:8080/index',
                                editorData: {
                                    mode: EDITOR_MODE.EDIT,
                                    canEditPage: true,
                                    page: {
                                        isLocked: false,
                                        canLock: true,
                                        lockedByUser: ''
                                    }
                                },
                                editor: {
                                    page: {
                                        identifier: '123',
                                        inode: '456'
                                    },
                                    viewAs: {
                                        persona: {
                                            id: '123'
                                        },
                                        language: {
                                            id: 1
                                        }
                                    }
                                },
                                showWorkflowActions: true,
                                showInfoDisplay: false
                            }),
                            load: jest.fn(),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            updateEditorState: jest.fn()
                        }
                    }
                ]
            });
            store = spectator.inject(EditEmaStore);
            messageService = spectator.inject(MessageService);
            router = spectator.inject(Router);
            confirmationService = spectator.inject(ConfirmationService);
        });

        describe('dot-device-selector-seo', () => {
            let deviceSelector: DebugElement;

            beforeEach(() => {
                deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );
            });
            it('should have attr', () => {
                expect(deviceSelector.attributes).toEqual({
                    appendTo: 'body',
                    'data-testId': 'dot-device-selector',
                    'ng-reflect-api-link': 'http://localhost:8080/index',
                    'ng-reflect-hide-social-media': 'true'
                });
            });

            it('should call store.setDevice', () => {
                jest.spyOn(store, 'setDevice');

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

        describe('edit-url-content-map', () => {
            it('should be hidden', () => {
                const editURLContentButton = spectator.debugElement.query(
                    By.css('[data-testId="edit-url-content-map"]')
                );
                expect(editURLContentButton).toBeNull();
            });
        });

        describe('ema-preview', () => {
            let emaPreviewButton: DebugElement;

            beforeEach(() => {
                emaPreviewButton = spectator.debugElement.query(
                    By.css('[data-testId="ema-preview"]')
                );
            });

            it('should have attr', () => {
                expect(emaPreviewButton.attributes).toEqual({
                    class: 'p-element',
                    'data-testId': 'ema-preview',
                    icon: 'pi pi-desktop',
                    'ng-reflect-icon': 'pi pi-desktop',
                    'ng-reflect-style-class': 'p-button-text p-button-sm',
                    styleClass: 'p-button-text p-button-sm'
                });
            });

            it('should call deviceSelector.openMenu', () => {
                const deviceSelector = spectator.debugElement.query(
                    By.css('[data-testId="dot-device-selector"]')
                );

                jest.spyOn(deviceSelector.componentInstance, 'openMenu');

                spectator.triggerEventHandler(emaPreviewButton, 'onClick', { hello: 'world' });
                spectator.detectChanges();

                expect(deviceSelector.componentInstance.openMenu).toHaveBeenCalledWith({
                    hello: 'world'
                });
            });
        });

        describe('dot-ema-bookmarks', () => {
            it('should have attr', () => {
                const bookmarks = spectator.query(DotEmaBookmarksComponent);

                expect(bookmarks.url).toBe('http://localhost:8080/fav');
            });
        });

        describe('ema-copy-url', () => {
            let button: DebugElement;

            beforeEach(() => {
                button = spectator.debugElement.query(By.css('[data-testId="ema-copy-url"]'));
            });

            it('should have attr', () => {
                expect(button.attributes).toEqual({
                    class: 'p-element',
                    'data-testId': 'ema-copy-url',
                    icon: 'pi pi-copy',
                    'ng-reflect-icon': 'pi pi-copy',
                    'ng-reflect-style-class': 'p-button-text p-button-sm',
                    'ng-reflect-text': 'http://localhost:8080/index',
                    styleClass: 'p-button-text p-button-sm'
                });
            });

            it('should call messageService.add', () => {
                spectator.triggerEventHandler(button, 'cdkCopyToClipboardCopied', {});

                expect(messageService.add).toHaveBeenCalledWith({
                    severity: 'success',
                    summary: 'Copied',
                    life: 3000
                });
            });
        });

        describe('dot-edit-ema-language-selector', () => {
            it('should have attr', () => {
                const languageSelector = spectator.query(EditEmaLanguageSelectorComponent);

                expect(languageSelector.language).toEqual({ id: 1 });
            });

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
            it('should have attr', () => {
                const personaSelector = spectator.query(EditEmaPersonaSelectorComponent);

                expect(personaSelector.pageId).toBe('123');
                expect(personaSelector.value).toEqual({
                    id: '123'
                });
            });

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
            it('should have attr', () => {
                const workflowActions = spectator.query(DotEditEmaWorkflowActionsComponent);

                expect(workflowActions.inode).toBe('456');
            });
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

        describe('dot-ema-info-display', () => {
            it('should be hidden', () => {
                const infoDisplay = spectator.query(byTestId('info-display'));
                expect(infoDisplay).toBeNull();
            });
        });

        describe('dot-ema-running-experiment', () => {
            it('should be hidden', () => {
                const experiments = spectator.query(byTestId('ema-running-experiment'));
                expect(experiments).toBeNull();
            });
        });
    });

    describe('preview', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    {
                        provide: EditEmaStore,
                        useValue: {
                            editorToolbarData$: of({
                                favoritePageURL: 'http://localhost:8080/fav',
                                iframeURL: 'http://localhost:8080/index',
                                clientHost: 'http://localhost:3000',
                                apiURL: 'http://localhost/api/v1/page/json/page-one',
                                editorData: {
                                    mode: EDITOR_MODE.DEVICE,
                                    canEditPage: true,
                                    page: {
                                        isLocked: false,
                                        canLock: true,
                                        lockedByUser: ''
                                    }
                                },
                                editor: {
                                    page: {
                                        identifier: '123',
                                        inode: '456'
                                    },
                                    viewAs: {
                                        persona: {
                                            id: '123'
                                        },
                                        language: {
                                            id: 1
                                        }
                                    }
                                },
                                showWorkflowActions: true,
                                showInfoDisplay: true
                            }),
                            load: jest.fn(),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            updateEditorState: jest.fn()
                        }
                    }
                ]
            });
            store = spectator.inject(EditEmaStore);
            messageService = spectator.inject(MessageService);
            router = spectator.inject(Router);
            confirmationService = spectator.inject(ConfirmationService);
        });

        describe('dot-ema-running-experiment', () => {
            it('should be hidden', () => {
                const experiments = spectator.query(byTestId('ema-running-experiment'));
                expect(experiments).toBeNull();
            });
        });

        describe('dot-ema-info-display', () => {
            it('should have attr', () => {
                const infoDisplay = spectator.query(DotEmaInfoDisplayComponent);
                expect(infoDisplay.editorData).toEqual({
                    canEditPage: true,
                    mode: EDITOR_MODE.DEVICE,
                    page: {
                        isLocked: false,
                        canLock: true,
                        lockedByUser: ''
                    }
                });

                expect(infoDisplay.currentExperiment).not.toBeDefined();
            });
        });
    });

    describe('experiments', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    {
                        provide: EditEmaStore,
                        useValue: {
                            editorToolbarData$: of({
                                favoritePageURL: 'http://localhost:8080/fav',
                                iframeURL: 'http://localhost:8080/index',
                                clientHost: 'http://localhost:3000',
                                apiURL: 'http://localhost/api/v1/page/json/page-one',
                                editorData: {
                                    mode: EDITOR_MODE.DEVICE,
                                    canEditPage: true,
                                    page: {
                                        isLocked: false,
                                        canLock: true,
                                        lockedByUser: ''
                                    }
                                },
                                currentExperiment: {
                                    status: DotExperimentStatus.RUNNING
                                },
                                editor: {
                                    page: {
                                        identifier: '123',
                                        inode: '456'
                                    },
                                    viewAs: {
                                        persona: {
                                            id: '123'
                                        },
                                        language: {
                                            id: 1
                                        }
                                    }
                                },
                                showWorkflowActions: true,
                                showInfoDisplay: true
                            }),
                            load: jest.fn(),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            updateEditorState: jest.fn()
                        }
                    }
                ]
            });
            store = spectator.inject(EditEmaStore);
            messageService = spectator.inject(MessageService);
            router = spectator.inject(Router);
            confirmationService = spectator.inject(ConfirmationService);
        });

        describe('dot-ema-running-experiment', () => {
            it('should have attr', () => {
                const experiments = spectator.query(DotEmaRunningExperimentComponent);
                expect(experiments.runningExperiment).toEqual({
                    status: DotExperimentStatus.RUNNING
                });
            });
        });
    });

    describe('urlContentMap', () => {
        beforeEach(() => {
            spectator = createComponent({
                providers: [
                    {
                        provide: EditEmaStore,
                        useValue: {
                            editorToolbarData$: of({
                                favoritePageURL: 'http://localhost:8080/fav',
                                iframeURL: 'http://localhost:8080/index',
                                clientHost: 'http://localhost:3000',
                                apiURL: 'http://localhost/api/v1/page/json/page-one',
                                editorData: {
                                    mode: EDITOR_MODE.EDIT,
                                    canEditPage: true,
                                    page: {
                                        isLocked: false,
                                        canLock: true,
                                        lockedByUser: ''
                                    }
                                },
                                editor: {
                                    urlContentMap: {
                                        identifier: '123',
                                        inode: '456',
                                        title: 'This is the content title'
                                    },
                                    page: {
                                        identifier: '123',
                                        inode: '456'
                                    },
                                    viewAs: {
                                        persona: {
                                            id: '123'
                                        },
                                        language: {
                                            id: 1
                                        }
                                    }
                                },
                                showWorkflowActions: true,
                                showInfoDisplay: true
                            }),
                            load: jest.fn(),
                            setDevice: jest.fn(),
                            setSocialMedia: jest.fn(),
                            updateEditorState: jest.fn()
                        }
                    }
                ]
            });
            store = spectator.inject(EditEmaStore);
            messageService = spectator.inject(MessageService);
            router = spectator.inject(Router);
            confirmationService = spectator.inject(ConfirmationService);
        });

        it('should have attr', () => {
            const editURLContentButton = spectator.debugElement.query(
                By.css('[data-testId="edit-url-content-map"]')
            );

            expect(editURLContentButton.attributes).toEqual({
                class: 'p-element',
                'data-testId': 'edit-url-content-map',
                icon: 'pi pi-pencil',
                'ng-reflect-icon': 'pi pi-pencil',
                'ng-reflect-style-class': 'p-button-text p-button-sm',
                styleClass: 'p-button-text p-button-sm'
            });
        });
        it('should emit', () => {
            let output;
            spectator.output('editUrlContentMap').subscribe((result) => (output = result));

            const editURLContentButton = spectator.debugElement.query(
                By.css('[data-testId="edit-url-content-map"]')
            );

            spectator.triggerEventHandler(editURLContentButton, 'onClick', {
                identifier: '123',
                inode: '456',
                title: 'This is the content title'
            });

            expect(output).toEqual({
                identifier: '123',
                inode: '456',
                title: 'This is the content title'
            });
        });
    });

    describe('locked', () => {
        describe('locked with unlock permission', () => {
            beforeEach(() => {
                spectator = createComponent({
                    providers: [
                        {
                            provide: EditEmaStore,
                            useValue: {
                                editorToolbarData$: of({
                                    favoritePageURL: 'http://localhost:8080/fav',
                                    iframeURL: 'http://localhost:8080/index',
                                    clientHost: 'http://localhost:3000',
                                    apiURL: 'http://localhost/api/v1/page/json/page-one',
                                    editorData: {
                                        mode: EDITOR_MODE.EDIT,
                                        canEditPage: true,
                                        page: {
                                            isLocked: true,
                                            canLock: true,
                                            lockedByUser: 'user'
                                        }
                                    },
                                    editor: {
                                        page: {
                                            identifier: '123',
                                            inode: '456'
                                        },
                                        viewAs: {
                                            persona: {
                                                id: '123'
                                            },
                                            language: {
                                                id: 1
                                            }
                                        }
                                    },
                                    showWorkflowActions: false,
                                    showInfoDisplay: true
                                }),
                                load: jest.fn(),
                                setDevice: jest.fn(),
                                setSocialMedia: jest.fn(),
                                updateEditorState: jest.fn()
                            }
                        }
                    ]
                });
                store = spectator.inject(EditEmaStore);
                messageService = spectator.inject(MessageService);
                router = spectator.inject(Router);
                confirmationService = spectator.inject(ConfirmationService);
            });

            it('should render a unlock button', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('unlock-button'))).toBeDefined();
            });
        });

        describe('locked without unlock permission', () => {
            beforeEach(() => {
                spectator = createComponent({
                    providers: [
                        {
                            provide: EditEmaStore,
                            useValue: {
                                editorToolbarData$: of({
                                    favoritePageURL: 'http://localhost:8080/fav',
                                    iframeURL: 'http://localhost:8080/index',
                                    clientHost: 'http://localhost:3000',
                                    apiURL: 'http://localhost/api/v1/page/json/page-one',
                                    editorData: {
                                        mode: EDITOR_MODE.EDIT,
                                        canEditPage: true,
                                        page: {
                                            isLocked: true,
                                            canLock: false,
                                            lockedByUser: 'user'
                                        }
                                    },
                                    editor: {
                                        page: {
                                            identifier: '123',
                                            inode: '456'
                                        },
                                        viewAs: {
                                            persona: {
                                                id: '123'
                                            },
                                            language: {
                                                id: 1
                                            }
                                        }
                                    },
                                    showWorkflowActions: false,
                                    showInfoDisplay: true
                                }),
                                load: jest.fn(),
                                setDevice: jest.fn(),
                                setSocialMedia: jest.fn(),
                                updateEditorState: jest.fn()
                            }
                        }
                    ]
                });
                store = spectator.inject(EditEmaStore);
                messageService = spectator.inject(MessageService);
                router = spectator.inject(Router);
                confirmationService = spectator.inject(ConfirmationService);
            });

            it('should not render a unlock button', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('unlock-button'))).toBeNull();
            });
        });
    });
});
