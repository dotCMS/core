import { createTool } from '@hashbrownai/angular';
import { lastValueFrom } from 'rxjs';

import { inject } from '@angular/core';

import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';

// eslint-disable-next-line @nx/enforce-module-boundaries
import {
    DotPageListService,
    ListPagesParams
} from '../../../../../../apps/dotcms-ui/src/app/portlets/dot-pages/services/dot-page-list.service';

export interface FavoritePageToolItem {
    title: string;
    url: string;
    identifier: string;
    inode: string;
    languageId: number;
    screenshot: string;
}

const DEFAULT_PAGES_PARAMS: ListPagesParams = {
    search: '',
    sort: 'modDate DESC',
    limit: 40,
    offset: 0,
    languageId: null,
    host: '',
    archived: false
};

function mapFavoritePage(page: DotCMSContentlet): FavoritePageToolItem {
    return {
        title: page.title ?? 'Untitled page',
        url: page.url ?? '',
        identifier: page.identifier ?? '',
        inode: page.inode ?? '',
        languageId: page.languageId ?? 1,
        screenshot: page['screenshot'] ?? ''
    };
}

export const getFavoritePagesTool = createTool({
    name: 'getFavoritePages',
    description: 'Get favorite pages for the current user',
    handler: async (): Promise<FavoritePageToolItem[]> => {
        const dotPageListService = inject(DotPageListService);
        const globalStore = inject(GlobalStore);
        const userId = globalStore.loggedUser()?.userId ?? 'dotcms.org.1';

        const response = await lastValueFrom(
            dotPageListService.getFavoritePages(DEFAULT_PAGES_PARAMS, userId)
        );

        return response.jsonObjectView.contentlets.map(mapFavoritePage);
    }
});
