import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { DotCMSActionSubtype, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

type ButtonSize = 'normal' | 'small' | 'large';

/**
 * Maximum number of workflow actions rendered as inline buttons.
 * Actions beyond this limit are collapsed into the overflow menu.
 *
 * Layout (right to left):
 *   [overflow ···] [text] [outlined] [primary]
 */
const MAX_INLINE_ACTIONS = 3;

/**
 * Displays workflow actions as a command bar.
 *
 * Up to three actions are shown inline, ordered right to left:
 * - 1st action (rightmost): default solid button (no variant)
 * - 2nd action: outlined button (border, transparent background)
 * - 3rd action: text button (no border, no background)
 *
 * When more than three actions exist, the excess are collapsed into
 * an overflow menu (···) rendered to the right of the inline buttons.
 * SEPARATOR actions are always filtered out before rendering.
 *
 * @example
 * <dot-workflow-actions
 *   [actions]="workflowActions"
 *   [loading]="isSaving"
 *   (actionFired)="onAction($event)" />
 */
@Component({
    selector: 'dot-workflow-actions',
    imports: [ButtonModule, MenuModule, DotMessagePipe],
    templateUrl: './dot-workflow-actions.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-row-reverse gap-2' }
})
export class DotWorkflowActionsComponent {
    /**
     * List of workflow actions to display.
     * SEPARATOR actions are filtered out automatically.
     */
    actions = input.required<DotCMSWorkflowAction[]>();

    /**
     * Shows a loading spinner on the primary (first) action button.
     */
    loading = input<boolean>(false);

    /**
     * Disables all action buttons.
     */
    disabled = input<boolean>(false);

    /**
     * Button size passed through to PrimeNG.
     * 'normal' maps to PrimeNG's default (no size attribute).
     */
    size = input<ButtonSize>('normal');

    /**
     * Emits the selected {@link DotCMSWorkflowAction} when the user clicks any action,
     * including actions inside the overflow menu.
     */
    actionFired = output<DotCMSWorkflowAction>();

    /**
     * All actions with SEPARATOR entries removed.
     */
    protected $flatActions = computed(() =>
        this.actions().filter(
            (action) => action?.metadata?.subtype !== DotCMSActionSubtype.SEPARATOR
        )
    );

    /**
     * The first {@link MAX_INLINE_ACTIONS} actions rendered as inline buttons.
     */
    protected $visibleActions = computed(() => this.$flatActions().slice(0, MAX_INLINE_ACTIONS));

    /**
     * Actions beyond {@link MAX_INLINE_ACTIONS}, mapped to PrimeNG {@link MenuItem}
     * for use in the overflow popup menu.
     */
    protected $overflowActions = computed((): MenuItem[] =>
        this.$flatActions()
            .slice(MAX_INLINE_ACTIONS)
            .map((action) => ({
                label: action.name,
                command: () => this.actionFired.emit(action)
            }))
    );

    /**
     * Maps the component's ButtonSize to PrimeNG's accepted size values.
     * PrimeNG does not accept 'normal', so it is converted to undefined (default).
     */
    protected getButtonSize(): 'small' | 'large' | undefined {
        const s = this.size();

        return s === 'normal' ? undefined : s;
    }

    /**
     * Returns the PrimeNG button variant for a given position index.
     * - 0 → null       (no variant — PrimeNG renders the default solid button)
     * - 1 → 'outlined' (border, transparent background)
     * - 2+ → 'text'    (no border, no background)
     *
     * null is intentional for index 0: Angular drops null bindings entirely,
     * so PrimeNG receives no variant and renders its default button style.
     */
    protected getVariant(index: number): 'outlined' | 'text' | null {
        if (index === 1) return 'outlined';
        if (index > 1) return 'text';

        return null;
    }

    protected fireAction(action: DotCMSWorkflowAction): void {
        this.actionFired.emit(action);
    }
}
