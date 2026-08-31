import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService, DotRolesService } from '@dotcms/data-access';
import { DotRole } from '@dotcms/dotcms-models';

import { DotUsersRolesTabComponent } from './dot-users-roles-tab.component';

/**
 * Baseline role shape. `DotRoleView` was collapsed into the shared `DotRole`
 * when the roles read surface moved to `DotRolesService`.
 */
function fakeRole(overrides: Partial<DotRole> = {}): DotRole {
    return {
        id: 'role-x',
        name: 'Role X',
        roleKey: 'ROLE_X',
        editUsers: true,
        childCount: 0,
        ...overrides
    };
}

/**
 * Tree fixture the spec uses across tree-walk tests:
 *
 *   Root A (id=1)                — grantable subtree
 *     ├── Sub A1 (id=2)
 *     │     ├── Leaf A1a (id=3, editUsers=true)
 *     │     └── Leaf A1b (id=4, editUsers=true)
 *     └── Leaf A2 (id=5, editUsers=true)
 *   Root B (id=6)                — bare grantable leaf (no children)
 *   Root C (id=7)                — mixed, one leaf editUsers=false
 *     ├── Leaf C1 (id=8, editUsers=false)  ← must not surface a checkbox
 *     └── Leaf C2 (id=9, editUsers=true)
 *
 * Shaped the way `getRoots(true)` actually returns it: roots nested with
 * their first level only. Sub A1's own children are one level too deep, so
 * they arrive through the walk's follow-up fetch (see GET_BY_ID below) —
 * which is exactly the two-level hydration limit the backend imposes.
 */
const ROLE_ROOTS: DotRole[] = [
    fakeRole({
        id: '1',
        name: 'Root A',
        roleKey: 'ROOT_A',
        childCount: 2,
        roleChildren: [
            fakeRole({ id: '2', name: 'Sub A1', roleKey: 'SUB_A1', childCount: 2 }),
            fakeRole({ id: '5', name: 'Leaf A2', roleKey: 'A2' })
        ]
    }),
    fakeRole({ id: '6', name: 'Root B', roleKey: 'ROOT_B' }),
    fakeRole({
        id: '7',
        name: 'Root C',
        roleKey: 'ROOT_C',
        childCount: 2,
        roleChildren: [
            fakeRole({ id: '8', name: 'Leaf C1', roleKey: 'C1', editUsers: false }),
            fakeRole({ id: '9', name: 'Leaf C2', roleKey: 'C2' })
        ]
    })
];

/**
 * Structural shape returned by `$availableTree()`. Kept in the spec so
 * tests can walk the tree without reaching into the component's
 * native-private (`#`) fields — those aren't accessible via bracket
 * notation like `private` members are.
 */
interface AvailableNode {
    role: {
        id: string;
        roleKey: string;
        name: string;
        description: string;
        parent?: string;
        editUsers: boolean;
    };
    children: AvailableNode[];
}

function flatten(nodes: AvailableNode[]): AvailableNode[] {
    return nodes.flatMap((node) => [node, ...flatten(node.children)]);
}

