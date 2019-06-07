import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';
import { dotTestUtil } from '../../utils';
import { DotCMSContentTypeField } from './models';

const basicField: DotCMSContentTypeField = {
    clazz: '',
    contentTypeId: '',
    dataType: '',
    defaultValue: '',
    fieldContentTypeProperties: [],
    fieldType: '',
    fieldTypeLabel: '',
    fieldVariables: [],
    fixed: true,
    hint: '',
    iDate: 100,
    id: '',
    indexed: true,
    listed: true,
    modDate: 100,
    name: '',
    readOnly: true,
    regexCheck: '',
    required: true,
    searchable: true,
    sortOrder: 100,
    unique: true,
    values: '',
    variable: ''
};

const fieldsMock: DotCMSContentTypeField[] = [
    {
        ...basicField,
        variable: 'textfield1',
        required: true,
        name: 'TexField',
        fieldType: 'Text'
    },
    {
        ...basicField,
        defaultValue: 'key|value,llave|valor',
        fieldType: 'Key-Value',
        name: 'Key Value:',
        required: false,
        variable: 'keyvalue2'
    },
    {
        ...basicField,
        defaultValue: '2',
        fieldType: 'Select',
        name: 'Dropdwon',
        required: false,
        values: '|,labelA|1,labelB|2,labelC|3',
        variable: 'dropdown3'
    }
];


const fieldMockNotRequired = [
    {
        ...fieldsMock[0],
        required: false
    },
    fieldsMock[1],
    fieldsMock[2]
];

describe('dot-form', () => {
    let page: E2EPage;
    let element: E2EElement;
    let submitSpy: EventSpy;

    const getFields = () => page.findAll('form .form__fields > *');

    const getResetButton = () =>
        page.find('.form__buttons button:not([type="submit"])');

    const getSubmitButton = () => page.find('.form__buttons button[type="submit"]');

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
        page = await newE2EPage();
        await page.setContent(`<dot-form></dot-form>`);
        element = await page.find('dot-form');
        submitSpy = await element.spyOnEvent('onSubmit');
    });

    describe('css class', () => {
        beforeEach(async () => {
            element.setProperty('fields', fieldMockNotRequired);
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

    describe('@Props', () => {
        describe('fieldsToShow', () => {
            beforeEach(() => {
                element.setProperty('fields', fieldsMock);
            });

            it('should render specified fields', async () => {
                element.setProperty('fieldsToShow', ['textfield1', 'dropdown3']);
                await page.waitForChanges();

                const fields = await getFields();
                expect(fields.length).toBe(2);

                const keyValueField = await element.find('form > dot-key-value');
                expect(keyValueField).toBeNull();
            });

            it('should render no fields', async () => {
                element.setProperty('fieldsToShow', ['no', 'field', 'to', 'render']);
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
                element.setProperty('fields', fieldsMock);
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
            element.setProperty('fields', fieldsMock);
            await page.waitForChanges();
        });

        describe('onSubmit', () => {
            it('should emit when form is valid', async () => {
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

            it('should not emit when form is invalid', async () => {
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
            element.setProperty('fields', fieldMockNotRequired);
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
                expect(element).toHaveClasses(dotTestUtil.class.empty);
            });
        });
    });

    describe('<slot />', () => {
        beforeEach(async () => {
            page = await newE2EPage();
            await page.setContent(`
                <dot-form>
                    <dot-textfield label="Hello World" />
                </dot-form>
            `);
            element = await page.find('dot-form');
        });

        it('should render ast first child', async () => {
            const slot = await element.find('.form__fields *:nth-child(1n)');
            expect(slot.tagName).toBe('DOT-TEXTFIELD');
        });
    });
});
