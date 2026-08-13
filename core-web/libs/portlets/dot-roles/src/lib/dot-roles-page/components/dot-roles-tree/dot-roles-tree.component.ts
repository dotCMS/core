import { Subject } from 'rxjs';

import { Component, DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';

import { TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogService, DynamicDialogModule } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { TreeModule, TreeNodeSelectEvent } from 'primeng/tree';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesAddComponent } from '../../../dot-roles-add/dot-roles-add.component';
import { DotRoleNode } from '../../../models/dot-roles.models';
import { DotRolesStore } from '../../store/dot-roles.store';

interface DotRoleTreeNode extends TreeNode {
    data: DotRoleNode;
    key: string;
    label: string;
    children?: DotRoleTreeNode[];
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
        TreeModule,
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

    protected readonly $filterInput = computed(() => this.store.filter());

    protected readonly $treeNodes = computed<DotRoleTreeNode[]>(() =>
        this.#toTreeNodes(this.store.filteredRoles())
    );

    protected readonly $selectedNode = computed<DotRoleTreeNode | null>(() => {
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
        const node = event.node as DotRoleTreeNode;
        this.store.selectRole(node.data.id);
    }

    protected onAddRole(parentRoleId?: string): void {
        this.#dialogService.open(DotRolesAddComponent, {
            header: undefined, // Component sets its own header
            width: '700px',
            closable: true,
            closeOnEscape: true,
            data: { parentRoleId: parentRoleId ?? null }
        });
    }

    /**
     * TODO: wire drag-and-drop reparent when #36936 (PUT /v1/roles/{roleId})
     * lands. The Angular `p-tree` supports `draggableNodes` + `droppableNodes`
     * but the server-side reparent has no v1 endpoint today.
     */
    protected onNodeDrop(_event: unknown): void {
        // Intentionally no-op until backend endpoint is available.
    }

    #toTreeNodes(nodes: DotRoleNode[]): DotRoleTreeNode[] {
        return nodes.map((node) => {
            const children = node.children ? this.#toTreeNodes(node.children) : [];

            return {
                key: node.id,
                label: node.name,
                data: node,
                icon: children.length > 0 ? 'folder' : 'shield',
                children,
                leaf: children.length === 0
            };
        });
    }

    #findNode(nodes: DotRoleTreeNode[], id: string): DotRoleTreeNode | null {
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
