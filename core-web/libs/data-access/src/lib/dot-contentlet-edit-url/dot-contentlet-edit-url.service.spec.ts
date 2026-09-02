import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    FeaturedFlags
} from '@dotcms/dotcms-models';

import { DotContentletEditUrlService } from './dot-contentlet-edit-url.service';

import { DotContentTypeService } from '../dot-content-type/dot-content-type.service';

const HTMLPAGE_CONTENTLET = {
    inode: 'page-inode',
    identifier: 'page-id',
    contentType: 'htmlpageasset',
    baseType: DotCMSBaseTypesContentTypes.HTMLPAGE,
    url: '/about-us',
    languageId: 1
} as unknown as DotCMSContentlet;

const REGULAR_CONTENTLET = {
    inode: 'content-inode',
    identifier: 'content-id',
    contentType: 'Blog',
    baseType: DotCMSBaseTypesContentTypes.CONTENT
} as unknown as DotCMSContentlet;

describe('DotContentletEditUrlService', () => {
    let spectator: SpectatorService<DotContentletEditUrlService>;
    let getContentTypeSpy: jest.Mock;

    const createService = createServiceFactory({
        service: DotContentletEditUrlService,
        providers: [
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        getContentTypeSpy = spectator.inject(DotContentTypeService).getContentType as jest.Mock;
        // mockProvider's jest.fn() is captured once at factory definition, so it
        // accumulates calls across spectator instances. Clear it per test so call-count
        // assertions reflect only the test under inspection.
        getContentTypeSpy.mockReset();
    });

    describe('HTMLPAGE branch', () => {
        it('returns the page-editor URL without hitting the content-type service', (done) => {
            spectator.service.resolveEditUrl(HTMLPAGE_CONTENTLET).subscribe((url) => {
                expect(url).toContain('/dotAdmin/#/edit-page/content?');
                expect(url).toContain('url=%2Fabout-us');
                expect(url).toContain('language_id=1');
                expect(url).toContain('mId=edit');
                expect(getContentTypeSpy).not.toHaveBeenCalled();
                done();
            });
        });

        it('falls back to the inode-based resolver when an HTMLPAGE has no url/urlMap', (done) => {
            getContentTypeSpy.mockReturnValue(of({ metadata: {} }));
            const malformedPage = {
                ...HTMLPAGE_CONTENTLET,
                url: undefined,
                urlMap: undefined
            } as unknown as DotCMSContentlet;

            spectator.service.resolveEditUrl(malformedPage).subscribe((url) => {
                expect(url).toBe('/dotAdmin/#/c/content/page-inode');
                done();
            });
        });
    });

    describe('Contentlet branch', () => {
        it('returns the new editor URL when CONTENT_EDITOR2_ENABLED is true', (done) => {
            getContentTypeSpy.mockReturnValue(
                of({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
                })
            );

            spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe((url) => {
                expect(url).toBe('/dotAdmin/#/content/content-inode');
                done();
            });
        });

        it('returns the legacy editor URL when CONTENT_EDITOR2_ENABLED is missing', (done) => {
            getContentTypeSpy.mockReturnValue(of({ metadata: {} }));

            spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe((url) => {
                expect(url).toBe('/dotAdmin/#/c/content/content-inode');
                done();
            });
        });

        it('returns the legacy editor URL when getContentType errors (graceful fallback)', (done) => {
            getContentTypeSpy.mockReturnValue(throwError(() => new Error('boom')));

            spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe((url) => {
                expect(url).toBe('/dotAdmin/#/c/content/content-inode');
                done();
            });
        });
    });

    describe('Caching', () => {
        it('only hits getContentType once for repeated resolutions of the same content type', (done) => {
            getContentTypeSpy.mockReturnValue(
                of({
                    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
                })
            );

            spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe(() => {
                spectator.service
                    .resolveEditUrl({ ...REGULAR_CONTENTLET, inode: 'another-inode' })
                    .subscribe((url) => {
                        expect(url).toBe('/dotAdmin/#/content/another-inode');
                        expect(getContentTypeSpy).toHaveBeenCalledTimes(1);
                        done();
                    });
            });
        });

        it('caches independently per content type', (done) => {
            getContentTypeSpy.mockImplementation((ct: string) =>
                of({
                    metadata: {
                        [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: ct === 'Modern'
                    }
                })
            );

            spectator.service
                .resolveEditUrl({ ...REGULAR_CONTENTLET, contentType: 'Modern' })
                .subscribe((modernUrl) => {
                    expect(modernUrl).toBe('/dotAdmin/#/content/content-inode');
                    spectator.service
                        .resolveEditUrl({ ...REGULAR_CONTENTLET, contentType: 'Legacy' })
                        .subscribe((legacyUrl) => {
                            expect(legacyUrl).toBe('/dotAdmin/#/c/content/content-inode');
                            expect(getContentTypeSpy).toHaveBeenCalledTimes(2);
                            done();
                        });
                });
        });
    });
});
