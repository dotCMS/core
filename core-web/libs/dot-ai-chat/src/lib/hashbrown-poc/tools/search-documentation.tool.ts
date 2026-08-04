import { createTool } from '@hashbrownai/angular';
import { s } from '@hashbrownai/core';
import { lastValueFrom } from 'rxjs';

import { inject } from '@angular/core';

import { HashbrownDocSearchService } from '../services/hashbrown-doc-search.service';

export const searchDocumentationTool = createTool({
    name: 'searchDocumentation',
    description: 'Search the dotCMS documentation for a given query',
    schema: s.object('Input for searching dotCMS documentation', {
        query: s.string('The search query to find relevant documentation')
    }),
    handler: async (input) => {
        const docSearchService = inject(HashbrownDocSearchService);

        return await lastValueFrom(docSearchService.search(input.query));
    }
});
