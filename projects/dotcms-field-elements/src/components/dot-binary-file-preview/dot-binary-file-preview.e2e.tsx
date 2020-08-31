import { E2EElement, E2EPage, newE2EPage } from '@stencil/core/testing';
import { EventSpy } from '@stencil/core/dist/declarations';

const FILE_MOCK = {
    id: 'temp_09ef3de32b',
    mimeType: 'image/jpeg',
    referenceUrl: '/dA/temp_09ef3de32b/tmp/002.jpg',
    thumbnailUrl: 'https://upload.002.jpg',
    fileName: '002.jpg',
    folder: '',
    image: true,
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
        describe('fileName', () => {
            it('should not display if fileName is empty', async () => {
                await page.waitForChanges();
                expect(element.innerHTML).toEqual('');
            });

            it('should display correct elements when is a file', async () => {
                element.setProperty('fileName', FILE_MOCK.fileName);
                await page.waitForChanges();
                const fileExtention = (await page.find('.dot-file-preview__extension span'))
                    .innerText;
                const fileName = (await page.find('.dot-file-preview__name')).innerText;

                expect(fileExtention).toEqual('.jpg');
                expect(fileName).toEqual(FILE_MOCK.fileName);
            });
        });

        describe('previewUrl', () => {
            it('should not display image tag if previewUrl is empty', async () => {
                element.setProperty('fileName', FILE_MOCK.fileName);
                await page.waitForChanges();
                const imageElement = await page.find('img');

                expect(imageElement).toBeNull();
            });

            it('should display preview image when previewUrl is set', async () => {
                element.setProperty('fileName', FILE_MOCK.fileName);
                element.setProperty('previewUrl', FILE_MOCK.thumbnailUrl);
                await page.waitForChanges();
                const imageSrc = (await page.find('img')).getAttribute('src');
                const fileName = (await page.find('.dot-file-preview__name')).innerText;

                expect(imageSrc).toEqual(FILE_MOCK.thumbnailUrl);
                expect(fileName).toEqual(FILE_MOCK.fileName);
            });
        });

        describe('deleteLabel', () => {
            it('should render default value correctly with the button type', async () => {
                element.setProperty('fileName', FILE_MOCK.fileName);
                await page.waitForChanges();
                const button= (await page.find('button'));

                expect(button.innerText).toBe('Delete');
                expect(button.getAttribute('type')).toEqual('button');
            });

            it('should render value correctly', async () => {
                element.setProperty('fileName', FILE_MOCK.fileName);
                element.setProperty('deleteLabel', 'Test');

                await page.waitForChanges();
                const buttonText = (await page.find('button')).innerText;

                expect(buttonText).toBe('Test');
            });
        });
    });

    describe('@Events', () => {
        beforeEach(async () => {
            element.setProperty('fileName', FILE_MOCK.fileName);
            spyDeleteEvent = await page.spyOnEvent('delete');
            await page.waitForChanges();
        });

        describe('delete', () => {
            it('should emit status, value and clear value on Reset', async () => {
                element.setProperty('fileName', FILE_MOCK.fileName);
                element.setProperty('previewUrl', FILE_MOCK.thumbnailUrl);
                const button = await page.find('button');
                button.click();
                await page.waitForChanges();
                expect(spyDeleteEvent).toHaveReceivedEvent();
                expect(await element.getProperty('fileName')).toBeNull();
                expect(await element.getProperty('previewUrl')).toBeNull();
            });
        });
    });
});
