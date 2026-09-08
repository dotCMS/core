import { describe, expect, it } from '@jest/globals';

import {
    buildUploadAccept,
    isUploadAllowed,
    resolveUploadRestrictionLabel
} from './upload-restriction';

/** `new File()` defaults `type` to `''`, so pass it explicitly wherever the type is the subject. */
const fileOfType = (type: string, name = 'asset.bin') => new File([''], name, { type });

/** Stands in for `DotMessageService.get` — the module takes the lookup as a parameter. */
const translate = (key: string) =>
    ({
        'dot.asset.picker.upload.types.image': 'images',
        'dot.asset.picker.upload.types.video': 'video files',
        'dot.asset.picker.upload.types.audio': 'audio files'
    })[key] ?? key;

describe('upload-restriction', () => {
    describe('isUploadAllowed', () => {
        describe('when there is no restriction', () => {
            // The File field and the browse entry point both arrive here. Absence is the
            // unrestricted state — a reader that defaults to refusing breaks them silently.
            it('should allow anything when mimeTypes is undefined', () => {
                expect(isUploadAllowed(fileOfType('application/pdf'), undefined)).toBe(true);
            });

            it('should allow anything when mimeTypes is empty', () => {
                expect(isUploadAllowed(fileOfType('application/pdf'), [])).toBe(true);
            });
        });

        describe('when the browser reports no type', () => {
            // AC-010: an unclassifiable file is allowed through and the server stays the
            // authority. Refusing it would block a legitimate image behind a message saying only
            // images are allowed.
            it('should allow a file whose type is empty', () => {
                expect(isUploadAllowed(fileOfType(''), ['image/*'])).toBe(true);
            });
        });

        describe('wildcard patterns', () => {
            it('should allow a file in the restricted family', () => {
                expect(isUploadAllowed(fileOfType('image/png'), ['image/*'])).toBe(true);
            });

            it('should reject a file outside the restricted family', () => {
                expect(isUploadAllowed(fileOfType('application/pdf'), ['image/*'])).toBe(false);
            });

            it('should reject a media file of the wrong family', () => {
                expect(isUploadAllowed(fileOfType('audio/mpeg'), ['video/*'])).toBe(false);
            });

            it('should not match on a family prefix', () => {
                // `x-image/foo` contains `image/`; a substring match would wrongly allow it.
                expect(isUploadAllowed(fileOfType('x-image/foo'), ['image/*'])).toBe(false);
            });
        });

        describe('exact patterns', () => {
            // Only reachable through `browse`, whose caller supplies its own list.
            it('should allow an exact match', () => {
                expect(isUploadAllowed(fileOfType('application/pdf'), ['application/pdf'])).toBe(
                    true
                );
            });

            it('should reject a different type', () => {
                expect(isUploadAllowed(fileOfType('application/zip'), ['application/pdf'])).toBe(
                    false
                );
            });

            it('should compare case-insensitively', () => {
                expect(isUploadAllowed(fileOfType('IMAGE/PNG'), ['image/*'])).toBe(true);
                expect(isUploadAllowed(fileOfType('application/PDF'), ['APPLICATION/pdf'])).toBe(
                    true
                );
            });
        });

        describe('several patterns', () => {
            it('should allow a file matching any of them', () => {
                expect(isUploadAllowed(fileOfType('video/mp4'), ['image/*', 'video/*'])).toBe(true);
            });

            it('should reject a file matching none of them', () => {
                expect(isUploadAllowed(fileOfType('application/pdf'), ['image/*', 'video/*'])).toBe(
                    false
                );
            });
        });

        describe('the filename', () => {
            // AC-002: the restriction comes from `config.mimeTypes` alone. An extension fallback
            // would be a second, hand-maintained list of types.
            it('should never be consulted — a mislabelled name does not rescue a rejected type', () => {
                expect(
                    isUploadAllowed(fileOfType('application/pdf', 'photo.png'), ['image/*'])
                ).toBe(false);
            });

            it('should never be consulted — a wrong extension does not condemn an allowed type', () => {
                expect(isUploadAllowed(fileOfType('image/png', 'report.pdf'), ['image/*'])).toBe(
                    true
                );
            });
        });
    });

    describe('buildUploadAccept', () => {
        it('should pass a single pattern through verbatim', () => {
            // `accept` takes the same `type/*` syntax the browse filter already uses, so no
            // translation layer is needed.
            expect(buildUploadAccept(['image/*'])).toBe('image/*');
        });

        it('should join several patterns with a comma', () => {
            expect(buildUploadAccept(['image/*', 'video/*'])).toBe('image/*,video/*');
        });

        it('should return null when there is no restriction', () => {
            // Null, not empty string: the binding has to *remove* the attribute. An empty `accept`
            // is not the same thing to the browser as no `accept`.
            expect(buildUploadAccept(undefined)).toBeNull();
            expect(buildUploadAccept([])).toBeNull();
        });
    });

    describe('resolveUploadRestrictionLabel', () => {
        it('should resolve the label for each known media family', () => {
            expect(resolveUploadRestrictionLabel(['image/*'], translate)).toBe('images');
            expect(resolveUploadRestrictionLabel(['video/*'], translate)).toBe('video files');
            expect(resolveUploadRestrictionLabel(['audio/*'], translate)).toBe('audio files');
        });

        it('should return undefined when there is no restriction', () => {
            expect(resolveUploadRestrictionLabel(undefined, translate)).toBeUndefined();
            expect(resolveUploadRestrictionLabel([], translate)).toBeUndefined();
        });

        it('should fall back to the raw patterns for an unknown family', () => {
            // A `browse` caller may pass anything. The message has to stay correct rather than
            // rendering "Only  can be uploaded here."
            expect(resolveUploadRestrictionLabel(['application/pdf'], translate)).toBe(
                'application/pdf'
            );
        });

        it('should join several resolved families', () => {
            expect(resolveUploadRestrictionLabel(['image/*', 'video/*'], translate)).toBe(
                'images, video files'
            );
        });

        it('should not repeat a family listed twice', () => {
            expect(resolveUploadRestrictionLabel(['image/png', 'image/jpeg'], translate)).toBe(
                'images'
            );
        });
    });
});
