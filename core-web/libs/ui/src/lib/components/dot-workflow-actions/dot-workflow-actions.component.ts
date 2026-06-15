import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { SplitButtonModule } from 'primeng/splitbutton';

import { map } from 'rxjs/operators';

import { DotCMSActionSubtype, DotCMSWorkflowAction } from '@dotcms/dotcms-models';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

type ButtonSize = 'normal' | 'small' | 'large';

interface WorkflowActionsGroup {
    mainAction: MenuItem;
    subActions: MenuItem[];
}

/**
 * Maximum number of workflow actions rendered as inline buttons on a wide viewport.
 * Narrower screens use a lower cap via {@link DotWorkflowActionsComponent.#inlineCap}.
 */
const MAX_INLINE_ACTIONS = 4;

/**
 * Displays workflow actions as a command bar.
 *
 * ## Flat mode (default, `groupActions=false`)
 * Up to four actions are shown inline when the viewport allows, ordered right to left:
 * - 1st action (rightmost): default solid button (no variant)
 * - 2nd+ actions: outlined buttons
 *
 * When there are more actions than the inline cap, the rest go to an overflow menu (···).
 * The inline cap follows CDK {@link Breakpoints}: XSmall → 0, Small → 1,
 * Medium → 2, Large → 3, XLarge and wider → {@link MAX_INLINE_ACTIONS} (4).
 * SEPARATOR actions are always filtered out before rendering.
 *
 * ## Grouped mode (`groupActions=true`)
 * Actions are grouped by SEPARATOR entries. Each group renders as a `p-splitButton`:
 * the first action in the group is the main button; the rest appear in its dropdown.
 * Groups with a single action render as a plain `p-button`.
 * Use this mode in constrained UIs like the UVE toolbar to preserve the classic split-button layout.
 *
 * @example
 * <!-- Flat mode (edit-content) -->
 * <dot-workflow-actions [actions]="workflowActions" (actionFired)="onAction($event)" />
 *
 * @example
 * <!-- Grouped/split-button mode (UVE) -->
 * <dot-workflow-actions [actions]="workflowActions" [groupActions]="true" (actionFired)="onAction($event)" />
 *
 * ## Stacked mode (`stacked=true`)
 * Renders ALL actions as full-width buttons stacked vertically (top to bottom), with no overflow
 * menu and no breakpoint-based cap. The first action is the solid/primary button; the rest are
 * outlined. Used in the narrow edit-content sidebar where actions must list one above another.
 *
 * @example
 * <!-- Stacked mode (edit-content sidebar) -->
 * <dot-workflow-actions [actions]="workflowActions" [stacked]="true" (actionFired)="onAction($event)" />
 */
@Component({
    selector: 'dot-workflow-actions',
    imports: [ButtonModule, MenuModule, SplitButtonModule, DotMessagePipe],
    templateUrl: './dot-workflow-actions.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex gap-2',
        '[class.flex-col]': 'stacked()',
        '[class.flex-row-reverse]': '!stacked()'
    }
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
     * Only used in flat mode (`groupActions=false`).
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
     * When true, renders actions as `p-splitButton` groups separated by SEPARATOR actions.
     * Use this in the UVE toolbar to preserve the classic split-button layout.
     * When false (default), renders actions as inline buttons with an overflow menu.
     */
    groupActions = input<boolean>(false);

    /**
     * When true, renders ALL actions as full-width buttons stacked vertically (top to bottom),
     * with no overflow menu and no breakpoint-based cap. The first action is solid/primary, the
     * rest are outlined. Takes precedence over the flat overflow layout; ignored when
     * `groupActions` is true. Use this in the narrow edit-content sidebar.
     *
     * NOTE: stacked mode iterates the SEPARATOR-filtered action list, so any SEPARATOR actions
     * sent by the backend are dropped — it cannot render visual dividers between action groups.
     */
    stacked = input<boolean>(false);

    /**
     * Button size passed through to PrimeNG.
     * 'normal' maps to PrimeNG's default (no size attribute).
     */
    size = input<ButtonSize>('normal');

    /**
     * Emits the selected {@link DotCMSWorkflowAction} when the user clicks any action.
     */
    actionFired = output<DotCMSWorkflowAction>();

    // --- Grouped mode (p-splitButton, groupActions=true) ---

    /**
     * Actions grouped by SEPARATOR entries, each mapped to a mainAction + subActions pair.
     * Empty array when `groupActions=false`.
     */
    protected $groupedActions = computed((): WorkflowActionsGroup[] => {
        if (!this.groupActions()) return [];

        return this.actions()
            .reduce<DotCMSWorkflowAction[][]>(
                (acc, action) => {
                    if (action?.metadata?.subtype === DotCMSActionSubtype.SEPARATOR) {
                        acc.push([]);
                    } else {
                        acc[acc.length - 1].push(action);
                    }

                    return acc;
                },
                [[]]
            )
            .filter((group) => group.length > 0)
            .map(([first, ...rest]) => ({
                mainAction: {
                    label: first.name,
                    command: () => this.actionFired.emit(first)
                },
                subActions: rest.map((action) => ({
                    label: action.name,
                    command: () => this.actionFired.emit(action)
                }))
            }));
    });

    // --- Flat mode (inline buttons + overflow menu, groupActions=false) ---

    /**
     * All actions with SEPARATOR entries removed.
     * Used only in flat mode (`groupActions=false`).
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
     * - 0 → undefined (no variant — default solid button)
     * - 1+ → 'outlined'
     *
     * undefined is intentional for index 0: Angular drops undefined bindings entirely,
     * so PrimeNG receives no variant and renders its default button style.
     */
    protected getVariant(index: number): 'outlined' | undefined {
        return index > 0 ? 'outlined' : undefined;
    }

    protected fireAction(action: DotCMSWorkflowAction): void {
        this.actionFired.emit(action);
    }
}
