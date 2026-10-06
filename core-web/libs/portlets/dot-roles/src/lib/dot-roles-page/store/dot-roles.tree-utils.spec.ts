import {
    appendChildToParent,
    collectAncestorChain,
    findRoleInTree,
    mergeTreesForLookup,
    patchNodeChildren,
    patchNodeInPlace,
    removeNodeFromTree
} from './dot-roles.tree-utils';

import { DotRoleNode } from '../../models/dot-roles.models';

/**
 * Tree used across the suite:
 *
 *   root-a
 *     ├─ child-a1
 *     │    └─ grand-a1a
 *     └─ child-a2
 *   root-b
 *
 * `parent` mirrors the Java `RoleAPI.findRoleHierarchy` convention where a
 * root's `parent` points to itself.
 */
const node = (
    id: string,
    parent: string,
    children: DotRoleNode[] = [],
    extra: Partial<DotRoleNode> = {}
): DotRoleNode => ({
    id,
    name: id.toUpperCase(),
    parent,
    roleChildren: children,
    ...extra
});

const buildTree = (): DotRoleNode[] => [
    node('root-a', 'root-a', [
        node('child-a1', 'root-a', [node('grand-a1a', 'child-a1')]),
        node('child-a2', 'root-a')
    ]),
    node('root-b', 'root-b')
];

