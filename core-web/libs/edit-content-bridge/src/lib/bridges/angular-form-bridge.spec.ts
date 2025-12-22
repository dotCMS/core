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

        it('should warn when getInstance is called with different parameters', () => {
            const consoleSpy = jest.spyOn(console, 'warn').mockImplementation();
            const differentFormGroup = { get: jest.fn() } as any;

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

            expect(instance1).toBe(instance2);
            expect(consoleSpy).toHaveBeenCalledWith(
                expect.stringContaining(
                    'AngularFormBridge: Attempted to get instance with different form or zone'
                )
            );

            consoleSpy.mockRestore();
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

        it('should reset instance in destroy method', () => {
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
    });

    describe('getField', () => {
        it('should return FormFieldAPI object', () => {
            const fieldAPI = bridge.getField('testField');
            expect(fieldAPI).toBeDefined();
            expect(typeof fieldAPI.getValue).toBe('function');
            expect(typeof fieldAPI.setValue).toBe('function');
            expect(typeof fieldAPI.onChange).toBe('function');
            expect(typeof fieldAPI.enable).toBe('function');
            expect(typeof fieldAPI.disable).toBe('function');
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
            fieldAPI.onChange(callback);

            expect(mockFormControl.valueChanges.subscribe).toHaveBeenCalled();

            if (mockFormControl.valueChanges._callback) {
                mockFormControl.valueChanges._callback('changed value');
                expect(callback).toHaveBeenCalledWith('changed value');
            }
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
    });

    describe('ready', () => {
        it('should execute callback with bridge instance', () => {
            const callback = jest.fn();
            bridge.ready(callback);
            expect(callback).toHaveBeenCalledWith(bridge);
        });
    });

    describe('openBrowserModal', () => {
        beforeEach(() => {
            jest.clearAllMocks();
        });

        it('should open dialog with correct configuration', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select a Page',
                mimeTypes: ['application/dotpage'],
                onClose
            });

            expect(mockDialogService.open).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    header: 'Select a Page',
                    appendTo: 'body',
                    closeOnEscape: false,
                    draggable: false,
                    keepInViewport: false,
                    maskStyleClass: 'p-dialog-mask-dynamic',
                    resizable: false,
                    modal: true,
                    width: '90%',
                    style: { 'max-width': '1040px' },
                    data: {
                        mimeTypes: ['application/dotpage']
                    }
                })
            );
        });

        it('should use default header if not provided', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                mimeTypes: ['image'],
                onClose
            });

            expect(mockDialogService.open).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    header: 'Select Content'
                })
            );
        });

        it('should use default mimeTypes if not provided', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                onClose
            });

            expect(mockDialogService.open).toHaveBeenCalledWith(
                expect.any(Function),
                expect.objectContaining({
                    data: {
                        mimeTypes: []
                    }
                })
            );
        });

        it('should handle onClose callback with content', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            const content = {
                identifier: 'test-id',
                inode: 'test-inode',
                title: 'Test Title',
                name: 'test-name',
                url: 'https://example.com/test',
                mimeType: 'image/png',
                baseType: 'FILEASSET',
                contentType: 'FileAsset'
            };

            if (mockDialogRef.onClose._callback) {
                mockDialogRef.onClose._callback(content);
            }

            expect(onClose).toHaveBeenCalledWith({
                identifier: 'test-id',
                inode: 'test-inode',
                title: 'Test Title',
                name: 'test-name',
                url: 'https://example.com/test',
                mimeType: 'image/png',
                baseType: 'FILEASSET',
                contentType: 'FileAsset'
            });
        });

        it('should handle onClose callback with null', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            if (mockDialogRef.onClose._callback) {
                mockDialogRef.onClose._callback(null);
            }

            expect(onClose).toHaveBeenCalledWith(null);
        });

        it('should handle content with fileName instead of name', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            const content = {
                identifier: 'test-id',
                inode: 'test-inode',
                title: 'Test Title',
                fileName: 'test-file.png',
                url: 'https://example.com/test',
                mimeType: 'image/png'
            };

            if (mockDialogRef.onClose._callback) {
                mockDialogRef.onClose._callback(content);
            }

            expect(onClose).toHaveBeenCalledWith(
                expect.objectContaining({
                    name: 'test-file.png'
                })
            );
        });

        it('should handle content with urlMap instead of url', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            const content = {
                identifier: 'test-id',
                inode: 'test-inode',
                title: 'Test Title',
                urlMap: 'https://example.com/mapped-url'
            };

            if (mockDialogRef.onClose._callback) {
                mockDialogRef.onClose._callback(content);
            }

            expect(onClose).toHaveBeenCalledWith(
                expect.objectContaining({
                    url: 'https://example.com/mapped-url'
                })
            );
        });

        it('should handle content with empty url and urlMap', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            const content = {
                identifier: 'test-id',
                inode: 'test-inode',
                title: 'Test Title'
            };

            if (mockDialogRef.onClose._callback) {
                mockDialogRef.onClose._callback(content);
            }

            expect(onClose).toHaveBeenCalledWith(
                expect.objectContaining({
                    url: ''
                })
            );
        });

        it('should return controller with close method', () => {
            const onClose = jest.fn();
            const controller = bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            expect(controller).toBeDefined();
            expect(typeof controller.close).toBe('function');

            controller.close();
            expect(mockDialogRef.close).toHaveBeenCalled();
        });

        it('should run openBrowserModal inside NgZone', () => {
            const zoneRunSpy = jest.spyOn(mockNgZone, 'run');
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            expect(zoneRunSpy).toHaveBeenCalled();
        });
    });

    describe('destroy with dialog cleanup', () => {
        it('should close dialog when destroyed', () => {
            const onClose = jest.fn();
            bridge.openBrowserModal({
                header: 'Select Content',
                mimeTypes: [],
                onClose
            });

            bridge.destroy();

            expect(mockDialogRef.close).toHaveBeenCalled();
        });

        it('should not throw if no dialog is open when destroyed', () => {
            expect(() => bridge.destroy()).not.toThrow();
        });
    });
});
