// import { E2EPage, newE2EPage } from '@stencil/core/testing';

// const mockData = {
//     dropFilesText :'Drag and Drop or paste a file',
//     browserButtonText: 'Browse',
//     writeCodeButtonText: 'Write Code',
//     cancelButtonText: 'Cancel',
//     assets: [{
//         fileName: "test.png",
//         folder: '',
//         id: 'test',
//         image: true,
//         length: null,
//         mimeType: '',
//         referenceUrl: '',
//         thumbnailUrl: '',
//     }]
// }

// fdescribe('dot-file-upload', () => {
//     let page: E2EPage;

//     beforeEach(async () => {
//         page = await newE2EPage({
//             html: `<dot-file-upload></dot-file-upload>`,
//         });
//         const element = await page.find('dot-file-upload');
//         element.setProperty('dropFilesText', mockData.dropFilesText)
//         element.setProperty('browserButtonText', mockData.browserButtonText)
//         element.setProperty('cancelButtonText', mockData.cancelButtonText)
//         element.setProperty('writeCodeButtonText', mockData.writeCodeButtonText)
//     });

//   it('renders', async () => {

//     const element = await page.find('dot-file-upload');
//     expect(element).toHaveClass('hydrated');
//   });

//   describe('@Elements', () => {
//     it('should show file upload', async () => {
//         const [browseButton, writeCodeButton] = await page.findAll('.dot-file-upload >>> button');
//         const dropFilesText = await page.find('dot-file-upload >>> p');
//         const uploadIcon = await page.find('dot-file-upload >>> mwc-icon');
//         expect(browseButton.innerHTML).toBe(mockData.browserButtonText);
//         expect(writeCodeButton.innerHTML).toBe(mockData.writeCodeButtonText);
//         expect(dropFilesText.innerHTML).toBe(mockData.dropFilesText);
//         expect(uploadIcon.innerHTML).toBe('<mwc-icon>insert_drive_file</mwc-icon>');
//     })

//   })
// });
