import { Subject } from 'rxjs';

import { Component, DestroyRef, computed, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { TreeNode } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { TreeSelect, TreeSelectModule } from 'primeng/treeselect';

import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent,
    DotMessagePipe
} from '@dotcms/ui';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';
import {
    DotRoleFormValue,
    DotRoleNode,
    ROOT_PARENT_OPTION_KEY,
    toParentRoleId
} from '../models/dot-roles.models';

/**
 * Add Role dialog. POST /v1/roles already exists so this form is fully
 * functional. Opened from the `New` button in the roles panel and from
 * the inline `+` on a parent row (which prefills `Parent`).
 */
@Component({
    selector: 'dot-roles-add',
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        CheckboxModule,
        TreeSelectModule,
        DotMessagePipe,
        DotFieldRequiredDirective,
        DotFieldValidationMessageComponent
    ],
    templateUrl: './dot-roles-add.component.html'
})
export class DotRolesAddComponent {
    readonly #store = inject(DotRolesStore);
    readonly #fb = inject(FormBuilder);
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);
    readonly #messageService = inject(DotMessageService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Deep-search results, scoped to this dialog. Non-null means a search is
     * active and the picker shows matches with their ancestor path instead of
     * the cached tree. Kept local rather than in the store so the roles tree
     * behind the dialog does not re-filter itself.
     */
    readonly #searchResults = signal<DotRoleNode[] | null>(null);

    /**
     * Which branches are open. PrimeNG records expansion by mutating
     * `node.expanded` on the node object itself; our options come from a
     * `computed`, so every store change hands it brand-new objects and the
     * mutation is lost — the branch snaps shut the moment its children load.
     * Tracking the keys here and re-applying them on each rebuild is what
     * makes expansion survive.
     */
    readonly #expandedKeys = signal(new Set<string>());

    // `protected`, not `#`: Angular rejects `viewChild` on an ES-private
    // field ("Cannot use viewChild on a class member that is declared as ES
    // private").
    protected readonly treeSelect = viewChild(TreeSelect);
    protected readonly $searching = signal(false);
    readonly #filterInput$ = new Subject<string>();

    /**
     * Guards against an older search overwriting a newer one. The debounce
     * gates how often a search STARTS, not the order responses come back, and
     * these calls are plain promises with no switchMap to cancel the loser.
     */
    #searchToken = 0;

    protected readonly $submitting = signal(false);
    protected readonly $error = signal<string | null>(null);

    protected readonly form = this.#fb.nonNullable.group({
        roleName: ['', Validators.required],
        roleKey: [''],
        parent: [null as TreeNode | null],
        canEditUsers: [true],
        canEditPermissions: [true],
        canEditLayouts: [true],
        description: ['']
    });

    /**
     * Candidates, with the explicit root entry pinned first. Prepended here
     * rather than inside `#toTreeNodes` so it survives the search path too —
     * `#searchResults()` replaces the whole tree, and "None (Top Level)" is a
     * choice, not a search hit.
     */
    protected readonly $parentTree = computed<TreeNode[]>(() => [
        {
            key: ROOT_PARENT_OPTION_KEY,
            label: this.#messageService.get('roles.form.parent.root'),
            leaf: true
        },
        ...this.#toTreeNodes(this.#searchResults() ?? this.#store.roleTree(), this.#expandedKeys())
    ]);

    constructor() {
        // Same 3-char gate and debounce as the roles tree filter, so the two
        // search surfaces behave identically.
        this.#filterInput$
            .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.#destroyRef))
            .subscribe((query) => this.#runSearch(query));

        const prefilledParent = this.#config.data?.parentRoleId ?? null;
        if (prefilledParent) {
            // The `+` on a tree row prefills the parent, so resolve the id it
            // handed us back to the node the picker is bound to.
            //
            // The fallback matters: dropping an unresolvable id would leave the
            // picker empty, and saving would then create a ROOT role instead of
            // the child the admin asked for. A synthetic node keeps the intent
            // even if the tree has not loaded that branch.
            this.form.controls.parent.setValue(
                this.#findNode(this.$parentTree(), prefilledParent) ?? {
                    key: prefilledParent,
                    label: prefilledParent
                }
            );
        } else {
            // Opened from `New` rather than a row's `+`: default to the explicit
            // root entry. Same outcome as leaving the picker empty — both map to
            // `parentRoleId: null` — but the field states what will happen
            // instead of leaving the admin to infer it from a blank.
            this.form.controls.parent.setValue(this.$parentTree()[0]);
        }
    }

    protected onSave(): void {
        if (this.$submitting()) {
            return;
        }

        // Save is never disabled, so an incomplete form reaches here. Name the
        // problem in the footer instead of leaving a dead button the admin has
        // to reverse-engineer; `markAllAsTouched` lights up the per-field
        // messages at the same time.
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            this.$error.set('roles.form.error.required');

            return;
        }

        this.$submitting.set(true);
        this.$error.set(null);

        const { parent, ...rest } = this.form.getRawValue();
        // An empty picker and the explicit "None (Top Level)" entry both mean a
        // root role — see `toParentRoleId`.
        const value: DotRoleFormValue = {
            ...rest,
            parentRoleId: toParentRoleId(parent)
        };

        this.#store.createRole(value).then((created) => {
            this.$submitting.set(false);
            if (created) {
                this.#ref.close(created);
            } else {
                this.$error.set('roles.add.error');
            }
        });
    }

    /**
     * The backend hydrates only two levels per request, so a node the admin
     * opens may have children that were never fetched. Hydrating on expand
     * lets them drill as deep as the hierarchy goes.
     */
    protected onNodeExpand(event: { node: TreeNode }): void {
        const key = event.node?.key;
        if (!key) {
            return;
        }
        this.#expandedKeys.update((keys) => new Set(keys).add(key));
        this.#store.loadRoleChildren(key);
    }

    protected onNodeCollapse(event: { node: TreeNode }): void {
        const key = event.node?.key;
        if (!key) {
            return;
        }
        this.#expandedKeys.update((keys) => {
            const next = new Set(keys);
            next.delete(key);

            return next;
        });
    }

    protected onFilter(event: { filter: string }): void {
        this.#filterInput$.next(event.filter ?? '');
    }

    async #runSearch(query: string): Promise<void> {
        // Under 3 chars the picker falls back to the cached tree — matching
        // the left-hand tree, and avoiding a request per keystroke.
        const token = ++this.#searchToken;

        if (query.trim().length < 3) {
            this.#searchResults.set(null);
            // The token bump above orphans any search still in flight, and an
            // orphaned run leaves the flag alone (see the `finally`). Clearing
            // it here is what keeps the picker from spinning forever when the
            // admin backspaces below three characters mid-request.
            this.$searching.set(false);
            // Reset the widget's own filter too. `Tree.getRootNode()` keeps
            // serving its cached `filteredNodes` once the filter has run, so
            // reverting the options alone would leave the last search's rows
            // on screen after the admin backspaces out of the query.
            this.treeSelect()?.treeViewChild?._filter('');

            return;
        }

        this.$searching.set(true);

        try {
            const results = await this.#store.searchRoleTree(query);

            // A newer query already landed — drop this one rather than
            // replacing fresh results with stale ones.
            if (token !== this.#searchToken) {
                return;
            }

            this.#searchResults.set(results);

            // Once the client-side filter has run, `Tree.getRootNode()` returns
            // its cached `filteredNodes` and stops reading `value` — so
            // swapping the options in is not enough, the results would never
            // render. Re-running the filter over the new options is what makes
            // them visible.
            this.treeSelect()?.treeViewChild?._filter(query);
        } finally {
            // Only the run that is still current owns the flag. A superseded
            // run clearing it would drop the spinner while the search that
            // replaced it is still going.
            if (token === this.#searchToken) {
                this.$searching.set(false);
            }
        }
    }

    protected onCancel(): void {
        this.#ref.close();
    }

    #toTreeNodes(nodes: DotRoleNode[], expandedKeys: Set<string>): TreeNode[] {
        return nodes.map((node) => ({
            key: node.id,
            label: node.name,
            expanded: expandedKeys.has(node.id),
            // `leaf: false` is what makes PrimeNG render a toggler for a node
            // whose children have not been fetched yet. `childCount` is the
            // authoritative source; legacy search nodes lack it, so they fall
            // back to whatever children they shipped with.
            leaf:
                node.childCount !== undefined
                    ? node.childCount === 0
                    : (node.roleChildren?.length ?? 0) === 0,
            children: this.#toTreeNodes(node.roleChildren ?? [], expandedKeys)
        }));
    }

    #findNode(nodes: TreeNode[], key: string): TreeNode | null {
        for (const node of nodes) {
            if (node.key === key) {
                return node;
            }
            const found = this.#findNode(node.children ?? [], key);
            if (found) {
                return found;
            }
        }

        return null;
    }
}
