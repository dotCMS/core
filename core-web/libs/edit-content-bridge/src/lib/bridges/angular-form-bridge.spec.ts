/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/no-empty-function */

import { of, throwError } from 'rxjs';

import { DotSite } from '@dotcms/dotcms-models';
import { DotAssetPickerComponent } from '@dotcms/ui';

import { AngularFormBridge } from './angular-form-bridge';

import { DotBrowserOptions } from '../interfaces/asset-browser.interface';

/** The site the picker browses; the bridge is handed a way to resolve it. */
const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

// Mock Angular dependencies
const mockFormGroup = {
    get: jest.fn(),
    setValue: jest.fn(),
    events: {
        subscribe: jest.fn(() => {
            return { unsubscribe: jest.fn() };
        })
    }
};

const mockFormControl = {
    value: '',
    valid: true,
    invalid: false,
    touched: false,
    dirty: false,
    errors: null as Record<string, unknown> | null,
    setValue: jest.fn(),
    markAsTouched: jest.fn(),
    markAsDirty: jest.fn(),
    updateValueAndValidity: jest.fn(),
    enable: jest.fn(),
    disable: jest.fn(),
    valueChanges: {
        subscribe: jest.fn((callback) => {
            mockFormControl.valueChanges._callback = callback;

            return {
                unsubscribe: jest.fn()
            };
        }),
        _callback: null as ((value: string) => void) | null
    },
    events: {
        subscribe: jest.fn((callback) => {
            mockFormControl.events._callback = callback;

            return {
                unsubscribe: jest.fn()
            };
        }),
        _callback: null as ((event: unknown) => void) | null
    }
};

const mockNgZone = {
    run: (fn: () => void) => fn()
};

const mockDialogRef = {
    close: jest.fn(),
    onClose: {
        subscribe: jest.fn((callback) => {
            mockDialogRef.onClose._callback = callback;
            return {
                unsubscribe: jest.fn()
            };
        }),
        _callback: null as ((content: any) => void) | null
    }
};

const mockDialogService = {
    open: jest.fn().mockReturnValue(mockDialogRef)
};

