import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';

import { DotToolsService } from './dot-tools.service';

import {
    DotToolsCatalogEntry,
    DotToolsCustomToolConfig,
    DotToolsSection
} from '../models/dot-tools.models';

const MOCK_SECTION: DotToolsSection = {
    id: 'site',
    name: 'Site',
    icon: 'language',
    tabOrder: 0,
    portletIds: ['pages'],
    portletTitles: ['Pages']
};

const MOCK_CATALOG: DotToolsCatalogEntry[] = [{ id: 'pages', title: 'Pages', isCustom: false }];

const MOCK_CUSTOM: DotToolsCustomToolConfig = {
    portletId: 'c_press-releases',
    portletName: 'Press Releases',
    baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
    contentTypes: ['Blog'],
    dataViewMode: 'card'
};

describe('DotToolsService', () => {
    let spectator: SpectatorHttp<DotToolsService>;
    const createService = createHttpFactory(DotToolsService);

    beforeEach(() => {
        spectator = createService();
    });

    describe('sections', () => {
        it('GET /v1/layouts → entity', () => {
            let received: DotToolsSection[] | undefined;
            spectator.service.getSections().subscribe((r) => (received = r));
            const req = spectator.expectOne('/api/v1/layouts', HttpMethod.GET);
            req.flush({ entity: [MOCK_SECTION] });
            expect(received).toEqual([MOCK_SECTION]);
        });

        it('POST /v1/layouts with form body', () => {
            spectator.service.createSection({ name: 'Reporting', icon: 'analytics' }).subscribe();
            const req = spectator.expectOne('/api/v1/layouts', HttpMethod.POST);
            expect(req.request.body).toEqual({ name: 'Reporting', icon: 'analytics' });
            req.flush({ entity: MOCK_SECTION });
        });

        it('PUT /v1/layouts/{id} with form body (id is encoded)', () => {
            spectator.service
                .updateSection('a b', { name: 'Renamed', icon: 'public' })
                .subscribe();
            const req = spectator.expectOne('/api/v1/layouts/a%20b', HttpMethod.PUT);
            expect(req.request.body).toEqual({ name: 'Renamed', icon: 'public' });
            req.flush({ entity: MOCK_SECTION });
        });

        it('DELETE /v1/layouts/{id}', () => {
            spectator.service.deleteSection('site').subscribe();
            const req = spectator.expectOne('/api/v1/layouts/site', HttpMethod.DELETE);
            req.flush({ entity: [] });
        });

        it('PUT /v1/layouts/_reorder with { layoutIds }', () => {
            spectator.service.reorderSections(['site', 'content']).subscribe();
            const req = spectator.expectOne('/api/v1/layouts/_reorder', HttpMethod.PUT);
            expect(req.request.body).toEqual({ layoutIds: ['site', 'content'] });
            req.flush({ entity: [] });
        });

        it('PUT /v1/layouts/{id}/portlets with { portletIds }', () => {
            spectator.service.setSectionTools('site', ['pages', 'browser']).subscribe();
            const req = spectator.expectOne('/api/v1/layouts/site/portlets', HttpMethod.PUT);
            expect(req.request.body).toEqual({ portletIds: ['pages', 'browser'] });
            req.flush({ entity: [] });
        });
    });

    describe('catalog + custom tools', () => {
        it('GET /v1/portlet/_catalog → entity', () => {
            let received: DotToolsCatalogEntry[] | undefined;
            spectator.service.getCatalog().subscribe((r) => (received = r));
            const req = spectator.expectOne('/api/v1/portlet/_catalog', HttpMethod.GET);
            req.flush({ entity: MOCK_CATALOG });
            expect(received).toEqual(MOCK_CATALOG);
        });

        it('GET /v1/portlet/custom/{id} → entity (id encoded)', () => {
            let received: DotToolsCustomToolConfig | undefined;
            spectator.service.getCustomTool('c id').subscribe((r) => (received = r));
            const req = spectator.expectOne('/api/v1/portlet/custom/c%20id', HttpMethod.GET);
            req.flush({ entity: MOCK_CUSTOM });
            expect(received).toEqual(MOCK_CUSTOM);
        });

        it('POST /v1/portlet/custom joins arrays into comma-separated strings and chains GET', () => {
            // Backend's CustomPortletForm declares baseTypes / contentTypes
            // as `String` (comma-separated). Sending arrays verbatim makes
            // Jackson reject the body, so this is a load-bearing conversion.
            let received: DotToolsCatalogEntry | undefined;
            spectator.service
                .createCustomTool({
                    portletId: 'c_press-releases',
                    portletName: 'Press Releases',
                    baseTypes: [
                        DotCMSBaseTypesContentTypes.CONTENT,
                        DotCMSBaseTypesContentTypes.PERSONA
                    ],
                    contentTypes: ['Blog', 'Event'],
                    dataViewMode: 'card'
                })
                .subscribe((r) => (received = r));

            const create = spectator.expectOne('/api/v1/portlet/custom', HttpMethod.POST);
            expect(create.request.body).toEqual({
                portletId: 'c_press-releases',
                portletName: 'Press Releases',
                baseTypes: 'CONTENT,PERSONA',
                contentTypes: 'Blog,Event',
                dataViewMode: 'card'
            });
            // Backend returns only `{ entity: { portlet: id } }`, not the
            // full config, so the service follows up with a GET to populate
            // the catalog entry.
            create.flush({ entity: { portlet: 'c_press-releases' } });

            const follow = spectator.expectOne(
                '/api/v1/portlet/custom/c_press-releases',
                HttpMethod.GET
            );
            follow.flush({ entity: MOCK_CUSTOM });

            expect(received).toEqual({
                id: 'c_press-releases',
                title: 'Press Releases',
                isCustom: true
            });
        });

        it('PUT /v1/portlet/custom follows the same chain as POST', () => {
            let received: DotToolsCatalogEntry | undefined;
            spectator.service
                .updateCustomTool({
                    portletId: 'c_press-releases',
                    portletName: 'Press Releases v2',
                    baseTypes: [DotCMSBaseTypesContentTypes.CONTENT],
                    contentTypes: [],
                    dataViewMode: 'list'
                })
                .subscribe((r) => (received = r));

            const update = spectator.expectOne('/api/v1/portlet/custom', HttpMethod.PUT);
            expect(update.request.body).toEqual({
                portletId: 'c_press-releases',
                portletName: 'Press Releases v2',
                baseTypes: 'CONTENT',
                contentTypes: '',
                dataViewMode: 'list'
            });
            update.flush({ entity: { portlet: 'c_press-releases' } });

            spectator
                .expectOne('/api/v1/portlet/custom/c_press-releases', HttpMethod.GET)
                .flush({ entity: { ...MOCK_CUSTOM, portletName: 'Press Releases v2' } });

            expect(received?.title).toBe('Press Releases v2');
        });

        it('DELETE /v1/portlet/custom/{id} (id encoded)', () => {
            spectator.service.deleteCustomTool('c id').subscribe();
            spectator.expectOne('/api/v1/portlet/custom/c%20id', HttpMethod.DELETE);
        });
    });
});
