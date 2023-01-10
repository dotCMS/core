// import { DotPageRenderState } from '../../../../apps/dotcms-ui/src/app/portlets/dot-edit-page/shared/models';
// import { mockDotRenderedPage } from '../../../../apps/dotcms-ui/src/app/test/dot-page-render.mock';
// import { mockUser } from '../../../../apps/dotcms-ui/src/app/test/login-service.mock';
// import { getDownloadLink } from './dot-utils';
// import { DotPageRender } from '@dotcms/dotcms-models';

// describe('Dot Utils', () => {
//     it('should return anchor with the correct values', () => {
//         const blobMock = new Blob(['']);
//         const fileName = 'doc.txt';
//         spyOn(window.URL, 'createObjectURL');
//         const anchor = getDownloadLink(blobMock, fileName);

//         expect(anchor.download).toEqual(fileName);
//         expect(window.URL.createObjectURL).toHaveBeenCalledWith(blobMock);
//     });

//     it('should return unique URL with host, language and device Ids', () => {
//         const mockRenderedPageState = new DotPageRenderState(
//             mockUser(),
//             new DotPageRender({
//                 ...mockDotRenderedPage(),
//                 viewAs: {
//                     ...mockDotRenderedPage().viewAs,
//                     device: {
//                         identifier: 'abc123',
//                         cssHeight: '800',
//                         cssWidth: '1200',
//                         name: 'custom',
//                         inode: '123zxc'
//                     }
//                 }
//             })
//         );

//         const url = dotUtils.generateDotFavoritePageUrl(mockRenderedPageState);

//         expect(url).toEqual('/an/url/test?&language_id=1&device_id=abc123');
//     });
// });
