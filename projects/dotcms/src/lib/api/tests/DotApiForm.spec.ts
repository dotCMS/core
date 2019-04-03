import { DotApiForm } from '../DotApiForm';
import { DotCMSContentTypeField } from 'dotcms/lib/models';

function validateForm(formScript: string): void {
    expect(formScript.includes('label="field1"')).toEqual(true);
    expect(formScript.includes('hint="hint1"')).toEqual(true);
    expect(formScript.includes('value="defaultValue1"')).toEqual(true);
    expect(formScript.includes('required')).toEqual(true);
}

const fieldReturned = [
    {
        fieldType: 'Text',
        defaultValue: 'defaultValue1',
        hint: 'hint1',
        name: 'field1',
        required: true,
        value: 'value1',
        variable: 'field1'
    }
];

class DotApiContentTypeMock {
    getFields(): Promise<DotCMSContentTypeField[]> {
        return new Promise((resolve) => {
            resolve(Array.from(fieldReturned));
        });
    }
}

describe('DotApiForm', () => {
    let dotApiContentType;
    let dotApiForm;

    beforeEach(() => {
        dotApiContentType = new DotApiContentTypeMock();
    });

    it('should render a Form', async () => {
        const config = { identifier: '321', fields: ['field1', 'field2'] };
        const container = document.createElement('div');
        spyOn(container, 'append').and.callThrough();
        dotApiForm = new DotApiForm(dotApiContentType, config);
        dotApiForm.render(container).then(() => {
            expect(container.append).toHaveBeenCalled();
            expect(container.innerHTML.includes('<script type="module">')).toEqual(true);
            expect(container.innerHTML.includes(
                    `import { defineCustomElements } from "https://unpkg.com/dotcms-field-elements@0.0.2/dist/loader";`
                )).toEqual(true);
            expect(container.innerHTML.includes('defineCustomElements(window);</script>')).toEqual(true);
            validateForm(container.innerHTML);
        });
    });
});
