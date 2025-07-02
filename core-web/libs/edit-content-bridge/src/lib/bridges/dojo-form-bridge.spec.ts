/* eslint-disable @typescript-eslint/no-empty-function */

import { DojoFormBridge } from './dojo-form-bridge';

describe('DojoFormBridge', () => {
    let bridge: DojoFormBridge;
    let inputElement: HTMLInputElement;
    let textareaElement: HTMLTextAreaElement;

    beforeEach(() => {
        bridge = new DojoFormBridge();
        inputElement = document.createElement('input');
        inputElement.id = 'testInput';
        textareaElement = document.createElement('textarea');
        textareaElement.id = 'testTextarea';
        document.body.appendChild(inputElement);
        document.body.appendChild(textareaElement);
    });

    afterEach(() => {
        document.body.removeChild(inputElement);
        document.body.removeChild(textareaElement);
        bridge.destroy();
    });

    describe('get', () => {
        it('should get value from input element', () => {
            inputElement.value = 'test value';
            expect(bridge.get('testInput')).toBe('test value');
        });

        it('should get value from textarea element', () => {
            textareaElement.value = 'test value';
            expect(bridge.get('testTextarea')).toBe('test value');
        });

        it('should return null for non-existent element', () => {
            expect(bridge.get('nonExistent')).toBeNull();
        });

        it('should return null for non-input/textarea element', () => {
            const div = document.createElement('div');
            div.id = 'testDiv';
            document.body.appendChild(div);
            expect(bridge.get('testDiv')).toBeNull();
            document.body.removeChild(div);
        });

        it('should handle errors gracefully', () => {
            const consoleSpy = jest.spyOn(console, 'warn');
            // Simulate an error by removing the element during get
            const getSpy = jest.spyOn(document, 'getElementById').mockImplementation(() => {
                throw new Error('Test error');
            });

            expect(bridge.get('testInput')).toBeNull();
            expect(consoleSpy).toHaveBeenCalledWith(
                'Unable to get field value:',
                expect.any(Error)
            );

            getSpy.mockRestore();
        });
    });

    describe('set', () => {
        it('should set value in input element', () => {
            bridge.set('testInput', 'new value');
            expect(inputElement.value).toBe('new value');
        });

        it('should set value in textarea element', () => {
            bridge.set('testTextarea', 'new value');
            expect(textareaElement.value).toBe('new value');
        });

        it('should handle null values', () => {
            bridge.set('testInput', null);
            expect(inputElement.value).toBe('');
        });

        it('should dispatch change event after setting value', () => {
            const changeSpy = jest.fn();
            inputElement.addEventListener('change', changeSpy);

            bridge.set('testInput', 'new value');

            expect(changeSpy).toHaveBeenCalled();
        });

        it('should handle errors gracefully', () => {
            const consoleSpy = jest.spyOn(console, 'warn');
            // Simulate an error by removing the element during set
            const getSpy = jest.spyOn(document, 'getElementById').mockImplementation(() => {
                throw new Error('Test error');
            });

            bridge.set('testInput', 'new value');
            expect(consoleSpy).toHaveBeenCalledWith(
                'Error setting field value:',
                expect.any(Error)
            );

            getSpy.mockRestore();
        });
    });

    describe('onChangeField', () => {
        it('should watch input changes', () => {
            const callback = jest.fn();
            bridge.onChangeField('testInput', callback);

            inputElement.value = 'changed value';
            inputElement.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith('changed value');
        });

        it('should watch textarea changes', () => {
            const callback = jest.fn();
            bridge.onChangeField('testTextarea', callback);

            textareaElement.value = 'changed value';
            textareaElement.dispatchEvent(new Event('change'));

            expect(callback).toHaveBeenCalledWith('changed value');
        });

        it('should support multiple callbacks for same field', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            bridge.onChangeField('testInput', callback1);
            bridge.onChangeField('testInput', callback2);

            inputElement.value = 'changed value';
            inputElement.dispatchEvent(new Event('change'));

            expect(callback1).toHaveBeenCalledWith('changed value');
            expect(callback2).toHaveBeenCalledWith('changed value');
        });

        it('should handle both keyup and change events', () => {
            const callback = jest.fn();
            bridge.onChangeField('testInput', callback);

            inputElement.value = 'keyup value';
            inputElement.dispatchEvent(new Event('keyup'));
            expect(callback).toHaveBeenCalledWith('keyup value');

            inputElement.value = 'change value';
            inputElement.dispatchEvent(new Event('change'));
            expect(callback).toHaveBeenCalledWith('change value');
        });

        it('should return no-op function for non-existent element', () => {
            const unsubscribe = bridge.onChangeField('nonExistent', () => {});
            expect(typeof unsubscribe).toBe('function');
            unsubscribe(); // Should not throw
        });

        it('should return no-op function for non-input/textarea element', () => {
            const div = document.createElement('div');
            div.id = 'testDiv';
            document.body.appendChild(div);

            const unsubscribe = bridge.onChangeField('testDiv', () => {});
            expect(typeof unsubscribe).toBe('function');
            unsubscribe(); // Should not throw

            document.body.removeChild(div);
        });

        it('should handle errors gracefully', () => {
            const consoleSpy = jest.spyOn(console, 'warn');
            // Simulate an error during onChangeField
            const getSpy = jest.spyOn(document, 'getElementById').mockImplementation(() => {
                throw new Error('Test error');
            });

            const unsubscribe = bridge.onChangeField('testInput', () => {});
            expect(typeof unsubscribe).toBe('function');
            expect(consoleSpy).toHaveBeenCalledWith('Error watching field:', expect.any(Error));

            getSpy.mockRestore();
        });
    });

    describe('unsubscribe', () => {
        it('should remove specific callback and keep others', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            const unsubscribe1 = bridge.onChangeField('testInput', callback1);
            bridge.onChangeField('testInput', callback2);

            unsubscribe1();

            inputElement.value = 'changed value';
            inputElement.dispatchEvent(new Event('change'));

            expect(callback1).not.toHaveBeenCalled();
            expect(callback2).toHaveBeenCalledWith('changed value');
        });

        it('should cleanup event listeners when last callback is removed', () => {
            const callback = jest.fn();
            const unsubscribe = bridge.onChangeField('testInput', callback);

            unsubscribe();

            inputElement.value = 'changed value';
            inputElement.dispatchEvent(new Event('change'));

            expect(callback).not.toHaveBeenCalled();
        });
    });

    describe('destroy', () => {
        it('should cleanup all event listeners on destroy', () => {
            const callback1 = jest.fn();
            const callback2 = jest.fn();

            bridge.onChangeField('testInput', callback1);
            bridge.onChangeField('testTextarea', callback2);

            bridge.destroy();

            inputElement.value = 'changed value';
            inputElement.dispatchEvent(new Event('change'));
            textareaElement.value = 'changed value';
            textareaElement.dispatchEvent(new Event('change'));

            expect(callback1).not.toHaveBeenCalled();
            expect(callback2).not.toHaveBeenCalled();
        });

        it('should cleanup load handler on destroy', () => {
            const callback = jest.fn();
            bridge.ready(callback);

            bridge.destroy();

            window.dispatchEvent(new Event('load'));

            expect(callback).not.toHaveBeenCalled();
        });
    });

    describe('ready', () => {
        it('should execute callback when loaded', (done) => {
            bridge.ready((api) => {
                expect(api).toBeDefined();
                done();
            });

            window.dispatchEvent(new Event('load'));
        });

        it('should not execute callback if bridge is destroyed', () => {
            const callback = jest.fn();
            bridge.ready(callback);

            bridge.destroy();
            window.dispatchEvent(new Event('load'));

            expect(callback).not.toHaveBeenCalled();
        });
    });
});
