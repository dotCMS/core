import { createComponentFactory, mockProvider } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import {
    DotMessageService,
    DotUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';

import { DotAssetPickerComponent } from './dot-asset-picker.component';
import { DotAssetPickerConfig } from './store/models';

/**
 * The picker has to construct in the **legacy Dojo binary-field host**, not just in the Angular
 * shell — the File/Image field still renders there as the `dotcms-binary-field` custom element, and
 * "Select Existing File" opens this component from inside it.
 *
 * That host (`apps/dotcms-binary-field-builder/src/app/app.module.ts`) bootstraps with only
 * `provideHttpClient`, `provideAnimations`, `DotMessageService`, `DotUploadService`,
 * `DotWorkflowActionsFireService` and the theme. No Router, no app-shell providers. This factory
 * mirrors exactly that, so anything the picker pulls in has to survive it.
 *
 * Kept in its own file because the main picker spec provides the full shell, which is precisely
 * what hides this class of failure.
 */
const SITE_MOCK: DotSite = {
    identifier: 'site-1',
    hostname: 'demo.dotcms.com',
    aliases: null,
    archived: false
};

const CONFIG: DotAssetPickerConfig = { site: SITE_MOCK };

describe('DotAssetPickerComponent — legacy Dojo host (no Router, no app shell)', () => {
    const createComponent = createComponentFactory({
        component: DotAssetPickerComponent,
        // Mirrors `apps/dotcms-binary-field-builder/src/app/app.module.ts` exactly — no more, no
        // less. Everything below `provideHttpClientTesting` is what that host declares; the two
        // dialog tokens come from `DialogService` at runtime. Deliberately NO Router and no
        // app-shell providers.
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            mockProvider(DotUploadService),
            mockProvider(DotWorkflowActionsFireService),
            { provide: DynamicDialogConfig, useValue: { data: CONFIG } },
            mockProvider(DynamicDialogRef, { close: jest.fn(), onClose: of(undefined) })
        ],
        shallow: true
    });

    it('should construct without the app-shell providers', () => {
        // What this guards, concretely: nothing the picker reaches may need a `Router`.
        //
        // The picker used to inject `DotHttpErrorManagerService` in its store features, which pulls in
        //
        //     DotHttpErrorManagerService
        //       ├── DotAlertConfirmService   → ConfirmationService
        //       ├── DotMessageDisplayService → DotRouterService → Router, DotEventsSocket
        //       └── DotRouterService         → Router
        //
        // and this host has none of that — the very reason `GlobalStore` was kept out of
        // `DotFileFieldComponent`. Adding it to the picker's own `providers` could not fix it either:
        // `provideRouter` returns `EnvironmentProviders` and cannot go in a component's `providers`.
        //
        // The store now records failures as `requestError` state and this component toasts them, so
        // the dependency is gone. Re-introducing anything router-bound will fail here.
        expect(() => createComponent()).not.toThrow();
    });
});
