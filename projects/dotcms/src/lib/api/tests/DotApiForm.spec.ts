import { DotApiForm } from '../DotApiForm';
import { DotCMSContentTypeField } from 'dotcms/lib/models';

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

    const expectedForm = `<script type="module">
            import { defineCustomElements } from "https://unpkg.com/dotcms-field-elements@0.0.2/dist/loader";
            defineCustomElements(window);</script><div><form>
            <dot-textfield label="field1" value="defaultValue1" hint="hint1" required=""></dot-textfield></form></div>`;

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
            expect(container.innerHTML).toBe(expectedForm);
        });
    });
});
