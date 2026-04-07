import { createTool } from '@hashbrownai/angular';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

export const getContentTypesTool = (dotContentTypeService: DotContentTypeService) =>
    createTool({
        name: 'getContentTypes',
        description: 'Get all dotCMS content types',
        handler: async (): Promise<DotCMSContentType[]> => {
            const contentTypes: DotCMSContentType[] = [];
            let currentPage = 1;
            let totalPages = 1;

            do {
                const pageResponse = await dotContentTypeService
                    .getContentTypesWithPagination({
                        page: currentPage,
                        per_page: 100
                    })
                    .toPromise();

                if (!pageResponse) {
                    break;
                }

                const { contentTypes: pageContentTypes, pagination } = pageResponse;

                contentTypes.push(...pageContentTypes);
                totalPages = Math.ceil(pagination.totalEntries / pagination.perPage);
                currentPage += 1;
            } while (currentPage <= totalPages);

            return contentTypes;
        }
    });
