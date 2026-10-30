import { createHttpFactory, HttpMethod, SpectatorHttp } from '@openng/spectator/vitest';

import { HttpErrorResponse } from '@angular/common/http';

import { DotMenu } from '@dotcms/dotcms-models';

import { DotMenuService } from './dot-menu.service';

/**
 * The menu HTTP client is a thin, stateless wrapper (issue #38078): every subscription is one
 * `GET /api/v1/menu`, and nothing is kept between calls. The only in-memory copy of the menu lives
 * in the navigation store, never here.
 */
describe('DotMenuService', () => {
    const MENU_URL = '/api/v1/menu';

    const menus = [
        {
            id: 'aaaa1111-0000-4000-8000-000000000001',
            name: 'Site',
            tabName: 'Site',
            tabIcon: 'icon',
            url: '/pages',
            menuItems: [{ id: 'pages', label: 'Pages', url: '/pages', angular: true, ajax: false }]
        }
    ] as unknown as DotMenu[];

    let spectator: SpectatorHttp<DotMenuService>;
    const createHttp = createHttpFactory(DotMenuService);

    beforeEach(() => {
        spectator = createHttp();
    });

    it('sends one GET to /api/v1/menu and returns the entity', () => {
        let received: DotMenu[] | undefined;

        spectator.service.getMenus().subscribe((value) => (received = value));

        const request = spectator.expectOne(MENU_URL, HttpMethod.GET);
        request.flush({ entity: menus });

        expect(received).toEqual(menus);
    });

    it('sends nothing until it is subscribed', () => {
        spectator.service.getMenus();

        spectator.controller.expectNone(MENU_URL);
    });

    it('keeps no state: every subscription is a new request', () => {
        spectator.service.getMenus().subscribe();
        spectator.expectOne(MENU_URL, HttpMethod.GET).flush({ entity: menus });

        spectator.service.getMenus().subscribe();
        spectator.expectOne(MENU_URL, HttpMethod.GET).flush({ entity: [] });

        spectator.service.getMenus().subscribe();
        spectator.expectOne(MENU_URL, HttpMethod.GET).flush({ entity: menus });
    });

    it('exposes no menu cache or legacy query', () => {
        const service = spectator.service as unknown as Record<string, unknown>;

        expect(service['menu$']).toBeUndefined();
        for (const removed of [
            'loadMenu',
            'reloadMenu',
            'isPortletInMenu',
            'getUrlById',
            'getDotMenuId'
        ]) {
            expect(service[removed]).toBeUndefined();
        }
    });

    it('propagates an HTTP error to the subscriber', () => {
        let error: HttpErrorResponse | undefined;

        spectator.service.getMenus().subscribe({ error: (e) => (error = e) });
        spectator
            .expectOne(MENU_URL, HttpMethod.GET)
            .flush({ message: 'boom' }, { status: 500, statusText: 'Server Error' });

        expect(error?.status).toBe(500);
    });
});
