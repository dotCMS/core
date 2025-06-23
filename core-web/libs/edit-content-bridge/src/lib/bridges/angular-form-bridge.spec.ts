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

    it('should not set value if control is not found', () => {
        mockFormGroup.get.mockReturnValue(null);
        bridge.set('nonExistentField', 'new value');
        expect(mockFormControl.setValue).not.toHaveBeenCalled();
    });

    it('should not set value if new value is same as current', () => {
        mockFormControl.value = 'same value';
        bridge.set('testField', 'same value');
        expect(mockFormControl.setValue).not.toHaveBeenCalled();
    });

    it('should run set value inside NgZone', () => {
        const zoneRunSpy = jest.spyOn(mockNgZone, 'run');
        bridge.set('testField', 'new value');
        expect(zoneRunSpy).toHaveBeenCalled();
    });

    describe('field changes', () => {
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

        it('should support multiple callbacks for the same field', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            bridge.onChangeField('testField', callback1);
            bridge.onChangeField('testField', callback2);

            if (mockFormControl.valueChanges._callback) {
                mockFormControl.valueChanges._callback('changed value');
                expect(callback1).toHaveBeenCalledWith('changed value');
                expect(callback2).toHaveBeenCalledWith('changed value');
            }
        });

        it('should remove only the specified callback when unsubscribing', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            const unsubscribe1 = bridge.onChangeField('testField', callback1);
            bridge.onChangeField('testField', callback2);

            unsubscribe1();

            if (mockFormControl.valueChanges._callback) {
                mockFormControl.valueChanges._callback('changed value');
                expect(callback1).not.toHaveBeenCalled();
                expect(callback2).toHaveBeenCalledWith('changed value');
            }
        });

        it('should cleanup subscription when last callback is removed', () => {
            const unsubscribeSpy = jest.fn();
            mockFormControl.valueChanges.subscribe.mockReturnValue({ unsubscribe: unsubscribeSpy });

            const unsubscribe = bridge.onChangeField('testField', () => {});
            unsubscribe();

            expect(unsubscribeSpy).toHaveBeenCalled();
        });

        it('should return no-op function when field not found', () => {
            mockFormGroup.get.mockReturnValue(null);
            const unsubscribe = bridge.onChangeField('nonExistentField', () => {});
            expect(typeof unsubscribe).toBe('function');
            unsubscribe(); // Should not throw
        });

        it('should log warning when field not found', () => {
            const consoleSpy = jest.spyOn(console, 'warn');
            mockFormGroup.get.mockReturnValue(null);

            bridge.onChangeField('nonExistentField', () => {});

            expect(consoleSpy).toHaveBeenCalledWith("Field 'nonExistentField' not found in form");
        });

        it('should maintain separate callback IDs for different fields', () => {
            const unsubscribe1 = bridge.onChangeField('field1', () => {});
            const unsubscribe2 = bridge.onChangeField('field2', () => {});

            expect(unsubscribe1).not.toBe(unsubscribe2);
        });

        it('should maintain separate callback IDs for same field', () => {
            const unsubscribe1 = bridge.onChangeField('testField', () => {});
            const unsubscribe2 = bridge.onChangeField('testField', () => {});

            expect(unsubscribe1).not.toBe(unsubscribe2);
        });

        it('should run callbacks inside NgZone', () => {
            const zoneRunSpy = jest.spyOn(mockNgZone, 'run');
            const callback = jest.fn();

            bridge.onChangeField('testField', callback);

            if (mockFormControl.valueChanges._callback) {
                mockFormControl.valueChanges._callback('changed value');
                expect(zoneRunSpy).toHaveBeenCalled();
            }
        });
    });

    describe('cleanup', () => {
        it('should cleanup subscriptions on destroy', () => {
            const unsubscribeSpy = jest.fn();
            mockFormControl.valueChanges.subscribe.mockReturnValue({ unsubscribe: unsubscribeSpy });

            bridge.onChangeField('testField', () => {});
            bridge.destroy();

            expect(unsubscribeSpy).toHaveBeenCalled();
        });

        it('should cleanup all subscriptions on destroy', () => {
            const unsubscribeSpy1 = jest.fn();
            const unsubscribeSpy2 = jest.fn();

            mockFormControl.valueChanges.subscribe
                .mockReturnValueOnce({ unsubscribe: unsubscribeSpy1 })
                .mockReturnValueOnce({ unsubscribe: unsubscribeSpy2 });

            bridge.onChangeField('field1', () => {});
            bridge.onChangeField('field2', () => {});

            bridge.destroy();

            expect(unsubscribeSpy1).toHaveBeenCalled();
            expect(unsubscribeSpy2).toHaveBeenCalled();
        });

        it('should clear fieldSubscriptions map after destroy', () => {
            bridge.onChangeField('testField', () => {});
            bridge.destroy();

            // Try to add a new subscription after destroy
            const unsubscribe = bridge.onChangeField('testField', () => {});
            expect(typeof unsubscribe).toBe('function');
        });
    });
});
