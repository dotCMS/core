import { it, describe, expect } from '@jest/globals';
import {
    Spectator,
    SpyObject,
    byTestId,
    createComponentFactory,
    mockProvider
} from '@ngneat/spectator/jest';
import { patchState } from '@ngrx/signals';
import { of } from 'rxjs';

import { CommonModule } from '@angular/common';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService } from 'primeng/api';

import {
    DotContentletLockerService,
    DotExperimentsService,
    DotLanguagesService,
    DotLicenseService,
    DotMessageService
} from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DEFAULT_VARIANT_NAME } from '@dotcms/dotcms-models';
import {
    DotExperimentsServiceMock,
    DotLanguagesServiceMock,
    DotLicenseServiceMock,
    getRunningExperimentMock,
    mockDotDevices
} from '@dotcms/utils-testing';

import { DotEmaInfoDisplayComponent } from './dot-ema-info-display.component';

import { DotPageApiService } from '../../../services/dot-page-api.service';
import { DEFAULT_PERSONA } from '../../../shared/consts';
import { MOCK_RESPONSE_HEADLESS } from '../../../shared/mocks';
import { UVEStore } from '../../../store/dot-uve.store';

describe('DotEmaInfoDisplayComponent', () => {
    let spectator: Spectator<DotEmaInfoDisplayComponent>;
    let store: SpyObject<InstanceType<typeof UVEStore>>;
    let router: SpyObject<Router>;

    const createComponent = createComponentFactory({
        component: DotEmaInfoDisplayComponent,
        imports: [CommonModule, HttpClientTestingModule],
        providers: [
            UVEStore,
            MessageService,
            mockProvider(Router),
            mockProvider(ActivatedRoute),
            {
                provide: DotLanguagesService,
                useValue: new DotLanguagesServiceMock()
            },
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
            spectator = createComponent();

            store = spectator.inject(UVEStore);

            store.init({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });
            store.setDevice({ ...mockDotDevices[0], icon: 'test' });
        });

        it('should show name, sizes and icon of the selected device', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'iphone 200 x 100'
            );
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
        });

        it('should call clearDeviceAndSocialMedia when action button is clicked', () => {
            spectator.detectChanges();

            const clearDeviceAndSocialMediaSpy = jest.spyOn(store, 'clearDeviceAndSocialMedia');

            const infoAction = spectator.debugElement.query(By.css('[data-testId="info-action"]'));

            spectator.triggerEventHandler(infoAction, 'onClick', store.$infoDisplayOptions());

            expect(clearDeviceAndSocialMediaSpy).toHaveBeenCalled();
        });
    });

    describe('socialMedia', () => {
        beforeEach(() => {
            spectator = createComponent();

            store = spectator.inject(UVEStore);

            store.init({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier
            });
            store.setSocialMedia('facebook');
        });
        it('should text for current social media', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'Viewing facebook social media preview'
            );
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
        });
        it('should call clearDeviceAndSocialMedia when action button is clicked', () => {
            spectator.detectChanges();

            const clearDeviceAndSocialMediaSpy = jest.spyOn(store, 'clearDeviceAndSocialMedia');

            const infoAction = spectator.debugElement.query(By.css('[data-testId="info-action"]'));

            spectator.triggerEventHandler(infoAction, 'onClick', store.$infoDisplayOptions());

            expect(clearDeviceAndSocialMediaSpy).toHaveBeenCalled();
        });
    });

    describe('variant', () => {
        beforeEach(() => {
            spectator = createComponent();

            store = spectator.inject(UVEStore);
            router = spectator.inject(Router);

            store.init({
                clientHost: 'http://localhost:3000',
                url: 'index',
                language_id: '1',
                'com.dotmarketing.persona.id': DEFAULT_PERSONA.identifier,
                variantId: '555-5555-5555-5555'
            });

            const currentExperiment = getRunningExperimentMock();

            const variantID = currentExperiment.trafficProportion.variants.find(
                (variant) => variant.name !== DEFAULT_VARIANT_NAME
            ).id;

            patchState(store, {
                pageAPIResponse: {
                    ...MOCK_RESPONSE_HEADLESS,
                    viewAs: {
                        ...MOCK_RESPONSE_HEADLESS.viewAs,
                        variantId: variantID
                    }
                },
                experiment: currentExperiment
            });
        });
        it('should show have text for variant', () => {
            spectator.detectChanges();
            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'editpage.editing.variant'
            );
            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
        });
        it('should call router when action button is clicked', () => {
            spectator.detectChanges();

            const navigateSpy = jest.spyOn(router, 'navigate');

            const infoAction = spectator.debugElement.query(By.css('[data-testId="info-action"]'));

            spectator.triggerEventHandler(infoAction, 'onClick', store.$infoDisplayOptions());

            expect(navigateSpy).toHaveBeenCalledWith(
                ['/edit-page/experiments/', '456', '555-5555-5555-5555', 'configuration'],
                {
                    queryParams: { experimentId: null, mode: null, variantName: null },
                    queryParamsHandling: 'merge'
                }
            );
        });
    });

    describe('edit permissions', () => {
        beforeEach(() => {
            spectator = createComponent();

            store = spectator.inject(UVEStore);

            patchState(store, {
                canEditPage: false
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

    describe('edit permissions', () => {
        beforeEach(() => {
            spectator = createComponent();

            store = spectator.inject(UVEStore);

            patchState(store, {
                pageAPIResponse: {
                    ...MOCK_RESPONSE_HEADLESS,
                    page: {
                        ...MOCK_RESPONSE_HEADLESS.page,
                        locked: true,
                        canLock: true,
                        lockedByName: 'John Doe'
                    }
                }
            });
        });

        it('should show label and icon for no permissions', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('info-text')).textContent.trim()).toBe(
                'editpage.locked-by'
            );

            expect(spectator.query(byTestId('info-icon'))).not.toBeNull();
        });
    });
});
