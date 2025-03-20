import { DojoFormBridge } from './dojo-form-bridge';

describe('DojoFormBridge', () => {
    let bridge: DojoFormBridge;
    let inputElement: HTMLInputElement;

    beforeEach(() => {
        bridge = new DojoFormBridge();
        inputElement = document.createElement('input');
        inputElement.id = 'testField';
        document.body.appendChild(inputElement);
    });

    afterEach(() => {
        document.body.removeChild(inputElement);
    });

    it('should get field value from Dojo input', () => {
        inputElement.value = 'test value';
        expect(bridge.get('testField')).toBe('test value');
    });

    it('should set field value in Dojo input', () => {
        bridge.set('testField', 'new value');
        expect(inputElement.value).toBe('new value');
    });

    it('should watch field changes in Dojo input', () => {
        const callback = jest.fn();
        bridge.onChangeField('testField', callback);

        inputElement.value = 'changed value';
        inputElement.dispatchEvent(new Event('change'));
        expect(callback).toHaveBeenCalledWith('changed value');
    });

    it('should cleanup event listeners on destroy', () => {
        const callback = jest.fn();
        bridge.onChangeField('testField', callback);
        bridge.destroy();

        inputElement.dispatchEvent(new Event('change'));
        expect(callback).not.toHaveBeenCalled();
    });

    it('should execute ready callback when loaded', (done) => {
        bridge.ready((api) => {
            expect(api).toBeDefined();
            done();
        });
        window.dispatchEvent(new Event('load'));
    });
});
