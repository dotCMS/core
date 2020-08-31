import { newE2EPage, E2EElement, E2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations/testing';

describe('key-value-form', () => {
    let page: E2EPage;
    let element: E2EElement;

    const getButton = () => page.find('button[type="submit"]');
    const getKeyInput = () => page.find('input[name="key"]');
    const getValueInput = () => page.find('input[name="value"]');
    const getLabel = async (name: string) => {
        const [key, value] = await page.findAll('label');

        if (name === 'key') {
            return key;
        }

        if (name === 'value') {
            return value;
        }
    };

    const typeKey = async () => {
        const key = await getKeyInput();
        key.type('key');
        await page.waitForChanges();
    };

    const typeValue = async () => {
        const value = await getValueInput();
        value.type('value');
        await page.waitForChanges();
    };

    const submitForm = async () => {
        const button = await getButton();
        button.click();
        await page.waitForChanges();
    };

    const submitValidForm = async () => {
        await typeKey();
        await typeValue();
        await submitForm();
    };

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<key-value-form />`
        });
        element = await page.find('key-value-form');
        await page.waitForChanges();
    });

    describe('@Props', () => {
        describe('disabled', () => {
            it('set disable fields and button', async () => {
                element.setProperty('disabled', true);
                await page.waitForChanges();

                const key = await getKeyInput();
                const value = await getValueInput();
                const button = await getButton();

                expect(key.getAttribute('disabled')).not.toBeNull();
                expect(value.getAttribute('disabled')).not.toBeNull();
                expect(button.getAttribute('disabled')).not.toBeNull();
            });

            it('should not set disabled in fields but no in button', async () => {
                element.setProperty('disabled', false);
                await page.waitForChanges();

                const key = await getKeyInput();
                const value = await getValueInput();
                const button = await getButton();

                expect(key.getAttribute('disabled')).toBeNull();
                expect(value.getAttribute('disabled')).toBeNull();
                expect(button.getAttribute('disabled')).not.toBeNull(); // also depends on form valid
            });
        });

        describe('addButtonLabel', () => {
            it('should set default label', async () => {
                const button = await getButton();
                expect(button.innerText).toBe('Add');
            });

            it('should set a label correctly', async () => {
                element.setProperty('addButtonLabel', 'Delete');
                await page.waitForChanges();

                const button = await getButton();
                expect(button.innerText).toBe('Delete');
            });

            it('should handle label with invalid type', async () => {
                element.setProperty('addButtonLabel', []);
                await page.waitForChanges();

                const button = await getButton();
                expect(button.innerText).toBe('');
            });
        });

        describe('keyPlaceholder', () => {
            it('should set default key input placeholder', async () => {
                const key = await getKeyInput();
                expect(key.getAttribute('placeholder')).toBe('');
            });

            it('should set a key input placeholder correctly', async () => {
                element.setProperty('keyPlaceholder', 'this is a placeholder');
                await page.waitForChanges();
                const key = await getKeyInput();
                expect(key.getAttribute('placeholder')).toBe('this is a placeholder');
            });

            it('should handle key input placeholder with invalid type', async () => {
                element.setProperty('keyPlaceholder', { i: 'am', a: 'object' });
                await page.waitForChanges();
                const key = await getKeyInput();
                expect(key.getAttribute('placeholder')).toBe('[object Object]');
            });
        });

        describe('valuePlaceholder', () => {
            it('should set default value input placeholder', async () => {
                const value = await getValueInput();
                expect(value.getAttribute('placeholder')).toBe('');
            });

            it('should set a value input placeholder correctly', async () => {
                element.setProperty('valuePlaceholder', 'this is a placeholder');
                await page.waitForChanges();
                const value = await getValueInput();
                expect(value.getAttribute('placeholder')).toBe('this is a placeholder');
            });

            it('should handle value input placeholder with invalid type', async () => {
                element.setProperty('valuePlaceholder', { i: 'am', a: 'object' });
                await page.waitForChanges();
                const value = await getValueInput();
                expect(value.getAttribute('placeholder')).toBe('[object Object]');
            });
        });

        describe('keyLabel', () => {
            it('should set default text to key input label', async () => {
                const label = await getLabel('key');
                expect(label.textContent).toBe('Key');
            });

            it('should set custom text to key input label', async () => {
                element.setProperty('keyLabel', 'some label');
                await page.waitForChanges();
                const label = await getLabel('key');
                expect(label.textContent).toBe('some label');
            });
        });

        describe('valueLabel', () => {
            it('should set default text to key input label', async () => {
                const label = await getLabel('value');
                expect(label.textContent).toBe('Value');
            });

            it('should set custom text to key input label', async () => {
                element.setProperty('valueLabel', 'some label');
                await page.waitForChanges();
                const label = await getLabel('value');
                expect(label.textContent).toBe('some label');
            });
        });
    });

    describe('@Events', () => {
        let spyAddEvent: EventSpy;

        describe('add', () => {
            beforeEach(async () => {
                spyAddEvent = await page.spyOnEvent('add');
            });

            it('should emit on form valid and submit', async () => {
                await submitValidForm();

                expect(spyAddEvent).toHaveReceivedEventDetail({
                    key: 'key',
                    value: 'value'
                });
            });

            it('should not emit on invalid form', async () => {
                await typeKey();
                await submitForm();

                expect(spyAddEvent).not.toHaveReceivedEvent();
            });
        });

        xdescribe('lostFocus', () => {
            it('should emit when input gets blur', async () => {});
        });
    });

    describe('@Behaviour', () => {
        it('should clear the form after submit', async () => {
            await submitValidForm();
            await page.waitForChanges();

            const keyInput = await getKeyInput();
            const valueInput = await getValueInput();
            const button = await getButton();

            expect(await keyInput.getProperty('value')).toBe('');
            expect(await valueInput.getProperty('value')).toBe('');
            expect(button.getAttribute('disabled')).not.toBeNull();
        });

        it('should focus on key input after valid submit', async () => {
            await submitValidForm();
            const focus = await page.find('input:focus');
            expect(focus.getAttribute('name')).toBe('key');
        });
    });
});
