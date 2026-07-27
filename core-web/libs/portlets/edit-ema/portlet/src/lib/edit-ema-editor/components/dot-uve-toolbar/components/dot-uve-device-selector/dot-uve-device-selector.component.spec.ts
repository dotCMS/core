import { Spectator, createComponentFactory } from '@openng/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService, mockDotDevices } from '@dotcms/utils-testing';

import { DotUveDeviceSelectorComponent } from './dot-uve-device-selector.component';
import { DeviceSelectorChange, DeviceSelectorState } from './dot-uve-device-selector.models';

import { DEFAULT_DEVICE, DEFAULT_DEVICES } from '../../../../../shared/consts';

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

        spectator.component.stateChange.subscribe((change) => {
            emittedChanges.push(change);
        });
    });

    describe('Component Creation', () => {
        it('should create', () => {
            const initialState: DeviceSelectorState = {
                device: null,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', initialState);
            spectator.setInput('devices', mockDevices);
            spectator.detectChanges();

            expect(spectator.component).toBeTruthy();
        });
    });

    describe('Custom Device Selection', () => {
        beforeEach(() => {
            const state: DeviceSelectorState = {
                device: DEFAULT_DEVICE,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', true);
            spectator.detectChanges();
        });

        it('should emit device change when selecting a custom device', () => {
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
                device: customDevice,
                socialMedia: null,
                orientation: null
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

        it('should show custom device name in more button label', () => {
            const customDevice = mockDevices.find((d) => !d._isDefault);
            const state: DeviceSelectorState = {
                device: customDevice,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            expect(spectator.component.$moreButtonLabel()).toBe(customDevice.name);
        });
    });

    describe('Social Media Selection', () => {
        beforeEach(() => {
            const state: DeviceSelectorState = {
                device: DEFAULT_DEVICE,
                socialMedia: null,
                orientation: null
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
                device: null,
                socialMedia: 'facebook',
                orientation: null
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
                device: null,
                socialMedia: 'facebook',
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.detectChanges();

            expect(spectator.component.$moreButtonLabel()).toBe('facebook');
        });
    });

    describe('Menu Items', () => {
        it('should include custom devices in menu', () => {
            const state: DeviceSelectorState = {
                device: DEFAULT_DEVICE,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', true);
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();

            expect(menuItems.length).toBeGreaterThan(0);
            expect(menuItems.some((item) => item.id === 'custom-devices')).toBeTruthy();
        });

        it('should include social media menu for traditional pages', () => {
            const state: DeviceSelectorState = {
                device: DEFAULT_DEVICE,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', true);
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();

            expect(menuItems.some((item) => item.id === 'social-media')).toBeTruthy();
            expect(menuItems.some((item) => item.id === 'search-engine')).toBeTruthy();
        });

        it('should NOT include social media menu for non-traditional pages', () => {
            const state: DeviceSelectorState = {
                device: DEFAULT_DEVICE,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.setInput('isTraditionalPage', false);
            spectator.detectChanges();

            const menuItems = spectator.component.$menuItems();

            expect(menuItems.some((item) => item.id === 'social-media')).toBeFalsy();
            expect(menuItems.some((item) => item.id === 'search-engine')).toBeFalsy();
        });
    });

    describe('More Button Active State', () => {
        it('should be active when custom device is selected', () => {
            const customDevice = mockDevices.find((d) => !d._isDefault);
            const state: DeviceSelectorState = {
                device: customDevice,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.detectChanges();

            expect(spectator.component.$isMoreButtonActive()).toBe(true);
        });

        it('should NOT be active when default device is selected', () => {
            const state: DeviceSelectorState = {
                device: DEFAULT_DEVICE,
                socialMedia: null,
                orientation: null
            };
            spectator.setInput('state', state);
            spectator.setInput('devices', mockDevices);
            spectator.detectChanges();

            expect(spectator.component.$isMoreButtonActive()).toBe(false);
        });
    });
});
