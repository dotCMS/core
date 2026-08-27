import {
    appendChildToParent,
    collectAncestorChain,
    findRoleInTree,
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

        it('falls back to a synthetic node when the role is not in the tree', () => {
            const chain = collectAncestorChain(buildTree(), { id: 'ghost' });

            expect(chain).toHaveLength(1);
            expect(chain[0]).toEqual({ id: 'ghost', name: 'ghost' });
        });

        it('stops when a parent cannot be resolved in the tree', () => {
            // `orphan` points to `missing` which is nowhere in the tree.
            const orphan = node('orphan', 'missing');
            const tree = [orphan];
            const chain = collectAncestorChain(tree, { id: 'orphan' });

            expect(chain.map((n) => n.id)).toEqual(['orphan']);
        });

        it('caps the walk at 20 to break accidental cycles', () => {
            // Build a chain that self-loops via a two-node cycle: a -> b -> a.
            const a: DotRoleNode = { id: 'a', name: 'A', parent: 'b' };
            const b: DotRoleNode = { id: 'b', name: 'B', parent: 'a' };
            const chain = collectAncestorChain([a, b], { id: 'a' });

            // Without the guard this would loop forever; with the guard it
            // returns at most 21 entries (the start + up to 20 iterations).
            expect(chain.length).toBeLessThanOrEqual(21);
        });
    });
});
