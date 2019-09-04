import { DOTTestBed } from 'src/app/test/dot-test-bed';
import { DotEditContentTypeCacheService } from './dot-edit-content-type-cache.service';
import { DotCMSContentType } from 'dotcms-models';
import { dotcmsContentTypeBasicMock } from '@tests/dot-content-types.mock';

const contentTypeMock: DotCMSContentType = {
    ...dotcmsContentTypeBasicMock,
    clazz: 'clazz',
    defaultType: false,
    fixed: false,
    folder: 'folder',
    host: 'host',
    name: 'Banner',
    id: '1',
    variable: 'banner',
    owner: 'user',
    system: true,
};

describe('DotEditContentTypeCacheService', () => {

    let dotEditContentTypeCacheService: DotEditContentTypeCacheService;

    beforeEach(() => {
        this.injector = DOTTestBed.resolveAndCreate([
            DotEditContentTypeCacheService
        ]);

        dotEditContentTypeCacheService = this.injector.get(DotEditContentTypeCacheService);
    });

    it('it should set a content type into cache', () => {
        dotEditContentTypeCacheService.set(contentTypeMock);
        expect(dotEditContentTypeCacheService.get()).not.toBe(contentTypeMock);
        expect(dotEditContentTypeCacheService.get()).toEqual(contentTypeMock);
    });
});
