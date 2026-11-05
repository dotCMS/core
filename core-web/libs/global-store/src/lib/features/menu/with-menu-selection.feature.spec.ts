import { signalStore, signalStoreFeature, withFeature, withState } from '@ngrx/signals';
import { Subject } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { NavigationEnd, Router } from '@angular/router';

import { DotMenu } from '@dotcms/dotcms-models';

import { withMenuSelection } from './with-menu-selection.feature';
import { withMenu } from './with-menu.feature';

import { withBreadcrumbs } from '../breadcrumb/breadcrumb.feature';

/**
 * The highlight, the breadcrumbs and the document title answer the same question — "which menu item
 * is the user on?" — and must give the same answer (issue #38078). The three features are composed
 * the way `GlobalStore` composes them, with the router replaced by a fake that fires `NavigationEnd`.
 */
describe('menu selection (highlight, breadcrumbs, title)', () => {
    const MENU_URL = '/api/v1/menu';
    const SITE = 'aaaa1111-0000-4000-8000-000000000001';
    const CONTENT = 'bbbb2222-0000-4000-8000-000000000002';

    const SelectionStore = signalStore(
        withState({}),
        withMenu(),
        withFeature((menu) =>
            signalStoreFeature(withBreadcrumbs(menu.menuItemsEntities), withMenuSelection(menu))
        )
    );

    const layout = (id: string, items: string[]) =>
        ({
            id,
            name: id,
            tabName: `Tab ${id.substring(0, 4)}`,
            tabIcon: 'icon',
            url: `/${items[0]}`,
            menuItems: items.map((portlet) => ({
                id: portlet,
                label: `Label ${portlet}`,
                url: `/${portlet}`,
                angular: true,
                ajax: false
            }))
        }) as unknown as DotMenu;

    const menu = () => [layout(SITE, ['pages', 'templates']), layout(CONTENT, ['pages', 'drive'])];

    let store: InstanceType<typeof SelectionStore>;
    let httpMock: HttpTestingController;
    let routerEvents: Subject<unknown>;
    let router: { events: Subject<unknown>; url: string };

    const setup = (initialUrl = '') => {
        sessionStorage.clear();
        localStorage.clear();
        routerEvents = new Subject<unknown>();
        router = { events: routerEvents, url: initialUrl };
        TestBed.resetTestingModule();
        TestBed.configureTestingModule({
            providers: [
                SelectionStore,
                { provide: Router, useValue: router },
                provideHttpClient(),
                provideHttpClientTesting()
            ]
        });
        store = TestBed.inject(SelectionStore);
        httpMock = TestBed.inject(HttpTestingController);
        TestBed.tick();
    };

    const seed = (menus: DotMenu[]) => {
        store.reload();
        httpMock.expectOne(MENU_URL).flush({ entity: menus });
        TestBed.tick();
    };

    const navigate = (url: string) => {
        router.url = url;
        routerEvents.next(new NavigationEnd(1, url, url));
        TestBed.tick();
    };

    beforeEach(() => setup());

    describe('on NavigationEnd', () => {
        beforeEach(() => seed(menu()));

        it('highlights the item of the url and opens its group', () => {
            navigate('/templates?mId=aaaa');

            expect(store.activeMenuItem()?.id).toBe('templates');
            expect(store.openParentMenuId()).toBe(SITE);
        });

        it('picks the layout named by mId when the portlet is in two layouts', () => {
            navigate('/pages?mId=bbbb');

            expect(store.activeMenuItem()?.parentMenuId).toBe(CONTENT);
        });

        it('highlights a bookmark without mId by its portlet id', () => {
            navigate('/templates');

            expect(store.activeMenuItem()?.id).toBe('templates');
        });

        it('keeps the current highlight when the url is not a menu item (e.g. an edit screen)', () => {
            navigate('/templates?mId=aaaa');

            navigate('/content/abc123');

            expect(store.activeMenuItem()?.id).toBe('templates');
        });

        it('never highlights an item for a url that carries other params and no mId', () => {
            navigate('/templates?mId=aaaa');

            navigate('/drive?other=1');

            expect(store.activeMenuItem()?.id).toBe('templates');
        });
    });

    describe('one answer for highlight, breadcrumbs and title', () => {
        beforeEach(() => seed(menu()));

        it.each([
            ['/templates?mId=aaaa', 'templates', SITE],
            ['/pages?mId=aaaa', 'pages', SITE],
            ['/pages?mId=bbbb', 'pages', CONTENT],
            ['/drive?mId=bbbb', 'drive', CONTENT],
            ['/templates', 'templates', SITE]
        ])('%s', (url, portletId, layoutId) => {
            navigate(url);

            const item = store.activeMenuItem();
            expect(item?.id).toBe(portletId);
            expect(item?.parentMenuId).toBe(layoutId);
            expect(store.lastBreadcrumb()?.label).toBe(item?.label);
            expect(store.breadcrumbs()[1]?.label).toBe(item?.parentMenuLabel);
            expect(store.getPageTitleByUrl(url)).toBe(item?.label);
        });
    });

    describe('browser reload (the highlight starts empty)', () => {
        it('fills it from the current url once the menu has loaded', () => {
            setup('/templates?mId=aaaa');

            expect(store.activeKey()).toBeNull();

            seed(menu());

            expect(store.activeMenuItem()?.id).toBe('templates');
            expect(store.openParentMenuId()).toBe(SITE);
        });

        it('leaves it empty when the url is not in the menu', () => {
            setup('/nothing');

            seed(menu());

            expect(store.activeKey()).toBeNull();
        });
    });

    describe('replacing the menu (layout update, login, login-as)', () => {
        beforeEach(() => {
            seed(menu());
            navigate('/templates');
        });

        it('keeps the highlight while the portlet still exists, with or without mId in the url', () => {
            seed([layout(SITE, ['pages', 'templates', 'containers']), layout(CONTENT, ['drive'])]);

            expect(store.activeMenuItem()?.id).toBe('templates');
        });

        it('leaves nothing highlighted when the portlet was removed', () => {
            seed([layout(CONTENT, ['drive'])]);

            expect(store.activeKey()).toBeNull();
            expect(store.activeMenuItem()).toBeNull();
        });

        it('follows the portlet when it moves to another layout and the url has no mId', () => {
            seed([layout(SITE, ['pages']), layout(CONTENT, ['templates', 'drive'])]);

            expect(store.activeMenuItem()?.id).toBe('templates');
            expect(store.activeMenuItem()?.parentMenuId).toBe(CONTENT);
        });
    });

    describe('document title', () => {
        beforeEach(() => seed(menu()));

        it('uses the item chosen for the url, not a menu link that merely starts like it', () => {
            seed([
                {
                    ...layout(SITE, ['content']),
                    menuItems: [
                        {
                            id: 'content',
                            label: 'Content Search',
                            url: '/c/content',
                            angular: false,
                            ajax: false
                        }
                    ]
                } as unknown as DotMenu
            ]);

            expect(store.getPageTitleByUrl('/c/content-types')).toBe('');
            expect(store.getPageTitleByUrl('/c/content')).toBe('Content Search');
        });
    });
});
