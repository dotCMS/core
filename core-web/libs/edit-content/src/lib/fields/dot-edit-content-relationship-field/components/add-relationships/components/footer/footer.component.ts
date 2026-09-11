import { ChangeDetectionStrategy, Component, computed, inject, output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

import { AddRelationshipsInput } from '../../models/add-relationships.models';
import { AddRelationshipsStore } from '../../store/add-relationships.store';

/**
 * The dialog's footer.
 *
 * Its whole reason for existing as a component is the thing it does **not** do: bind `disabled` on
 * the confirm action.
 *
 * The dialog this replaces carried `[disabled]="totalItems === 0"`, which made "uncheck the last
 * row" the single case where unchecking did not unrelate — the editor could remove every other
 * relationship through the dialog but not the last one. Content Drive's own footer already had it
 * right, with the comment "Apply stays enabled at zero selections here: clearing is a valid filter
 * state". This is the same rule, applied to the edit-time surface.
 *
 * Confirming an empty selection empties the relationship. On a required field that leaves an empty
 * required field, which the form's existing validation reports exactly as it does when the editor
 * removes the last row by hand — no validation rule lives here.
 */
@Component({
    selector: 'dot-add-relationships-footer',
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './footer.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class AddRelationshipsFooterComponent {
    protected readonly store = inject(AddRelationshipsStore);

    readonly #config = inject<DynamicDialogConfig<AddRelationshipsInput>>(DynamicDialogConfig, {
        optional: true
    });

    /**
     * Content Drive opens this dialog as a filter, where the verb is "Apply" rather than "Add
     * Relationships". One footer, one label input — see {@link AddRelationshipsInput.confirmLabel}.
     */
    protected readonly $confirmLabel = computed(
        () => this.#config?.data?.confirmLabel ?? 'dot.relationship.add.dialog.confirm'
    );

    readonly confirm = output<void>();
    readonly cancel = output<void>();
}
