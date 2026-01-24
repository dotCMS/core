import { Logger } from '../../utils/logger';
import { executeWithErrorHandling, createSuccessResponse } from '../../utils/response';
import { ContentSearchService } from '../../services/search';
import type { ListFolderInput } from './index';
import { formatListFolderResponse } from './formatters';
import { getContextStore } from '../../utils/context-store';

const logger = new Logger('LIST_FOLDER_TOOL');
const searchService = new ContentSearchService();

function normalizeFolderPath(raw: string): string {
    const trimmed = raw.trim();
    if (trimmed === '') return '/';
    // Ensure leading slash
    const withLeading = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
    // Remove trailing slash except for root
    if (withLeading.length > 1 && withLeading.endsWith('/')) {
        return withLeading.slice(0, -1);
    }
    return withLeading;
}

export async function listFolderContentsHandler(params: ListFolderInput) {
    return executeWithErrorHandling(async () => {
        const folderPath = normalizeFolderPath(params.folder);
        const limit = params.limit ?? 100;
        const offset = params.offset ?? 0;
        logger.log('Listing folder contents', { folderPath, limit, offset });

        // Use parentPath:"/folder/" which matches immediate children under that folder
        const parentPath = folderPath.endsWith('/') ? folderPath : `${folderPath}/`;

        // Do NOT filter by site at the Lucene level; filter after fetching results
        const query = `+parentPath:"${parentPath}"`;

        const searchResponse = await searchService.search({
            query,
            limit,
            offset,
            // Provide defaults expected by the service type
            depth: 1,
            languageId: 1,
            allCategoriesInfo: false
        });

        const contentlets = searchResponse.entity.jsonObjectView.contentlets;

        const text = formatListFolderResponse(folderPath, contentlets, {
            limit,
            offset,
            total: contentlets.length
        });

        // Optionally store raw JSON in context for LLM-side filtering
        const shouldStore = params.store_raw !== false;
        if (shouldStore) {
            const contextKey = params.context_key || `folder_list:${parentPath}`;
            getContextStore().setData(contextKey, searchResponse);
            logger.log('Stored raw search results in context', { contextKey, count: contentlets.length });
        }

        logger.log('Folder contents listed', { fetchedCount: contentlets.length });

        const withHint =
            shouldStore
                ? `${text}\n\nContext:\n- Raw results stored for LLM filtering.`
                : text;

        return createSuccessResponse(withHint);
    }, 'Error listing folder contents');
}


