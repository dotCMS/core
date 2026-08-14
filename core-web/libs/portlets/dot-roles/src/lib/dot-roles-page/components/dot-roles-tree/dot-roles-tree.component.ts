import { Subject } from 'rxjs';

import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TreeNodeExpandEvent, TreeNodeSelectEvent } from 'primeng/types/tree';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotFolderTreeComponent, DotMessagePipe } from '@dotcms/ui';

import { DotRolesAddComponent } from '../../../dot-roles-add/dot-roles-add.component';
import { DotRoleNode } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';

interface DotRolePrimeTreeNode extends TreeNode {
    data: DotRoleNode;
    key: string;
    label: string;
    children?: DotRolePrimeTreeNode[];
}

@Component({
    selector: 'dot-roles-tree',
    standalone: true,
    imports: [
        FormsModule,
        ButtonModule,
        InputTextModule,
        IconFieldModule,
        InputIconModule,
        DynamicDialogModule,
        DotFolderTreeComponent,
        DotMessagePipe
    ],
    providers: [DialogService],
    templateUrl: './dot-roles-tree.component.html',
    host: { class: 'flex flex-col flex-1 min-h-0 p-4 gap-3' }
})
export class DotRolesTreeComponent {
    protected readonly store = inject(DotRolesStore);
    readonly #destroyRef = inject(DestroyRef);
    readonly #dialogService = inject(DialogService);
    readonly #filterInput$ = new Subject<string>();

    /**
     * Nodes whose children have already been lazy-loaded from the backend.
     * We use this to hide the chevron on nodes we've confirmed are leaves
     * so users don't get to click into empty branches after expanding.
     */
    readonly #expandedRoles = signal(new Set<string>());

    /**
     * PassThrough config for the shared `DotFolderTreeComponent` — matches
     * the compact chrome used by Content Drive so row heights stay tight.
     */
    protected readonly treePt = {
        root: { class: 'w-full h-full border-none overflow-y-auto' },
        nodeLabel: { class: 'overflow-hidden text-ellipsis whitespace-nowrap flex-1' }
    };

    protected readonly $filterInput = computed(() => this.store.filter());

    protected readonly $treeNodes = computed<DotRolePrimeTreeNode[]>(() =>
        this.#toTreeNodes(this.store.filteredRoles())
    );

    protected readonly $selectedNode = computed<DotRolePrimeTreeNode | null>(() => {
        const id = this.store.selectedRoleId();
        if (!id) {
            return null;
        }

        return this.#findNode(this.$treeNodes(), id);
    });

    constructor() {
        this.#filterInput$
            .pipe(debounceTime(200), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((value) => this.store.setFilter(value));
    }

    protected onFilterChange(value: string): void {
        this.#filterInput$.next(value);
    }

    protected onNodeSelect(event: TreeNodeSelectEvent): void {
        const node = event.node as DotRolePrimeTreeNode;
        this.store.selectRole(node.data.id);
    }

    /**
     * PrimeNG lazy-load hook. The backend only returns 2 levels deep per
     * request, so any node whose children we haven't fetched yet triggers
     * a load on first expand.
     */
    protected onNodeExpand(event: TreeNodeExpandEvent): void {
        const node = event.node as DotRolePrimeTreeNode;
        const id = node.data.id;
        if (this.#expandedRoles().has(id)) {
            return;
        }

        this.#expandedRoles.update((set) => new Set(set).add(id));

        // Only fetch when we don't already have children populated.
        const hasLoadedChildren = (node.data.roleChildren?.length ?? 0) > 0;
        if (!hasLoadedChildren && !node.data.user) {
            this.store.loadRoleChildren(id);
        }
    }

    protected onAddRole(parentRoleId?: string): void {
        this.#dialogService.open(DotRolesAddComponent, {
            header: undefined,
            width: '700px',
            closable: true,
            closeOnEscape: true,
            data: { parentRoleId: parentRoleId ?? null }
        });
    }

    #toTreeNodes(nodes: DotRoleNode[]): DotRolePrimeTreeNode[] {
        return nodes.map((node) => {
            const children = this.#toTreeNodes(node.roleChildren ?? []);
            const hasChildren = children.length > 0;
            // A node is a confirmed leaf when it's a user-role, OR we've
            // fetched its children and got none back. Otherwise we keep
            // the chevron so admins can drill into deeper levels.
            const confirmedLeaf =
                node.user === true || (this.#expandedRoles().has(node.id) && !hasChildren);

            return {
                key: node.id,
                label: node.name,
                data: node,
                children,
                leaf: confirmedLeaf,
                expanded: hasChildren
            };
        });
    }

    #findNode(nodes: DotRolePrimeTreeNode[], id: string): DotRolePrimeTreeNode | null {
        for (const node of nodes) {
            if (node.data.id === id) {
                return node;
            }
            const found = node.children ? this.#findNode(node.children, id) : null;
            if (found) {
                return found;
            }
        }

        return null;
    }
}
