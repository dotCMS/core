import * as dotUtils from '@dotcms/utils/lib/dot-utils';

describe('Dot Utils', () => {
    it('should return anchor with the correct values', () => {
        const blobMock = new Blob(['']);
        const fileName = 'doc.txt';

        // Mock URL.createObjectURL for JSDOM
        const mockCreateObjectURL = jest.fn().mockReturnValue('mock-url');
        Object.defineProperty(window.URL, 'createObjectURL', {
            value: mockCreateObjectURL,
            writable: true
        });

        const anchor = dotUtils.getDownloadLink(blobMock, fileName);

        expect(anchor.download).toEqual(fileName);
        expect(mockCreateObjectURL).toHaveBeenCalledWith(blobMock);
        expect(mockCreateObjectURL).toHaveBeenCalledTimes(1);
    });

    it('should return unique URL with host, language and device Ids', () => {
        const url = dotUtils.generateDotFavoritePageUrl({
            languageId: 1,
            pageURI: '/an/url/test',
            deviceInode: '123zxc'
        });

        expect(url).toEqual('/an/url/test?&language_id=1&device_inode=123zxc');
    });
});
