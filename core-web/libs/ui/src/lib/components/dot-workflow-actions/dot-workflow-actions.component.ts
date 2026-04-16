import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';

import { map } from 'rxjs/operators';

import { DotCMSActionSubtype, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

type ButtonSize = 'normal' | 'small' | 'large';

/**
 * Maximum number of workflow actions rendered as inline buttons on a wide viewport.
 * Narrower screens use a lower cap via {@link DotWorkflowActionsComponent.#inlineCap}.
 */
const MAX_INLINE_ACTIONS = 4;

/**
 * Displays workflow actions as a command bar.
 *
 * Up to four actions are shown inline when the viewport allows, ordered right to left:
 * - 1st action (rightmost): default solid button (no variant)
 * - 2nd action: outlined button (border, transparent background)
 * - 3rd action: outlined (same tier as 2nd in current styling)
 *
 * When there are more actions than the inline cap, the rest go to an overflow menu (···).
 * The inline cap follows CDK {@link Breakpoints} in {@link #inlineCap}: XSmall → 0, Small → 1,
 * Medium → 2, Large → 3, XLarge and wider → {@link MAX_INLINE_ACTIONS} (4).
 *
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
     * CDK helper that listens to viewport media queries; used to derive {@link #inlineCap}
     * without manual `matchMedia` subscriptions.
     */
    readonly #breakpointObserver = inject(BreakpointObserver);

    /**
     * How many workflow actions render as inline buttons (0–4) for the current viewport.
     * XSmall → 0, Small → 1, Medium → 2, Large → 3, XLarge and wider → 4.
     */
    readonly #inlineCap = toSignal(
        this.#breakpointObserver
            .observe([Breakpoints.XSmall, Breakpoints.Small, Breakpoints.Medium, Breakpoints.Large])
            .pipe(
                map(() => {
                    if (this.#breakpointObserver.isMatched(Breakpoints.XSmall)) return 0;
                    if (this.#breakpointObserver.isMatched(Breakpoints.Small)) return 1;
                    if (this.#breakpointObserver.isMatched(Breakpoints.Medium)) return 2;
                    if (this.#breakpointObserver.isMatched(Breakpoints.Large)) return 3;

                    return MAX_INLINE_ACTIONS;
                })
            ),
        { initialValue: MAX_INLINE_ACTIONS }
    );

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
     * Actions rendered as inline buttons — slice length follows {@link #inlineCap} (can be 0).
     */
    protected $visibleActions = computed(() => this.$flatActions().slice(0, this.#inlineCap()));

    /**
     * Actions not shown inline, mapped to PrimeNG {@link MenuItem} for the overflow popup menu.
     */
    protected $overflowActions = computed((): MenuItem[] =>
        this.$flatActions()
            .slice(this.#inlineCap())
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
     * Returns the PrimeNG button variant for a given position index among visible inline buttons.
     * - 0 → null (no variant — default solid button)
     * - 1+ → 'outlined'
     *
     * null is intentional for index 0: Angular drops null bindings entirely,
     * so PrimeNG receives no variant and renders its default button style.
     */
    protected getVariant(index: number): 'outlined' | null {
        if (index > 0) return 'outlined';

        return null;
    }

    protected fireAction(action: DotCMSWorkflowAction): void {
        this.actionFired.emit(action);
    }
}
