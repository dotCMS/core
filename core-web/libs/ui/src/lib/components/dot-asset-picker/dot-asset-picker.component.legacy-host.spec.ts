import { createComponentFactory, mockProvider } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { readdirSync, readFileSync } from 'fs';
import { join } from 'path';

import {
    DotMessageService,
    DotUploadService,
    DotWorkflowActionsFireService
} from '@dotcms/data-access';
import { DotSite } from '@dotcms/dotcms-models';

import { DotAssetPickerComponent } from './dot-asset-picker.component';
import { DotAssetPickerConfig } from './store/models';

/**
 * The picker has to construct under the **legacy Dojo binary-field host's provider set**, not just
 * the Angular shell's.
 *
 * It is no longer *opened* from that host — `ASSET_PICKER_LAUNCHER` is absent there, so the File and
 * Image fields fall back to `DotBrowserSelectorComponent` (#37132). This spec is kept anyway, as a
 * dependency-graph guard rather than a behavioral one: the picker lives in `@dotcms/ui`, which the
 * custom-element bootstraps do import, so the moment anything it pulls in starts needing a `Router`
 * or an app-shell provider, that whole bundle breaks in a host that has neither. That failure mode
 * is unchanged by which host opens the dialog.
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

    // The construction check above renders shallow, so it never instantiates the toolbar's chips —
    // and those are exactly what this feature added to the picker's tree. Reading the sources
    // covers what a shallow render cannot: nothing in either tree may so much as *reference* the
    // dependencies that break this host, whether or not a test happens to render it.
    describe('the picker and the shared filter chips (source-level guard)', () => {
        /**
         * What may not appear in code — `Router` itself and everything that reaches it, plus the
         * app shell's global store, kept out for the same reason.
         *
         * Matched against code with comments stripped: several of these names appear in prose in
         * this very tree, explaining why they are absent, and a guard that fired on its own
         * rationale would be unfixable except by deleting the explanation.
         */
        const FORBIDDEN = [
            'Router',
            'DotRouterService',
            'DotHttpErrorManagerService',
            'DotEventsSocket',
            'DotMessageDisplayService',
            'GlobalStore'
        ];

        /** Every non-spec TypeScript source under `dir`, recursively. */
        const sourcesUnder = (dir: string): string[] =>
            readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
                const path = join(dir, entry.name);

                if (entry.isDirectory()) {
                    return sourcesUnder(path);
                }

                return entry.name.endsWith('.ts') && !entry.name.endsWith('.spec.ts') ? [path] : [];
            });

        /** The file's code, with block and line comments removed. */
        const codeOf = (file: string): string =>
            readFileSync(file, 'utf8')
                .replace(/\/\*[\s\S]*?\*\//g, '')
                .replace(/\/\/.*$/gm, '');

        it.each([
            ['the AssetPicker', __dirname],
            ['the shared filter bar', join(__dirname, '..', 'dot-filter-bar')]
        ])('should keep %s free of app-shell dependencies', (_name, dir) => {
            const offenders = sourcesUnder(dir).flatMap((file) => {
                const code = codeOf(file);

                return FORBIDDEN.filter((name) => new RegExp(`\\b${name}\\b`).test(code)).map(
                    (name) => `${file.slice(file.indexOf('src/'))} → ${name}`
                );
            });

            // A failure here is not a test to relax: the dependency has to be inverted into a
            // surface-provided capability, the way `DOT_RELATIONSHIP_PICKER` and the chips' `error`
            // output already are (FR-015, FR-020).
            expect(offenders).toEqual([]);
        });
    });
});
