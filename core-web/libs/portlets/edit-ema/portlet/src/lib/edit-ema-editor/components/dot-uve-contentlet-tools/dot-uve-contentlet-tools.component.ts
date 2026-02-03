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
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { ActionPayload, ClientData, VTLFile } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

/**
 * @class DotUveContentletToolsComponent
 * @description Show actions for a single contentlet in the UVE editor.
 */
@Component({
    selector: 'dot-uve-contentlet-tools',
    imports: [NgStyle, ButtonModule, MenuModule, JsonPipe, TooltipModule, DotMessagePipe],
    templateUrl: './dot-uve-contentlet-tools.component.html',
    styleUrls: ['./dot-uve-contentlet-tools.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUveContentletToolsComponent {
    readonly #dotMessageService = inject(DotMessageService);

    readonly menu = viewChild<Menu>('menu');
    readonly menuVTL = viewChild<Menu>('menuVTL');

    /**
     * Whether the current environment is Enterprise.
     * When `true`, additional Enterprise-only actions (such as adding forms) are enabled.
     */
    readonly isEnterprise = input<boolean>(false, { alias: 'isEnterprise' });
    /**
     * Positional and contextual data for the currently hovered contentlet.
     * This comes from the iframe mouse enter events.
     */
    readonly contentletArea = input.required<ContentletArea>({ alias: 'contentletArea' });

    /**
     * Internal state tracking the selected/clicked contentlet.
     * This persists even when hovering different contentlets.
     */
    protected readonly selectedContentletArea = signal<ContentletArea | null>(null);
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
     * Emitted when the user wants to edit the contentlet itself.
     * Carries the current `ActionPayload` built from `contentletArea`.
     */
    readonly editContent = output<ActionPayload>();
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


    readonly outputSelectedContentlet = output<Pick<ClientData, 'container' | 'contentlet'>>();
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

    /**
     * Indicates where newly added contentlets should be inserted relative to the current one.
     * - `'before'`: add above
     * - `'after'`: add below
     */
    protected readonly buttonPosition = signal<'after' | 'before'>('after');

    /**
     * Helper function to compare two contentlets by their identifier.
     * Returns true if they represent the same contentlet.
     */
    protected isSameContentlet(area1: ContentletArea | null, area2: ContentletArea | null): boolean {
        if (!area1 || !area2) {
            return false;
        }
        const id1 = area1.payload?.contentlet?.identifier;
        const id2 = area2.payload?.contentlet?.identifier;
        return id1 !== undefined && id1 === id2;
    }

    /**
     * Computed property to determine if the hovered contentlet is different from the selected one.
     */
    readonly isHoveredDifferentFromSelected = computed(() => {
        const hovered = this.contentletArea();
        const selected = this.selectedContentletArea();
        if (!hovered || !selected) {
            return true;
        }
        return !this.isSameContentlet(hovered, selected);
    });

    /**
     * Computed property to determine if we should show the hover overlay.
     * Show when hovered exists and is different from selected.
     */
    readonly showHoverOverlay = computed(() => {
        const hovered = this.contentletArea();
        return hovered !== null && this.isHoveredDifferentFromSelected();
    });

    /**
     * Computed property to determine if we should show the selected overlay.
     * Show when selected exists.
     */
    readonly showSelectedOverlay = computed(() => {
        return this.selectedContentletArea() !== null;
    });

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
        const selected = this.selectedContentletArea();
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
     * Tooltip key for the delete button.
     * Returns an i18n key when delete is disabled, or `null` when the button is enabled.
     */
    protected readonly deleteButtonTooltip = computed(() => {
        if (!this.allowContentDelete()) {
            return 'uve.disable.delete.button.on.personalization';
        }

        return null;
    });

    /**
     * Menu items used for adding new content to the layout (content, widget, and optionally form).
     * Items are localized and wired so that selecting them emits `addContent`.
     * Uses selected context when available, otherwise falls back to hovered context.
     */
    readonly menuItems = computed<MenuItem[]>(() => {
        const context = this.selectedContentletArea() ? this.selectedContentContext() : this.contentContext();
        return [
            {
                label: this.#dotMessageService.get('content'),
                command: () =>
                    this.addContent.emit({ type: 'content', payload: context })
            },
            {
                label: this.#dotMessageService.get('Widget'),
                command: () =>
                    this.addContent.emit({ type: 'widget', payload: context })
            },
            {
                label: this.#dotMessageService.get('form'),
                command: () =>
                    this.addContent.emit({ type: 'form', payload: context })
            }
        ];
    });

    /**
     * Menu items corresponding to the VTL files of the selected contentlet.
     * Each item represents a file and triggers the `editVTL` output when clicked.
     */
    readonly vtlMenuItems = computed<MenuItem[]>(() => {
        const context = this.selectedContentletArea() ? this.selectedContentContext() : this.contentContext();
        const { vtlFiles } = context ?? {};
        return vtlFiles?.map((file) => ({
            label: file?.name,
            command: () => this.editVTL.emit(file)
        }));
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
     * Inline styles that bound the floating toolbar to the visual rectangle of the selected contentlet.
     * The toolbar is absolutely positioned based on the coordinates in `selectedContentletArea`.
     */
    protected readonly selectedBoundsStyles = computed(() => {
        const selectedArea = this.selectedContentletArea();
        return {
            left: `${selectedArea?.x ?? 0}px`,
            top: `${selectedArea?.y ?? 0}px`,
            width: `${selectedArea?.width ?? 0}px`,
            height: `${selectedArea?.height ?? 0}px`
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
     * Ensures PrimeNG menus close when the hovered contentlet changes.
     */
    protected hideMenus(): void {
        this.menu()?.hide();
        this.menuVTL()?.hide();
    }

    protected handleClick(): void {
        const hoveredArea = this.contentletArea();

        if (!hoveredArea) {
            return;
        }

        // Set the hovered contentlet as selected
        this.selectedContentletArea.set(hoveredArea);

        // Emit the selection event
        this.outputSelectedContentlet.emit({
            container: hoveredArea.payload.container,
            contentlet: hoveredArea.payload.contentlet
        });
    }
}
