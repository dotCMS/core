import { E2EElement, E2EPage, EventSpy, newE2EPage } from '@stencil/core/testing';
import { dotTestUtil } from '../../../test';
import { DotBinaryMessageError } from '../../../models';

describe('dot-binary-file', () => {
    let page: E2EPage;
    let element: E2EElement;
    let dotBinaryText: E2EElement;
    let dotBinaryButton: E2EElement;
    let dotBinaryPreview: E2EElement;
    let spyStatusChangeEvent: EventSpy;
    let spyValueChangeEvent: EventSpy;

    describe('filled', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-binary-file preview-image-url="http://dotcms.com/image.png" preview-image-name="Hola Mundo"></dot-binary-file>`
            });
            await page.waitForChanges();

            element = await page.find('dot-binary-file');
            dotBinaryPreview = await page.find('dot-binary-file-preview');
            spyValueChangeEvent = await page.spyOnEvent('dotValueChange');
        });

        it('should handle clear with no errors', async () => {
            dotBinaryPreview.triggerEvent('delete', {});
            await page.waitForChanges();

            expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                name: '',
                value: null
            });
        });
    });

    describe('empty', () => {
        beforeEach(async () => {
            page = await newE2EPage({
                html: `<dot-binary-file></dot-binary-file>`
            });

            element = await page.find('dot-binary-file');
            dotBinaryText = await page.find('dot-binary-text-field');
            dotBinaryButton = await page.find('dot-binary-upload-button');
            dotBinaryPreview = await page.find('dot-binary-file-preview');
        });

        describe('render CSS classes', () => {
            it('should be valid, untouched & pristine on load', async () => {
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.empty);
            });

            it('should be valid, touched & dirty when filled', async () => {
                dotBinaryText.triggerEvent('fileChange', {
                    detail: { file: 'http://www.test.com/file.pdf', errorType: '' }
                });
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.filled);
            });

            it('should have touched but pristine on blur', async () => {
                dotBinaryText.triggerEvent('lostFocus');
                await page.waitForChanges();
                expect(element).toHaveClasses(dotTestUtil.class.touchedPristine);
            });

            describe('required', () => {
                beforeEach(async () => {
                    element.setProperty('required', 'true');
                });

                it('should be valid, touched & dirty and required when filled', async () => {
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: 'http://www.test.com/file.pdf', errorType: '' }
                    });
                    await page.waitForChanges();
                    expect(element).toHaveClasses(dotTestUtil.class.filledRequired);
                });

                it('should be invalid, untouched, pristine and required when empty on load', async () => {
                    element.setProperty('value', '');
                    await page.waitForChanges();
                    expect(element).toHaveClasses(dotTestUtil.class.emptyRequiredPristine);
                });

                it('should be invalid, touched, dirty and required when valued is cleared', async () => {
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: null, errorType: '' }
                    });
                    await page.waitForChanges();
                    expect(element).toHaveClasses(dotTestUtil.class.emptyRequired);
                });
            });
        });

        describe('@Props', () => {
            describe('dot-attr', () => {
                it('should set value correctly', async () => {
                    page = await newE2EPage({
                        html: `<dot-binary-file dotmultiple="true"></dot-binary-file>`
                    });
                    await page.waitForChanges();
                    const inputUploadButton = await page.find('input[type="file"]');
                    expect(inputUploadButton.getAttribute('multiple')).toBe('true');
                });
            });

            describe('name', () => {
                it('should set name prop in dot-binary-upload-button', async () => {
                    element.setProperty('name', 'text01');
                    await page.waitForChanges();
                    expect(dotBinaryButton.getAttribute('name')).toBe('text01');
                });

                it('should not set name prop in dot-binary-upload-button', async () => {
                    await page.waitForChanges();
                    expect(dotBinaryButton.getAttribute('name')).toBe('');
                });
            });

            describe('label', () => {
                it('should set label prop in dot-label', async () => {
                    element.setProperty('label', 'file:');
                    await page.waitForChanges();
                    const label = await dotTestUtil.getDotLabel(page);
                    expect(label.getAttribute('label')).toBe('file:');
                });
            });

            describe('placeholder', () => {
                it('should render default placeholder correctly', async () => {
                    await page.waitForChanges();
                    expect(dotBinaryText.getAttribute('placeholder')).toBe(
                        'Drop or paste a file or url'
                    );
                });

                it('should set placeholder correctly', async () => {
                    element.setProperty('placeholder', 'Test');
                    await page.waitForChanges();
                    expect(dotBinaryText.getAttribute('placeholder')).toBe('Test');
                });

                xit('should set placeholder correctly in windows', async () => {});
            });

            describe('hint', () => {
                it('should set hint correctly', async () => {
                    element.setProperty('hint', 'Test');
                    await page.waitForChanges();
                    expect((await dotTestUtil.getHint(page)).innerText).toBe('Test');
                    expect(dotBinaryText.getAttribute('hint')).toBe('Test');
                });

                it('should not render hint and do not set aria attribute', async () => {
                    expect(await dotTestUtil.getHint(page)).toBeNull();
                    expect(dotBinaryText.getAttribute('hint')).toBe('');
                });

                it('should not break hint with invalid value', async () => {
                    element.setProperty('hint', { test: 'hint' });
                    await page.waitForChanges();
                    expect(await dotTestUtil.getHint(page)).toBeNull();
                });
            });

            describe('errorMessage', () => {
                it('should display Error message', async () => {
                    element.setProperty('errorMessage', 'Error');
                    await page.waitForChanges();
                    expect((await page.find('dot-error-message')).innerText).toBe('Error');
                });

                it('should not display Error message', async () => {
                    expect(await page.find('.dot-binary__error-message')).toBeNull();
                });
            });

            describe('required', () => {
                it('should render required attribute with invalid value', async () => {
                    element.setProperty('required', { test: 'test' });
                    await page.waitForChanges();
                    expect(dotBinaryText.getAttribute('required')).toBeDefined();
                    expect(dotBinaryButton.getAttribute('required')).toBeDefined();
                });

                it('should not render required attribute', async () => {
                    element.setProperty('required', 'false');
                    await page.waitForChanges();
                    const label = await dotTestUtil.getDotLabel(page);
                    expect(dotBinaryText.getAttribute('required')).toBeNull();
                    expect(dotBinaryButton.getAttribute('required')).toBeNull();
                    expect(label.getAttribute('required')).toBeNull();
                });

                it('should render required attribute for the dot-label', async () => {
                    element.setProperty('required', 'true');
                    await page.waitForChanges();
                    const label = await dotTestUtil.getDotLabel(page);
                    expect(label.getAttribute('required')).toBeDefined();
                });
            });

            describe('requiredMessage', () => {
                it('should show default value of requiredMessage', async () => {
                    element.setProperty('required', 'true');
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: null, errorType: DotBinaryMessageError.REQUIRED }
                    });
                    await page.waitForChanges();
                    expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                        'This field is required'
                    );
                });

                it('should show requiredMessage', async () => {
                    element.setProperty('required', 'true');
                    element.setProperty('requiredMessage', 'Test');
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: null, errorType: DotBinaryMessageError.REQUIRED }
                    });
                    await page.waitForChanges();
                    expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe('Test');
                });

                it('should not render requiredMessage', async () => {
                    await page.waitForChanges();
                    expect(await dotTestUtil.getErrorMessage(page)).toBe(null);
                });

                it('should not render and not break with with invalid value', async () => {
                    element.setProperty('required', 'true');
                    element.setProperty('requiredMessage', { test: 'hi' });
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: null, errorType: '' }
                    });
                    await page.waitForChanges();
                    expect(await dotTestUtil.getErrorMessage(page)).toBeNull();
                });
            });

            describe('validationMessage', () => {
                it('should show default value of validationMessage', async () => {
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: '', errorType: DotBinaryMessageError.INVALID }
                    });
                    await page.waitForChanges();
                    expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                        "The field doesn't comply with the specified format"
                    );
                });

                it('should render validationMessage', async () => {
                    element.setProperty('validationMessage', 'Test');
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: 'test.png', errorType: DotBinaryMessageError.INVALID }
                    });
                    await page.waitForChanges();
                    expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe('Test');
                });

                it('should not render validationMessage whe value is valid', async () => {
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: 'test.png', errorType: '' }
                    });
                    await page.waitForChanges();
                    expect(await dotTestUtil.getErrorMessage(page)).toBeNull();
                });
            });

            describe('URLValidationMessage', () => {
                it('should show default value of URLValidationMessage', async () => {
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: '', errorType: DotBinaryMessageError.URLINVALID }
                    });
                    await page.waitForChanges();
                    expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                        'The specified URL is not valid'
                    );
                });

                it('should render validationMessage', async () => {
                    element.setProperty('URLValidationMessage', 'Test');
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: 'test.png', errorType: DotBinaryMessageError.URLINVALID }
                    });
                    await page.waitForChanges();
                    expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe('Test');
                });

                it('should not render validationMessage whe value is valid', async () => {
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: 'test.png', errorType: '' }
                    });
                    await page.waitForChanges();
                    expect(await dotTestUtil.getErrorMessage(page)).toBeNull();
                });
            });

            describe('disabled', () => {
                it('should render disabled attribute', async () => {
                    element.setProperty('disabled', 'true');
                    await page.waitForChanges();
                    expect(dotBinaryText.getAttribute('disabled')).toBeDefined();
                    expect(dotBinaryButton.getAttribute('disabled')).toBeDefined();
                });

                it('should not render disabled attribute', async () => {
                    element.setProperty('disabled', 'false');
                    await page.waitForChanges();
                    expect(dotBinaryText.getAttribute('disabled')).toBeNull();
                    expect(dotBinaryButton.getAttribute('disabled')).toBeNull();
                });

                it('should render disabled attribute with invalid value', async () => {
                    element.setProperty('disabled', { test: 'test' });
                    await page.waitForChanges();
                    expect(dotBinaryText.getAttribute('disabled')).toBeDefined();
                    expect(dotBinaryButton.getAttribute('disabled')).toBeDefined();
                });
            });

            describe('accept', () => {
                it('should set accept value correctly', async () => {
                    element.setAttribute('accept', '.pdf,.png,.jpg');
                    await page.waitForChanges();
                    expect(await dotBinaryText.getProperty('accept')).toEqual('.pdf,.png,.jpg');
                    expect(await dotBinaryButton.getProperty('accept')).toEqual('.pdf,.png,.jpg');
                });

                it('should set accept as empty value when not set', async () => {
                    await page.waitForChanges();
                    expect(await dotBinaryText.getProperty('accept')).toEqual('');
                    expect(await dotBinaryButton.getProperty('accept')).toEqual('');
                });
            });

            describe('buttonLabel', () => {
                it('should set default buttonLabel prop in dot-binary-upload-button', async () => {
                    element.setProperty('buttonLabel', 'Browse');
                    await page.waitForChanges();
                    expect(await dotBinaryButton.getProperty('buttonLabel')).toEqual('Browse');
                });

                it('should set buttonLabel prop in dot-binary-upload-button', async () => {
                    element.setProperty('buttonLabel', 'Buscar');
                    await page.waitForChanges();
                    expect(await dotBinaryButton.getProperty('buttonLabel')).toEqual('Buscar');
                });
            });

            describe('previewImageName', () => {
                it('should show the preview component and hide others', async () => {
                    element.setProperty('previewImageName', 'image.png');
                    await page.waitForChanges();

                    dotBinaryText = await page.find('dot-binary-text-field');
                    dotBinaryButton = await page.find('dot-binary-upload-button');
                    dotBinaryPreview = await page.find('dot-binary-file-preview');

                    expect(dotBinaryButton).toBeNull();
                    expect(dotBinaryText).toBeNull();
                    expect(dotBinaryPreview.getAttribute('file-name')).toEqual('image.png');
                });
            });

            describe('previewImageUrl', () => {
                it('should set the attribute correctly on DotBinaryPreview', async () => {
                    element.setProperty('previewImageName', 'image.png');
                    element.setProperty('previewImageUrl', 'url/image.png');
                    await page.waitForChanges();

                    dotBinaryPreview = await page.find('dot-binary-file-preview');

                    expect(dotBinaryPreview.getAttribute('preview-url')).toEqual('url/image.png');
                });
            });
        });

        describe('@Events', () => {
            beforeEach(async () => {
                spyStatusChangeEvent = await page.spyOnEvent('dotStatusChange');
                spyValueChangeEvent = await page.spyOnEvent('dotValueChange');
            });
            describe('drag & drop', () => {
                it('should set dot-dragover adn remove dot-dropped class on dragover', async () => {
                    element.classList.add('dot-dropped');
                    element.triggerEvent('dragover');
                    await page.waitForChanges();
                    expect(element).not.toHaveClass('dot-dropped');
                    expect(element).toHaveClass('dot-dragover');
                });

                it('should remove dot-dropped & dot-dragover class on dragleave', async () => {
                    element.classList.add('dot-dropped', 'dot-dragover');
                    element.triggerEvent('dragleave');
                    await page.waitForChanges();
                    expect(element).not.toHaveClasses(['dot-dropped', 'dot-dragover']);
                });

                it('should not add any class when disable', async () => {
                    element.setAttribute('disabled', true);
                    element.triggerEvent('dragover');
                    await page.waitForChanges();
                    expect(element).not.toHaveClass('dot-dragover');
                });

                // TODO: Need to find a way to Mock drop event correctly.
                xit('should not emit when value is not supported on  drop', async () => {});

                // TODO: Need to find a way to Mock drop event correctly.
                xit('should add dot-dropped and remove dot-dragover class on drop', async () => {
                    element.classList.add('dot-dragover');
                    // element.triggerEvent('drop');
                    await page.waitForChanges();
                    expect(element).not.toHaveClass('dot-dragover');
                    expect(element).toHaveClass('dot-dropped');
                });
            });

            describe('dot-binary-text-field', () => {
                it('should listen to fileChange event and emit status and event change', async () => {
                    dotBinaryText.triggerEvent('fileChange', {
                        detail: { file: 'http://www.test.com/file.pdf', errorType: '' }
                    });
                    await page.waitForChanges();

                    expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        status: {
                            dotPristine: false,
                            dotTouched: true,
                            dotValid: true
                        }
                    });
                    expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        value: 'http://www.test.com/file.pdf'
                    });
                });
            });

            describe('dot-binary-upload-button', () => {
                it('should listen to fileChange event, emit status and event change and set binaryTextField', async () => {
                    dotBinaryButton.triggerEvent('fileChange', {
                        detail: { file: { name: 'test.pdf' }, errorType: '' }
                    });
                    await page.waitForChanges();

                    expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        status: {
                            dotPristine: false,
                            dotTouched: true,
                            dotValid: true
                        }
                    });
                    expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        value: { name: 'test.pdf' }
                    });

                    expect(await dotBinaryText.getProperty('value')).toEqual('test.pdf');
                });
            });

            describe('status and value change', () => {
                it('should emit status, value and clear value on Reset', async () => {
                    await element.callMethod('reset');
                    expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        status: {
                            dotPristine: true,
                            dotTouched: false,
                            dotValid: true
                        }
                    });
                    expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        value: ''
                    });
                    expect(await dotBinaryText.getProperty('value')).toEqual('');
                    expect(await element.getProperty('errorMessage')).toEqual('');
                });

                it('should emit status, value and clear value on clearValue', async () => {
                    await element.callMethod('clearValue');
                    expect(spyStatusChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        status: {
                            dotPristine: false,
                            dotTouched: true,
                            dotValid: true
                        }
                    });
                    expect(spyValueChangeEvent).toHaveReceivedEventDetail({
                        name: '',
                        value: null
                    });
                    expect(await dotBinaryText.getProperty('value')).toEqual('');
                });
            });

            describe('clearValue', () => {
                it('should display required message clear preview data on clearValue', async () => {
                    element.setProperty('previewImageName', 'test.png');
                    element.setProperty('previewImageUrl', 'url/test.png');
                    element.setProperty('required', true);
                    await element.callMethod('clearValue');
                    await page.waitForChanges();

                    expect((await dotTestUtil.getErrorMessage(page)).innerText).toBe(
                        'This field is required'
                    );
                    expect(await dotBinaryText.getProperty('value')).toEqual('');
                    expect(element.getAttribute('preview-image-name')).toEqual('');
                    expect(element.getAttribute('preview-image-url')).toEqual('');
                });
            });
        });
    });
});
