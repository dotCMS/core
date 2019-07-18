import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

const IMAGE_FILE_MOCK = {
    id: 'temp_09ef3de32b',
    mimeType: 'image/jpeg',
    referenceUrl: '/dA/temp_09ef3de32b/tmp/002.jpg',
    thumbnailUrl: 'https://upload.002.jpg',
    fileName: '002.jpg',
    folder: '',
    image: true,
    length: 1606323
};

const PDF_FILE_MOCK = {
    id: 'temp_09ef3de32b',
    mimeType: 'image/jpeg',
    referenceUrl: '/dA/temp_09ef3de32b/tmp/002.jpg',
    thumbnailUrl: 'https://upload.002.pdf',
    fileName: '002.test.pdf',
    folder: '',
    image: false,
    length: 1606323
};

describe('dot-binary-file-preview', () => {
    let page: E2EPage;
    let element: E2EElement;
    let spyDeleteEvent: EventSpy;

    beforeEach(async () => {
        page = await newE2EPage({
            html: `<dot-binary-file-preview></dot-binary-file-preview>`
        });

        element = await page.find('dot-binary-file-preview');
    });

    describe('@Props', () => {
        describe('file', () => {
            it('should not display if file is empty', async () => {
                await page.waitForChanges();
                expect(element.innerHTML).toEqual('');
            });

            it('should display correct elements when is an image', async () => {
                element.setProperty('file', IMAGE_FILE_MOCK);
                await page.waitForChanges();
                const imageTag = page.find('img');
                const fileName = (await page.find('.dot-file-preview__name')).innerText;

                expect(imageTag).toBeDefined();
                expect(fileName).toEqual(IMAGE_FILE_MOCK.fileName);
            });

            it('should display correct elements when is a file', async () => {
                element.setProperty('file', PDF_FILE_MOCK);
                await page.waitForChanges();
                const fileExtention = (await page.find('.dot-file-preview__extension span'))
                    .innerText;
                const fileName = (await page.find('.dot-file-preview__name')).innerText;

                expect(fileExtention).toEqual('.pdf');
                expect(fileName).toEqual(PDF_FILE_MOCK.fileName);
            });
        });

        describe('deleteLabel', () => {
            it('should render default value correctly', async () => {
                element.setProperty('file', PDF_FILE_MOCK);
                await page.waitForChanges();
                const buttonText = (await page.find('button')).innerText;

                expect(buttonText).toBe('Delete');
            });

            it('should render value correctly', async () => {
                element.setProperty('file', PDF_FILE_MOCK);
                element.setProperty('deleteLabel', 'Test');

                await page.waitForChanges();
                const buttonText = (await page.find('button')).innerText;

                expect(buttonText).toBe('Test');
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            element.setProperty('file', PDF_FILE_MOCK);
            spyDeleteEvent = await page.spyOnEvent('delete');
            await page.waitForChanges();
        });

        describe('delete', () => {
            it('should emit status, value and clear value on Reset', async () => {
                const button = await page.find('button');
                button.click();
                await page.waitForChanges();
                expect(spyDeleteEvent).toHaveReceivedEventDetail(PDF_FILE_MOCK);
            });
        });
    });
});
