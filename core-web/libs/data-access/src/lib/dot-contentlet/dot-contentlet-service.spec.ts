import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotCMSContentlet, DotLanguage } from '@dotcms/dotcms-models';

import { DotContentletService } from './dot-contentlet.service';

const mockContentletVersionsResponse = {
    entity: {
        versions: {
            en: [{ content: 'one' }, { content: 'two' }] as unknown as DotCMSContentlet[]
        }
    }
};

const mockContentletByInodeResponse = {
    entity: {
        archived: false,
        baseType: 'CONTENT',
        caategory: [{ boys: 'Boys' }, { girls: 'Girls' }],
        contentType: 'ContentType1',
        date: 1639548000000,
        dateTime: 1639612800000,
        folder: 'SYSTEM_FOLDER',
        hasLiveVersion: true,
        hasTitleImage: false,
        host: '48190c8c-42c4-46af-8d1a-0cd5db894797',
        hostName: 'demo.dotcms.com',
        identifier: '758cb37699eae8500d64acc16ebc468e',
        inode: '18f707db-ebf3-45f8-9b5a-d8bf6a6f383a',
        keyValue: { Colorado: 'snow', 'Costa Rica': 'summer' },
        languageId: 1,
        live: true,
        locked: false,
        modDate: 1639784363639,
        modUser: 'dotcms.org.1',
        modUserName: 'Admin User',
        owner: 'dotcms.org.1',
        publishDate: 1639784363639,
        sortOrder: 0,
        stInode: '0121c052881956cd95bfe5dde968ca07',
        text: 'final value',
        time: 104400000,
        title: '758cb37699eae8500d64acc16ebc468e',
        titleImage: 'TITLE_IMAGE_NOT_FOUND',
        url: '/content.40e5d7cd-2117-47d5-b96d-3278b188deeb',
        working: true
    } as unknown as DotCMSContentlet
};

export const mockDotContentletCanLock = {
    entity: {
        canLock: true,
        id: '1',
        inode: '1',
        locked: true
    }
};

describe('DotContentletService', () => {
    let spectator: SpectatorHttp<DotContentletService>;
    const createHttp = createHttpFactory(DotContentletService);

    beforeEach(() => (spectator = createHttp()));

    it('should bring the contentlet versions by language', () => {
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
        spectator.service
            .getContentletByInode(mockContentletByInodeResponse.entity.inode)
            .subscribe((res) => {
                expect(res).toEqual(mockContentletByInodeResponse.entity);
            });

        const req = spectator.expectOne(
            '/api/v1/content/' + mockContentletByInodeResponse.entity.inode,
            HttpMethod.GET
        );
        req.flush(mockContentletByInodeResponse);
    });

    it('should retrieve available languages for a contentlet', () => {
        const mockLanguagesResponse = {
            entity: [
                { languageId: 1, language: 'English' },
                { languageId: 2, language: 'Spanish' }
            ]
        };

        spectator.service.getLanguages('1').subscribe((res) => {
            expect(res).toEqual(mockLanguagesResponse.entity as unknown as DotLanguage[]);
        });

        const req = spectator.expectOne('/api/v1/content/1/languages', HttpMethod.GET);
        req.flush(mockLanguagesResponse);
    });

    it('should lock a contentlet', () => {
        spectator.service.lockContent('1').subscribe((res) => {
            expect(res).toEqual(mockContentletByInodeResponse.entity);
        });

        const req = spectator.expectOne('/api/v1/content/_lock/1', HttpMethod.PUT);
        req.flush(mockContentletByInodeResponse);
    });

    it('should unlock a contentlet', () => {
        spectator.service.unlockContent('1').subscribe((res) => {
            expect(res).toEqual(mockContentletByInodeResponse.entity);
        });

        const req = spectator.expectOne('/api/v1/content/_unlock/1', HttpMethod.PUT);
        req.flush(mockContentletByInodeResponse);
    });

    it('should check if a contentlet can be locked', () => {
        spectator.service.canLock('1').subscribe((res) => {
            expect(res).toEqual(mockDotContentletCanLock.entity);
        });

        const req = spectator.expectOne('/api/v1/content/_canlock/1', HttpMethod.GET);
        req.flush(mockDotContentletCanLock);
    });
});
