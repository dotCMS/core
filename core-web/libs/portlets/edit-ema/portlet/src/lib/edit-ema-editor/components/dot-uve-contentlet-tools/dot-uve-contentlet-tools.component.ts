import { signalMethod } from '@ngrx/signals';

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
import { DotMessagePipe } from '@dotcms/ui';
import { TEMP_EMPTY_CONTENTLET_TYPE } from '@dotcms/uve/internal';

import { ActionPayload, VTLFile } from '../../../shared/models';
import { UVEStore } from '../../../store/dot-uve.store';
import { ContentletArea } from '../ema-page-dropzone/types';

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
     * Emitted when the user chooses to edit a VTL file associated with the current contentlet.
     */
    readonly editVTL = output<VTLFile>();
    /**
     * Emitted when the user clicks the quick-edit (sliders) button to
     * open the side panel for inline-style edits.
     */
    readonly openQuickEdit = output<void>();
    /**
     * Emitted when the user clicks the pencil button to open the full
     * content editor as a modal dialog. Carries the hovered contentlet's
     * payload so the parent can act on it without writing to
     * editorSelected — pencil
     * is intentionally "stateless" (doesn't pin the contentlet as
     * selected or active in the editor).
     */
    readonly openFullEditor = output<ActionPayload>();
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
     * Helper function to compare two contentlets by their identifier and container uuid.
     * Returns true if they represent the same contentlet in the same container instance.
     * Accepts any record carrying a `payload` field — works for both
     * `ContentletArea` (hover) and `SelectedContentlet` (selected).
     */
    isSameContentlet(
        a: { payload?: ActionPayload } | null | undefined,
        b: { payload?: ActionPayload } | null | undefined
    ): boolean {
        if (!a || !b) {
            return false;
        }

        const id1 = a.payload?.contentlet?.identifier;
        const id2 = b.payload?.contentlet?.identifier;
        const containerKey1 = `${a.payload?.container?.identifier}:${a.payload?.container?.uuid}`;
        const containerKey2 = `${b.payload?.container?.identifier}:${b.payload?.container?.uuid}`;

        return id1 !== undefined && id1 === id2 && containerKey1 === containerKey2;
    }

    /**
     * Computed property to determine if the hovered contentlet is different from the selected one.
     */
    readonly isHoveredDifferentFromSelected = computed(() => {
        const hovered = this.contentletArea();
        const selected = this.selected();
        if (!hovered || !selected) {
            return true;
        }
        return !this.isSameContentlet(hovered, selected);
    });

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
     * Whether the selected container is represented by a temporary "empty" contentlet.
     */
    readonly isSelectedContainerEmpty = computed(() => {
        return this.selectedContentContext()?.contentlet?.identifier === 'TEMP_EMPTY_CONTENTLET';
    });

    /**
     * Whether there is at least one VTL file associated with the selected contentlet.
     */
    readonly selectedHasVtlFiles = computed(() => {
        return !!this.selectedContentContext()?.vtlFiles?.length;
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
     * Menu items corresponding to the VTL files of the selected contentlet.
     * Each item represents a file and triggers the `editVTL` output when clicked.
     */
    readonly vtlMenuItems = computed<MenuItem[]>(() => {
        const context = this.selected() ? this.selectedContentContext() : this.contentContext();
        const { vtlFiles } = context ?? {};
        return vtlFiles?.map((file) => ({
            label: file?.name,
            command: () => this.editVTL.emit(file)
        }));
    });

    /**
     * Menu items for the collapsed actions toolbar (small contentlets).
     * Mirrors the icon-row buttons one-for-one. The drag button is NOT
     * included — it lives outside `.actions` (left-center of the border)
     * and stays visible at all sizes. VTL is a nested submenu (PrimeNG
     * `<p-menu>` honors `items` on a MenuItem).
     */
    readonly actionsMenuItems = computed<MenuItem[]>(() => {
        const context = this.contentContext();
        const items: MenuItem[] = [];

        const vtlSubmenu = this.vtlMenuItems();
        if (vtlSubmenu?.length) {
            items.push({
                label: this.#dotMessageService.get('uve.tooltip.edit.vtl'),
                icon: 'pi pi-code',
                items: vtlSubmenu
            });
        }

        items.push({
            label: this.#dotMessageService.get('uve.tooltip.edit.quick'),
            icon: 'pi pi-bolt',
            command: () => {
                this.promoteHoverToSelected();
                this.openQuickEdit.emit();
            }
        });

        if (this.showStyleEditorOption()) {
            items.push({
                label: this.#dotMessageService.get('uve.tooltip.edit.style'),
                icon: 'pi pi-palette',
                command: () => {
                    this.promoteHoverToSelected();
                    this.selectContent.emit(context);
                }
            });
        }

        items.push({
            label: this.#dotMessageService.get('uve.tooltip.edit.full'),
            icon: 'pi pi-pencil',
            command: () => this.openFullEditor.emit(context)
        });

        items.push({
            label: this.#dotMessageService.get('uve.tooltip.remove'),
            icon: 'pi pi-times',
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
     * Describes the draggable payload for the selected contentlet controls.
     * Returns null-like values when the source data is incomplete, allowing
     * the template to disable the drag affordance gracefully.
     */
    readonly dragPayload = computed(() => {
        const selectedContext = this.selectedContentContext();
        const { container, contentlet } = selectedContext;

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
     * Drag payload for the hovered contentlet's action toolbar. Mirrors
     * `dragPayload` but reads from the hover context so the hover toolbar's
     * drag handle dispatches the right contentlet.
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
