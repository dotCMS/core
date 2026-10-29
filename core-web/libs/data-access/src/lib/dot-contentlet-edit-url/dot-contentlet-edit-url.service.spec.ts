import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

import {
    DotCMSBaseTypesContentTypes,
    DotCMSContentlet,
    DotCMSContentType,
    FeaturedFlags
} from '@dotcms/dotcms-models';
import { createFakeContentType } from '@dotcms/utils-testing';

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

/** A content type that opted into the new editor. */
const NEW_EDITOR_TYPE: DotCMSContentType = createFakeContentType({
    variable: 'Blog',
    metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
});

/** A content type on the legacy editor. */
const LEGACY_TYPE: DotCMSContentType = createFakeContentType({
    variable: 'Banner',
    metadata: {}
});

describe('DotContentletEditUrlService', () => {
    let spectator: SpectatorService<DotContentletEditUrlService>;
    let getContentTypeSpy: Mock;

    const createService = createServiceFactory({
        service: DotContentletEditUrlService,
        providers: [
            mockProvider(DotContentTypeService, {
                getContentType: vi.fn()
            })
        ]
    });

    beforeEach(() => {
        spectator = createService();
        getContentTypeSpy = spectator.inject(DotContentTypeService).getContentType as Mock;
        // mockProvider's vi.fn() is captured once at factory definition, so it
        // accumulates calls across spectator instances. Clear it per test so call-count
        // assertions reflect only the test under inspection.
        getContentTypeSpy.mockReset();
    });

    /**
     * The one decision point for "which editor opens this content type" (#38303, FR-003). The
     * opener in `@dotcms/edit-content` calls this; `resolveEditUrl` is built on it.
     */
    describe('resolveEditor', () => {
        it('chooses the new editor when the content type opted in, and hands the type back', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(of(NEW_EDITOR_TYPE));

                spectator.service.resolveEditor('Blog').subscribe((choice) => {
                    expect(choice).toEqual({ editor: 'new', contentType: NEW_EDITOR_TYPE });
                    expect(getContentTypeSpy).toHaveBeenCalledWith('Blog');
                    done();
                });
            }));

        it('chooses the legacy editor when the content type did not opt in', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(of(LEGACY_TYPE));

                spectator.service.resolveEditor('Banner').subscribe((choice) => {
                    expect(choice).toEqual({ editor: 'legacy', contentType: LEGACY_TYPE });
                    done();
                });
            }));

        it('falls back to the legacy editor, with no content type, when the lookup fails', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(throwError(() => new Error('boom')));

                spectator.service.resolveEditor('Banner').subscribe((choice) => {
                    expect(choice).toEqual({ editor: 'legacy', contentType: null });
                    done();
                });
            }));

        it('asks the server once per content type', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(of(NEW_EDITOR_TYPE));

                spectator.service.resolveEditor('Blog').subscribe(() => {
                    spectator.service.resolveEditor('Blog').subscribe((choice) => {
                        expect(choice.editor).toBe('new');
                        expect(getContentTypeSpy).toHaveBeenCalledTimes(1);
                        done();
                    });
                });
            }));

        it('does not cache a failed lookup, so the next call asks again', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValueOnce(throwError(() => new Error('boom')));
                getContentTypeSpy.mockReturnValueOnce(of(NEW_EDITOR_TYPE));

                spectator.service.resolveEditor('Blog').subscribe((first) => {
                    expect(first.editor).toBe('legacy');

                    spectator.service.resolveEditor('Blog').subscribe((second) => {
                        expect(second.editor).toBe('new');
                        expect(getContentTypeSpy).toHaveBeenCalledTimes(2);
                        done();
                    });
                });
            }));
    });

    describe('HTMLPAGE branch', () => {
        it('returns the page-editor URL without hitting the content-type service', () =>
            new Promise<void>((done) => {
                spectator.service.resolveEditUrl(HTMLPAGE_CONTENTLET).subscribe((url) => {
                    expect(url).toContain('/dotAdmin/#/edit-page/content?');
                    expect(url).toContain('url=%2Fabout-us');
                    expect(url).toContain('language_id=1');
                    expect(url).toContain('mId=edit');
                    expect(getContentTypeSpy).not.toHaveBeenCalled();
                    done();
                });
            }));

        it('falls back to the inode-based resolver when an HTMLPAGE has no url/urlMap', () =>
            new Promise<void>((done) => {
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
            }));
    });

    describe('Contentlet branch', () => {
        it('returns the new editor URL when CONTENT_EDITOR2_ENABLED is true', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(
                    of({
                        metadata: { [FeaturedFlags.FEATURE_FLAG_CONTENT_EDITOR2_ENABLED]: true }
                    })
                );

                spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe((url) => {
                    expect(url).toBe('/dotAdmin/#/content/content-inode');
                    done();
                });
            }));

        it('returns the legacy editor URL when CONTENT_EDITOR2_ENABLED is missing', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(of({ metadata: {} }));

                spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe((url) => {
                    expect(url).toBe('/dotAdmin/#/c/content/content-inode');
                    done();
                });
            }));

        it('returns the legacy editor URL when getContentType errors (graceful fallback)', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(throwError(() => new Error('boom')));

                spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe((url) => {
                    expect(url).toBe('/dotAdmin/#/c/content/content-inode');
                    done();
                });
            }));
    });

    describe('Caching', () => {
        it('only hits getContentType once for repeated resolutions of the same content type', () =>
            new Promise<void>((done) => {
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
            }));

        it('caches independently per content type', () =>
            new Promise<void>((done) => {
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
            }));

        it('shares one cache between resolveEditor and resolveEditUrl', () =>
            new Promise<void>((done) => {
                getContentTypeSpy.mockReturnValue(of(NEW_EDITOR_TYPE));

                spectator.service.resolveEditor('Blog').subscribe(() => {
                    spectator.service.resolveEditUrl(REGULAR_CONTENTLET).subscribe((url) => {
                        expect(url).toBe('/dotAdmin/#/content/content-inode');
                        expect(getContentTypeSpy).toHaveBeenCalledTimes(1);
                        done();
                    });
                });
            }));
    });
});
