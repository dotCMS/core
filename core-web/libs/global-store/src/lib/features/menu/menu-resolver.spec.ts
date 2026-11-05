import { MenuItem } from 'primeng/api';

import { MenuItemEntity } from '@dotcms/dotcms-models';

import { resolveMenuItem } from './menu-resolver';

/**
 * One rule decides which menu item a URL belongs to (issue #38078): the highlight, the breadcrumbs
 * and the document title all call it. The order is: (1) portlet id plus `mId`, (2) with no query at
 * all, the first breadcrumb's label and then the portlet id, (3) exact `menuLink` path equality.
 */
describe('resolveMenuItem', () => {
    const SITE = 'aaaa1111-0000-4000-8000-000000000001';
    const CONTENT = 'bbbb2222-0000-4000-8000-000000000002';
    const SITE_CLONE = 'aaaa9999-0000-4000-8000-000000000003';

    const entity = (
        id: string,
        parentMenuId: string,
        overrides: Partial<MenuItemEntity> = {}
    ): MenuItemEntity =>
        ({
            id,
            label: id,
            url: `/${id}`,
            menuLink: `/${id}`,
            angular: true,
            ajax: false,
            parentMenuId,
            parentMenuLabel: parentMenuId,
            parentMenuIcon: 'icon',
            shortLayoutId: parentMenuId.substring(0, 4),
            tabOrder: 0,
            itemIndex: 0,
            ...overrides
        }) as MenuItemEntity;

    const resolve = (
        url: string,
        items: MenuItemEntity[],
        extra: {
            openParentMenuId?: string | null;
            breadcrumbs?: MenuItem[];
            exactPath?: boolean;
        } = {}
    ) => resolveMenuItem({ url, items, ...extra });

    describe('portlet id and mId', () => {
        const items = [entity('pages', SITE), entity('pages', CONTENT, { tabOrder: 1 })];

        it('picks the layout the mId names when the same portlet is in several layouts', () => {
            expect(resolve('/pages?mId=aaaa', items)?.parentMenuId).toBe(SITE);
            expect(resolve('/pages?mId=bbbb', items)?.parentMenuId).toBe(CONTENT);
        });

        it('accepts the hash form and extra query params next to mId', () => {
            expect(resolve('#/pages?mId=bbbb&x=1', items)?.parentMenuId).toBe(CONTENT);
        });

        it('resolves a legacy portlet opened through /c/<id>', () => {
            const legacy = [
                entity('publishing-queue', SITE, {
                    angular: false,
                    menuLink: '/c/publishing-queue'
                })
            ];

            expect(resolve('/c/publishing-queue?mId=aaaa', legacy)?.id).toBe('publishing-queue');
        });

        it('resolves a multi-segment portlet id', () => {
            const analytics = [entity('analytics-dashboard', SITE)];

            expect(resolve('/c/analytics/dashboard?mId=aaaa', analytics)?.id).toBe(
                'analytics-dashboard'
            );
        });

        it('keeps the mId rule even when a longer id prefix is used', () => {
            expect(resolve(`/pages?mId=${CONTENT.substring(0, 6)}`, items)?.parentMenuId).toBe(
                CONTENT
            );
        });
    });

    describe('two layouts that share the first 4 characters of their id', () => {
        const items = [
            entity('pages', SITE, { tabOrder: 2 }),
            entity('pages', SITE_CLONE, { tabOrder: 1 })
        ];

        it('prefers the layout that is already open', () => {
            expect(
                resolve('/pages?mId=aaaa', items, { openParentMenuId: SITE })?.parentMenuId
            ).toBe(SITE);
            expect(
                resolve('/pages?mId=aaaa', items, { openParentMenuId: SITE_CLONE })?.parentMenuId
            ).toBe(SITE_CLONE);
        });

        it('otherwise takes the first in tabOrder, then in position inside the group', () => {
            expect(resolve('/pages?mId=aaaa', items)?.parentMenuId).toBe(SITE_CLONE);

            const samePlace = [
                entity('pages', SITE, { tabOrder: 1, itemIndex: 3 }),
                entity('pages', SITE_CLONE, { tabOrder: 1, itemIndex: 1 })
            ];
            expect(resolve('/pages?mId=aaaa', samePlace)?.parentMenuId).toBe(SITE_CLONE);
        });

        it('never drops either item', () => {
            expect(items).toHaveLength(2);
        });
    });

    describe('bookmarks without a query', () => {
        const items = [
            entity('templates', SITE, { label: 'Templates' }),
            entity('containers', SITE, { label: 'Containers', itemIndex: 1 })
        ];

        it('finds the portlet by id', () => {
            expect(resolve('/containers', items)?.id).toBe('containers');
        });

        it('prefers the label of the first breadcrumb that has a url', () => {
            const breadcrumbs: MenuItem[] = [
                { label: 'Home', disabled: true },
                { label: 'Containers', url: '/dotAdmin/#/containers' }
            ];

            expect(resolve('/templates', items, { breadcrumbs })?.id).toBe('containers');
        });

        it('falls back to the portlet id when no breadcrumb label matches', () => {
            const breadcrumbs: MenuItem[] = [{ label: 'Nothing', url: '/x' }];

            expect(resolve('/templates', items, { breadcrumbs })?.id).toBe('templates');
        });

        it('ignores breadcrumbs without a url', () => {
            const breadcrumbs: MenuItem[] = [{ label: 'Containers' }];

            expect(resolve('/templates', items, { breadcrumbs })?.id).toBe('templates');
        });

        it('picks the first item when two share the label', () => {
            const same = [
                entity('a', SITE, { label: 'Same' }),
                entity('b', CONTENT, { label: 'Same', tabOrder: 1 })
            ];
            const breadcrumbs: MenuItem[] = [{ label: 'Same', url: '/x' }];

            expect(resolve('/zzz', same, { breadcrumbs })?.id).toBe('a');
        });
    });

    describe('exact path', () => {
        it('matches the menuLink when the portlet id does not appear in the url', () => {
            const custom = [entity('plugin-a', SITE, { menuLink: '/my/custom/path' })];

            expect(resolve('/my/custom/path', custom)?.id).toBe('plugin-a');
        });

        it('does not let a shorter menuLink claim a longer path', () => {
            const items = [entity('content', SITE, { menuLink: '/c/content' })];

            expect(resolve('/c/content-types', items)).toBeNull();
            expect(resolve('/c/contents', items)).toBeNull();
        });
    });

    describe('exactPath', () => {
        const items = [entity('containers', SITE)];

        it('follows an editor route to its portlet by default, for the highlight', () => {
            expect(resolve('/containers/edit/123', items)?.id).toBe('containers');
            expect(resolve('/containers/edit/123?mId=aaaa', items)?.id).toBe('containers');
        });

        it('matches only the link of the item itself, for the breadcrumbs', () => {
            expect(resolve('/containers/edit/123', items, { exactPath: true })).toBeNull();
            expect(resolve('/containers/edit/123?mId=aaaa', items, { exactPath: true })).toBeNull();
            expect(resolve('/containers?mId=aaaa', items, { exactPath: true })?.id).toBe(
                'containers'
            );
            expect(resolve('/containers', items, { exactPath: true })?.id).toBe('containers');
        });
    });

    describe('what it does not match', () => {
        const items = [entity('templates', SITE)];

        it('returns null for an empty menu', () => {
            expect(resolve('/templates', [])).toBeNull();
        });

        it('returns null when a query carries no mId (an old bookmark)', () => {
            expect(resolve('/templates?foo=1', items)).toBeNull();
        });

        it('returns null when the mId names no layout of that portlet', () => {
            expect(resolve('/templates?mId=zzzz', items)).toBeNull();
        });

        it('returns null for a portlet that is not in the menu', () => {
            expect(resolve('/unknown?mId=aaaa', items)).toBeNull();
            expect(resolve('/unknown', items)).toBeNull();
        });
    });
});
