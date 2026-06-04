import { createServiceFactory, mockProvider, SpectatorService } from '@ngneat/spectator/jest';
import { signalStore } from '@ngrx/signals';
import { of, throwError } from 'rxjs';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

import { withContentTypeCache } from './withContentTypeCache';

const mockContentType = {
    id: 'ct-id',
    variable: 'Article',
    name: 'Article',
    layout: [
        {
            divider: {},
            columns: [{ columnDivider: {}, fields: [{ variable: 'title', name: 'Title' }] }]
        }
    ]
} as unknown as DotCMSContentType;

const TestStore = signalStore(withContentTypeCache());

describe('withContentTypeCache', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;
    let contentTypeService: DotContentTypeService;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [
            mockProvider(DotContentTypeService, {
                getContentType: jest.fn().mockReturnValue(of(mockContentType))
            })
        ]
    });

    beforeEach(() => {
        jest.clearAllMocks();
        spectator = createService();
        store = spectator.service;
        contentTypeService = spectator.inject(DotContentTypeService);
    });

    it('should initialize with an empty cache', () => {
        expect(store.contentTypeCache()).toEqual({});
    });

    describe('loadContentType', () => {
        it('should fetch the content type and store it in the cache', () => {
            store.loadContentType('Article');

            expect(contentTypeService.getContentType).toHaveBeenCalledWith('Article');
            expect(store.contentTypeCache()['Article']).toEqual(mockContentType);
        });

        it('should not fetch again when the variable is already cached', () => {
            store.loadContentType('Article');
            store.loadContentType('Article');

            expect(contentTypeService.getContentType).toHaveBeenCalledTimes(1);
        });

        it('should cache multiple distinct content types', () => {
            const mockBlog = { ...mockContentType, variable: 'Blog', name: 'Blog' };
            jest.spyOn(contentTypeService, 'getContentType')
                .mockReturnValueOnce(of(mockContentType))
                .mockReturnValueOnce(of(mockBlog as unknown as DotCMSContentType));

            store.loadContentType('Article');
            store.loadContentType('Blog');

            expect(store.contentTypeCache()['Article']).toEqual(mockContentType);
            expect(store.contentTypeCache()['Blog']).toEqual(mockBlog);
        });

        it('should not fetch when the variable is an empty string', () => {
            store.loadContentType('');

            expect(contentTypeService.getContentType).not.toHaveBeenCalled();
        });

        it('should not throw and return EMPTY on fetch error', () => {
            jest.spyOn(contentTypeService, 'getContentType').mockReturnValue(
                throwError(() => new Error('network error'))
            );

            expect(() => store.loadContentType('Article')).not.toThrow();
            expect(store.contentTypeCache()['Article']).toBeUndefined();
        });
    });
});
