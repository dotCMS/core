import { createTool } from '@hashbrownai/angular';
import { lastValueFrom } from 'rxjs';

import { inject } from '@angular/core';

import { DotContentTypeService } from '@dotcms/data-access';
import { StructureTypeView } from '@dotcms/dotcms-models';

export const getContentTypesTool = createTool({
    name: 'getContentTypes',
    description: 'Get all dotCMS content types',
    handler: async (): Promise<StructureTypeView[]> => {
        const dotContentTypeService = inject(DotContentTypeService);
        return await lastValueFrom(dotContentTypeService.getAllContentTypes());
    }
});
