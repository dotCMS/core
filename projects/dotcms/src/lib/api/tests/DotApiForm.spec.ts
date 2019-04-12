import { DotApiForm } from '../DotApiForm';
import { DotCMSContentType } from '../../models';

const fieldReturned = [
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

const contentTypeReturned: DotCMSContentType = {
    clazz: 'A',
    defaultType: true,
    fields: fieldReturned,
    fixed: true,
    folder: 'FolderA',
    host: 'HostA',
    name: 'TestA',
    owner: 'me',
    system: true,
    variable: 'contentTest1'
};

class DotApiContentTypeMock {
    get(): Promise<DotCMSContentType> {
        return new Promise((resolve) => {
            resolve(contentTypeReturned);
        });
    }
}

class DotApiContentMock {
    save(): Promise<any> {
        return new Promise((resolve) => {
            resolve({
                status: 200
            });
        });
    }
}

describe('DotApiForm', () => {
    let dotApiContent;
    let dotApiContentType;
    let dotApiForm;

    beforeEach(() => {
        dotApiContent = new DotApiContentMock();
        dotApiContentType = new DotApiContentTypeMock();
    });

    it('should render a Form and execute "onSucess" function on submit', (done) => {
        const expectedForm = `<script type="module">
            import { defineCustomElements } from 'http://localhost:8080/fieldElements/loader/index.js';
            //import { defineCustomElements } from 'https://unpkg.com/dotcms-field-elements@latest/dist/loader';
            defineCustomElements(window);</script><dot-form submit-label="Save" reset-label="Clear"></dot-form>`;

        const config = {
            identifier: '321',
            labels: {
                submit: 'Save',
                reset: 'Clear'
            },
            onSuccess: jasmine.createSpy().and.callFake((data) => {
                expect(data).toEqual({
                    status: 200
                });
                done();
            }),
            onError: function(error: any) {
                console.log('*** onError data', error);
            }
        };
        const container = document.createElement('div');
        spyOn(container, 'append').and.callThrough();
        dotApiForm = new DotApiForm(dotApiContentType, config, dotApiContent);

        dotApiForm.render(container).then(() => {
            expect(container.append).toHaveBeenCalled();
            expect(container.innerHTML).toBe(expectedForm);

            const formTag = container.querySelector('dot-form');
            const customEvent = document.createEvent('CustomEvent');
            customEvent.initCustomEvent('onSubmit', true, false, {});
            formTag.dispatchEvent(customEvent);
        });
    });
});
