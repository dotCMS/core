import { Location } from '@angular/common';
import { TestBed } from '@angular/core/testing';

import { DotLegacyUrlBuilderService } from './dot-legacy-url-builder.service';

describe('DotLegacyUrlBuilderService', () => {
    let service: DotLegacyUrlBuilderService;
    let location: jasmine.SpyObj<Location>;

    beforeEach(() => {
        const locationSpy = jasmine.createSpyObj('Location', ['path']);

        TestBed.configureTestingModule({
            providers: [DotLegacyUrlBuilderService, { provide: Location, useValue: locationSpy }]
        });

        service = TestBed.inject(DotLegacyUrlBuilderService);
        location = TestBed.inject(Location) as jasmine.SpyObj<Location>;
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('buildCreateContentUrl', () => {
        beforeEach(() => {
            location.path.and.returnValue('/current/path');
            // Mock window.location.origin
            Object.defineProperty(window, 'location', {
                value: {
                    origin: 'https://localhost:8080'
                },
                writable: true
            });
        });

        it('should build correct URL for creating new content', () => {
            const params = {
                contentTypeVariable: 'blog',
                languageId: '1'
            };

            const result = service.buildCreateContentUrl(params);
            const url = new URL(result);

            expect(url.origin).toBe('https://localhost:8080');
            expect(url.searchParams.get('p_p_state')).toBe('maximized');
            expect(url.searchParams.get('struts_action')).toBe('/ext/contentlet/edit_contentlet');
            expect(url.searchParams.get('cmd')).toBe('new');
            expect(url.searchParams.get('referer')).toBe('https://localhost:8080/current/path');
            expect(url.searchParams.get('inode')).toBe('');
            expect(url.searchParams.get('selectedStructure')).toBe('blog');
            expect(url.searchParams.get('lang')).toBe('1');
        });

        it('should use custom referer when provided', () => {
            const params = {
                contentTypeVariable: 'blog',
                referer: 'https://custom.referer.com/path'
            };

            const result = service.buildCreateContentUrl(params);
            const url = new URL(result);

            expect(url.searchParams.get('referer')).toBe('https://custom.referer.com/path');
        });

        it('should use custom inode when provided', () => {
            const params = {
                contentTypeVariable: 'blog',
                inode: 'custom-inode-123'
            };

            const result = service.buildCreateContentUrl(params);
            const url = new URL(result);

            expect(url.searchParams.get('inode')).toBe('custom-inode-123');
        });

        it('should handle string languageId', () => {
            const params = {
                contentTypeVariable: 'blog',
                languageId: 'en'
            };

            const result = service.buildCreateContentUrl(params);
            const url = new URL(result);

            expect(url.searchParams.get('lang')).toBe('en');
        });

        it('should handle numeric languageId', () => {
            const params = {
                contentTypeVariable: 'blog',
                languageId: 42
            };

            const result = service.buildCreateContentUrl(params);
            const url = new URL(result);

            expect(url.searchParams.get('lang')).toBe('42');
        });

        it('should not include lang parameter when languageId is undefined', () => {
            const params = {
                contentTypeVariable: 'blog'
            };

            const result = service.buildCreateContentUrl(params);
            const url = new URL(result);

            expect(url.searchParams.has('lang')).toBe(false);
        });
    });

    describe('buildEditContentUrl', () => {
        beforeEach(() => {
            location.path.and.returnValue('/current/path');
            Object.defineProperty(window, 'location', {
                value: {
                    origin: 'https://localhost:8080'
                },
                writable: true
            });
        });

        it('should build correct URL for editing existing content', () => {
            const params = {
                contentTypeVariable: 'blog',
                inode: 'content-inode-123',
                languageId: '1'
            };

            const result = service.buildEditContentUrl(params);
            const url = new URL(result);

            expect(url.origin).toBe('https://localhost:8080');
            expect(url.searchParams.get('p_p_state')).toBe('maximized');
            expect(url.searchParams.get('struts_action')).toBe('/ext/contentlet/edit_contentlet');
            expect(url.searchParams.get('cmd')).toBe('edit');
            expect(url.searchParams.get('inode')).toBe('content-inode-123');
            expect(url.searchParams.get('selectedStructure')).toBe('blog');
            expect(url.searchParams.get('lang')).toBe('1');
        });
    });
});
