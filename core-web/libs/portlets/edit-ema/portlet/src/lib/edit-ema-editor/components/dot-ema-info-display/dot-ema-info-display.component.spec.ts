import { Spectator, SpyObject, byTestId, createComponentFactory } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLicenseService,
    DotMessageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotExperimentsServiceMock,
    DotLicenseServiceMock,
    getRunningExperimentMock,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotEmaInfoDisplayComponent } from './dot-ema-info-display.component';

import { EditEmaStore } from '../../../dot-ema-shell/store/dot-ema.store';
import { DotPageApiService } from '../../../services/dot-page-api.service';
import { DEFAULT_PERSONA, MOCK_RESPONSE_HEADLESS } from '../../../shared/consts';
import { EDITOR_MODE } from '../../../shared/enums';

describe('DotEmaInfoDisplayComponent', () => {
    let spectator: Spectator<DotEmaInfoDisplayComponent>;
    let store: SpyObject<EditEmaStore>;

    const createComponent = createComponentFactory({
        component: DotEmaInfoDisplayComponent,
        imports: [CommonModule, HttpClientTestingModule],
        providers: [
            EditEmaStore,
            MessageService,
            {
                provide: DotExperimentsService,
                useValue: DotExperimentsServiceMock
            },
            {
                provide: DotPageApiService,
                useValue: {
                    get: () => of(MOCK_RESPONSE_HEADLESS)
                }
            },
            {
                provide: DotLicenseService,
                useValue: new DotLicenseServiceMock()
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: (key) => key
                }
            },
            {
                provide: DotContentletLockerService,
                useValue: {
                    unlock: (_inode: string) => of({})
                }
            },
            {
                provide: LoginService,
                useValue: {
                    getCurrentUser: () => of({})
                }
            }
        ]
    });

    describe('device', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    currentExperiment: getRunningExperimentMock(),
                    editorData: {
                        mode: EDITOR_MODE.DEVICE,
                        device: { ...mockDotDevices[0], icon: 'someIcon' },
                        page: {
                            canLock: true,
                            isLocked: false,
                            lockedByUser: ''
                        }
                    }
                }
            });

            store = spectator.inject(EditEmaStore);

            store.load({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });
        });
        it('should show name, sizes and icon of the selected device', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'iphone 200 x 100'
            );
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
        });

        describe('action', () => {
            it('should call updateEditorData when action button is clicked and variant is default', () => {
                spectator.detectChanges();

                const infoAction = spectator.debugElement.query(
                    By.css('[data-testId="info-action"]')
                );

                const updateEditorDataSpy = jest.spyOn(store, 'updateEditorData');

                spectator.triggerEventHandler(infoAction, 'onClick', {});

                expect(updateEditorDataSpy).toHaveBeenCalledWith({
                    mode: EDITOR_MODE.EDIT
                });
            });

            it("should call updateEditorData with mode 'EDIT_VARIANT' when action button is clicked and variant is not default and user can edit variant", () => {
                spectator.setInput('editorData', {
                    mode: EDITOR_MODE.DEVICE,
                    device: { ...mockDotDevices[0], icon: 'someIcon' },
                    variantId: '123',
                    canEditVariant: true,
                    page: {
                        canLock: true,
                        isLocked: false,
                        lockedByUser: ''
                    }
                });

                spectator.detectChanges();

                const infoAction = spectator.debugElement.query(
                    By.css('[data-testId="info-action"]')
                );

                const updateEditorDataSpy = jest.spyOn(store, 'updateEditorData');

                spectator.triggerEventHandler(infoAction, 'onClick', {});

                expect(updateEditorDataSpy).toHaveBeenCalledWith({
                    mode: EDITOR_MODE.EDIT_VARIANT
                });
            });

            it("should call updateEditorData with mode 'PREVIEW_VARIANT' when action button is clicked and variant is not default and user cannot edit variant", () => {
                spectator.setInput('editorData', {
                    mode: EDITOR_MODE.DEVICE,
                    device: { ...mockDotDevices[0], icon: 'someIcon' },
                    variantId: '123',
                    canEditVariant: false,
                    page: {
                        canLock: true,
                        isLocked: false,
                        lockedByUser: ''
                    }
                });

                spectator.detectChanges();

                const infoAction = spectator.debugElement.query(
                    By.css('[data-testId="info-action"]')
                );

                const updateEditorDataSpy = jest.spyOn(store, 'updateEditorData');

                spectator.triggerEventHandler(infoAction, 'onClick', {});

                expect(updateEditorDataSpy).toHaveBeenCalledWith({
                    mode: EDITOR_MODE.PREVIEW_VARIANT
                });
            });
        });
    });

    describe('social media', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    currentExperiment: getRunningExperimentMock(),
                    editorData: {
                        mode: EDITOR_MODE.SOCIAL_MEDIA,
                        socialMedia: 'Facebook',
                        page: {
                            canLock: true,
                            isLocked: false,
                            lockedByUser: ''
                        }
                    }
                }
            });

            store = spectator.inject(EditEmaStore);

            store.load({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });
        });

        it('should show social media name, icon and action button', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'Viewing Facebook social media preview'
            );
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
        });

        it('should call goToEdit when clicking on action button', () => {
            const goToEditSpy = jest.spyOn(spectator.component, 'goToEdit');
            spectator.detectChanges();

            const infoAction = spectator.debugElement.query(By.css('[data-testId="info-action"]'));

            spectator.triggerEventHandler(infoAction, 'onClick', {});

            expect(goToEditSpy).toHaveBeenCalledTimes(1);
        });
    });

    describe('edit permissions', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    currentExperiment: getRunningExperimentMock(),
                    editorData: {
                        mode: EDITOR_MODE.EDIT,
                        canEditPage: false,
                        page: {
                            canLock: true,
                            isLocked: false,
                            lockedByUser: ''
                        }
                    }
                }
            });
        });
        it('should show label and icon for no permissions', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'editema.dont.have.edit.permission'
            );
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
        });
    });

    describe('variant', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    currentExperiment: getRunningExperimentMock(),
                    editorData: {
                        mode: EDITOR_MODE.EDIT_VARIANT,
                        canEditPage: true,
                        canEditVariant: true,
                        page: {
                            canLock: true,
                            isLocked: false,
                            lockedByUser: ''
                        }
                    }
                }
            });
        });
        describe('can edit variant', () => {
            it('should show label and icon for editing variant', () => {
                spectator.detectChanges();
                expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                    'editpage.editing.variant'
                );
                expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
                expect(spectator.query(byTestId('info-action'))).not.toBeNull();
            });
        });
        describe('cannot edit variant', () => {
            it('should show label and icon for viewing variant', () => {
                spectator.setInput('editorData', {
                    mode: EDITOR_MODE.PREVIEW_VARIANT,
                    canEditPage: true,
                    canEditVariant: false,
                    page: {
                        canLock: true,
                        isLocked: false,
                        lockedByUser: ''
                    }
                });

                spectator.detectChanges();
                expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                    'editpage.viewing.variant'
                );
                expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
                expect(spectator.query(byTestId('info-action'))).not.toBeNull();
            });
        });

        describe('action', () => {
            it('should navigate to the experiment configuration page when action button is clicked', () => {
                const router = spectator.inject(Router);
                const navigateSpy = jest.spyOn(router, 'navigate');
                spectator.detectChanges();

                const infoAction = spectator.debugElement.query(
                    By.css('[data-testId="info-action"]')
                );

                spectator.triggerEventHandler(infoAction, 'onClick', {});

                expect(navigateSpy).toHaveBeenCalledWith(
                    ['/edit-page/experiments/', '456', '555-5555-5555-5555', 'configuration'],
                    {
                        queryParams: {
                            mode: null,
                            variantName: null,
                            experimentId: null
                        },
                        queryParamsHandling: 'merge'
                    }
                );
            });
        });
    });

    describe('locked', () => {
        beforeEach(() => {
            spectator = createComponent({
                props: {
                    currentExperiment: getRunningExperimentMock(),
                    editorData: {
                        mode: EDITOR_MODE.EDIT_VARIANT,
                        canEditPage: true,
                        canEditVariant: true,
                        page: {
                            canLock: true,
                            isLocked: false,
                            lockedByUser: ''
                        }
                    }
                }
            });
        });
        describe('locked with permission to unlock', () => {
            it('should show lock icon when page is locked', () => {
                spectator.setInput('editorData', {
                    mode: EDITOR_MODE.EDIT_VARIANT,
                    canEditPage: true,
                    canEditVariant: true,
                    page: {
                        canLock: true,
                        isLocked: true,
                        lockedByUser: 'user'
                    }
                });

                spectator.detectChanges();

                expect(spectator.query(byTestId('info-text')).innerHTML).toBe('editpage.locked-by');
            });
        });

        describe('locked without permission to unlock', () => {
            it('should show lock icon when page is locked', () => {
                spectator.setInput('editorData', {
                    mode: EDITOR_MODE.EDIT_VARIANT,
                    canEditPage: false,
                    canEditVariant: true,
                    page: {
                        canLock: false,
                        isLocked: true,
                        lockedByUser: 'user'
                    }
                });

                spectator.detectChanges();

                expect(spectator.query(byTestId('info-text')).innerHTML).toBe(
                    'editpage.locked-contact-with'
                );
            });
        });
    });
});
