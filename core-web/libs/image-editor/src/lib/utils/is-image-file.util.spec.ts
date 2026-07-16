import { isImageFile } from './is-image-file.util';

describe('isImageFile', () => {
    it('returns false for null or undefined metadata', () => {
        expect(isImageFile(null)).toBe(false);
        expect(isImageFile(undefined)).toBe(false);
    });

    it('returns false for empty metadata', () => {
        expect(isImageFile({})).toBe(false);
    });

    it('trusts the authoritative isImage flag', () => {
        expect(isImageFile({ isImage: true })).toBe(true);
        expect(isImageFile({ isImage: false, contentType: 'application/pdf', name: 'a.pdf' })).toBe(
            false
        );
    });

    it('falls back to an image/* content type when isImage is absent', () => {
        expect(isImageFile({ contentType: 'image/png' })).toBe(true);
        expect(isImageFile({ contentType: 'IMAGE/PNG' })).toBe(true);
        expect(isImageFile({ contentType: 'application/pdf' })).toBe(false);
    });

    it('does not infer image-ness from the file name alone', () => {
        // Extension sniffing is intentionally avoided; name is not a trusted signal.
        expect(isImageFile({ name: 'photo.png' })).toBe(false);
    });

    it('detects image metadata coming from a referenced dotAsset', () => {
        // Shape of a dotAsset's assetMetaData resolved via getFileMetadata upstream.
        expect(isImageFile({ isImage: true, contentType: 'image/jpeg', name: 'image 2.jpg' })).toBe(
            true
        );
    });
});
