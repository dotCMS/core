import { it, expect, describe } from '@jest/globals';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';

import { Menu } from 'primeng/menu';

import { DotMessageService } from '@dotcms/data-access';
import { mockDotDevices } from '@dotcms/utils-testing';

import { DotUveDeviceSelectorComponent } from './dot-uve-device-selector.component';

import { DEFAULT_PERSONA, DEFAULT_DEVICES, DEFAULT_DEVICE } from '../../../../../shared/consts';
import {
    HEADLESS_BASE_QUERY_PARAMS,
    MOCK_RESPONSE_HEADLESS,
    MOCK_RESPONSE_VTL
} from '../../../../../shared/mocks';
import { UVEStore } from '../../../../../store/dot-uve.store';
import { Orientation } from '../../../../../store/models';
import {
    sanitizeURL,
    getFullPageURL,
    createFavoritePagesURL,
    createFullURL
} from '../../../../../utils';

const $apiURL = '/api/v1/page/json/123-xyz-567-xxl?host_id=123-xyz-567-xxl&language_id=1';

const params = HEADLESS_BASE_QUERY_PARAMS;
const url = sanitizeURL(params?.url);

const pageAPIQueryParams = getFullPageURL({ url, params });
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
    setOrientation: jest.fn(),
    setSEO: jest.fn(),
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
    device: signal(DEFAULT_DEVICE),
    socialMedia: signal(null),
    isTraditionalPage: signal(true)
};

