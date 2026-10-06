import Lara from '@primeuix/themes/lara';

import { provideHttpClient } from '@angular/common/http';
import {
    ApplicationConfig,
    importProvidersFrom,
    provideBrowserGlobalErrorListeners
} from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

import { providePrimeNG } from 'primeng/config';

import { BlockEditorModule } from '@dotcms/block-editor';

/**
 * PrimeNG is required for components used inside `@dotcms/new-block-editor` (e.g. DataView in
 * image/video dotCMS pickers). Theme + cssLayer order must match `apps/dotcms-block-editor/src/styles.css`.
 *
 * `BlockEditorModule` is imported so the legacy `DotBlockEditorComponent` (registered as the
 * `dotcms-old-block-editor` web component) has its NgModule providers available at runtime.
 */
export const appConfig: ApplicationConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideHttpClient(),
        provideAnimationsAsync(),
        providePrimeNG({
            theme: {
                preset: Lara,
                options: {
                    darkModeSelector: '.dark'
                }
            }
        }),
        importProvidersFrom(BlockEditorModule)
    ]
};
