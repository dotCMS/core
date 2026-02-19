import {
    createHttpFactory,
    HttpMethod,
    mockProvider,
    SpectatorHttp,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotContentletCanLock } from '@dotcms/dotcms-models';
import { createFakeContentlet, createFakeLanguage } from '@dotcms/utils-testing';

import { DotContentletService } from './dot-contentlet.service';

import { DotUploadFileService } from '../dot-upload-file/dot-upload-file.service';

describe('DotContentletService', () => {
    let spectator: SpectatorHttp<DotContentletService>;
    let dotUploadFileService: SpyObject<DotUploadFileService>;

    const createHttp = createHttpFactory({
        service: DotContentletService,
        providers: [mockProvider(DotUploadFileService)]
    });

    beforeEach(() => {
        spectator = createHttp();
        dotUploadFileService = spectator.inject(DotUploadFileService);
    });

    it('should bring the contentlet versions by language', () => {
        const mockContentlet1 = createFakeContentlet({ content: 'one' });
        const mockContentlet2 = createFakeContentlet({ content: 'two' });
        const mockContentletVersionsResponse = {
            entity: {
                versions: {
                    en: [mockContentlet1, mockContentlet2]
                }
            }
        };

        spectator.service.getContentletVersions('123', 'en').subscribe((res) => {
            expect(res).toEqual(mockContentletVersionsResponse.entity.versions.en);
        });

        const req = spectator.expectOne(
            '/api/v1/content/versions?identifier=123&groupByLang=1',
            HttpMethod.GET
        );
        req.flush(mockContentletVersionsResponse);
    });

    it('should retrieve a contentlet by its inode', () => {
        const mockContentlet = createFakeContentlet({
            inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a'
        });
        const mockResponse = {
            entity: mockContentlet
        };

        spectator.service.getContentletByInode(mockContentlet.inode).subscribe((res) => {
            expect(res).toEqual(mockContentlet);
        });

        const req = spectator.expectOne(`/api/v1/content/${mockContentlet.inode}`, HttpMethod.GET);
        req.flush(mockResponse);
    });

    it('should retrieve a contentlet by its inode with content', () => {
        const mockContentlet = createFakeContentlet({
            inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a'
        });
        const mockContentletWithContent = { ...mockContentlet, content: 'file content' };
        const mockResponse = {
            entity: mockContentlet
        };

        dotUploadFileService.addContent.mockReturnValue(of(mockContentletWithContent));

        spectator.service.getContentletByInodeWithContent(mockContentlet.inode).subscribe((res) => {
            expect(res).toEqual(mockContentletWithContent);
            expect(dotUploadFileService.addContent).toHaveBeenCalledWith(mockContentlet);
        });

        const req = spectator.expectOne(`/api/v1/content/${mockContentlet.inode}`, HttpMethod.GET);
        req.flush(mockResponse);
    });

    it('should retrieve available languages for a contentlet', () => {
        const mockLanguage1 = createFakeLanguage({ id: 1, language: 'English' });
        const mockLanguage2 = createFakeLanguage({ id: 2, language: 'Spanish' });
        const mockLanguagesResponse = {
            entity: [mockLanguage1, mockLanguage2]
        };

        spectator.service.getLanguages('1').subscribe((res) => {
            expect(res).toEqual(mockLanguagesResponse.entity);
        });

        const req = spectator.expectOne('/api/v1/content/1/languages', HttpMethod.GET);
        req.flush(mockLanguagesResponse);
    });

    it('should lock a contentlet', () => {
        const mockContentlet = createFakeContentlet({ inode: '1' });
        const mockResponse = {
            entity: mockContentlet
        };

        spectator.service.lockContent('1').subscribe((res) => {
            expect(res).toEqual(mockContentlet);
        });

        const req = spectator.expectOne('/api/v1/content/_lock/1', HttpMethod.PUT);
        req.flush(mockResponse);
    });

    it('should unlock a contentlet', () => {
        const mockContentlet = createFakeContentlet({ inode: '1' });
        const mockResponse = {
            entity: mockContentlet
        };

        spectator.service.unlockContent('1').subscribe((res) => {
            expect(res).toEqual(mockContentlet);
        });

        const req = spectator.expectOne('/api/v1/content/_unlock/1', HttpMethod.PUT);
        req.flush(mockResponse);
    });

    it('should check if a contentlet can be locked', () => {
        const mockDotContentletCanLock: DotContentletCanLock = {
            canLock: true,
            id: '1',
            inode: '1',
            locked: true,
            lockedBy: 'user1'
        };
        const mockResponse = {
            entity: mockDotContentletCanLock
        };

        spectator.service.canLock('1').subscribe((res) => {
            expect(res).toEqual(mockDotContentletCanLock);
        });

        const req = spectator.expectOne('/api/v1/content/_canlock/1', HttpMethod.GET);
        req.flush(mockResponse);
    });
});
