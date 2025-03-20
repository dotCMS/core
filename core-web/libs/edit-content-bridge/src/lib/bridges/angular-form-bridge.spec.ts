/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-empty-function */

import { AngularFormBridge } from './angular-form-bridge';

// Mock Angular dependencies
const mockFormGroup = {
    get: jest.fn(),
    setValue: jest.fn()
};

const mockFormControl = {
    value: '',
    setValue: jest.fn(),
    markAsTouched: jest.fn(),
    markAsDirty: jest.fn(),
    updateValueAndValidity: jest.fn(),
    valueChanges: {
        subscribe: jest.fn((callback) => {
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

describe('AngularFormBridge', () => {
    let bridge: AngularFormBridge;

    beforeEach(() => {
        mockFormGroup.get.mockReturnValue(mockFormControl);
        bridge = new AngularFormBridge(mockFormGroup as any, mockNgZone as any);
        jest.clearAllMocks();
    });

    it('should get field value from Angular form', () => {
        mockFormControl.value = 'test value';
        expect(bridge.get('testField')).toBe('test value');
        expect(mockFormGroup.get).toHaveBeenCalledWith('testField');
    });

    it('should set field value in Angular form', () => {
        bridge.set('testField', 'new value');
        expect(mockFormControl.setValue).toHaveBeenCalledWith('new value', {
            emitEvent: true
        });
        expect(mockFormControl.markAsTouched).toHaveBeenCalled();
        expect(mockFormControl.markAsDirty).toHaveBeenCalled();
        expect(mockFormControl.updateValueAndValidity).toHaveBeenCalledWith({
            emitEvent: true
        });
    });

    it('should watch field changes in Angular form', () => {
        const callback = jest.fn();
        bridge.onChangeField('testField', callback);

        expect(mockFormGroup.get).toHaveBeenCalledWith('testField');
        expect(mockFormControl.valueChanges.subscribe).toHaveBeenCalled();

        if (mockFormControl.valueChanges._callback) {
            mockFormControl.valueChanges._callback('changed value');
            expect(callback).toHaveBeenCalledWith('changed value');
        }
    });

    it('should cleanup subscriptions on destroy', () => {
        const unsubscribeSpy = jest.fn();
        mockFormControl.valueChanges.subscribe.mockReturnValue({ unsubscribe: unsubscribeSpy });

        bridge.onChangeField('testField', () => {});
        bridge.destroy();

        expect(unsubscribeSpy).toHaveBeenCalled();
    });
});
