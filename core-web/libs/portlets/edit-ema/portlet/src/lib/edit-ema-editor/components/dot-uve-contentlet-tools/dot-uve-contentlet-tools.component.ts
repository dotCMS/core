import { signalMethod } from '@ngrx/signals';
import { injectDispatch } from '@ngrx/signals/events';

import { JsonPipe, NgStyle } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    output,
    signal,
    viewChild
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { TieredMenu, TieredMenuModule } from 'primeng/tieredmenu';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentlet } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { TEMP_EMPTY_CONTENTLET_TYPE } from '@dotcms/uve/internal';

import { ActionPayload } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { uveContentEditorEvents } from '../dot-uve-content-editor/dot-uve-content-editor.events';
import { ContentletArea } from '../ema-page-dropzone/types';

/**
 * An entry of the collapsed actions menu. `symbol` names its Material Symbol, which the menu's
 * item template draws as text (PrimeNG's own `icon` only takes a class).
 */
type DotHoverActionMenuItem = MenuItem & { symbol?: string };

/**
 * @class DotUveContentletToolsComponent
 * @description Show actions for a single contentlet in the UVE editor.
 */
@Component({
    selector: 'dot-uve-contentlet-tools',
    imports: [
        NgStyle,
        ButtonModule,
        MenuModule,
        TieredMenuModule,
        JsonPipe,
        TooltipModule,
        DotMessagePipe
    ],
    templateUrl: './dot-uve-contentlet-tools.component.html',
    styleUrls: ['./dot-uve-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveContentletToolsComponent {
    readonly #dotMessageService = inject(DotMessageService);
    readonly #uveStore = inject(UVEStore);
    readonly #contentEditor = injectDispatch(uveContentEditorEvents);

    readonly menuHover = viewChild<Menu>('menuHover');
    readonly menuHoverVTL = viewChild<Menu>('menuHoverVTL');
    readonly menuHoverActions = viewChild<TieredMenu>('menuHoverActions');

    /**
     * Positional and contextual data for the currently hovered contentlet.
     * This comes from the iframe mouse enter events.
     */
    readonly contentletArea = input<ContentletArea | null>(null, { alias: 'contentletArea' });

    /**
     * Bounds + payload for the selected (clicked) contentlet, sourced from the
     * UVE store. The store's resize/scroll handlers null this out when the
     * canvas changes, so the floating toolbar disappears with stale bounds and
     * re-anchors after the user clicks again.
     */
    protected readonly selected = this.#uveStore.editorSelected;
    /**
     * Controls whether the delete-content action is allowed.
     * When `false`, the delete button is disabled and a tooltip explaining why is shown.
     */
    readonly allowContentDelete = input<boolean>(true, { alias: 'allowContentDelete' });

    /**
     * FEATURE UNDER DEVELOPMENT HIDEN BEHIND A FEATURE FLAG
     * Controls whether the style editor option is shown.
     * When `true`, the style editor option is shown.
     */
    readonly showStyleEditorOption = input<boolean>(false, { alias: 'showStyleEditorOption' });

    /**
     * Emitted when the user clicks the quick-edit (sliders) button to
     * open the side panel for inline-style edits.
     */
    readonly openQuickEdit = output<void>();
    /**
     * Emitted when the user requests deletion of the current contentlet.
     * The parent component is responsible for performing and confirming the deletion.
     */
    readonly deleteContent = output<ActionPayload>();
    /**
     * Emitted when the user invokes the "add content" menu.
     * The `type` indicates what the user wants to add (content, form or widget).
     */
    readonly addContent = output<{
        type: 'content' | 'form' | 'widget';
        payload: ActionPayload;
    }>();

    /**
     * Emitted when the contentlet is selected from the tools (for example, via a drag handle).
     */
    readonly selectContent = output<ActionPayload>();

    /**
     * Opt-in flag indicating this drag source should use the custom drag image.
     * Surfaced as `data-use-custom-drag-image` so the host editor can decide
     * generically when to apply the drag preview.
     */
    protected readonly useCustomDragImage = true;

    protected readonly TEMP_EMPTY_CONTENTLET_TYPE = TEMP_EMPTY_CONTENTLET_TYPE;

    /**
     * Indicates where newly added contentlets should be inserted relative to the current one.
     * - `'before'`: add above
     * - `'after'`: add below
     */
    protected readonly buttonPosition = signal<'after' | 'before'>('after');

    /**
     * Show the hover overlay whenever a contentlet is hovered. The hover
     * overlay is the only place that renders the action toolbar (drag,
     * edit, delete, etc.); the selected overlay is just a persistent
     * border so the user can see what's selected after the panel opens.
     */
    readonly showHoverOverlay = computed(() => this.contentletArea() !== null);

    /**
     * Show the selected overlay whenever something is selected AND the
     * iframe layout isn't mid-flux. The store's `$iframeLayoutLocked`
     * predicate aggregates all transient phases (scroll, scroll+drag,
     * resize) so this gate doesn't have to enumerate them; new phases
     * added to the lock automatically apply here.
     */
    protected readonly $iframeLayoutLocked = this.#uveStore.$iframeLayoutLocked;
    readonly showSelectedOverlay = computed(
        () => this.selected() !== null && !this.$iframeLayoutLocked()
    );

    /**
     * Snapshot of the area payload augmented with the current insert position for hovered contentlet.
     * Consumers can dispatch the returned object directly.
     */
    readonly contentContext = computed<ActionPayload>(() => ({
        ...this.contentletArea()?.payload,
        position: this.buttonPosition()
    }));

    /**
     * Snapshot of the area payload augmented with the current insert position for selected contentlet.
     */
    readonly selectedContentContext = computed<ActionPayload>(() => {
        const selected = this.selected();
        return {
            ...selected?.payload,
            position: this.buttonPosition()
        };
    });

    /**
     * Whether there is at least one VTL file associated with the current contentlet.
     * Used to determine if the VTL menu should be rendered/enabled.
     */
    readonly hasVtlFiles = computed(() => !!this.contentContext()?.vtlFiles?.length);

    /**
     * Whether the current container is represented by a temporary "empty" contentlet.
     * This is used to decide when to show "add" affordances instead of regular actions.
     */
    readonly isContainerEmpty = computed(() => {
        return this.contentContext()?.contentlet?.identifier === 'TEMP_EMPTY_CONTENTLET';
    });

    /**
     * Whether the current user holds EDIT permission on the hovered contentlet.
     * Sourced from `data-dot-can-edit`, which the container renderer stamps on
     * every contentlet in EDIT mode from a WRITE-level check against the
     * contentlet instance.
     *
     * Fail-open by design: a missing value means allowed. Headless and
     * SDK-rendered pages build their own wrappers and never emit the
     * attribute, so denying on absence would grey out the toolbar on all of
     * them.
     */
    readonly canEditContentlet = computed(
        () => this.contentContext()?.contentlet?.canEdit !== false
    );

    /**
     * Tooltip key for the edit button. When the user lacks edit permission on
     * this contentlet the tooltip explains why; otherwise it just labels the
     * action, matching the other toolbar tooltips.
     */
    protected readonly editButtonTooltip = computed(() => {
        if (!this.canEditContentlet()) {
            return 'uve.contentlet.no.edit.permission';
        }

        return 'uve.tooltip.edit.full';
    });

    /**
     * Tooltip key for the quick-edit button. Shares the edit button's
     * permission message — the quick-edit form writes the same contentlet, so
     * the same explanation applies.
     */
    protected readonly quickEditButtonTooltip = computed(() => {
        if (!this.canEditContentlet()) {
            return 'uve.contentlet.no.edit.permission';
        }

        return 'uve.tooltip.edit.quick';
    });

    /**
     * Tooltip key for the style button. Style properties describe how this
     * contentlet presents itself, so they follow contentlet permission.
     */
    protected readonly styleButtonTooltip = computed(() => {
        if (!this.canEditContentlet()) {
            return 'uve.contentlet.no.edit.permission';
        }

        return 'uve.tooltip.edit.style';
    });

    /**
     * Tooltip key for the delete button. When the button is disabled
     * (e.g. on personalization), the tooltip explains why; otherwise
     * it just labels the action ("Remove") to match the other toolbar
     * tooltips.
     */
    protected readonly deleteButtonTooltip = computed(() => {
        if (!this.allowContentDelete()) {
            return 'uve.disable.delete.button.on.personalization';
        }

        return 'uve.tooltip.remove';
    });

    /**
     * Menu items used for adding new content to the layout (content, widget, and optionally form).
     * Items are localized and wired so that selecting them emits `addContent`.
     * Uses selected context when available, otherwise falls back to hovered context.
     */
    readonly menuItems = computed<MenuItem[]>(() => {
        const context = this.selected() ? this.selectedContentContext() : this.contentContext();
        return [
            {
                label: this.#dotMessageService.get('content'),
                command: () => this.addContent.emit({ type: 'content', payload: context })
            },
            {
                label: this.#dotMessageService.get('Widget'),
                command: () => this.addContent.emit({ type: 'widget', payload: context })
            },
            {
                label: this.#dotMessageService.get('form'),
                command: () => this.addContent.emit({ type: 'form', payload: context })
            }
        ];
    });

    /**
     * Menu items for the VTL files of the **hovered** contentlet — the same
     * contentlet `hasVtlFiles()` gates the `</>` button on.
     *
     * Hover, not selection: the button lives only in the hover toolbar, so the
     * hovered contentlet is the only one it can belong to. Reading the selection
     * here meant the button could be offered from one contentlet and filled from
     * another, and the selected payload loses `vtlFiles` on every SET_BOUNDS
     * re-anchor — so the menu opened empty after any layout shift.
     *
     * Choosing a file promotes the hovered contentlet to selected, so the
     * selection border follows the contentlet whose VTL was opened. Promotion is
     * on the command rather than the button click deliberately: opening the menu
     * and dismissing it without picking should leave the selection alone.
     *
     * Returns `[]` rather than `undefined` so PrimeNG is never handed an
     * undefined `[model]`, and so this honors its own declared return type.
     */
    readonly vtlMenuItems = computed<MenuItem[]>(() => {
        const { vtlFiles } = this.contentContext() ?? {};
        return (
            vtlFiles?.map((file) => ({
                label: file?.name,
                command: () => {
                    this.promoteHoverToSelected();
                    this.#contentEditor.uveEditSourceRequested(file);
                }
            })) ?? []
        );
    });

    /**
     * Menu items for the collapsed actions toolbar (small contentlets).
     * Mirrors the icon-row buttons one-for-one, in the same order: Edit (quick
     * edit), Styles, Full Editor, Code, then Remove. The drag button is NOT
     * included — it lives outside `.actions` (left-center of the border) and
     * stays visible at all sizes. Code is a nested submenu of the
     * contentlet's VTL files (PrimeNG `<p-menu>` honors `items` on a MenuItem).
     * Each entry carries the toolbar button's Material Symbol as `symbol`.
     */
    readonly actionsMenuItems = computed<DotHoverActionMenuItem[]>(() => {
        const context = this.contentContext();
        const items: DotHoverActionMenuItem[] = [];

        items.push({
            label: this.#dotMessageService.get('uve.tooltip.edit.quick'),
            symbol: 'edit',
            disabled: !this.canEditContentlet(),
            command: () => {
                this.promoteHoverToSelected();
                this.openQuickEdit.emit();
            }
        });

        if (this.showStyleEditorOption()) {
            items.push({
                label: this.#dotMessageService.get('uve.tooltip.edit.style'),
                symbol: 'palette',
                disabled: !this.canEditContentlet(),
                command: () => {
                    this.promoteHoverToSelected();
                    this.selectContent.emit(context);
                }
            });
        }

        items.push({
            label: this.#dotMessageService.get('uve.tooltip.edit.full'),
            symbol: 'edit_note',
            disabled: !this.canEditContentlet(),
            command: () => this.editContent(context)
        });

        const vtlSubmenu = this.vtlMenuItems();
        if (vtlSubmenu.length) {
            items.push({
                label: this.#dotMessageService.get('uve.tooltip.edit.vtl'),
                symbol: 'code',
                items: vtlSubmenu
            });
        }

        items.push({
            label: this.#dotMessageService.get('uve.tooltip.remove'),
            symbol: 'close',
            disabled: !this.allowContentDelete(),
            command: () => this.deleteContent.emit(context)
        });

        return items;
    });

    /**
     * Inline styles that bound the floating toolbar to the visual rectangle of the hovered contentlet.
     * The toolbar is absolutely positioned based on the coordinates in `contentletArea`.
     */
    protected readonly hoverBoundsStyles = computed(() => {
        const contentletArea = this.contentletArea();
        return {
            left: `${contentletArea?.x ?? 0}px`,
            top: `${contentletArea?.y ?? 0}px`,
            width: `${contentletArea?.width ?? 0}px`,
            height: `${contentletArea?.height ?? 0}px`
        };
    });

    /**
     * How far (in px) the top toolbar row (add-top button + actions) must shift
     * down from the hovered contentlet's own top edge to stay inside the visible
     * iframe area. `null` (no override) when the top edge is already visible —
     * the contentlet's top can scroll above the iframe's internal viewport,
     * which would otherwise push the toolbar off-screen along with it.
     */
    protected readonly hoverTopClipOffset = computed<number | null>(() => {
        const area = this.contentletArea();
        if (!area || area.y >= 0) {
            return null;
        }

        return Math.min(-area.y, area.height);
    });

    /**
     * How far (in px) the add-bottom button must shift up from the hovered
     * contentlet's own bottom edge to stay inside the visible iframe area.
     * `null` (no override) when the bottom edge is already visible.
     */
    protected readonly hoverBottomClipOffset = computed<number | null>(() => {
        const area = this.contentletArea();
        if (!area) {
            return null;
        }

        const overflow = area.y + area.height - this.#uveStore.viewIframeHeight();

        return overflow > 0 ? Math.min(overflow, area.height) : null;
    });

    /**
     * The drag handle sits at the vertical center of the hovered contentlet
     * (`top: 50%` in CSS). On a tall contentlet, that center point can itself
     * be scrolled outside the visible iframe area even while the handle's
     * default position would otherwise be off-screen. This clamps the
     * handle's `top` to the closest visible point along the contentlet's own
     * height, keeping the CSS `translate(-50%, -50%)` centering unchanged.
     * `null` (no override) when the natural center is already visible.
     */
    protected readonly hoverDragButtonTopOffset = computed<number | null>(() => {
        const area = this.contentletArea();
        if (!area) {
            return null;
        }

        const iframeHeight = this.#uveStore.viewIframeHeight();
        const center = area.y + area.height / 2;
        const clampedCenter = Math.min(Math.max(center, 0), iframeHeight);

        if (clampedCenter === center) {
            return null;
        }

        return Math.min(Math.max(clampedCenter - area.y, 0), area.height);
    });

    /**
     * Inline styles that bound the floating toolbar to the visual rectangle of the selected contentlet.
     * The toolbar is absolutely positioned based on `editorSelected.bounds`.
     */
    protected readonly selectedBoundsStyles = computed(() => {
        const bounds = this.selected()?.bounds;
        return {
            left: `${bounds?.x ?? 0}px`,
            top: `${bounds?.y ?? 0}px`,
            width: `${bounds?.width ?? 0}px`,
            height: `${bounds?.height ?? 0}px`
        };
    });

    /**
     * Drag payload for the hovered contentlet's action toolbar — reads the
     * hover context so the drag handle dispatches the contentlet under the
     * pointer.
     */
    readonly hoverDragPayload = computed(() => {
        const { container, contentlet } = this.contentContext();

        if (!contentlet) {
            return {
                container: null,
                contentlet: null,
                showLabelImage: false,
                move: false
            };
        }

        return {
            container,
            contentlet,
            showLabelImage: true,
            move: true
        };
    });

    /**
     * Hides the menus when the contentlet area changes.
     */
    readonly hideMenusMethod = signalMethod<ContentletArea>((_area) => {
        this.hideMenus();
    });

    constructor() {
        this.hideMenusMethod(this.contentletArea);
    }

    /**
     * Pencil: asks for the full content editor on the hovered contentlet. Sends its container
     * along, so a contentlet on more than one page is first offered as a copy on this page only.
     *
     * Reads the hovered payload, NOT editorSelected — the pencil is intentionally stateless with
     * respect to editor selection, so it does not call {@link promoteHoverToSelected}.
     *
     * Defense in depth for the disabled pencil: the toolbar can act from stale bounds or be
     * driven programmatically, so the permission check must not rest on the disabled button
     * alone. Fail-open: only an explicit `false` denies, because headless pages carry no
     * permission.
     */
    protected editContent({ contentlet, container }: ActionPayload): void {
        if (!contentlet?.inode || contentlet.canEdit === false) {
            return;
        }

        this.#contentEditor.uveEditContentRequested({
            contentlet: contentlet as unknown as DotCMSContentlet,
            forkFrom: container
        });
    }

    /**
     * Stores the desired insertion position used when the add-menu commands fire.
     */
    protected setPositionFlag(position: 'before' | 'after'): void {
        this.buttonPosition.set(position);
    }

    /**
     * Pin the hovered contentlet as the selected one — bounds for the
     * persistent overlay border AND payload for the side panel. One
     * write covers both surfaces because they live in the same
     * `editorSelected` record now.
     *
     * Used by the bolt (quick edit) and palette (style editor) buttons.
     * The pencil button is intentionally stateless and does NOT call
     * this — opening the full-editor modal mustn't change what's
     * selected in the editor.
     */
    protected promoteHoverToSelected(): void {
        const hovered = this.contentletArea();
        if (!hovered) return;

        // Hover x/y/width/height are already in the editor's canvas
        // coordinate space — the SDK's auto-bounds channel reports them
        // post-zoom (the page inside the iframe sees an iframe-relative
        // viewport, but the dispatched coords are mapped to the canvas).
        // Copying them straight into editorSelected.bounds is correct.
        this.#uveStore.setSelected({
            bounds: {
                x: hovered.x,
                y: hovered.y,
                width: hovered.width,
                height: hovered.height
            },
            payload: this.contentContext()
        });
    }

    /**
     * Ensures PrimeNG menus close when the hovered contentlet changes.
     */
    protected hideMenus(): void {
        this.menuHover()?.hide();
        this.menuHoverVTL()?.hide();
        this.menuHoverActions()?.hide();
    }
}
