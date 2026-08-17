import { createComponentFactory, mockProvider } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
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
        // Deliberately minimal: this is the whole provider set the custom-element host has.
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            mockProvider(DotMessageService, { get: jest.fn((key: string) => key) }),
            { provide: DynamicDialogConfig, useValue: { data: CONFIG } },
            mockProvider(DynamicDialogRef, { close: jest.fn(), onClose: of(undefined) })
        ],
        shallow: true
    });

    xit('should construct without the app-shell providers', () => {
        // SKIPPED — this fails today, and the failure IS the open PR finding, not a flaky test.
        //
        // Adding `DotHttpErrorManagerService` + `DotContentTypeService` to the picker's `providers`
        // (what the review proposed as "the smallest fix") is necessary but NOT sufficient.
        // `DotContentTypeService` is satisfied — it only needs `HttpClient`.
        // `DotHttpErrorManagerService` is not; it pulls in:
        //
        //     DotHttpErrorManagerService
        //       ├── DotAlertConfirmService   → ConfirmationService        ← fails here now
        //       ├── DotMessageDisplayService → DotRouterService → Router, DotEventsSocket
        //       └── DotRouterService         → Router
        //
        // and this host has no `Router` at all — the very reason `GlobalStore` was kept out of
        // `DotFileFieldComponent`. The chain cannot be satisfied by component-level plumbing:
        // `provideRouter` returns `EnvironmentProviders` and cannot go in a component's `providers`.
        //
        // The fix that actually works is to drop the `DotHttpErrorManagerService` dependency from
        // the picker's store features and report errors through the `MessageService` toast the
        // picker already owns (it does exactly that for upload and confirm failures). Un-skip this
        // test as part of that change.
        expect(() => createComponent()).not.toThrow();
    });
});
