import * as dotUtils from '@shared/dot-utils';

describe('Dot Utils', () => {
    it('should return anchor with the correct values', () => {
        const blobMock = new Blob(['']);
        const fileName = 'doc.txt';
        spyOn(window.URL, 'createObjectURL');
        const anchor = dotUtils.getDownloadLink(blobMock, fileName);

        expect(anchor.download).toEqual(fileName);
        expect(window.URL.createObjectURL).toHaveBeenCalledWith(blobMock);
    });
});
