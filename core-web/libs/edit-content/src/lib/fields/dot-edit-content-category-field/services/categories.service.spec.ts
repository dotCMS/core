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
            `${API_URL}/children?inode=${inode}&per_page=${ITEMS_PER_PAGE}&direction=ASC&showChildrenCount=true`,
            HttpMethod.GET
        );
    });

    it('can getChildren with inode & filter', () => {
        const inode = 'inode-identifier';
        const filter = 'query';
        spectator.service.getChildren(inode, { filter }).subscribe();
        spectator.expectOne(
            `${API_URL}/children?inode=${inode}&per_page=${ITEMS_PER_PAGE}&direction=ASC&filter=${filter}&allLevels=true`,

            HttpMethod.GET
        );
    });

    it('can getSelectedHierarchy of selected categories', () => {
        const keys = ['key1', 'key2', 'key3'];
        spectator.service.getSelectedHierarchy(keys).subscribe();
        spectator.expectOne(
            `${API_URL}/hierarchy`,

            HttpMethod.POST
        );
    });
});
