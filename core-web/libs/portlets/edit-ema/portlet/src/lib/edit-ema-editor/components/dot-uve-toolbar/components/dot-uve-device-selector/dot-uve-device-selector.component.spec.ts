import { it, expect, describe } from '@jest/globals';
import { Spectator, createComponentFactory, mockProvider } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { mockDotDevices } from '@dotcms/utils-testing';

import { DotUveDeviceSelectorComponent } from './dot-uve-device-selector.component';

import { DEFAULT_PERSONA, DEFAULT_DEVICES } from '../../../../../shared/consts';
import {
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL
} from '../../../../../shared/mocks';
import { UVEStore } from '../../../../../store/dot-uve.store';
import {
    sanitizeURL,
    createPageApiUrlWithQueryParams,
    createFavoritePagesURL,
    createFullURL
} from '../../../../../utils';

const $apiURL = '/api/v1/page/json/123-xyz-567-xxl?host_id=123-xyz-567-xxl&language_id=1';

const params = HEADLESS_BASE_QUERY_PARAMS;
const url = sanitizeURL(params?.url);

const pageAPIQueryParams = createPageApiUrlWithQueryParams(url, params);
const pageAPI = `/api/v1/page/${'json'}/${pageAPIQueryParams}`;
const pageAPIResponse = MOCK_RESPONSE_HEADLESS;
const shouldShowInfoDisplay = false || pageAPIResponse?.page.locked;
const bookmarksUrl = createFavoritePagesURL({
    languageId: Number(params?.language_id),
    pageURI: url,
    siteId: pageAPIResponse?.site.identifier
});

const baseUVEToolbarState = {
    editor: {
        bookmarksUrl,
        copyUrl: createFullURL(params, pageAPIResponse?.site.identifier),
        apiUrl: `${'http://localhost'}${pageAPI}`
    },
    preview: null,
    currentLanguage: pageAPIResponse?.viewAs.language,
    urlContentMap: null,
    runningExperiment: null,
    workflowActionsInode: pageAPIResponse?.page.inode,
    unlockButton: null,
    showInfoDisplay: shouldShowInfoDisplay
};

const baseUVEState = {
    $uveToolbar: signal(baseUVEToolbarState),
    setDevice: jest.fn(),
    setSocialMedia: jest.fn(),
    pageParams: signal(params),
    pageAPIResponse: signal(MOCK_RESPONSE_VTL),
    $apiURL: signal($apiURL),
    reloadCurrentPage: jest.fn(),
    loadPageAsset: jest.fn(),
    $isPreviewMode: signal(false),
    $personaSelector: signal({
        pageId: pageAPIResponse?.page.identifier,
        value: pageAPIResponse?.viewAs.persona ?? DEFAULT_PERSONA
    }),
    $infoDisplayProps: signal(undefined),
    viewParams: signal({
        seo: undefined,
        device: undefined,
        orientation: undefined
    }),
    languages: signal([
        { id: 1, translated: true },
        { id: 2, translated: false },
        { id: 3, translated: true }
    ]),
    patchViewParams: jest.fn(),
    orientation: signal(''),
    clearDeviceAndSocialMedia: jest.fn(),
    device: signal(DEFAULT_DEVICES.find((device) => device.inode === 'default'))
};

describe('DotUveDeviceSelectorComponent', () => {
    let spectator: Spectator<DotUveDeviceSelectorComponent>;

    const createComponent = createComponentFactory({
        component: DotUveDeviceSelectorComponent,

        providers: [
            UVEStore,
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn((value) => value)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            providers: [mockProvider(UVEStore, { ...baseUVEState })]
        });

        spectator.setInput('devices', [...DEFAULT_DEVICES, ...mockDotDevices]);

        spectator.detectChanges();
    });

    describe('DOM', () => {
        describe('Default devices button', () => {
            it.each(DEFAULT_DEVICES)('should have a button for $inode', ({ inode }) => {
                const button = spectator.query(`[data-testid="${inode}"]`);
                expect(button).toBeTruthy();
            });

            it.each(DEFAULT_DEVICES)('should trigger onDeviceSelect for $inode', ({ inode }) => {
                const onDeviceSelectSpy = jest.spyOn(spectator.component, 'onDeviceSelect');

                const button = spectator.query(`[data-testid="${inode}"]`);

                spectator.click(button);

                expect(onDeviceSelectSpy).toHaveBeenCalledWith(
                    DEFAULT_DEVICES.find((device) => device.inode === inode)
                );
            });
        });

        describe('orientation button', () => {
            it('should be disabled if the device is default', () => {
                const button = spectator.query(`[data-testid="orientation"]`);

                baseUVEState.viewParams.set({
                    device: 'default',
                    orientation: 'landscape',
                    seo: undefined
                });
                spectator.detectChanges();

                expect(button.getAttribute('ng-reflect-disabled')).toBe('true');
            });

            it("should call onOrientationChange when the orientation button is clicked and the device isn't default", () => {
                const onOrientationChangeSpy = jest.spyOn(
                    spectator.component,
                    'onOrientationChange'
                );

                const button = spectator.query(`[data-testid="orientation"]`);

                spectator.click(button);

                expect(onOrientationChangeSpy).toHaveBeenCalled();
            });
        });

        describe('Custom devices', () => {
            it('should render a button when custom devices are present', () => {
                spectator.detectChanges();
                const moreButton = spectator.query(`[data-testid="more-button"]`);
                expect(moreButton).toBeTruthy();
            });

            it('should trigger onDeviceSelect when a custom device is clicked', () => {
                spectator.detectChanges();

                const onDeviceSelectSpy = jest.spyOn(spectator.component, 'onDeviceSelect');
                const firstCustomDevice = spectator.component.$menuItems()[0].items[0];

                firstCustomDevice.command();

                expect(onDeviceSelectSpy).toHaveBeenCalledWith(mockDotDevices[0]);
            });
        });
    });
});
