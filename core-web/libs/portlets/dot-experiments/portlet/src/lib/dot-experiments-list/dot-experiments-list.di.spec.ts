/**
 * Dependency-injection smoke test for the experiments list.
 *
 * This spec deliberately does **not** mock `DotExperimentsListStore` nor `DotExperimentsService`,
 * which is the opposite of `dot-experiments-list.component.spec.ts`. Its whole purpose is to build
 * the same injector chain the router builds at runtime and let it fail if a provider is missing.
 *
 * Why it exists: dotCMS has many `@Injectable()` services with no `providedIn: 'root'`, kept alive
 * by the app-level `apps/dotcms-ui/src/app/providers.ts`. A lazily loaded standalone portlet
 * inherits none of them automatically, so anything the portlet injects but does not provide throws
 * `NG0201: No provider found` on real route activation — a failure neither the store-mocking specs
 * nor the AOT build (types only) can see. Two of those shipped in a row.
 *
 * Only what the surrounding application would supply is mocked here: the app-level services from
 * `providers.ts` and the root-provided `GlobalStore`. Everything the portlet itself is responsible
 * for is left real. Replacing the store or the service with a mock "to simplify" destroys the whole
 * value of this file.
 */
import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideLocationMocks } from '@angular/common/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, provideRouter } from '@angular/router';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotPushPublishDialogService, LoggerService } from '@dotcms/dotcms-js';
import { HealthStatusTypes } from '@dotcms/dotcms-models';
import { GlobalStore } from '@dotcms/store';
import { getExperimentMock, MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsListComponent } from './dot-experiments-list.component';

import { DotExperimentsListStore } from '../store/dot-experiments-list.store';

const CURRENT_SITE_ID = 'site-123';

/** `GlobalStore` is `providedIn: 'root'`; only the signals this screen reads are stubbed. */
const globalStoreMock = {
    currentSiteId: signal(CURRENT_SITE_ID),
    siteDetails: signal({
        identifier: CURRENT_SITE_ID,
        hostname: 'demo.dotcms.com',
        aliases: null,
        archived: false
    })
};

/** A pristine `/experiments` URL: the store hydrates its view state from these params. */
const activatedRouteStub = { snapshot: { queryParams: {} } };

/** Providers declared by the `@Component` decorator, read from the JIT metadata. */
const declaredProviders = (): unknown[] => {
    const annotations = (
        DotExperimentsListComponent as unknown as { __annotations__?: { providers?: unknown[] }[] }
    ).__annotations__;

    return annotations?.[0]?.providers ?? [];
};

describe('DotExperimentsListComponent dependency injection', () => {
    let spectator: Spectator<DotExperimentsListComponent>;
    let httpTesting: HttpTestingController;

    const createComponent = createComponentFactory({
        component: DotExperimentsListComponent,
        // Nothing here overrides the component's own `providers`: the store, `ConfirmationService`
        // and `DotExperimentsService` must all resolve exactly as they do in the browser.
        providers: [
            provideHttpClient(),
            provideHttpClientTesting(),
            provideRouter([]),
            provideLocationMocks(),
            { provide: ActivatedRoute, useValue: activatedRouteStub },
            { provide: GlobalStore, useValue: globalStoreMock },
            // Everything below stands in for the app-level `providers.ts`, which is outside
            // this lib and therefore not the portlet's responsibility.
            { provide: DotMessageService, useValue: new MockDotMessageService({}) },
            mockProvider(DotMessageDisplayService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(DotPushPublishDialogService),
            mockProvider(LoggerService)
        ],
        detectChanges: false
    });

    const PAGE_ID = 'page-di-1';

    /**
     * The real store holds an empty shell until the Analytics gate answers, so the list is only
     * rendered after the health check — and the real service is what issues that request.
     *
     * A row is flushed rather than an empty list, deliberately: with nothing to show the table is
     * replaced by the empty state, and none of the per-row children — the kebab, the tags, the
     * action buttons — would be constructed. Those children are exactly what this spec is here to
     * instantiate against a real injector.
     */
    const openList = () => {
        spectator.detectChanges();

        httpTesting
            .expectOne((request) => request.url.endsWith('/experiments/health'))
            .flush({ entity: { health: HealthStatusTypes.OK } });
        httpTesting
            .expectOne((request) => request.url.endsWith('/api/v1/experiments'))
            .flush({
                entity: [{ ...getExperimentMock(0), id: 'exp-di-1', pageId: PAGE_ID }]
            });
        // The page lookup is what resolves the row's site; without it the site filter drops the
        // experiment and we are back to an empty table.
        httpTesting
            .expectOne((request) => request.url.endsWith('/api/content/_search'))
            .flush({
                entity: {
                    jsonObjectView: {
                        contentlets: [
                            { identifier: PAGE_ID, url: '/di-page', host: CURRENT_SITE_ID }
                        ]
                    }
                }
            });

        spectator.detectChanges();
    };

    it('should construct with the real store and the real DotExperimentsService', () => {
        expect(() => (spectator = createComponent())).not.toThrow();
        expect(spectator.component.store).toBeTruthy();
    });

    it('should render the list without a missing provider in any child of the template', () => {
        spectator = createComponent();
        httpTesting = spectator.inject(HttpTestingController);

        expect(() => openList()).not.toThrow();
        // Guards the test against silently asserting on an empty shell.
        expect(spectator.query(byTestId('experiments-table'))).not.toBeNull();
    });

    it('should render the add to bundle dialog without a missing provider', () => {
        spectator = createComponent();
        httpTesting = spectator.inject(HttpTestingController);
        openList();

        spectator.component.$addToBundleAssetId.set('experiment-1');

        expect(() => spectator.detectChanges()).not.toThrow();
        expect(spectator.query('dot-add-to-bundle')).not.toBeNull();
    });

    it('should declare the providers this portlet owns', () => {
        const providers = declaredProviders();

        expect(providers.length).toBeGreaterThan(0);
        expect(providers).toContain(DotExperimentsService);
        expect(providers).toContain(DotExperimentsListStore);
    });
});
