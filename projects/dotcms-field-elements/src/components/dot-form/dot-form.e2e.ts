import { newE2EPage, E2EPage, E2EElement } from '@stencil/core/testing';

describe('dot-form', () => {
    let page: E2EPage;
    let element: E2EElement;
    let formStatus = {};

    const fields = [
        {
            fieldType: 'Text',
            defaultValue: 'defaultValue1',
            hint: 'hint1',
            name: 'field1',
            required: true,
            value: 'value1',
            variable: 'field1'
        },
        {
            fieldType: 'Text',
            defaultValue: 'defaultValue',
            hint: 'hint2',
            name: 'field2',
            required: true,
            value: 'value2',
            variable: 'field2'
        }
    ];

    beforeEach(async () => {
        page = await newE2EPage();
        await page.setContent(
            `<dot-form submit-label="Saved" reset-label="Reseted"></dot-form>`
        );
        element = await page.find('dot-form');
        element.setProperty('fields', fields);
        await page.waitForChanges();
        const txtFields = await element.findAll('dot-textfield');
        txtFields.forEach((field, index) => {
            field.triggerEvent('valueChanges', {
                bubbles: true,
                cancelable: false,
                detail: {
                    name: fields[index].name,
                    value: fields[index].value
                }
            });
        });
        await page.waitForChanges();
        element.getProperty('value').then((data) => { formStatus = data; });
    });

    it('should renders', async () => {
        // tslint:disable-next-line:max-line-length
        const tagsRenderExpected = `<form><dot-textfield class="hydrated"><label for="field1">field1</label><input name="field1" type="text" required=""><span class="dot-textfield__hint">hint1</span></dot-textfield><dot-textfield class="hydrated"><label for="field2">field2</label><input name="field2" type="text" required=""><span class="dot-textfield__hint">hint2</span></dot-textfield><button type="submit">Saved</button><button type="button">Reseted</button></form>`;
        expect(element.innerHTML).toBe(tagsRenderExpected);
    });

    it('should send "submit" event', async () => {
        const expectedSubmit = {};
        const spy = await page.spyOnEvent('onSubmit');
        const saveBtn = await element.find('button[type="submit"]');

        fields.forEach((field) => {
            expectedSubmit[field.name] = field.value;
        });

        saveBtn.click();
        await page.waitForChanges();
        expect(spy).toHaveReceivedEventDetail(expectedSubmit);
    });

    it('should listen for valueChanges', async () => {
        const textField = await page.find('dot-textfield');
        const newValue = {
            name: 'field1',
            value: 'test2'
        };

        textField.triggerEvent('valueChanges', {
            bubbles: true,
            cancelable: false,
            detail: newValue
        });

        formStatus = {...formStatus, field1: 'test2' };

        await page.waitForChanges();
        element.getProperty('value').then((data) => {
            expect(data).toEqual(formStatus);
        });
    });

    it('should reset event', async () => {
        const resetBtn = await element.find('button[type="button"]');
        const expectedStatus = Object.assign({}, formStatus);
        Object.keys(expectedStatus).forEach(e => expectedStatus[e] = '');

        resetBtn.click();

        await page.waitForChanges();

        const data = await element.getProperty('value');
        expect(data).toEqual(expectedStatus);
    });
});