describe('DotUveDeviceSelectorComponent', () => {
    let spectator: Spectator<DotUveDeviceSelectorComponent>;
    let uveStore: InstanceType<typeof UVEStore>;

    const createComponent = createComponentFactory({
        component: DotUveDeviceSelectorComponent,
        providers: [
            {
                provide: UVEStore,
                useValue: baseUVEState
            },
            {
                provide: DotMessageService,
                useValue: {
                    get: jest.fn((value) => value)
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        uveStore = spectator.inject(UVEStore, true);
    });

    it('should set the device when is present in viewParams', () => {
        const setDeviceSpy = jest.spyOn(uveStore, 'setDevice');
        const device = DEFAULT_DEVICES[1];

        baseUVEState.viewParams.set({
            device: device.inode,
            orientation: undefined,
            seo: undefined
        });

        spectator.setInput('devices', [...DEFAULT_DEVICES, ...mockDotDevices]);
        spectator.detectChanges();

        expect(setDeviceSpy).toHaveBeenCalledWith(device, undefined);
    });

    it('should set the device and orientation when is present in viewParams', () => {
        const setDeviceSpy = jest.spyOn(baseUVEState, 'setDevice');

        const device = DEFAULT_DEVICES[1];
        const orientation = Orientation.PORTRAIT;
        baseUVEState.viewParams.set({
            device: device.inode,
            orientation: orientation,
            seo: undefined
        });

        spectator.setInput('devices', [...DEFAULT_DEVICES, ...mockDotDevices]);
        spectator.detectChanges();

        expect(setDeviceSpy).toHaveBeenCalledWith(device, orientation);
    });

    it('should set the default device when is not present in viewParams', () => {
        const setDeviceSpy = jest.spyOn(baseUVEState, 'setDevice');

        baseUVEState.viewParams.set({
            device: undefined,
            orientation: undefined,
            seo: undefined
        });

        spectator.setInput('devices', [...DEFAULT_DEVICES, ...mockDotDevices]);
        spectator.detectChanges();
        expect(setDeviceSpy).toHaveBeenCalledWith(DEFAULT_DEVICE, undefined);
    });

    it('should set the default device when the device is not found in the devices list', () => {
        const setDeviceSpy = jest.spyOn(baseUVEState, 'setDevice');

        baseUVEState.viewParams.set({
            device: 'not-found',
            orientation: undefined,
            seo: undefined
        });
        spectator.component.ngOnInit();

        spectator.setInput('devices', [...DEFAULT_DEVICES, ...mockDotDevices]);
        spectator.detectChanges();

        expect(setDeviceSpy).toHaveBeenCalledWith(DEFAULT_DEVICE, undefined);
    });

    describe('DOM', () => {
        beforeEach(() => {
            spectator.setInput('devices', [...DEFAULT_DEVICES, ...mockDotDevices]);
        });

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
                baseUVEState.device.set(
                    DEFAULT_DEVICES.find((device) => device.inode === 'default')
                );
                spectator.detectChanges();

                // In Angular 20, ng-reflect-* attributes are not available
                // Verify the disabled property on the p-button component instance
                const buttonDebugElement = spectator.debugElement.query(
                    By.css('[data-testId="orientation"]')
                );
                const buttonComponent = buttonDebugElement?.componentInstance;
                expect(buttonComponent?.disabled).toBe(true);
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
                const setDeviceSpy = jest.spyOn(uveStore, 'setDevice');

                const customDevices = spectator.component
                    .$menuItems()
                    .find((item) => item.id === 'custom-devices');
                const customDeviceItem = customDevices.items[0];

                const deviceSelected = mockDotDevices.find(
                    (device) => device.inode === customDeviceItem.id
                );

                spectator.detectChanges();
                customDeviceItem.command({});

                expect(setDeviceSpy).toHaveBeenCalledWith(deviceSelected);
            });
        });

        describe('Social media', () => {
            it('should trigger onSocialMediaSelect when the social media button is clicked', () => {
                const setSEOSpy = jest.spyOn(uveStore, 'setSEO');

                const socialMedia = spectator.component
                    .$menuItems()
                    .find((item) => item.id === 'social-media');
                const socialMediaItem = socialMedia.items[0];

                socialMediaItem.command({});

                spectator.detectChanges();

                expect(setSEOSpy).toHaveBeenCalledWith(socialMediaItem.value);
            });

            it('should set the default device when the social media is the same as the current one', () => {
                const setDeviceSpy = jest.spyOn(uveStore, 'setDevice');
                const setSEOSpy = jest.spyOn(uveStore, 'setSEO');

                const socialMedia = spectator.component
                    .$menuItems()
                    .find((item) => item.id === 'social-media');
                const socialMediaItem = socialMedia.items[0];

                baseUVEState.socialMedia.set(socialMediaItem.value);

                spectator.detectChanges();
                socialMediaItem.command({});

                expect(setDeviceSpy).toHaveBeenCalledWith(DEFAULT_DEVICE);
                expect(setSEOSpy).not.toHaveBeenCalled();
            });
        });

        describe('Search engine', () => {
            it('should trigger onSocialMediaSelect when the search engine button is clicked', () => {
                const setSEOSpy = jest.spyOn(uveStore, 'setSEO');

                const searchEngine = spectator.component
                    .$menuItems()
                    .find((item) => item.id === 'search-engine');
                const searchEngineItem = searchEngine.items[0];

                searchEngineItem.command({});
                expect(setSEOSpy).toHaveBeenCalledWith(searchEngineItem.value);
            });

            it('should set the default device when the search engine is the same as the current one', () => {
                const setDeviceSpy = jest.spyOn(uveStore, 'setDevice');
                const setSEOSpy = jest.spyOn(uveStore, 'setSEO');

                const socialMedia = spectator.component
                    .$menuItems()
                    .find((item) => item.id === 'search-engine');
                const socialMediaItem = socialMedia.items[0];

                baseUVEState.socialMedia.set(socialMediaItem.value);

                spectator.detectChanges();
                socialMediaItem.command({});

                expect(setDeviceSpy).toHaveBeenCalledWith(DEFAULT_DEVICE);
                expect(setSEOSpy).not.toHaveBeenCalled();
            });
        });

        describe('More items menu', () => {
            const EXPECT_MENU_ITEM_OPTION = [
                {
                    label: 'uve.preview.mode.device.subheader',
                    id: 'custom-devices',
                    items: [
                        { label: 'iphone (200x100)', id: '1', command: expect.any(Function) },
                        { label: 'bad device (0x0)', id: '2', command: expect.any(Function) }
                    ]
                },
                {
                    label: 'uve.preview.mode.social.media.subheader',
                    id: 'social-media',
                    items: [
                        {
                            label: 'Facebook',
                            id: 'Facebook',
                            value: 'Facebook',
                            command: expect.any(Function)
                        },
                        {
                            label: 'X (Formerly Twitter)',
                            id: 'Twitter',
                            value: 'Twitter',
                            command: expect.any(Function)
                        },
                        {
                            label: 'Linkedin',
                            id: 'LinkedIn',
                            value: 'LinkedIn',
                            command: expect.any(Function)
                        }
                    ]
                },
                {
                    label: 'uve.preview.mode.search.engine.subheader',
                    id: 'search-engine',
                    items: [
                        {
                            label: 'Google',
                            id: 'Google',
                            value: 'Google',
                            command: expect.any(Function)
                        }
                    ]
                }
            ];

            it('should receive the right items', () => {
                const menuElement = spectator.query(Menu);

                expect(menuElement.model).toEqual(EXPECT_MENU_ITEM_OPTION);
            });

            it('should show the menu after clicking the `more` button', () => {
                const moreButton = spectator.query(`[data-testid="more-button"]`);

                moreButton.dispatchEvent(new Event('click'));
                spectator.detectChanges();

                const menuList = spectator.query("[data-testid='more-menu'] > ul");
                expect(menuList).toBeDefined();
            });
        });

        describe('More button label', () => {
            it('should show "more" as default label', () => {
                // Simulate the default device and social media not being set
                baseUVEState.device.set(DEFAULT_DEVICE);
                baseUVEState.socialMedia.set(null);

                baseUVEState.viewParams.set({
                    device: undefined,
                    orientation: undefined,
                    seo: undefined
                });

                spectator.detectChanges();
                const moreButton = spectator.query('[data-testid="more-button"]');

                expect(moreButton.textContent.trim()).toBe('more');
            });

            it('should show custom device name when selected', () => {
                const customDevice = mockDotDevices[0];

                // Simulate the custom device selection

                baseUVEState.device.set(customDevice);
                baseUVEState.socialMedia.set(null);

                baseUVEState.viewParams.set({
                    device: customDevice.inode,
                    orientation: undefined,
                    seo: undefined
                });

                spectator.detectChanges();
                const moreButton = spectator.query('[data-testid="more-button"]');

                expect(moreButton.textContent.trim()).toBe(customDevice.name);
            });

            it('should show social media name when selected', () => {
                // Simulate the social media selection
                const socialMedia = 'Facebook';
                baseUVEState.socialMedia.set(socialMedia);
                baseUVEState.device.set(DEFAULT_DEVICE);

                baseUVEState.viewParams.set({
                    device: undefined,
                    orientation: undefined,
                    seo: socialMedia
                });

                spectator.detectChanges();
                const moreButton = spectator.query('[data-testid="more-button"]');

                expect(moreButton.textContent.trim()).toBe(socialMedia);
            });
        });
    });

    afterEach(() => jest.clearAllMocks());
});
