import { newE2EPage, E2EPage, E2EElement, EventSpy } from '@stencil/core/testing';

import { fieldMockNotRequired, dotFormLayoutMock } from '../../test';
import { dotTestUtil } from '../../utils';

describe('dot-form', () => {
    let page: E2EPage;
    let element: E2EElement;
    let submitSpy: EventSpy;

    const getFields = () => page.findAll('form dot-form-column > *');
    const getResetButton = () => page.find('.dot-form__buttons button:not([type="submit"])');
    const getSubmitButton = () => page.find('.dot-form__buttons button[type="submit"]');

    const fillTextfield = async (text?: string) => {
        const textfield = await element.find('input');
        await textfield.type(text || 'test');
    };

    const submitForm = async () => {
        const button = await getSubmitButton();
        await button.click();
    };

    const resetForm = async () => {
        const button = await getResetButton();
        await button.click();
    };

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-form></dot-form>`
        });
        element = await page.find('dot-form');
        submitSpy = await element.spyOnEvent('onSubmit');
    });

    describe('css class', () => {
        beforeEach(async () => {
            element.setProperty('layout', fieldMockNotRequired);
            await page.waitForChanges();
        });

        it('should have empty', () => {
            expect(element).toHaveClasses(dotTestUtil.class.empty);
        });

        it('should have filled', async () => {
            const keyValue = await element.find('dot-key-value');
            keyValue.triggerEvent('statusChange', {
                detail: {
                    name: 'keyvalue2',
                    status: {
                        dotValid: true,
                        dotTouched: true,
                        dotPristine: false
                    }
                }
            });
            keyValue.triggerEvent('valueChange', {
                detail: {
                    name: 'keyvalue2',
                    value: 'key|value,llave|valor'
                }
            });

            await page.waitForChanges();
            expect(element).toHaveClasses(dotTestUtil.class.filled);
        });

        it('should have touched pristine', async () => {
            await page.waitForChanges();
            const keyValue = await element.find('dot-key-value');
            keyValue.triggerEvent('statusChange', {
                detail: {
                    name: 'keyvalue2',
                    status: {
                        dotValid: true,
                        dotTouched: true,
                        dotPristine: true
                    }
                }
            });
            await page.waitForChanges();

            expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
        });
    });

    describe('rows', () => {
        beforeEach(async () => {
            element.setProperty('layout', dotFormLayoutMock);
            element.setProperty('fieldsToShow', 'test');
            await page.waitForChanges();
        });

        it('should have 2 rows', async () => {
            const rows = await element.findAll('dot-form-row');
            expect(rows.length).toBe(2);
        });

        it('should set values on dot-form-row', async () => {
            const firstRow = await element.find('dot-form-row');
            expect(await firstRow.getProperty('row')).toEqual(dotFormLayoutMock[0]);
            expect(await firstRow.getProperty('fieldsToShow')).toEqual('test');
        });
    });

    describe('@Props', () => {
        describe('fieldsToShow', () => {
            beforeEach(() => {
                element.setProperty('layout', dotFormLayoutMock);
            });

            it('should render specified fields', async () => {
                element.setProperty('fieldsToShow', 'textfield1,dropdown3');
                await page.waitForChanges();

                const fields = await getFields();
                expect(fields.length).toBe(2);

                const keyValueField = await element.find('form > dot-key-value');
                expect(keyValueField).toBeNull();
            });

            it('should render no fields', async () => {
                element.setProperty('fieldsToShow', 'no,field,to,render');
                await page.waitForChanges();

                const fields = await getFields();
                expect(fields.length).toBe(0);
            });
        });

        describe('resetLabel', () => {
            it('should set default label', async () => {
                const button = await getResetButton();
                expect(button.innerText).toBe('Reset');
            });

            it('should set a label correctly', async () => {
                element.setProperty('resetLabel', 'Reiniciar');
                await page.waitForChanges();

                const button = await getResetButton();
                expect(button.innerText).toBe('Reiniciar');
            });
        });

        describe('submitLabel', () => {
            it('should set default label', async () => {
                const button = await getSubmitButton();
                expect(button.innerText).toBe('Submit');
            });

            it('should set a label correctly', async () => {
                element.setProperty('submitLabel', 'Enviar');
                await page.waitForChanges();

                const button = await getSubmitButton();
                expect(button.innerText).toBe('Enviar');
            });
        });

        describe('fields', () => {
            beforeEach(() => {
                element.setProperty('layout', dotFormLayoutMock);
            });

            it('should render fields', async () => {
                await page.waitForChanges();

                const fields = await getFields();
                expect(fields.map((field: E2EElement) => field.tagName)).toEqual([
                    'DOT-TEXTFIELD',
                    'DOT-KEY-VALUE',
                    'DOT-SELECT'
                ]);
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            element.setProperty('layout', dotFormLayoutMock);
            await page.waitForChanges();
        });

        describe('onSubmit', () => {
            // TODO: these tests do not validate correctly the submit
            xit('should emit when form is valid', async () => {
                await fillTextfield('hello world');
                await page.waitForChanges();

                await submitForm();
                await page.waitForChanges();

                expect(submitSpy).toHaveReceivedEventDetail({
                    dropdown3: '2',
                    keyvalue2: 'key|value,llave|valor',
                    textfield1: 'hello world'
                });
            });

            xit('should not emit when form is invalid', async () => {
                await submitForm();
                await page.waitForChanges();

                expect(submitSpy).not.toHaveReceivedEvent();
            });
        });
    });

    describe('buttons', () => {
        describe('submit', () => {
            it('should have type submit', async () => {
                const button = await getSubmitButton();
                expect(button.getAttribute('type')).toBe('submit');
            });
        });

        describe('reset', () => {
            it('should have type reset', async () => {
                const button = await getResetButton();
                expect(button.getAttribute('type')).toBe('reset');
            });
        });
    });

    describe('actions', () => {
        beforeEach(async () => {
            element.setProperty('layout', dotFormLayoutMock);
            await page.waitForChanges();
        });

        describe('click reset button', () => {
            it('should empty values', async () => {
                await fillTextfield('hello world');
                await page.waitForChanges();
                const [textfield, keyvalue, select] = await getFields();

                expect(await textfield.getProperty('value')).toBe('hello world');

                await resetForm();
                await page.waitForChanges();

                expect(await textfield.getProperty('value')).toBe('');
                expect(await keyvalue.getProperty('value')).toBe('');
                expect(await select.getProperty('value')).toBe('');
                expect(element).toHaveClasses(dotTestUtil.class.emptyPristineInvalid);
            });
        });
    });
});
