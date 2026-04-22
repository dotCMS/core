import { byTestId } from '@ngneat/spectator';
import { Spectator, createComponentFactory } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotUveDeviceControlsComponent } from './dot-uve-device-controls.component';

import { DEFAULT_DEVICE, DEFAULT_DEVICES } from '../../../shared/consts';
import { Orientation } from '../../../store/models';
import {
    DeviceSelectorChange,
    DeviceSelectorState
} from '../dot-uve-toolbar/components/dot-uve-device-selector/dot-uve-device-selector.models';

const TABLET = DEFAULT_DEVICES[1];
const MOBILE = DEFAULT_DEVICES[2];

const DEFAULT_STATE: DeviceSelectorState = {
    device: DEFAULT_DEVICE,
    socialMedia: null,
    orientation: Orientation.PORTRAIT
};

describe('DotUveDeviceControlsComponent', () => {
    let spectator: Spectator<DotUveDeviceControlsComponent>;
    let emittedChanges: DeviceSelectorChange[] = [];

    const createComponent = createComponentFactory({
        component: DotUveDeviceControlsComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: new MockDotMessageService({
                    'uve.device.selector.default': 'Desktop',
                    'uve.device.selector.tablet': 'Tablet',
                    'uve.device.selector.mobile': 'Mobile'
                })
            }
        ],
        detectChanges: false
    });

    beforeEach(() => {
        emittedChanges = [];
        spectator = createComponent();
        spectator.component.stateChange.subscribe((change) => emittedChanges.push(change));
    });

    describe('Component Creation', () => {
        it('should create', () => {
            spectator.setInput('state', DEFAULT_STATE);
            spectator.detectChanges();

            expect(spectator.component).toBeTruthy();
        });

        it.each(DEFAULT_DEVICES)('should render a button for $name ($inode)', (device) => {
            spectator.setInput('state', DEFAULT_STATE);
            spectator.detectChanges();

            expect(spectator.query(`[data-testid="${device.inode}"]`)).toBeTruthy();
        });

        it('should render the orientation button', () => {
            spectator.setInput('state', DEFAULT_STATE);
            spectator.detectChanges();

            expect(spectator.query(byTestId('orientation'))).toBeTruthy();
        });
    });

    describe('Device Selection', () => {
        beforeEach(() => {
            spectator.setInput('state', DEFAULT_STATE);
            spectator.detectChanges();
        });

        it('should emit device change when selecting a device', () => {
            spectator.component.onDeviceSelect(TABLET);

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({ type: 'device', device: TABLET });
        });

        it('should emit DEFAULT_DEVICE when selecting the already active device (toggle off)', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, device: TABLET });
            spectator.detectChanges();

            spectator.component.onDeviceSelect(TABLET);

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({ type: 'device', device: DEFAULT_DEVICE });
        });

        it('should emit new device when switching from one device to another', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, device: TABLET });
            spectator.detectChanges();

            spectator.component.onDeviceSelect(MOBILE);

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({ type: 'device', device: MOBILE });
        });
    });

    describe('Orientation Toggle', () => {
        it('should emit LANDSCAPE when current orientation is PORTRAIT', () => {
            spectator.setInput('state', {
                ...DEFAULT_STATE,
                device: TABLET,
                orientation: Orientation.PORTRAIT
            });
            spectator.detectChanges();

            spectator.component.onOrientationChange();

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'orientation',
                orientation: Orientation.LANDSCAPE
            });
        });

        it('should emit PORTRAIT when current orientation is LANDSCAPE', () => {
            spectator.setInput('state', {
                ...DEFAULT_STATE,
                device: TABLET,
                orientation: Orientation.LANDSCAPE
            });
            spectator.detectChanges();

            spectator.component.onOrientationChange();

            expect(emittedChanges).toHaveLength(1);
            expect(emittedChanges[0]).toEqual({
                type: 'orientation',
                orientation: Orientation.PORTRAIT
            });
        });
    });

    describe('Orientation Disabled State', () => {
        it('should disable orientation when the default device is selected', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, device: DEFAULT_DEVICE });
            spectator.detectChanges();

            expect(spectator.component.$disableOrientation()).toBe(true);
        });

        it('should disable orientation when a social media is active', () => {
            spectator.setInput('state', {
                ...DEFAULT_STATE,
                device: TABLET,
                socialMedia: 'facebook'
            });
            spectator.detectChanges();

            expect(spectator.component.$disableOrientation()).toBe(true);
        });

        it('should enable orientation when a non-default device is selected and no social media', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, device: TABLET, socialMedia: null });
            spectator.detectChanges();

            expect(spectator.component.$disableOrientation()).toBe(false);
        });
    });

    describe('Active Device State', () => {
        it('should reflect the current device from state', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, device: TABLET });
            spectator.detectChanges();

            expect(spectator.component.$currentDevice()).toEqual(TABLET);
        });

        it('should reflect null device when no device is selected', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, device: null });
            spectator.detectChanges();

            expect(spectator.component.$currentDevice()).toBeNull();
        });
    });

    describe('Current Orientation State', () => {
        it('should reflect LANDSCAPE orientation from state', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, orientation: Orientation.LANDSCAPE });
            spectator.detectChanges();

            expect(spectator.component.$currentOrientation()).toBe(Orientation.LANDSCAPE);
        });

        it('should reflect PORTRAIT orientation from state', () => {
            spectator.setInput('state', { ...DEFAULT_STATE, orientation: Orientation.PORTRAIT });
            spectator.detectChanges();

            expect(spectator.component.$currentOrientation()).toBe(Orientation.PORTRAIT);
        });
    });
});
