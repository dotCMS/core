import { catchError, of, take } from 'rxjs';

import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { DotRolesService } from '@dotcms/data-access';
import { DotRole } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

/** What a workflow action's `assignable` / `commentable` inputs collect. */
export interface DotWorkflowAssignCommentValue {
    /** Role id of the assignee. Empty when the action is not assignable. */
    assign: string;
    /** Workflow comment. Empty when the action is not commentable. */
    comment: string;
}

/**
 * Collects the `assignable` and `commentable` inputs a workflow action declares.
 *
 * Both live in one component because the backend treats them as one concern — the legacy wizard merges
 * them into a single `commentAndAssign` step — and because an action commonly declares both. Either
 * field can be switched off independently, so this renders one, the other, or both.
 *
 * **Deliberately not the legacy `DotCommentAndAssignFormComponent`.** That one also renders a move-path
 * picker (`dot-page-selector`) as a third field, which conflates two separate action inputs; consumers
 * that collect a move destination their own way would get a duplicate control. This covers assign and
 * comment only, and leaves `moveable` to whoever owns that step.
 *
 * Presentational and dialog-free: it emits its value and validity outward and renders no header,
 * footer or submit control, so it can sit inside a host's own step frame.
 */
@Component({
    selector: 'dot-workflow-assign-comment',
    imports: [FormsModule, SelectModule, TextareaModule, DotMessagePipe],
    templateUrl: './dot-workflow-assign-comment.component.html',
    providers: [DotRolesService],
    host: { class: 'block' }
})
export class DotWorkflowAssignCommentComponent {
    readonly #rolesService = inject(DotRolesService);

    /** Render the assignee field. Mirrors the action's `assignable` input. */
    readonly assignable = input<boolean>(false);
    /** Render the comment field. Mirrors the action's `commentable` input. */
    readonly commentable = input<boolean>(false);
    /**
     * The action's `nextAssign` role, which scopes the assignable roles the backend returns.
     */
    readonly roleId = input<string>('');
    /** The action's `roleHierarchyForAssign`, passed straight through to the roles lookup. */
    readonly roleHierarchy = input<boolean>(false);
    /** Freezes both fields, used while an action is in flight. */
    readonly disabled = input<boolean>(false);

    /** The collected value, emitted on every change. */
    readonly valueChange = output<DotWorkflowAssignCommentValue>();
    /**
     * Whether the value is complete enough to fire.
     *
     * Emitted separately so a host can gate its own Continue without reaching into this component.
     */
    readonly validChange = output<boolean>();

    /** Assignable roles, empty until the lookup settles or when the action is not assignable. */
    protected readonly $roles = signal<DotRole[]>([]);
    /** True while the roles lookup is in flight; the select stays disabled until it settles. */
    protected readonly $loadingRoles = signal<boolean>(false);

    protected readonly $assign = signal<string>('');
    protected readonly $comment = signal<string>('');

    protected readonly $roleOptions = computed(() =>
        this.$roles().map((role) => ({ label: role.name, value: role.id }))
    );

    /**
     * Valid once an assignee is chosen, when one is required.
     *
     * A comment is never required — the backend accepts an empty one — so a commentable-only action is
     * valid immediately, which is correct: there is nothing the user must supply.
     */
    protected readonly $valid = computed(() => !this.assignable() || !!this.$assign());

    constructor() {
        // Roles are fetched only for an assignable action, and re-fetched if the action changes (a
        // different action scopes assignable roles differently through `nextAssign`).
        effect(() => {
            if (!this.assignable()) {
                this.$roles.set([]);

                return;
            }

            this.loadRoles(this.roleId(), this.roleHierarchy());
        });

        // One effect publishes both outputs, so a host can never see a value and a validity that
        // disagree about the same state.
        effect(() => {
            this.valueChange.emit({ assign: this.$assign(), comment: this.$comment() });
            this.validChange.emit(this.$valid());
        });
    }

    protected onAssignChange(assign: string): void {
        this.$assign.set(assign ?? '');
    }

    protected onCommentChange(comment: string): void {
        this.$comment.set(comment ?? '');
    }

    /**
     * Loads the roles this action can assign to, defaulting to the first.
     *
     * Defaulting matches the legacy form and means an assignable action is valid on arrival rather than
     * blocking behind a field the user has no opinion about. A failed lookup leaves no options and no
     * default, so validity correctly reports that nothing can be assigned.
     */
    private loadRoles(roleId: string, roleHierarchy: boolean): void {
        this.$loadingRoles.set(true);

        this.#rolesService
            .get(roleId, roleHierarchy)
            .pipe(
                take(1),
                catchError(() => of([] as DotRole[]))
            )
            .subscribe((roles) => {
                this.$roles.set(roles);
                this.$loadingRoles.set(false);
                this.$assign.set(roles.length ? roles[0].id : '');
            });
    }
}
