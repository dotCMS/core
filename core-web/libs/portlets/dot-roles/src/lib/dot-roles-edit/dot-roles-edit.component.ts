import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';
import { DotRoleDetail, DotRoleFormValue, DotRoleNode } from '../models/dot-roles.models';

interface ParentOption {
    label: string;
    value: string | null;
}

const ROOT_PARENT: ParentOption = { label: 'None (top level)', value: null };

/**
 * Edit Role dialog. Wired to PUT /v1/roles/{roleId} via the store (#36936).
 *
 * System and locked roles are read-only per the backend gate: the form is
 * still shown but Save is disabled with a tooltip explaining why. The
 * `Delete` action is deferred to task #36939 wiring (a follow-up PR).
 *
 * The parent picker is filtered so the currently-edited role and its
 * descendants aren't selectable — the backend would reject a cycle with 400,
 * but we surface the constraint visually.
 */
@Component({
    selector: 'dot-roles-edit',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        CheckboxModule,
        SelectModule,
        TooltipModule,
        ConfirmDialogModule,
        DotMessagePipe
    ],
    providers: [ConfirmationService],
    templateUrl: './dot-roles-edit.component.html'
})
export class DotRolesEditComponent {
    readonly #store = inject(DotRolesStore);
    readonly #fb = inject(FormBuilder);
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);

    protected readonly role: DotRoleDetail = this.#config.data?.role;

    protected readonly $submitting = signal(false);
    protected readonly $error = signal<string | null>(null);

    protected readonly readOnly = this.role?.system === true || this.role?.locked === true;

    protected readonly form = this.#fb.nonNullable.group({
        roleName: [this.role?.name ?? '', Validators.required],
        roleKey: [this.role?.roleKey ?? ''],
        parentRoleId: [this.#normalizeParentId(this.role) as string | null],
        canEditUsers: [this.role?.editUsers ?? true],
        canEditPermissions: [this.role?.editPermissions ?? true],
        canEditLayouts: [this.role?.editLayouts ?? true],
        description: [this.role?.description ?? '']
    });

    protected readonly $parentOptions = computed<ParentOption[]>(() => {
        const exclude = new Set<string>();
        this.#collectDescendantIds(this.#findInTree(this.#store.roleTree(), this.role.id), exclude);
        exclude.add(this.role.id);

        return [ROOT_PARENT, ...this.#flattenRoles(this.#store.roleTree(), exclude)];
    });

    constructor() {
        if (this.readOnly) {
            this.form.disable();
        }
    }

    protected onSave(): void {
        if (this.form.invalid || this.$submitting() || this.readOnly) {
            return;
        }

        this.$submitting.set(true);
        this.$error.set(null);

        const value = this.form.getRawValue() as DotRoleFormValue;

        this.#store.updateRole(this.role.id, value).then((updated) => {
            this.$submitting.set(false);
            if (updated) {
                this.#ref.close(updated);
            } else {
                this.$error.set('roles.edit.error');
            }
        });
    }

    protected onCancel(): void {
        this.#ref.close();
    }

    /**
     * Delete flow: confirms first, then delegates to the store. The BE
     * cascades, so the confirm copy names the blast radius when we already
     * know it (the store may enrich it via `usersAffected` post-delete via a
     * follow-up toast, but the confirm itself is intentionally conservative
     * — always call out that the deletion is permanent).
     *
     * System/locked roles never reach this button; it's disabled at the
     * template level. The BE also rejects them with 403.
     */
    protected onDelete(): void {
        if (this.readOnly) {
            return;
        }

        this.#confirmationService.confirm({
            message: this.#messageService.get('roles.confirm.delete.message', this.role.name),
            header: this.#messageService.get('roles.confirm.delete.header'),
            // Plain "Delete" (not "Delete Role") — the header already names the object.
            acceptLabel: this.#messageService.get('Delete'),
            rejectLabel: this.#messageService.get('roles.action.cancel'),
            // Default (primary) styling — no red — per UX guidance for this
            // confirm even though the action is destructive.
            rejectButtonStyleClass: 'p-button-text',
            defaultFocus: 'reject',
            closable: true,
            closeOnEscape: true,
            position: 'center',
            accept: () => {
                this.$submitting.set(true);
                this.#store.deleteRole(this.role.id).then((result) => {
                    this.$submitting.set(false);
                    if (result?.deleted) {
                        this.#ref.close({ deleted: true, ...result });
                    } else {
                        this.$error.set('roles.delete.error');
                    }
                });
            }
        });
    }

    /**
     * Roots come back from the BE with `parent === id` (self-referential).
     * Normalize to `null` so the select shows "None (top level)".
     */
    #normalizeParentId(role: DotRoleDetail | undefined): string | null {
        if (!role) {
            return null;
        }
        if (!role.parent || role.parent === role.id) {
            return null;
        }

        return role.parent;
    }

    #flattenRoles(nodes: DotRoleNode[], exclude: Set<string>, depth = 0): ParentOption[] {
        return nodes.reduce<ParentOption[]>((acc, node) => {
            if (exclude.has(node.id)) {
                return acc;
            }
            acc.push({
                label: `${' '.repeat(depth * 2)}${node.name}`,
                value: node.id
            });
            if (node.roleChildren?.length) {
                acc.push(...this.#flattenRoles(node.roleChildren, exclude, depth + 1));
            }

            return acc;
        }, []);
    }

    #findInTree(nodes: DotRoleNode[], id: string): DotRoleNode | null {
        for (const node of nodes) {
            if (node.id === id) {
                return node;
            }
            if (node.roleChildren?.length) {
                const found = this.#findInTree(node.roleChildren, id);
                if (found) {
                    return found;
                }
            }
        }

        return null;
    }

    #collectDescendantIds(node: DotRoleNode | null, into: Set<string>): void {
        if (!node?.roleChildren?.length) {
            return;
        }
        for (const child of node.roleChildren) {
            into.add(child.id);
            this.#collectDescendantIds(child, into);
        }
    }
}
