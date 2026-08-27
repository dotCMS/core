import { Component, computed, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotRolesStore } from '../dot-roles-page/store/dot-roles.store';
import { DotRoleFormValue, DotRoleNode } from '../models/dot-roles.models';

interface ParentOption {
    label: string;
    value: string | null;
}

/**
 * Add Role dialog. POST /v1/roles already exists so this form is fully
 * functional. Opened from the `New` button in the roles panel and from
 * the inline `+` on a parent row (which prefills `Parent`).
 */
@Component({
    selector: 'dot-roles-add',
    standalone: true,
    imports: [
        ReactiveFormsModule,
        ButtonModule,
        InputTextModule,
        TextareaModule,
        CheckboxModule,
        SelectModule,
        DotMessagePipe
    ],
    templateUrl: './dot-roles-add.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotRolesAddComponent {
    readonly #store = inject(DotRolesStore);
    readonly #fb = inject(FormBuilder);
    readonly #ref = inject(DynamicDialogRef);
    readonly #config = inject(DynamicDialogConfig);
    readonly #messageService = inject(DotMessageService);

    protected readonly $submitting = signal(false);
    protected readonly $error = signal<string | null>(null);

    protected readonly form = this.#fb.nonNullable.group({
        roleName: ['', Validators.required],
        roleKey: [''],
        parentRoleId: [null as string | null],
        canEditUsers: [true],
        canEditPermissions: [true],
        canEditLayouts: [true],
        description: ['']
    });

    protected readonly $parentOptions = computed<ParentOption[]>(() => [
        { label: this.#messageService.get('roles.form.parent.root'), value: null },
        ...this.#flattenRoles(this.#store.roleTree())
    ]);

    constructor() {
        const prefilledParent = this.#config.data?.parentRoleId ?? null;
        if (prefilledParent) {
            this.form.controls.parentRoleId.setValue(prefilledParent);
        }
    }

    protected onSave(): void {
        if (this.form.invalid || this.$submitting()) {
            return;
        }

        this.$submitting.set(true);
        this.$error.set(null);

        const value = this.form.getRawValue() as DotRoleFormValue;

        this.#store.createRole(value).then((created) => {
            this.$submitting.set(false);
            if (created) {
                this.#ref.close(created);
            } else {
                this.$error.set('roles.add.error');
            }
        });
    }

    protected onCancel(): void {
        this.#ref.close();
    }

    #flattenRoles(nodes: DotRoleNode[], depth = 0): ParentOption[] {
        return nodes.reduce<ParentOption[]>((acc, node) => {
            acc.push({
                label: `${' '.repeat(depth * 2)}${node.name}`,
                value: node.id
            });
            if (node.roleChildren?.length) {
                acc.push(...this.#flattenRoles(node.roleChildren, depth + 1));
            }

            return acc;
        }, []);
    }
}
