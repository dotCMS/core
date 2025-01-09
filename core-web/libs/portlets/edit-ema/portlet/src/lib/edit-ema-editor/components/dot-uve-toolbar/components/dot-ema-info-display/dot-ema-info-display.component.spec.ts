import { it, describe, expect } from '@jest/globals';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService,
    DotWorkflowsActionsService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import {
    DotLanguagesServiceMock,
    DotLicenseServiceMock,
    getRunningExperimentMock
} from '@dotcms/utils-testing';

import { DotEmaInfoDisplayComponent } from './dot-ema-info-display.component';

import { DotPageApiService } from '../../../../../services/dot-page-api.service';
import { MOCK_RESPONSE_HEADLESS } from '../../../../../shared/mocks';
import { UVEStore } from '../../../../../store/dot-uve.store';

describe('DotEmaInfoDisplayComponent', () => {
    let spectator: Spectator<DotEmaInfoDisplayComponent>;
    let store: SpyObject<InstanceType<typeof UVEStore>>;
    let router: SpyObject<Router>;

    const createComponent = createComponentFactory({
        component: DotEmaInfoDisplayComponent,
        imports: [CommonModule, HttpClientTestingModule],
        providers: [
            MessageService,
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            {
                provide: UVEStore,
                useValue: {
                    clearDeviceAndSocialMedia: jest.fn(),
                    experiment: signal(getRunningExperimentMock())
                }
            },
            {
                provide: DotWorkflowsActionsService,
                useValue: {
                    getByInode: () => of([])
                }
            },
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
            {
                provide: DotExperimentsService,
                useValue: {}
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

    beforeEach(() => {
        spectator = createComponent();

        store = spectator.inject(UVEStore);

        spectator.setInput('options', {
            icon: `pi pi-facebook}`,
            id: 'socialMedia',
            info: {
                message: `Viewing <b>facebook</b> social media preview`,
                args: []
            },
            actionIcon: 'pi pi-times'
        });
    });

    describe('DOM', () => {
        it('should show an icon and a text when passed through options', () => {
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'Viewing facebook social media preview'
            );
        });

        it('should have an actionIcon when provided', () => {
            expect(spectator.query(byTestId('info-action'))).not.toBeNull();
        });

        it('should call clearDeviceAndSocialMedia when action button is clicked', () => {
            const clearDeviceAndSocialMediaSpy = jest.spyOn(store, 'clearDeviceAndSocialMedia');

            const infoAction = spectator.debugElement.query(By.css('[data-testId="info-action"]'));

            spectator.triggerEventHandler(infoAction, 'onClick', {});

            expect(clearDeviceAndSocialMediaSpy).toHaveBeenCalled();
        });
    });

    describe('variant', () => {
        beforeEach(() => {
            spectator = createComponent();
            router = spectator.inject(Router);

            spectator.setInput('options', {
                icon: 'pi pi-file-edit',
                id: 'variant',
                info: {
                    message: 'editpage.editing.variant',
                    args: ['Variant A']
                },
                actionIcon: 'pi pi-arrow-left'
            });
        });
        it('should call router when action button is clicked', () => {
            const navigateSpy = jest.spyOn(router, 'navigate');

            const infoAction = spectator.debugElement.query(By.css('[data-testId="info-action"]'));

            spectator.triggerEventHandler(infoAction, 'onClick', {});

            expect(navigateSpy).toHaveBeenCalledWith(
                ['/edit-page/experiments/', '456', '555-5555-5555-5555', 'configuration'],
                {
                    queryParams: { experimentId: null, mode: null, variantName: null },
                    queryParamsHandling: 'merge'
                }
            );
        });
    });
});
