import { DotCMSHttpClient } from './../../utils/DotCMSHttpClient';
import { DotApiForm } from '../DotApiForm';

function validateForm(formScript: string): void {
    expect(formScript.includes('label="field1"')).toEqual(true);
    expect(formScript.includes('hint="hint1"')).toEqual(true);
    expect(formScript.includes('value="defaultValue1"')).toEqual(true);
    expect(formScript.includes('required')).toEqual(true);
}

describe('DotApiForm', () => {
    let httpClient: DotCMSHttpClient;
    let dotApiForm;
    let config;

    const fieldReturned = [
        {
            clazz: 'a.ImmutableTextField',
            defaultValue: 'defaultValue1',
            hint: 'hint1',
            name: 'field1',
            required: true,
            value: 'value1',
            variable: 'field1'
        }
    ];

    beforeEach(() => {
        httpClient = new DotCMSHttpClient({
            token: '',
            host: 'http://localhost'
        });
        dotApiForm = new DotApiForm(httpClient);

        spyOn(dotApiForm.dotApiContentType, 'getFields').and.returnValue(
            new Promise((resolve) => resolve(fieldReturned))
        );

        config = { identifier: '321', fields: ['field1', 'field2'] };
    });

    it('should request a DotApiForm instance', () => {
        dotApiForm.get(config).then((formInstance) => {
            expect(formInstance).toEqual(dotApiForm);
            validateForm(formInstance.formScript);
        });
    });

    it('should render a Form', async () => {
        const container = document.createElement('div');
        const formInstance = await dotApiForm.get(config);
        formInstance.render(container);
        expect(container.innerHTML.includes('<script type="module">')).toEqual(true);
        expect(container.innerHTML.includes(
            `import { defineCustomElements } from "https://unpkg.com/dotcms-field-elements@0.0.2/dist/loader";`)
        ).toEqual(true);
        expect(container.innerHTML.includes('defineCustomElements(window);</script>')).toEqual(true);
        validateForm(formInstance.formScript);
    });
});
