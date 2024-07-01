import { HttpMethod } from '@ngneat/spectator';
import { createHttpFactory, SpectatorHttp } from '@ngneat/spectator/jest';

import { API_URL, CategoriesService, ITEMS_PER_PAGE } from './categories.service';

describe('CategoriesService', () => {
    let spectator: SpectatorHttp<CategoriesService>;
    const createHttp = createHttpFactory(CategoriesService);

    beforeEach(() => (spectator = createHttp()));

    it('can getChildren with inode', () => {
        const inode = 'inode-identifier';
        spectator.service.getChildren(inode).subscribe();
        spectator.expectOne(
            `${API_URL}/children?per_page=${ITEMS_PER_PAGE}&direction=ASC&inode=${inode}&showChildrenCount=true`,
            HttpMethod.GET
        );
    });
});
