import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { DotMessageService } from '@dotcms/data-access';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotRoleToolsTabComponent } from './dot-role-tools-tab.component';

import { DotRoleToolGroupRow } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';

const MESSAGES = {
    'roles.tools.column.group': 'Tool Group',
    'roles.tools.column.included': 'Included Tools',
    'roles.tools.column.granted-from': 'Granted From',
    'roles.tools.empty': 'No tool groups',
    'roles.tools.cannot-edit': 'Cannot edit',
    'roles.error.load-failed': 'Failed'
};

const SELECTED_ROLE_ID = 'r-categories';

const row = (overrides: Partial<DotRoleToolGroupRow> = {}): DotRoleToolGroupRow => ({
    id: 'tg-1',
    name: 'Site',
    granted: true,
    grantedFromRoleId: SELECTED_ROLE_ID,
    grantedFromRoleName: 'Categories',
    ...overrides
});

describe('DotRoleToolsTabComponent', () => {
    let spectator: Spectator<DotRoleToolsTabComponent>;

    const createComponent = createComponentFactory({
        component: DotRoleToolsTabComponent,
        schemas: [CUSTOM_ELEMENTS_SCHEMA],
        detectChanges: false,
        componentProviders: [
            mockProvider(DotRolesStore, {
                toolGroups: jest.fn().mockReturnValue([]),
                toolGroupsStatus: jest.fn().mockReturnValue('LOADED'),
                toolGroupsSaving: jest.fn().mockReturnValue(false),
                selectedRoleStatus: jest.fn().mockReturnValue('LOADED'),
                selectedRoleId: jest.fn().mockReturnValue(SELECTED_ROLE_ID),
                canEditRoleLayouts: jest.fn().mockReturnValue(true),
                saveToolGroups: jest.fn().mockResolvedValue(true)
            })
        ],
        providers: [{ provide: DotMessageService, useValue: new MockDotMessageService(MESSAGES) }]
    });

    const store = () => spectator.inject(DotRolesStore, true);

    beforeEach(() => {
        spectator = createComponent();
        // `mockProvider` builds its jest.fn()s once, at factory scope, so a
        // `mockReturnValue` in one test leaks into the next. Re-seed the
        // defaults here and let each test override what it needs.
        (store().toolGroups as jest.Mock).mockReturnValue([]);
        (store().toolGroupsStatus as jest.Mock).mockReturnValue('LOADED');
        (store().toolGroupsSaving as jest.Mock).mockReturnValue(false);
        (store().selectedRoleStatus as jest.Mock).mockReturnValue('LOADED');
        (store().selectedRoleId as jest.Mock).mockReturnValue(SELECTED_ROLE_ID);
        (store().canEditRoleLayouts as jest.Mock).mockReturnValue(true);
        (store().saveToolGroups as jest.Mock).mockClear();
    });

    it('renders the empty state when the catalog is empty', () => {
        spectator.detectChanges();

        expect(spectator.query(byTestId('tools-empty'))).toBeTruthy();
        expect(spectator.query(byTestId('tools-table'))).toBeNull();
    });

    it('renders the skeleton while loading', () => {
        (store().toolGroupsStatus as jest.Mock).mockReturnValue('LOADING');
        spectator.detectChanges();

        expect(spectator.query(byTestId('tools-loading-skeleton'))).toBeTruthy();
    });

    it('renders the error state', () => {
        (store().toolGroupsStatus as jest.Mock).mockReturnValue('ERROR');
        spectator.detectChanges();

        expect(spectator.query(byTestId('tools-error'))).toBeTruthy();
    });

    it('shows the cannot-edit notice when the role blocks layout edits', () => {
        (store().canEditRoleLayouts as jest.Mock).mockReturnValue(false);
        spectator.detectChanges();

        expect(spectator.query(byTestId('cannot-edit-notice'))).toBeTruthy();
    });

    describe('included tools column', () => {
        const included = (titles?: string[]) =>
            (
                spectator.component as unknown as {
                    includedTools: (g: DotRoleToolGroupRow) => string;
                }
            ).includedTools(row({ portletTitles: titles }));

        it('joins every title when there are four or fewer', () => {
            expect(included(['Pages', 'Browser', 'Templates'])).toBe('Pages, Browser, Templates');
        });

        it('collapses the tail into "and N more..." past four', () => {
            expect(included(['Pages', 'Browser', 'Templates', 'Containers', 'Menus'])).toBe(
                'Pages, Browser, Templates, Containers and 1 more...'
            );
        });

        it('recovers a readable name when the portlet title came back untranslated', () => {
            // LanguageUtil.get returns the key itself when a portlet has no
            // title translation, which is what custom portlets hit.
            expect(
                included([
                    'com.dotcms.repackage.javax.portlet.title.c_Blog-Entries',
                    'com.dotcms.repackage.javax.portlet.title.c_Activities'
                ])
            ).toBe('Blog Entries, Activities');
        });

        it('leaves an already-translated title untouched', () => {
            expect(included(['Pages', 'Browser'])).toBe('Pages, Browser');
        });

        it('renders nothing for a group with no portlets', () => {
            expect(included([])).toBe('');
            expect(included(undefined)).toBe('');
        });
    });

    describe('direct vs inherited grants', () => {
        it('treats a grant from the selected role as direct', () => {
            (store().toolGroups as jest.Mock).mockReturnValue([row()]);
            spectator.detectChanges();

            const component = spectator.component as unknown as {
                isDirectGrant: (g: DotRoleToolGroupRow) => boolean;
                isLocked: (g: DotRoleToolGroupRow) => boolean;
            };
            expect(component.isDirectGrant(row())).toBe(true);
            expect(component.isLocked(row())).toBe(false);
        });

        it('locks an inherited row — it can only be revoked on the ancestor', () => {
            const inherited = row({ grantedFromRoleId: 'r-parent', grantedFromRoleName: 'Parent' });
            (store().toolGroups as jest.Mock).mockReturnValue([inherited]);
            spectator.detectChanges();

            const component = spectator.component as unknown as {
                isDirectGrant: (g: DotRoleToolGroupRow) => boolean;
                isLocked: (g: DotRoleToolGroupRow) => boolean;
            };
            expect(component.isDirectGrant(inherited)).toBe(false);
            expect(component.isLocked(inherited)).toBe(true);
        });

        it('locks every row while a save is in flight', () => {
            (store().toolGroupsSaving as jest.Mock).mockReturnValue(true);
            spectator.detectChanges();

            expect(
                (
                    spectator.component as unknown as {
                        isLocked: (g: DotRoleToolGroupRow) => boolean;
                    }
                ).isLocked(row())
            ).toBe(true);
        });
    });

    describe('toggling', () => {
        const toggle = (group: DotRoleToolGroupRow, checked: boolean) =>
            (
                spectator.component as unknown as {
                    onToggle: (g: DotRoleToolGroupRow, c: boolean) => void;
                }
            ).onToggle(group, checked);

        it('sends the full direct-grant set plus the newly checked group', () => {
            const granted = row({ id: 'tg-1' });
            const ungranted = row({
                id: 'tg-2',
                granted: false,
                grantedFromRoleId: null,
                grantedFromRoleName: null
            });
            (store().toolGroups as jest.Mock).mockReturnValue([granted, ungranted]);
            spectator.detectChanges();

            toggle(ungranted, true);

            expect(store().saveToolGroups).toHaveBeenCalledWith(['tg-1', 'tg-2']);
        });

        it('drops the unchecked group from the set', () => {
            const a = row({ id: 'tg-1' });
            const b = row({ id: 'tg-2' });
            (store().toolGroups as jest.Mock).mockReturnValue([a, b]);
            spectator.detectChanges();

            toggle(a, false);

            expect(store().saveToolGroups).toHaveBeenCalledWith(['tg-2']);
        });

        it('never echoes an inherited grant back — that would promote it to direct', () => {
            const direct = row({ id: 'tg-1' });
            const inherited = row({
                id: 'tg-2',
                grantedFromRoleId: 'r-parent',
                grantedFromRoleName: 'Parent'
            });
            const ungranted = row({
                id: 'tg-3',
                granted: false,
                grantedFromRoleId: null,
                grantedFromRoleName: null
            });
            (store().toolGroups as jest.Mock).mockReturnValue([direct, inherited, ungranted]);
            spectator.detectChanges();

            toggle(ungranted, true);

            expect(store().saveToolGroups).toHaveBeenCalledWith(['tg-1', 'tg-3']);
        });
    });
});
