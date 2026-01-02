import { Spectator, byTestId, createComponentFactory } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotDevice } from '@dotcms/dotcms-models';
import { MockDotMessageService, mockDotDevices } from '@dotcms/utils-testing';

import {
    DotUveDeviceSelectorComponent,
    DeviceSelectorState,
    DeviceSelectorChange
} from './dot-uve-device-selector.component';

import { DEFAULT_DEVICE, DEFAULT_DEVICES } from '../../../../../shared/consts';
import { Orientation } from '../../../../../store/models';

describe('DotUveDeviceSelectorComponent - Presentational', () => {
    let spectator: Spectator<DotUveDeviceSelectorComponent>;
    let emittedChanges: DeviceSelectorChange[] = [];

    const mockDevices = [...DEFAULT_DEVICES, ...mockDotDevices];

    const createComponent = createComponentFactory({
        component: DotUveDeviceSelectorComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'uve.preview.mode.device.subheader': 'Devices',
                    'uve.preview.mode.social.media.subheader': 'Social Media',
                    'uve.preview.mode.search.engine.subheader': 'Search Engines',
                    Desktop: 'Desktop',
                    Tablet: 'Tablet',
                    Mobile: 'Mobile'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        emittedChanges = [];
        spectator = createComponent();

        // Subscribe to state change events
        spectator.component.stateChange.subscribe((change) => {
            emittedChanges.push(change);
        });
    });

    describe('Component Creation', () => {
        it('should create', () => {
            const initialState: DeviceSelectorState = {
                currentDevice: null,
                currentSocialMedia: null,
                currentOrientation: null
            };
            spectator.setInput('state', initialState);
            spectator.setInput('devices', mockDevices);
            spectator.detectChanges();

            expect(spectator.component).toBeTruthy();
        });
    });

    describe('Device Selection', () => {
        beforeEach(() => {
            const state: DeviceSelectorState = {
                currentDevice: DEFAULT_DEVICE,
                currentSocialMedia: null,
                currentOrientation: Orientation.LANDSCAPE
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', true);
            spectator.detectChanges();
        });

        it('should emit device change when selecting a device', () => {
            const customDevice = mockDevices.find((d) => !d._isDefault);

            spectator.component.onDeviceSelect(customDevice);

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'device',
                device: customDevice
            });
        });

        it('should emit default device when selecting same device (toggle off)', () => {
            const customDevice = mockDevices.find((d) => !d._isDefault);
            const state: DeviceSelectorState = {
                currentDevice: customDevice,
                currentSocialMedia: null,
                currentOrientation: Orientation.LANDSCAPE
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            spectator.component.onDeviceSelect(customDevice);

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'device',
                device: DEFAULT_DEVICE
            });
        });

        it('should show custom device in more button label', () => {
            const customDevice = mockDevices.find((d) => !d._isDefault);
            const state: DeviceSelectorState = {
                currentDevice: customDevice,
                currentSocialMedia: null,
                currentOrientation: Orientation.LANDSCAPE
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            expect(spectator.component.$moreButtonLabel()).toBe(customDevice.name);
        });
    });

    describe('Social Media Selection', () => {
        beforeEach(() => {
            const state: DeviceSelectorState = {
                currentDevice: DEFAULT_DEVICE,
                currentSocialMedia: null,
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', true);
            spectator.detectChanges();
        });

        it('should emit social media change when selecting social media', () => {
            spectator.component.onSocialMediaSelect('facebook');

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'socialMedia',
                socialMedia: 'facebook'
            });
        });

        it('should emit default device when selecting same social media (toggle off)', () => {
            const state: DeviceSelectorState = {
                currentDevice: null,
                currentSocialMedia: 'facebook',
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            spectator.component.onSocialMediaSelect('facebook');

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'device',
                device: DEFAULT_DEVICE
            });
        });

        it('should show social media in more button label', () => {
            const state: DeviceSelectorState = {
                currentDevice: null,
                currentSocialMedia: 'facebook',
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            expect(spectator.component.$moreButtonLabel()).toBe('facebook');
        });
    });

    describe('Orientation Toggle', () => {
        beforeEach(() => {
            const customDevice = mockDevices.find((d) => !d._isDefault);
            const state: DeviceSelectorState = {
                currentDevice: customDevice,
                currentSocialMedia: null,
                currentOrientation: Orientation.LANDSCAPE
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.detectChanges();
        });

        it('should emit orientation change from landscape to portrait', () => {
            spectator.component.onOrientationChange();

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'orientation',
                orientation: Orientation.PORTRAIT
            });
        });

        it('should emit orientation change from portrait to landscape', () => {
            const state: DeviceSelectorState = {
                currentDevice: mockDevices.find((d) => !d._isDefault),
                currentSocialMedia: null,
                currentOrientation: Orientation.PORTRAIT
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            spectator.component.onOrientationChange();

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'orientation',
                orientation: Orientation.LANDSCAPE
            });
        });

        it('should disable orientation when default device is selected', () => {
            const state: DeviceSelectorState = {
                currentDevice: DEFAULT_DEVICE,
                currentSocialMedia: null,
                currentOrientation: Orientation.LANDSCAPE
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            expect(spectator.component.$disableOrientation()).toBe(true);
        });

        it('should disable orientation when social media is selected', () => {
            const state: DeviceSelectorState = {
                currentDevice: null,
                currentSocialMedia: 'facebook',
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            expect(spectator.component.$disableOrientation()).toBe(true);
        });
    });

    describe('Menu Items', () => {
        it('should include custom devices in menu', () => {
            const state: DeviceSelectorState = {
                currentDevice: DEFAULT_DEVICE,
                currentSocialMedia: null,
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', true);
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();

            expect(menuItems.length).toBeGreaterThan(0);
            expect(menuItems.some((item) => item.id === 'custom-devices')).toBe(true);
        });

        it('should include social media menu for traditional pages', () => {
            const state: DeviceSelectorState = {
                currentDevice: DEFAULT_DEVICE,
                currentSocialMedia: null,
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', true);
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();

            expect(menuItems.some((item) => item.id === 'social-media')).toBe(true);
            expect(menuItems.some((item) => item.id === 'search-engine')).toBe(true);
        });

        it('should NOT include social media menu for non-traditional pages', () => {
            const state: DeviceSelectorState = {
                currentDevice: DEFAULT_DEVICE,
                currentSocialMedia: null,
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', false);
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();

            expect(menuItems.some((item) => item.id === 'social-media')).toBe(false);
            expect(menuItems.some((item) => item.id === 'search-engine')).toBe(false);
        });
    });

    describe('More Button Active State', () => {
        it('should be active when custom device is selected', () => {
            const customDevice = mockDevices.find((d) => !d._isDefault);
            const state: DeviceSelectorState = {
                currentDevice: customDevice,
                currentSocialMedia: null,
                currentOrientation: Orientation.LANDSCAPE
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.detectChanges();

            expect(spectator.component.$isMoreButtonActive()).toBe(true);
        });

        it('should NOT be active when default device is selected', () => {
            const state: DeviceSelectorState = {
                currentDevice: DEFAULT_DEVICE,
                currentSocialMedia: null,
                currentOrientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.detectChanges();

            expect(spectator.component.$isMoreButtonActive()).toBe(false);
        });
    });
});