describe('DotUsersRolesTabComponent', () => {
    let spectator: Spectator<DotUsersRolesTabComponent>;

    const createComponent = createComponentFactory({
        component: DotUsersRolesTabComponent,
        detectChanges: false,
        providers: [
            // The tab composes getRoots + flattenRoleHierarchy. Only Sub A1
            // reports childCount > 0 below the first level, so the walk
            // fetches it and nothing else — every other node is a known leaf
            // and gets pruned.
            mockProvider(DotRolesService, {
                getRoots: jest.fn().mockReturnValue(of(ROLE_ROOTS)),
                getById: jest.fn().mockImplementation((id: string) =>
                    of(
                        id === '2'
                            ? fakeRole({
                                  id: '2',
                                  name: 'Sub A1',
                                  roleKey: 'SUB_A1',
                                  childCount: 2,
                                  roleChildren: [
                                      fakeRole({ id: '3', name: 'Leaf A1a', roleKey: 'A1A' }),
                                      fakeRole({ id: '4', name: 'Leaf A1b', roleKey: 'A1B' })
                                  ]
                              })
                            : fakeRole({ id, roleChildren: [] })
                    )
                )
            }),
            mockProvider(DotHttpErrorManagerService, { handle: jest.fn() })
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.detectChanges();
    });

    /**
     * Look up a RoleOption from the current available tree. Only
     * returns roles still shown in the Available panel — for hidden
     * roles (already granted) tests can push straight to the granted
     * signal and inspect that instead.
     */
    function findAvailable(id: string): AvailableNode['role'] | undefined {
        const tree = spectator.component['$availableTree']() as AvailableNode[];

        return flatten(tree).find((n) => n.role.id === id)?.role;
    }

    describe('grantableLeavesByRole (via canSelectRole)', () => {
        it('roots with at least one grantable leaf surface a checkbox', () => {
            const rootA = findAvailable('1');
            const rootB = findAvailable('6');
            expect(spectator.component['canSelectRole'](rootA!)).toBe(true);
            expect(spectator.component['canSelectRole'](rootB!)).toBe(true);
        });

        it('editUsers=false leaves do NOT surface a checkbox', () => {
            const c1 = findAvailable('8');
            expect(spectator.component['canSelectRole'](c1!)).toBe(false);
        });

        it('parents still surface a checkbox when some descendants are non-grantable — as long as at least one is', () => {
            const rootC = findAvailable('7');
            expect(spectator.component['canSelectRole'](rootC!)).toBe(true);
        });
    });

    describe('isPartiallyChecked on multi-level parents', () => {
        it('is false when nothing is selected', () => {
            const rootA = findAvailable('1');
            expect(spectator.component['isPartiallyChecked'](rootA!)).toBe(false);
        });

        it('is true when one of Sub A1’s two leaves is selected', () => {
            spectator.component['$selectedAvailable'].set(['3']);
            const subA1 = findAvailable('2');
            expect(spectator.component['isPartiallyChecked'](subA1!)).toBe(true);
            expect(spectator.component['isFullyChecked'](subA1!)).toBe(false);
        });

        it('flips fully-checked true when both A1 leaves are selected', () => {
            spectator.component['$selectedAvailable'].set(['3', '4']);
            const subA1 = findAvailable('2');
            expect(spectator.component['isPartiallyChecked'](subA1!)).toBe(false);
            expect(spectator.component['isFullyChecked'](subA1!)).toBe(true);
        });

        it('is partial on Root A when only Sub A1 leaves are picked (A2 still missing)', () => {
            spectator.component['$selectedAvailable'].set(['3', '4']);
            const rootA = findAvailable('1');
            expect(spectator.component['isPartiallyChecked'](rootA!)).toBe(true);
            expect(spectator.component['isFullyChecked'](rootA!)).toBe(false);
        });

        it('flips to fully-checked when every grantable leaf under Root A is selected', () => {
            spectator.component['$selectedAvailable'].set(['3', '4', '5']);
            const rootA = findAvailable('1');
            expect(spectator.component['isPartiallyChecked'](rootA!)).toBe(false);
            expect(spectator.component['isFullyChecked'](rootA!)).toBe(true);
        });
    });

    describe('availableTree pruning', () => {
        it('lists all three roots initially', () => {
            const rootIds = (spectator.component['$availableTree']() as AvailableNode[]).map(
                (node) => node.role.id
            );
            expect(rootIds).toEqual(['1', '6', '7']);
        });

        it('drops Root B once granted', () => {
            spectator.component['$granted'].set(['6']); // Root B is a bare grantable leaf
            const rootIds = (spectator.component['$availableTree']() as AvailableNode[]).map(
                (node) => node.role.id
            );
            expect(rootIds).not.toContain('6');
        });

        it('drops Root A once all three of its grantable leaves are granted', () => {
            spectator.component['$granted'].set(['3', '4', '5']); // A1a, A1b, A2
            const rootIds = (spectator.component['$availableTree']() as AvailableNode[]).map(
                (node) => node.role.id
            );
            expect(rootIds).not.toContain('1');
        });

        it('keeps Root A while at least one grantable leaf is still ungranted', () => {
            spectator.component['$granted'].set(['3', '4']); // A2 (id=5) still ungranted
            const rootIds = (spectator.component['$availableTree']() as AvailableNode[]).map(
                (node) => node.role.id
            );
            expect(rootIds).toContain('1');
        });
    });

    describe('grant / revoke', () => {
        it('grant() moves the selected leaves to the granted list and clears selection', () => {
            let emitted: string[] | undefined;
            spectator.component.grantedChange.subscribe((ids) => (emitted = ids));

            spectator.component['$selectedAvailable'].set(['3']); // Leaf A1a
            spectator.component['grant']();

            // Since #37218 the shuttle emits role IDs unconditionally
            // — no more roleKey fallback for keyless roles.
            expect(spectator.component['$granted']()).toContain('3');
            expect(spectator.component['$selectedAvailable']()).toEqual([]);
            expect(emitted).toContain('3');
        });

        it('revoke() removes the selected granted ids and re-emits', () => {
            let emitted: string[] | undefined;
            spectator.component.grantedChange.subscribe((ids) => (emitted = ids));

            spectator.component['$granted'].set(['3', '4']); // A1a, A1b
            spectator.component['$selectedGranted'].set(['4']);
            spectator.component['revoke']();

            expect(spectator.component['$granted']()).toEqual(['3']);
            expect(emitted).toEqual(['3']);
        });
    });

    describe('service error path', () => {
        it('surfaces the error via httpErrorManager and leaves the panel empty', () => {
            const service = spectator.inject(DotRolesService, true);
            (service.getRoots as jest.Mock).mockReturnValueOnce(
                throwError(() => new Error('boom'))
            );

            const spec2 = createComponent();
            spec2.detectChanges();

            const errorManager = spec2.inject(DotHttpErrorManagerService, true);
            expect(errorManager.handle).toHaveBeenCalled();
            expect(spec2.component['$availableTree']()).toEqual([]);
            expect(spec2.component['$isLoading']()).toBe(false);
        });
    });
});
