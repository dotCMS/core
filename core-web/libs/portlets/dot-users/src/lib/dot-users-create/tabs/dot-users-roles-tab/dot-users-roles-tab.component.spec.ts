import { createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotUsersRolesTabComponent } from './dot-users-roles-tab.component';

import { DotRoleView, DotUsersService } from '../../../services/dot-users.service';

/**
 * Baseline role shape. Passed through `mockProvider(DotUsersService,
 * { getAllRoles })` — the component transforms via `toRoleOption` so
 * these tests never see the internal `RoleOption` shape.
 */
function fakeRole(overrides: Partial<DotRoleView> = {}): DotRoleView {
    return {
        id: 'role-x',
        name: 'Role X',
        roleKey: 'ROLE_X',
        parent: undefined,
        editUsers: true,
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
 */
const ROLES: DotRoleView[] = [
    fakeRole({ id: '1', name: 'Root A', roleKey: 'ROOT_A' }),
    fakeRole({ id: '2', name: 'Sub A1', roleKey: 'SUB_A1', parent: '1' }),
    fakeRole({ id: '3', name: 'Leaf A1a', roleKey: 'A1A', parent: '2' }),
    fakeRole({ id: '4', name: 'Leaf A1b', roleKey: 'A1B', parent: '2' }),
    fakeRole({ id: '5', name: 'Leaf A2', roleKey: 'A2', parent: '1' }),
    fakeRole({ id: '6', name: 'Root B', roleKey: 'ROOT_B' }),
    fakeRole({ id: '7', name: 'Root C', roleKey: 'ROOT_C' }),
    fakeRole({ id: '8', name: 'Leaf C1', roleKey: 'C1', parent: '7', editUsers: false }),
    fakeRole({ id: '9', name: 'Leaf C2', roleKey: 'C2', parent: '7' })
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
            mockProvider(DotUsersService, {
                getAllRoles: jest.fn().mockReturnValue(of(ROLES))
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
            spectator.component['$granted'].set(['ROOT_B']);
            const rootIds = (spectator.component['$availableTree']() as AvailableNode[]).map(
                (node) => node.role.id
            );
            expect(rootIds).not.toContain('6');
        });

        it('drops Root A once all three of its grantable leaves are granted', () => {
            spectator.component['$granted'].set(['A1A', 'A1B', 'A2']);
            const rootIds = (spectator.component['$availableTree']() as AvailableNode[]).map(
                (node) => node.role.id
            );
            expect(rootIds).not.toContain('1');
        });

        it('keeps Root A while at least one grantable leaf is still ungranted', () => {
            spectator.component['$granted'].set(['A1A', 'A1B']); // A2 still ungranted
            const rootIds = (spectator.component['$availableTree']() as AvailableNode[]).map(
                (node) => node.role.id
            );
            expect(rootIds).toContain('1');
        });
    });

    describe('grant / revoke', () => {
        it('grant() moves the selected leaves to the granted list and clears selection', () => {
            let emitted: string[] | undefined;
            spectator.component.grantedChange.subscribe((keys) => (emitted = keys));

            spectator.component['$selectedAvailable'].set(['3']); // Leaf A1a
            spectator.component['grant']();

            expect(spectator.component['$granted']()).toContain('A1A');
            expect(spectator.component['$selectedAvailable']()).toEqual([]);
            expect(emitted).toContain('A1A');
        });

        it('revoke() removes the selected granted keys and re-emits', () => {
            let emitted: string[] | undefined;
            spectator.component.grantedChange.subscribe((keys) => (emitted = keys));

            spectator.component['$granted'].set(['A1A', 'A1B']);
            spectator.component['$selectedGranted'].set(['A1B']);
            spectator.component['revoke']();

            expect(spectator.component['$granted']()).toEqual(['A1A']);
            expect(emitted).toEqual(['A1A']);
        });
    });

    describe('service error path', () => {
        it('surfaces the error via httpErrorManager and leaves the panel empty', () => {
            const service = spectator.inject(DotUsersService, true);
            (service.getAllRoles as jest.Mock).mockReturnValueOnce(
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
