import { DotApiForm } from '../DotApiForm';
import { DotCMSContentType } from '@dotcms/models';
import { dotcmsContentTypeFieldBasicMock } from '@tests/dot-content-types.mock';

const fieldReturned = [
    {
        ...dotcmsContentTypeFieldBasicMock,
        fieldType: 'Text',
        defaultValue: 'defaultValue1',
        hint: 'hint1',
        name: 'field1',
        required: true,
        value: 'value1',
        variable: 'field1'
    },
    {
        ...dotcmsContentTypeFieldBasicMock,
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

/** @hidden */
class DotApiContentTypeMock {
    get(): Promise<DotCMSContentType> {
        return new Promise((resolve) => {
            resolve(contentTypeReturned);
        });
    }
}

/** @hidden */
class DotApiContentMock {
    constructor(private fail = false) {}

    save(): Promise<any> {
        return new Promise((resolve, reject) => {
            if (this.fail) {
                reject({
                    status: 500
                });
            } else {
                resolve({
                    status: 200
                });
            }
        });
    }
}

describe('DotApiForm', () => {
    let dotApiContent;
    let dotApiContentType;
    let dotApiForm;
    let defineCustomElements;

    beforeEach(() => {
        dotApiContentType = new DotApiContentTypeMock();
        defineCustomElements = jasmine.createSpy('defineCustomElements');
    });

    it('should render a dot-form', (done) => {
        dotApiContent = new DotApiContentMock();
        const expectedForm = `<dot-form></dot-form>`;

        const config = {
            identifier: '321',
            onSuccess: () => {},
            onError: () => {}
        };
        const container = document.createElement('div');
        spyOn(container, 'append').and.callThrough();

        dotApiForm = new DotApiForm(dotApiContentType, config, dotApiContent, defineCustomElements);
        expect(defineCustomElements).toHaveBeenCalledTimes(1);
        expect(defineCustomElements).toHaveBeenCalledWith(window);

        dotApiForm.render(container).then(() => {
            expect(container.append).toHaveBeenCalledTimes(1);
            expect(container.innerHTML).toBe(expectedForm);
            done();
        });
    });

    it('should render form on specified window', () => {
        dotApiContent = new DotApiContentMock();
        const customWin: Window = { ...window };

        const config = {
            identifier: '321',
            onSuccess: () => {},
            onError: () => {},
            win: customWin
        };

        dotApiForm = new DotApiForm(dotApiContentType, config, dotApiContent, defineCustomElements);

        expect(defineCustomElements).toHaveBeenCalledTimes(1);
        expect(defineCustomElements).toHaveBeenCalledWith(customWin);
    });

    it('should add labels to dot-form', (done) => {
        dotApiContent = new DotApiContentMock();
        const config = {
            identifier: '321',
            labels: {
                submit: 'Enviar',
                reset: 'Clear'
            }
        };

        dotApiForm = new DotApiForm(dotApiContentType, config, dotApiContent, defineCustomElements);

        const container = document.createElement('div');
        dotApiForm.render(container).then(() => {
            const formTag = container.querySelector('dot-form');
            expect(formTag.getAttribute('submit-label')).toBe('Enviar');
            expect(formTag.getAttribute('reset-label')).toBe('Clear');
            done();
        });
    });

    it('should save content and call onSuccess', (done) => {
        const onError = jasmine.createSpy();

        dotApiContent = new DotApiContentMock();
        const config = {
            identifier: '321',
            onSuccess: jasmine.createSpy().and.callFake((data) => {
                expect(data).toEqual(
                    {
                        status: 200
                    },
                    'onSuccess called correctly'
                );
                done();
            }),
            onError: () => {}
        };

        dotApiForm = new DotApiForm(dotApiContentType, config, dotApiContent, defineCustomElements);

        const container = document.createElement('div');
        dotApiForm.render(container).then(() => {
            const formTag = container.querySelector('dot-form');
            const customEvent = document.createEvent('CustomEvent');
            customEvent.initCustomEvent('onSubmit', true, false, {});
            formTag.dispatchEvent(customEvent);
            expect(onError).toHaveBeenCalledTimes(0);
        });
    });

    it('should save content and call onError', (done) => {
        const onSuccess = jasmine.createSpy();
        dotApiContent = new DotApiContentMock(true);
        const config = {
            identifier: '321',
            onSuccess: () => {},
            onError: jasmine.createSpy().and.callFake((data) => {
                expect(data).toEqual(
                    {
                        status: 500
                    },
                    'onError called correctly'
                );
                done();
            })
        };

        dotApiForm = new DotApiForm(dotApiContentType, config, dotApiContent, defineCustomElements);

        const container = document.createElement('div');
        dotApiForm.render(container).then(() => {
            const formTag = container.querySelector('dot-form');
            const customEvent = document.createEvent('CustomEvent');
            customEvent.initCustomEvent('onSubmit', true, false, {});
            formTag.dispatchEvent(customEvent);
            expect(onSuccess).toHaveBeenCalledTimes(0);
        });
    });
});
