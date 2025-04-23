import { DotCMSContentType } from '@dotcms/dotcms-models';
import { createFakeContentType } from '@dotcms/utils-testing';

import { DotEditContentTypeCacheService } from './dot-edit-content-type-cache.service';

const contentTypeMock: DotCMSContentType = createFakeContentType({
    clazz: 'clazz',
    defaultType: false,
    fixed: false,
    folder: 'folder',
    host: 'host',
    name: 'Banner',
    id: '1',
    variable: 'banner',
    owner: 'user',
    system: true
});

describe('DotEditContentTypeCacheService', () => {
    let service: DotEditContentTypeCacheService;

    beforeEach(() => {
        service = new DotEditContentTypeCacheService();
    });

    it('it should set a content type into cache', () => {
        service.set(contentTypeMock);
        expect(service.get()).not.toBe(contentTypeMock);
        expect(service.get()).toEqual(contentTypeMock);
    });
});
