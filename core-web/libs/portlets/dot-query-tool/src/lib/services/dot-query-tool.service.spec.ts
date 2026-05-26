import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotQueryToolService } from './dot-query-tool.service';

describe('DotQueryToolService', () => {
    let spectator: SpectatorHttp<DotQueryToolService>;

    const createService = createHttpFactory(DotQueryToolService);

    beforeEach(() => {
        spectator = createService();
    });

    it('should POST SearchForm to /api/v1/content/_search and unwrap entity', () => {
        const form = {
            query: '+contentType:htmlpageasset',
            sort: 'modDate desc',
            offset: 0,
            limit: 20
        };
        const entity = {
            resultsSize: 1,
            queryTook: 12,
            contentTook: 34,
            jsonObjectView: { contentlets: [{ inode: 'abc' }] }
        };

        let received: unknown;
        spectator.service.search(form).subscribe((response) => {
            received = response;
        });

        const req = spectator.expectOne('/api/v1/content/_search', HttpMethod.POST);
        expect(req.request.body).toEqual(form);
        req.flush({ entity });

        expect(received).toEqual(entity);
    });

    it('should forward the userId field when provided', () => {
        spectator.service
            .search({
                query: '+live:true',
                sort: '',
                offset: 0,
                limit: 5,
                userId: 'admin@dotcms.com'
            })
            .subscribe();

        const req = spectator.expectOne('/api/v1/content/_search', HttpMethod.POST);
        expect(req.request.body.userId).toBe('admin@dotcms.com');
    });
});