describe('AngularFormBridge', () => {
    let bridge: AngularFormBridge;

    beforeEach(() => {
        // Reset singleton instance before each test
        AngularFormBridge.resetInstance();
        mockFormGroup.get.mockReturnValue(mockFormControl);
        mockFormControl.valueChanges._callback = null;
        mockFormControl.events._callback = null;
        mockDialogRef.onClose._callback = null;
        bridge = AngularFormBridge.getInstance(
            mockFormGroup as any,
            mockNgZone as any,
            mockDialogService as any
        );
        jest.clearAllMocks();
    });

    afterEach(() => {
        // Clean up singleton instance after each test
        AngularFormBridge.resetInstance();
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
            // Reset and create a fresh bridge instance with spy already set up
            AngularFormBridge.resetInstance();
            // Reset the callback to ensure clean state
            mockFormControl.valueChanges._callback = null;
            const zoneRunSpy = jest.spyOn(mockNgZone, 'run');
            const testBridge = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            const callback = jest.fn();

            testBridge.onChangeField('testField', callback);

            const valueChangesCallback = mockFormControl.valueChanges._callback as
                | ((value: string) => void)
                | null;
            expect(valueChangesCallback).toBeDefined();
            if (valueChangesCallback) {
                valueChangesCallback('changed value');
                expect(zoneRunSpy).toHaveBeenCalled();
            }

            zoneRunSpy.mockRestore();
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

    describe('Singleton pattern', () => {
        beforeEach(() => {
            AngularFormBridge.resetInstance();
        });

        afterEach(() => {
            AngularFormBridge.resetInstance();
        });

        it('should return the same instance when getInstance is called multiple times', () => {
            const instance1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            const instance2 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );

            expect(instance1).toBe(instance2);
        });

        it('should reset and return a new instance when getInstance is called with a different FormGroup', () => {
            const differentFormGroup = {
                get: jest.fn(),
                events: { subscribe: jest.fn(() => ({ unsubscribe: jest.fn() })) }
            } as any;

            const instance1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            const instance2 = AngularFormBridge.getInstance(
                differentFormGroup,
                mockNgZone as any,
                mockDialogService as any
            );

            // A fresh instance is created so it binds to the new FormGroup's controls
            // and validation state from the previous form (e.g. touched controls after
            // a Save) cannot leak across navigations.
            expect(instance1).not.toBe(instance2);
        });

        it('should call forceDestroy on the old instance when FormGroup changes (cleans up subscriptions)', () => {
            const unsubscribeSpy = jest.fn();
            mockFormControl.valueChanges.subscribe.mockReturnValue({ unsubscribe: unsubscribeSpy });

            const instance1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            instance1.onChangeField('testField', () => {});

            // Simulate form recreation with a new FormGroup
            const differentFormGroup = {
                get: jest.fn(),
                events: { subscribe: jest.fn(() => ({ unsubscribe: jest.fn() })) }
            } as any;
            AngularFormBridge.getInstance(
                differentFormGroup,
                mockNgZone as any,
                mockDialogService as any
            );

            // forceDestroy on the old instance must have unsubscribed all field subscriptions
            expect(unsubscribeSpy).toHaveBeenCalled();
        });

        it('should reset instance when resetInstance is called', () => {
            const instance1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            AngularFormBridge.resetInstance();
            const instance2 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );

            expect(instance1).not.toBe(instance2);
        });

        it('should destroy subscriptions when resetInstance is called', () => {
            const unsubscribeSpy = jest.fn();
            mockFormControl.valueChanges.subscribe.mockReturnValue({ unsubscribe: unsubscribeSpy });

            const instance = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            instance.onChangeField('testField', () => {});

            AngularFormBridge.resetInstance();

            expect(unsubscribeSpy).toHaveBeenCalled();
        });

        it('should not allow direct instantiation with new', () => {
            // TypeScript will prevent this at compile time, but we can verify the constructor is private
            // by checking that getInstance is the only way to create an instance
            const instance = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            expect(instance).toBeInstanceOf(AngularFormBridge);
        });

        it('should reset instance in destroy method when refCount reaches zero', () => {
            const instance1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            instance1.destroy();

            const instance2 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            expect(instance1).not.toBe(instance2);
        });

        it('should NOT destroy singleton when other consumers still hold a reference', () => {
            const instance1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            const instance2 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );

            expect(instance1).toBe(instance2);

            // First consumer releases — singleton must survive
            instance1.destroy();

            const instance3 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            expect(instance3).toBe(instance1);
        });

        it('should destroy singleton only after all consumers release', () => {
            const instance1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            // Second consumer
            AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );

            instance1.destroy(); // refCount 2 → 1
            instance1.destroy(); // refCount 1 → 0, now truly destroyed

            const fresh = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            expect(fresh).not.toBe(instance1);
        });

        it('should keep subscriptions alive when one of two consumers calls destroy', () => {
            const unsubscribeSpy = jest.fn();
            mockFormControl.valueChanges.subscribe.mockReturnValue({ unsubscribe: unsubscribeSpy });

            // Two consumers
            const bridge1 = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );
            AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any
            );

            bridge1.onChangeField('testField', () => {});
            bridge1.destroy(); // one consumer releases, but refCount still > 0

            expect(unsubscribeSpy).not.toHaveBeenCalled();
        });
    });

    describe('getField', () => {
        it('should return FormFieldAPI object with all methods', () => {
            const fieldAPI = bridge.getField('testField');
            expect(fieldAPI).toBeDefined();
            expect(typeof fieldAPI.getValue).toBe('function');
            expect(typeof fieldAPI.setValue).toBe('function');
            expect(typeof fieldAPI.onChange).toBe('function');
            expect(typeof fieldAPI.getValidationState).toBe('function');
            expect(typeof fieldAPI.onValidationChange).toBe('function');
            expect(typeof fieldAPI.enable).toBe('function');
            expect(typeof fieldAPI.disable).toBe('function');
            expect(typeof fieldAPI.show).toBe('function');
            expect(typeof fieldAPI.hide).toBe('function');
        });

        it('should get value using getValue', () => {
            mockFormControl.value = 'test value';
            const fieldAPI = bridge.getField('testField');
            const value = fieldAPI.getValue();
            expect(value).toBe('test value');
            expect(mockFormGroup.get).toHaveBeenCalledWith('testField');
        });

        it('should set value using setValue', () => {
            const fieldAPI = bridge.getField('testField');
            fieldAPI.setValue('new value');
            expect(mockFormControl.setValue).toHaveBeenCalledWith('new value', {
                emitEvent: true
            });
            expect(mockFormControl.markAsTouched).toHaveBeenCalled();
            expect(mockFormControl.markAsDirty).toHaveBeenCalled();
        });

        it('should subscribe to changes using onChange', () => {
            const callback = jest.fn();
            const fieldAPI = bridge.getField('testField');
            const unsubscribe = fieldAPI.onChange(callback);

            expect(mockFormControl.valueChanges.subscribe).toHaveBeenCalled();
            expect(typeof unsubscribe).toBe('function');

            if (mockFormControl.valueChanges._callback) {
                mockFormControl.valueChanges._callback('changed value');
                expect(callback).toHaveBeenCalledWith('changed value');
            }
        });

        describe('validation state', () => {
            beforeEach(() => {
                mockFormControl.valid = true;
                mockFormControl.invalid = false;
                mockFormControl.touched = false;
                mockFormControl.dirty = false;
                mockFormControl.errors = null;
                mockFormControl.events._callback = null;
            });

            it('should return current validation snapshot via getValidationState', () => {
                mockFormControl.valid = false;
                mockFormControl.invalid = true;
                mockFormControl.touched = true;
                mockFormControl.dirty = true;
                mockFormControl.errors = { required: true };

                const state = bridge.getField('testField').getValidationState();

                expect(state).toEqual({
                    valid: false,
                    invalid: true,
                    touched: true,
                    dirty: true,
                    errors: { required: true }
                });
            });

            it('should return a neutral validation state when control is missing', () => {
                mockFormGroup.get.mockReturnValue(null);

                const state = bridge.getField('missingField').getValidationState();

                // Matches DojoFormBridge so VTL templates that read `state.valid`
                // get the same answer in both editors when the control is unknown.
                expect(state).toEqual({
                    valid: true,
                    invalid: false,
                    touched: false,
                    dirty: false,
                    errors: null
                });
            });

            it('should fire onValidationChange callback with initial state and on control events', () => {
                const callback = jest.fn();
                const fieldAPI = bridge.getField('testField');
                const unsubscribe = fieldAPI.onValidationChange(callback);

                expect(mockFormControl.events.subscribe).toHaveBeenCalled();
                expect(typeof unsubscribe).toBe('function');

                // Initial emit when the control is found on subscribe
                expect(callback).toHaveBeenCalledTimes(1);
                expect(callback).toHaveBeenLastCalledWith({
                    valid: true,
                    invalid: false,
                    touched: false,
                    dirty: false,
                    errors: null
                });

                mockFormControl.invalid = true;
                mockFormControl.touched = true;
                mockFormControl.errors = { required: true };
                mockFormControl.events._callback?.({});

                expect(callback).toHaveBeenLastCalledWith({
                    valid: true,
                    invalid: true,
                    touched: true,
                    dirty: false,
                    errors: { required: true }
                });
            });

            it('should re-attach to the control when it is registered after onValidationChange is called', () => {
                // Simulate the race condition: the control is not yet in the form when subscribe runs.
                mockFormGroup.get.mockReturnValueOnce(null);
                const formSubscribe = mockFormGroup.events.subscribe as jest.Mock;
                let reconcileOnFormEvent: (() => void) | null = null;
                formSubscribe.mockImplementationOnce((cb: () => void) => {
                    reconcileOnFormEvent = cb;
                    return { unsubscribe: jest.fn() };
                });

                const callback = jest.fn();
                bridge.getField('testField').onValidationChange(callback);

                // No initial emit because the control was missing.
                expect(callback).not.toHaveBeenCalled();

                // The control now appears in the form (e.g. after the FormGroup registers it).
                mockFormGroup.get.mockReturnValue(mockFormControl);
                reconcileOnFormEvent?.();

                // Initial state of the now-registered control is emitted exactly once.
                expect(callback).toHaveBeenCalledTimes(1);
                expect(callback).toHaveBeenLastCalledWith({
                    valid: true,
                    invalid: false,
                    touched: false,
                    dirty: false,
                    errors: null
                });
            });

            it('should not emit when control is missing on subscribe', () => {
                mockFormGroup.get.mockReturnValue(null);
                const callback = jest.fn();
                const unsubscribe = bridge.getField('missingField').onValidationChange(callback);

                expect(callback).not.toHaveBeenCalled();
                expect(typeof unsubscribe).toBe('function');
                expect(() => unsubscribe()).not.toThrow();
            });

            it('should unsubscribe from events when the returned function is called', () => {
                const controlUnsubscribeSpy = jest.fn();
                const formUnsubscribeSpy = jest.fn();
                mockFormControl.events.subscribe.mockReturnValueOnce({
                    unsubscribe: controlUnsubscribeSpy
                });
                (mockFormGroup.events.subscribe as jest.Mock).mockReturnValueOnce({
                    unsubscribe: formUnsubscribeSpy
                });

                const unsubscribe = bridge.getField('testField').onValidationChange(jest.fn());
                unsubscribe();

                expect(controlUnsubscribeSpy).toHaveBeenCalled();
                expect(formUnsubscribeSpy).toHaveBeenCalled();
            });
        });

        it('should enable field using enable', () => {
            const fieldAPI = bridge.getField('testField');
            fieldAPI.enable();
            expect(mockFormControl.enable).toHaveBeenCalledWith({ emitEvent: true });
        });

        it('should disable field using disable', () => {
            const fieldAPI = bridge.getField('testField');
            fieldAPI.disable();
            expect(mockFormControl.disable).toHaveBeenCalledWith({ emitEvent: true });
        });

        it('should not enable field if control is not found', () => {
            mockFormGroup.get.mockReturnValue(null);
            const fieldAPI = bridge.getField('nonExistentField');
            fieldAPI.enable();
            expect(mockFormControl.enable).not.toHaveBeenCalled();
        });

        it('should not disable field if control is not found', () => {
            mockFormGroup.get.mockReturnValue(null);
            const fieldAPI = bridge.getField('nonExistentField');
            fieldAPI.disable();
            expect(mockFormControl.disable).not.toHaveBeenCalled();
        });

        it('should run enable inside NgZone', () => {
            const zoneRunSpy = jest.spyOn(mockNgZone, 'run');
            const fieldAPI = bridge.getField('testField');
            fieldAPI.enable();
            expect(zoneRunSpy).toHaveBeenCalled();
        });

        it('should run disable inside NgZone', () => {
            const zoneRunSpy = jest.spyOn(mockNgZone, 'run');
            const fieldAPI = bridge.getField('testField');
            fieldAPI.disable();
            expect(zoneRunSpy).toHaveBeenCalled();
        });

        describe('show/hide', () => {
            beforeEach(() => {
                AngularFormBridge.resetInstance();
                bridge = AngularFormBridge.getInstance(
                    mockFormGroup as any,
                    mockNgZone as any,
                    mockDialogService as any
                );
            });

            it('should call onFieldVisibilityChange with true when show is called', () => {
                const onFieldVisibilityChange = jest.fn();
                AngularFormBridge.resetInstance();
                const bridgeWithCallback = AngularFormBridge.getInstance(
                    mockFormGroup as any,
                    mockNgZone as any,
                    mockDialogService as any,
                    onFieldVisibilityChange
                );

                const fieldAPI = bridgeWithCallback.getField('myField');
                fieldAPI.show();

                expect(onFieldVisibilityChange).toHaveBeenCalledWith('myField', true);
            });

            it('should call onFieldVisibilityChange with false when hide is called', () => {
                const onFieldVisibilityChange = jest.fn();
                AngularFormBridge.resetInstance();
                const bridgeWithCallback = AngularFormBridge.getInstance(
                    mockFormGroup as any,
                    mockNgZone as any,
                    mockDialogService as any,
                    onFieldVisibilityChange
                );

                const fieldAPI = bridgeWithCallback.getField('myField');
                fieldAPI.hide();

                expect(onFieldVisibilityChange).toHaveBeenCalledWith('myField', false);
            });

            it('should not throw when show is called without callback', () => {
                const fieldAPI = bridge.getField('testField');
                expect(() => fieldAPI.show()).not.toThrow();
            });

            it('should not throw when hide is called without callback', () => {
                const fieldAPI = bridge.getField('testField');
                expect(() => fieldAPI.hide()).not.toThrow();
            });

            it('should warn once when show is called without callback', () => {
                const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
                const fieldAPI = bridge.getField('testField');

                fieldAPI.show();
                expect(consoleSpy).toHaveBeenCalledWith(
                    expect.stringContaining(
                        "show() called on field 'testField' but no onFieldVisibilityChange callback is configured"
                    )
                );

                consoleSpy.mockClear();
                fieldAPI.show();
                expect(consoleSpy).not.toHaveBeenCalled();

                consoleSpy.mockRestore();
            });

            it('should warn once when hide is called without callback', () => {
                const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
                const fieldAPI = bridge.getField('testField');

                fieldAPI.hide();
                expect(consoleSpy).toHaveBeenCalledWith(
                    expect.stringContaining(
                        "hide() called on field 'testField' but no onFieldVisibilityChange callback is configured"
                    )
                );

                consoleSpy.mockClear();
                fieldAPI.hide();
                expect(consoleSpy).not.toHaveBeenCalled();

                consoleSpy.mockRestore();
            });

            it('should not warn when callback is provided', () => {
                const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
                const onFieldVisibilityChange = jest.fn();
                AngularFormBridge.resetInstance();
                const bridgeWithCallback = AngularFormBridge.getInstance(
                    mockFormGroup as any,
                    mockNgZone as any,
                    mockDialogService as any,
                    onFieldVisibilityChange
                );

                const fieldAPI = bridgeWithCallback.getField('testField');
                fieldAPI.show();
                fieldAPI.hide();

                expect(consoleSpy).not.toHaveBeenCalled();
                consoleSpy.mockRestore();
            });

            it('should run show inside NgZone', () => {
                const onFieldVisibilityChange = jest.fn();
                AngularFormBridge.resetInstance();
                const bridgeWithCallback = AngularFormBridge.getInstance(
                    mockFormGroup as any,
                    mockNgZone as any,
                    mockDialogService as any,
                    onFieldVisibilityChange
                );
                const zoneRunSpy = jest.spyOn(mockNgZone, 'run');

                const fieldAPI = bridgeWithCallback.getField('testField');
                fieldAPI.show();

                expect(zoneRunSpy).toHaveBeenCalled();
            });

            it('should run hide inside NgZone', () => {
                const onFieldVisibilityChange = jest.fn();
                AngularFormBridge.resetInstance();
                const bridgeWithCallback = AngularFormBridge.getInstance(
                    mockFormGroup as any,
                    mockNgZone as any,
                    mockDialogService as any,
                    onFieldVisibilityChange
                );
                const zoneRunSpy = jest.spyOn(mockNgZone, 'run');

                const fieldAPI = bridgeWithCallback.getField('testField');
                fieldAPI.hide();

                expect(zoneRunSpy).toHaveBeenCalled();
            });
        });
    });

    describe('ready', () => {
        it('should execute callback with bridge instance', () => {
            const callback = jest.fn();
            bridge.ready(callback);
            expect(callback).toHaveBeenCalledWith(bridge);
        });
    });

    describe('openBrowserModal', () => {
        let resolveSite: jest.Mock;

        /** A file asset row, as the picker hands one back. */
        const FILE_ITEM = {
            identifier: 'asset-id',
            inode: 'asset-inode',
            title: 'Logo',
            fileName: 'logo.png',
            url: '/images/logo.png',
            mimeType: 'image/png',
            baseType: 'FILEASSET',
            contentType: 'FileAsset'
        };

        beforeEach(() => {
            jest.clearAllMocks();
            window.localStorage.clear();
            AngularFormBridge.resetInstance();
            resolveSite = jest.fn(() => of(SITE));
            bridge = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any,
                undefined,
                resolveSite as any
            );
        });

        /** The picker config the bridge handed the dialog. */
        const openedConfig = () => mockDialogService.open.mock.calls[0][1].data;

        describe('opening', () => {
            it('should open the new AssetPicker', () => {
                bridge.openBrowserModal({ kinds: ['file'] });

                expect(mockDialogService.open).toHaveBeenCalledWith(
                    DotAssetPickerComponent,
                    expect.anything()
                );
            });

            it('should browse the resolved site', () => {
                // The picker cannot browse without one, which is why the bridge has to be given a
                // way to resolve it.
                bridge.openBrowserModal({ kinds: ['file'] });

                expect(resolveSite).toHaveBeenCalled();
                expect(openedConfig().site).toBe(SITE);
            });

            it('should show the caller-supplied title', () => {
                // The picker renders its own header, so the title travels in the config rather
                // than in `DynamicDialogConfig.header`.
                bridge.openBrowserModal({ title: 'Select a Page', kinds: ['page'] });

                expect(openedConfig().title).toBe('Select a Page');
            });

            it('should run inside NgZone', () => {
                // Callers are VTL scripts running outside Angular, so without this the dialog
                // opens with no change detection behind it.
                const zoneRunSpy = jest.spyOn(mockNgZone, 'run');

                bridge.openBrowserModal({ kinds: ['file'] });

                expect(zoneRunSpy).toHaveBeenCalled();
            });

            it('should default to asset-only browsing when given no options', () => {
                bridge.openBrowserModal();

                expect(openedConfig().allowedBaseTypes).toEqual(['DOTASSET', 'FILEASSET']);
                expect(openedConfig().browse?.showLinks).toBeFalsy();
            });
        });

        describe('option mapping', () => {
            it('should map kinds to the offered base types', () => {
                bridge.openBrowserModal({ kinds: ['file', 'page'] });

                expect(openedConfig().allowedBaseTypes).toEqual(['FILEASSET', 'HTMLPAGE']);
            });

            it('should map the link kind to a browse option', () => {
                bridge.openBrowserModal({ kinds: ['page', 'link'] });

                expect(openedConfig().browse).toEqual(
                    expect.objectContaining({ showLinks: true })
                );
            });

            it('should not carry a folder browse option for a caller that asks for folders', () => {
                // #37366: `'folder'` left the contract, but a VTL template is a string literal —
                // TypeScript polices nothing here, so the runtime has to. The kind is dropped, and
                // the picker is never handed an option that would list folders.
                bridge.openBrowserModal({
                    kinds: ['page', 'folder', 'link']
                } as unknown as DotBrowserOptions);

                expect(openedConfig().browse).not.toHaveProperty('showFolders');
                expect(openedConfig().browse).toEqual(
                    expect.objectContaining({ showLinks: true })
                );
            });

            it('should warn about an unsupported kind rather than ignore it silently', () => {
                // AC-008: a template author must not be able to ask for a kind the picker refuses
                // and get no signal. Same treatment the `link` + `mimeTypes` conflict already gets.
                const warn = jest.spyOn(console, 'warn').mockImplementation();

                bridge.openBrowserModal({
                    kinds: ['file', 'page', 'folder']
                } as unknown as DotBrowserOptions);

                expect(warn).toHaveBeenCalledTimes(1);
                expect(warn.mock.calls[0][0]).toContain('folder');
                expect(openedConfig().allowedBaseTypes).toEqual(['FILEASSET', 'HTMLPAGE']);

                warn.mockRestore();
            });

            it('should fall back to asset-only browsing when folder is the only kind asked for', () => {
                // Degenerate case: no requested kind maps to a base type, so `baseTypesFor` returns
                // undefined and the picker uses its own default. Must not throw — an exception
                // inside a VTL <script> takes the whole custom field down with it.
                const warn = jest.spyOn(console, 'warn').mockImplementation();

                expect(() =>
                    bridge.openBrowserModal({
                        kinds: ['folder']
                    } as unknown as DotBrowserOptions)
                ).not.toThrow();

                expect(openedConfig().allowedBaseTypes).toEqual(['DOTASSET', 'FILEASSET']);
                expect(warn).toHaveBeenCalledTimes(1);

                warn.mockRestore();
            });

            it('should not warn for a caller whose kinds are all supported', () => {
                const warn = jest.spyOn(console, 'warn').mockImplementation();

                bridge.openBrowserModal({ kinds: ['file', 'dotasset', 'page', 'link'] });

                expect(warn).not.toHaveBeenCalled();

                warn.mockRestore();
            });

            it.each([
                ['live', { showWorking: false, showArchived: false }],
                ['working', { showWorking: true, showArchived: false }],
                ['archived', { showArchived: true }]
            ] as const)('should map status %s', (status, expected) => {
                bridge.openBrowserModal({ status });

                expect(openedConfig().browse).toEqual(expect.objectContaining(expected));
            });

            it('should map sort direction', () => {
                bridge.openBrowserModal({ sort: { field: 'modDate', direction: 'desc' } });

                expect(openedConfig().browse).toEqual(
                    expect.objectContaining({ sortByDesc: true })
                );
            });

            it('should map the sort field, not just the direction', () => {
                // Reported in review of #37273: only `direction` was read, so a caller asking for
                // `title` silently got the picker's default `modDate`. Both shipped templates pass
                // `modDate`, which is the default, so the drop was invisible.
                bridge.openBrowserModal({ sort: { field: 'title', direction: 'asc' } });

                expect(openedConfig().browse).toEqual(
                    expect.objectContaining({ sortField: 'title', sortByDesc: false })
                );
            });

            it('should pass mimeTypes and path straight through', () => {
                // No `extensions` here on purpose: the browse endpoint has no such parameter yet,
                // so the option is not exposed rather than accepted and ignored.
                bridge.openBrowserModal({ mimeTypes: ['image/*'], path: '/images/' });

                expect(openedConfig().mimeTypes).toEqual(['image/*']);
                expect(openedConfig().path).toBe('/images/');
            });

            it('should warn when links are asked for together with mimeTypes', () => {
                // The endpoint drops links whenever a mimetype filter is set. Surfaced, not worked
                // around — silently returning fewer kinds than requested is the worse outcome.
                const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

                bridge.openBrowserModal({ kinds: ['link'], mimeTypes: ['image/*'] });

                expect(warn).toHaveBeenCalled();
                warn.mockRestore();
            });
        });

        describe('result', () => {
            /** Hands the picker's close payload back through the dialog ref. */
            const closeWith = (payload: any) => mockDialogRef.onClose._callback?.(payload);

            it('should report a file selection', () => {
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['file'], onClose });
                closeWith(FILE_ITEM);

                expect(onClose).toHaveBeenCalledWith({
                    kind: 'file',
                    identifier: 'asset-id',
                    inode: 'asset-inode',
                    title: 'Logo',
                    name: 'logo.png',
                    url: '/images/logo.png',
                    mimeType: 'image/png',
                    baseType: 'FILEASSET',
                    contentType: 'FileAsset'
                });
            });

            it('should report a dotAsset as its own kind', () => {
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['dotasset'], onClose });
                closeWith({ ...FILE_ITEM, baseType: 'DOTASSET' });

                expect(onClose).toHaveBeenCalledWith(expect.objectContaining({ kind: 'dotasset' }));
            });

            it('should report a page selection', () => {
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['page'], onClose });
                closeWith({
                    identifier: 'page-id',
                    inode: 'page-inode',
                    title: 'Home',
                    url: '/index',
                    baseType: 'HTMLPAGE',
                    contentType: 'htmlpageasset'
                });

                expect(onClose).toHaveBeenCalledWith(
                    expect.objectContaining({ kind: 'page', url: '/index' })
                );
            });

            it('should fall back to urlMap when there is no url', () => {
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['file'], onClose });
                closeWith({ ...FILE_ITEM, url: undefined, urlMap: '/mapped/url' });

                expect(onClose).toHaveBeenCalledWith(
                    expect.objectContaining({ url: '/mapped/url' })
                );
            });

            // Removed in #37366: "should report a folder with its path as the url". A folder can no
            // longer be picked, so there is no selection to report. The path a custom field stores
            // for a folder is now typed into the field, not returned by the picker.

            it('should report a menu link with its target as the url', () => {
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['link'], onClose });
                closeWith({
                    type: 'link',
                    extension: 'link',
                    identifier: 'link-1',
                    inode: 'link-inode',
                    title: 'Docs',
                    url: '/docs'
                });

                expect(onClose).toHaveBeenCalledWith({
                    kind: 'link',
                    identifier: 'link-1',
                    inode: 'link-inode',
                    title: 'Docs',
                    url: '/docs'
                });
            });

            it.each([
                // The `folder` case went with #37366 — a folder is no longer a selectable kind, so
                // there is no folder selection whose shape could be wrong.
                ['link', { extension: 'link', identifier: 'l', inode: 'li', title: 'l', url: '/l' }]
            ])('should not attach contentlet-only fields to a %s', (_kind, item) => {
                // The whole point of the discriminated union: a consumer can never read a mimetype
                // off something that has none.
                const onClose = jest.fn();
                bridge.openBrowserModal({ onClose });
                closeWith(item);

                const selection = onClose.mock.calls[0][0];

                expect(selection).not.toHaveProperty('mimeType');
                expect(selection).not.toHaveProperty('contentType');
                expect(selection.url).not.toBe('');
            });

            it('should fall back to fileName when there is no name', () => {
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['file'], onClose });
                closeWith({ ...FILE_ITEM, name: undefined });

                expect(onClose).toHaveBeenCalledWith(expect.objectContaining({ name: 'logo.png' }));
            });
        });

        describe('cancelling', () => {
            const closeWith = (payload: any) => mockDialogRef.onClose._callback?.(payload);

            it('should report null when the picker is dismissed', () => {
                // ✕, Esc and mask click all arrive as the same empty close from PrimeNG.
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['file'], onClose });
                closeWith(undefined);

                expect(onClose).toHaveBeenCalledWith(null);
            });

            it('should call onClose exactly once even if the dialog closes twice', () => {
                const onClose = jest.fn();
                bridge.openBrowserModal({ kinds: ['file'], onClose });
                closeWith(null);
                closeWith(FILE_ITEM);

                // The first outcome wins; a late second emission cannot overwrite it.
                expect(onClose).toHaveBeenCalledTimes(1);
                expect(onClose).toHaveBeenCalledWith(null);
            });

            it('should close the dialog when close() is called', () => {
                const onClose = jest.fn();
                const controller = bridge.openBrowserModal({ kinds: ['file'], onClose });

                controller.close();

                expect(mockDialogRef.close).toHaveBeenCalled();
                expect(onClose).toHaveBeenCalledWith(null);
            });

            it('should not call onClose a second time after close()', () => {
                // PrimeNG still emits its own close afterwards; the caller must not see two.
                const onClose = jest.fn();
                const controller = bridge.openBrowserModal({ kinds: ['file'], onClose });

                controller.close();
                closeWith(undefined);

                expect(onClose).toHaveBeenCalledTimes(1);
            });

            it('should report null when no site can be resolved', () => {
                // Opening a picker that cannot browse anything is worse than not opening it.
                resolveSite.mockReturnValue(of(null));
                const onClose = jest.fn();

                bridge.openBrowserModal({ kinds: ['file'], onClose });

                expect(mockDialogService.open).not.toHaveBeenCalled();
                expect(onClose).toHaveBeenCalledWith(null);
            });

            it('should report null when the site lookup fails', () => {
                resolveSite.mockReturnValue(throwError(() => new Error('offline')));
                const onClose = jest.fn();

                bridge.openBrowserModal({ kinds: ['file'], onClose });

                expect(mockDialogService.open).not.toHaveBeenCalled();
                expect(onClose).toHaveBeenCalledWith(null);
            });

            it('should report null when the host gave the bridge no way to resolve a site', () => {
                // A host that never wired one up cannot browse. Saying so beats opening an empty
                // picker.
                AngularFormBridge.resetInstance();
                const bridgeWithoutSite = AngularFormBridge.getInstance(
                    mockFormGroup as any,
                    mockNgZone as any,
                    mockDialogService as any
                );
                const onClose = jest.fn();

                bridgeWithoutSite.openBrowserModal({ kinds: ['file'], onClose });

                expect(mockDialogService.open).not.toHaveBeenCalled();
                expect(onClose).toHaveBeenCalledWith(null);
            });
        });
    });

    describe('destroy with dialog cleanup', () => {
        it('should close dialog when destroyed', () => {
            AngularFormBridge.resetInstance();
            const bridgeWithSite = AngularFormBridge.getInstance(
                mockFormGroup as any,
                mockNgZone as any,
                mockDialogService as any,
                undefined,
                (() => of(SITE)) as any
            );
            bridgeWithSite.openBrowserModal({ kinds: ['file'] });

            bridgeWithSite.destroy();

            expect(mockDialogRef.close).toHaveBeenCalled();
        });

        it('should not throw if no dialog is open when destroyed', () => {
            expect(() => bridge.destroy()).not.toThrow();
        });
    });
});
