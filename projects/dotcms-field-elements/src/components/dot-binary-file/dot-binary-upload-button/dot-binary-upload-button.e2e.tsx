import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

describe('dot-binary-upload-button', () => {
    let page: E2EPage;
    let element: E2EElement;
    let input: E2EElement;
    let button: E2EElement;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-binary-upload-button></dot-binary-upload-button>`
        });

        element = await page.find('dot-binary-upload-button');
        input = await page.find('input');
        button = await page.find('button');
    });

    describe('@Props', () => {
        describe('name', () => {
            it('should render with valid id name', async () => {
                element.setProperty('name', 'test');
                await page.waitForChanges();
                expect(input.getAttribute('id')).toBe('dot-test');
            });

            it('should render when is a unexpected value', async () => {
                element.setProperty('name', { input: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('id')).toBe('dot-object-object');
            });
        });

        describe('accept', () => {
            it('should render with accept', async () => {
                element.setProperty('accept', 'test');
                await page.waitForChanges();
                expect(input.getAttribute('accept')).toBe('test');
            });
        });

        describe('required', () => {
            it('should not render required attribute by default', () => {
                expect(input.getAttribute('required')).toBeNull();
            });

            it('should render required attribute with invalid value', async () => {
                element.setProperty('required', { test: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('required')).toBeDefined();
            });

            it('should not render required attribute', async () => {
                element.setProperty('required', 'false');
                await page.waitForChanges();
                expect(input.getAttribute('required')).toBeNull();
            });
        });

        describe('disabled', () => {
            it('should not render disabled attribute by default', () => {
                expect(input.getAttribute('disabled')).toBeNull();
            });

            it('should render disabled attribute', async () => {
                element.setProperty('disabled', 'true');
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeDefined();
            });

            it('should not render disabled attribute', async () => {
                element.setProperty('disabled', 'false');
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeNull();
            });

            it('should render disabled attribute with invalid value', async () => {
                element.setProperty('disabled', { test: 'test' });
                await page.waitForChanges();
                expect(input.getAttribute('disabled')).toBeDefined();
            });
        });

        describe('buttonLabel', () => {
            it('should render label correctly', async () => {
                element.setProperty('buttonLabel', 'test');
                await page.waitForChanges();
                expect(button.innerText).toBe('test');
            });

            it('should render when is a unexpected value', async () => {
                element.setProperty('buttonLabel', { input: 'test' });
                await page.waitForChanges();
                expect(button.innerText).toBe('[object Object]');
            });
        });
    });

    // TODO: Find a a way to Mock a input.files attribute.
    xdescribe('@Events', () => {
        let spyFileChangeEvent: EventSpy;

        beforeEach(async () => {
            spyFileChangeEvent = await page.spyOnEvent('fileChange');
        });

        it('should emit the selected file', async () => {});

        it('should emit null and invalid file error', async () => {
            element.setProperty('accept', ['.pdf']);
        });
    });
});
