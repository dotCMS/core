import {
    inject,
    provideAppInitializer,
    type EnvironmentProviders,
    type Provider
} from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import {
    DotContentSearchService,
    DotLanguagesService,
    DotMessageService,
    DotPropertiesService,
    DotWorkflowActionsFireService,
    DotContentTypeService
} from '@dotcms/data-access';
import { LoggerService, StringUtils } from '@dotcms/dotcms-js';

/**
 * Provides services required by the Block Editor when used as a standalone component.
 * Call this in your app config or parent component providers if you need these services
 * available outside the block editor tree (e.g. for tests or shared context).
 *
 * When using only DotBlockEditorComponent, you typically do not need to call this—
 * the component provides these services in its own injector.
 */
export function provideBlockEditor(): (Provider | EnvironmentProviders)[] {
    return [
        ConfirmationService,
        LoggerService,
        StringUtils,
        DotPropertiesService,
        DotContentSearchService,
        DotLanguagesService,
        DotMessageService,
        DotContentTypeService,
        DotWorkflowActionsFireService,
        provideAppInitializer(() => {
            const dotMessageService = inject(DotMessageService);
            return dotMessageService.init();
        })
    ];
}