describe('dot-roles.tree-utils', () => {
    describe('childCount stays in sync with local mutations', () => {
        // The tree treats childCount as authoritative for leaf-vs-chevron, so
        // a stale 0 renders a parent that just gained a child as a leaf — and
        // the new role becomes unreachable until a full reload.
        it('bumps a childless parent when a child is appended', () => {
            const tree = [{ id: 'p', name: 'Parent', childCount: 0, roleChildren: [] }];

            const [parent] = appendChildToParent(tree, 'p', { id: 'c', name: 'Child' });

            expect(parent.childCount).toBe(1);
            expect(parent.roleChildren).toHaveLength(1);
        });

        it('decrements the parent when one of its children is removed', () => {
            const tree = [
                {
                    id: 'p',
                    name: 'Parent',
                    childCount: 2,
                    roleChildren: [
                        { id: 'c1', name: 'C1' },
                        { id: 'c2', name: 'C2' }
                    ]
                }
            ];

            const [parent] = removeNodeFromTree(tree, 'c1');

            expect(parent.childCount).toBe(1);
            expect(parent.roleChildren).toHaveLength(1);
        });

        it('leaves an untouched branch count alone', () => {
            const tree = [
                {
                    id: 'p',
                    name: 'Parent',
                    childCount: 5,
                    roleChildren: [{ id: 'c1', name: 'C1' }]
                }
            ];

            const [parent] = removeNodeFromTree(tree, 'somewhere-else');

            expect(parent.childCount).toBe(5);
        });
    });

    describe('patchNodeChildren', () => {
        it('replaces children on a matching root node', () => {
            const tree = buildTree();
            const next = patchNodeChildren(tree, 'root-b', [node('new-b1', 'root-b')]);

            expect(next[1].roleChildren?.map((n) => n.id)).toEqual(['new-b1']);
        });

        it('replaces children on a nested node', () => {
            const tree = buildTree();
            const next = patchNodeChildren(tree, 'child-a1', [
                node('grand-a1a', 'child-a1'),
                node('grand-a1b', 'child-a1')
            ]);

            expect(next[0].roleChildren?.[0].roleChildren?.map((n) => n.id)).toEqual([
                'grand-a1a',
                'grand-a1b'
            ]);
        });

        it('is immutable — never mutates the input tree', () => {
            const tree = buildTree();
            const snapshot = JSON.parse(JSON.stringify(tree));

            patchNodeChildren(tree, 'root-a', []);

            expect(tree).toEqual(snapshot);
        });

        it('returns an equivalent tree when the id is not found', () => {
            const tree = buildTree();
            const next = patchNodeChildren(tree, 'nonexistent', [node('x', 'x')]);

            expect(next).toEqual(tree);
        });
    });

    describe('patchNodeInPlace', () => {
        it('replaces the node while preserving its existing children', () => {
            const tree = buildTree();
            const replacement = node('child-a1', 'root-a', [], { name: 'Renamed' });
            const next = patchNodeInPlace(tree, 'child-a1', replacement);

            const patched = next[0].roleChildren?.[0];
            expect(patched?.name).toBe('Renamed');
            expect(patched?.roleChildren?.map((n) => n.id)).toEqual(['grand-a1a']);
        });

        it('handles a root replacement', () => {
            const tree = buildTree();
            const replacement = node('root-a', 'root-a', [], { name: 'Renamed Root' });
            const next = patchNodeInPlace(tree, 'root-a', replacement);

            expect(next[0].name).toBe('Renamed Root');
            expect(next[0].roleChildren?.map((n) => n.id)).toEqual(['child-a1', 'child-a2']);
        });

        it('is immutable', () => {
            const tree = buildTree();
            const snapshot = JSON.parse(JSON.stringify(tree));

            patchNodeInPlace(tree, 'child-a1', node('child-a1', 'root-a', []));

            expect(tree).toEqual(snapshot);
        });

        it('returns an equivalent tree when the id is not found', () => {
            const tree = buildTree();
            const next = patchNodeInPlace(tree, 'nonexistent', node('x', 'x'));

            expect(next).toEqual(tree);
        });
    });

    describe('removeNodeFromTree', () => {
        it('drops a root-level node', () => {
            const next = removeNodeFromTree(buildTree(), 'root-b');

            expect(next.map((n) => n.id)).toEqual(['root-a']);
        });

        it('drops a nested node while preserving siblings', () => {
            const next = removeNodeFromTree(buildTree(), 'child-a1');

            expect(next[0].roleChildren?.map((n) => n.id)).toEqual(['child-a2']);
        });

        it('drops a deeply nested node', () => {
            const next = removeNodeFromTree(buildTree(), 'grand-a1a');

            expect(next[0].roleChildren?.[0].roleChildren).toEqual([]);
        });

        it('is immutable', () => {
            const tree = buildTree();
            const snapshot = JSON.parse(JSON.stringify(tree));

            removeNodeFromTree(tree, 'child-a1');

            expect(tree).toEqual(snapshot);
        });

        it('returns an equivalent tree when the id is not found', () => {
            const tree = buildTree();

            expect(removeNodeFromTree(tree, 'nonexistent')).toEqual(tree);
        });
    });

    describe('appendChildToParent', () => {
        it('appends when the parent already has children', () => {
            const child = node('new-child', 'root-a');
            const next = appendChildToParent(buildTree(), 'root-a', child);

            expect(next[0].roleChildren?.map((n) => n.id)).toEqual([
                'child-a1',
                'child-a2',
                'new-child'
            ]);
        });

        it('appends when the parent has undefined roleChildren', () => {
            const tree: DotRoleNode[] = [{ id: 'r', name: 'R', parent: 'r' }];
            const next = appendChildToParent(tree, 'r', node('n', 'r'));

            expect(next[0].roleChildren?.map((n) => n.id)).toEqual(['n']);
        });

        it('appends to a nested parent', () => {
            const child = node('new-grand', 'child-a1');
            const next = appendChildToParent(buildTree(), 'child-a1', child);

            expect(next[0].roleChildren?.[0].roleChildren?.map((n) => n.id)).toEqual([
                'grand-a1a',
                'new-grand'
            ]);
        });

        it('is immutable', () => {
            const tree = buildTree();
            const snapshot = JSON.parse(JSON.stringify(tree));

            appendChildToParent(tree, 'root-a', node('new', 'root-a'));

            expect(tree).toEqual(snapshot);
        });

        it('returns an equivalent tree when the parent id is not found', () => {
            const tree = buildTree();
            const next = appendChildToParent(tree, 'nonexistent', node('new', 'x'));

            expect(next).toEqual(tree);
        });
    });

    describe('findRoleInTree', () => {
        it('finds a root node', () => {
            expect(findRoleInTree(buildTree(), 'root-b')?.id).toBe('root-b');
        });

        it('finds a nested node', () => {
            expect(findRoleInTree(buildTree(), 'child-a1')?.id).toBe('child-a1');
        });

        it('finds a deeply nested node', () => {
            expect(findRoleInTree(buildTree(), 'grand-a1a')?.id).toBe('grand-a1a');
        });

        it('returns null when the id is not present', () => {
            expect(findRoleInTree(buildTree(), 'nonexistent')).toBeNull();
        });
    });

    describe('collectAncestorChain', () => {
        it('returns [role] for a root node (self-parent)', () => {
            const chain = collectAncestorChain(buildTree(), { id: 'root-a' });

            expect(chain.map((n) => n.id)).toEqual(['root-a']);
        });

        it('walks parent → grandparent → root, in order', () => {
            const chain = collectAncestorChain(buildTree(), { id: 'grand-a1a' });

            expect(chain.map((n) => n.id)).toEqual(['grand-a1a', 'child-a1', 'root-a']);
        });

        it('returns an empty chain when the role is not in the tree', () => {
            // Not `[stub]`: a one-element chain is what a root role produces,
            // so a stub would let the caller fan out to direct grants only and
            // render the result as the complete picture.
            expect(collectAncestorChain(buildTree(), { id: 'ghost' })).toEqual([]);
        });

        it('climbs by nesting, not by `parent` — search nodes carry no parent', () => {
            // `unwrapLegacySearchNode` keeps id/name/locked and drops `parent`,
            // so every node in a search result looks parentless. The ancestor
            // path is still there as nesting, and that is what has to be used
            // or a role opened from search loses all its inherited grants.
            const searchTree: DotRoleNode[] = [
                {
                    id: 'root-a',
                    name: 'ROOT-A',
                    roleChildren: [
                        {
                            id: 'child-a1',
                            name: 'CHILD-A1',
                            roleChildren: [{ id: 'grand-a1a', name: 'GRAND-A1A' }]
                        }
                    ]
                }
            ];

            const chain = collectAncestorChain(searchTree, { id: 'grand-a1a' });

            expect(chain.map((n) => n.id)).toEqual(['grand-a1a', 'child-a1', 'root-a']);
        });

        it('ignores a `parent` that contradicts the nesting', () => {
            // `orphan` claims a parent that is nowhere in the tree; structurally
            // it sits at the root, so the chain is just itself.
            const chain = collectAncestorChain([node('orphan', 'missing')], { id: 'orphan' });

            expect(chain.map((n) => n.id)).toEqual(['orphan']);
        });

        it('reaches a role that only the search copy of a root has hydrated', () => {
            // The cache holds `root-a` collapsed; the search result holds the
            // same root with the matched grandchild nested inside. Deduping
            // the roots would drop whichever copy holds the path.
            const cached: DotRoleNode[] = [node('root-a', 'root-a')];
            const searched: DotRoleNode[] = [
                {
                    id: 'root-a',
                    name: 'ROOT-A',
                    roleChildren: [
                        {
                            id: 'child-a1',
                            name: 'CHILD-A1',
                            roleChildren: [{ id: 'grand-a1a', name: 'GRAND-A1A' }]
                        }
                    ]
                }
            ];

            const chain = collectAncestorChain(mergeTreesForLookup(cached, searched), {
                id: 'grand-a1a'
            });

            expect(chain.map((n) => n.id)).toEqual(['grand-a1a', 'child-a1', 'root-a']);
        });

        it('caps the descent at MAX_ROLE_DEPTH', () => {
            // A branch spliced into its own subtree would otherwise recurse
            // forever. Nest deeper than the cap and the node past it is
            // unreachable rather than fatal.
            let deepest: DotRoleNode = { id: 'level-40', name: 'LEVEL-40' };
            for (let i = 39; i >= 0; i--) {
                deepest = { id: `level-${i}`, name: `LEVEL-${i}`, roleChildren: [deepest] };
            }

            expect(collectAncestorChain([deepest], { id: 'level-40' })).toEqual([]);
            expect(collectAncestorChain([deepest], { id: 'level-5' }).map((n) => n.id)).toEqual([
                'level-5',
                'level-4',
                'level-3',
                'level-2',
                'level-1',
                'level-0'
            ]);
        });
    });
});
