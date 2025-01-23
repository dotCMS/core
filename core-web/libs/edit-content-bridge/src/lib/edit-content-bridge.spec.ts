/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-unused-vars */
/* eslint-disable @typescript-eslint/no-empty-function */

import { DotFormBridge } from './edit-content-bridge';

// Mock Angular dependencies
const mockFormGroup = {
    get: jest.fn(),
    setValue: jest.fn()
};

const mockFormControl = {
    value: '',
    setValue: jest.fn(),
    markAsTouched: jest.fn(),
    updateValueAndValidity: jest.fn(),
    valueChanges: {
        subscribe: jest.fn((callback) => {
            // Store callback for later use
            mockFormControl.valueChanges._callback = callback;

            return {
                unsubscribe: jest.fn()
            };
        }),
        _callback: null as ((value: string) => void) | null
    }
};

const mockNgZone = {
    run: (fn: () => void) => fn()
};

describe('DotFormBridge', () => {
    let bridge: any;
    let iframe: HTMLIFrameElement;

    beforeEach(() => {
        iframe = document.createElement('iframe');
        mockFormGroup.get.mockReturnValue(mockFormControl);

        // Reset all mocks before each test
        jest.clearAllMocks();
    });

    describe('Angular Environment', () => {
        beforeEach(() => {
            bridge = new DotFormBridge({
                type: 'angular',
                form: mockFormGroup as any,
                iframe,
                zone: mockNgZone as any
            });
        });

        it('should create bridge API with Angular config', () => {
            const api = bridge.createPublicApi();
            expect(api.get).toBeDefined();
            expect(api.set).toBeDefined();
            expect(api.onChangeField).toBeDefined();
            expect(api.ready).toBeDefined();
        });

        it('should get field value from Angular form', () => {
            mockFormControl.value = 'test value';
            const api = bridge.createPublicApi();
            expect(api.get('testField')).toBe('test value');
            expect(mockFormGroup.get).toHaveBeenCalledWith('testField');
        });

        it('should set field value in Angular form', () => {
            const api = bridge.createPublicApi();
            api.set('testField', 'new value');
            expect(mockFormControl.setValue).toHaveBeenCalledWith('new value', {
                emitEvent: false
            });
            expect(mockFormControl.markAsTouched).toHaveBeenCalled();
            expect(mockFormControl.updateValueAndValidity).toHaveBeenCalledWith({
                emitEvent: false
            });
            expect(mockFormGroup.get).toHaveBeenCalledWith('testField');
        });

        it('should watch field changes in Angular form', () => {
            const api = bridge.createPublicApi();
            const callback = jest.fn();
            api.onChangeField('testField', callback);

            expect(mockFormGroup.get).toHaveBeenCalledWith('testField');
            expect(mockFormControl.valueChanges.subscribe).toHaveBeenCalled();

            // Get the first argument of the first call
            const [[mockSubscriber]] = mockFormControl.valueChanges.subscribe.mock.calls;
            if (mockFormControl.valueChanges._callback) {
                mockFormControl.valueChanges._callback('changed value');
                expect(callback).toHaveBeenCalledWith('changed value');
            }
        });
    });

    describe('Dojo Environment', () => {
        let inputElement: HTMLInputElement;

        beforeEach(() => {
            bridge = new DotFormBridge({ type: 'dojo' });
            inputElement = document.createElement('input');
            inputElement.id = 'testField';
            document.body.appendChild(inputElement);
        });

        afterEach(() => {
            document.body.removeChild(inputElement);
        });

        it('should create bridge API with Dojo config', () => {
            const api = bridge.createPublicApi();
            expect(api.get).toBeDefined();
            expect(api.set).toBeDefined();
            expect(api.onChangeField).toBeDefined();
            expect(api.ready).toBeDefined();
        });

        it('should get field value from Dojo input', () => {
            inputElement.value = 'test value';
            const api = bridge.createPublicApi();
            expect(api.get('testField')).toBe('test value');
        });

        it('should set field value in Dojo input', () => {
            const api = bridge.createPublicApi();
            api.set('testField', 'new value');
            expect(inputElement.value).toBe('new value');
        });

        it('should watch field changes in Dojo input', () => {
            const api = bridge.createPublicApi();
            const callback = jest.fn();
            api.onChangeField('testField', callback);

            inputElement.value = 'changed value';
            inputElement.dispatchEvent(new Event('change'));
            expect(callback).toHaveBeenCalledWith('changed value');
        });
    });

    describe('Cleanup', () => {
        it('should cleanup subscriptions on destroy in Angular environment', () => {
            const unsubscribeSpy = jest.fn();
            mockFormControl.valueChanges.subscribe.mockReturnValue({ unsubscribe: unsubscribeSpy });

            bridge = new DotFormBridge({
                type: 'angular',
                form: mockFormGroup as any,
                iframe,
                zone: mockNgZone as any
            });

            const api = bridge.createPublicApi();
            api.onChangeField('testField', () => {});
            bridge.destroy();

            expect(unsubscribeSpy).toHaveBeenCalled();
        });

        it('should cleanup event listeners on destroy in Dojo environment', () => {
            const inputElement = document.createElement('input');
            inputElement.id = 'testField';
            document.body.appendChild(inputElement);

            bridge = new DotFormBridge({ type: 'dojo' });
            const api = bridge.createPublicApi();
            const callback = jest.fn();
            api.onChangeField('testField', callback);
            bridge.destroy();

            inputElement.dispatchEvent(new Event('change'));
            expect(callback).not.toHaveBeenCalled();
            document.body.removeChild(inputElement);
        });
    });
});
