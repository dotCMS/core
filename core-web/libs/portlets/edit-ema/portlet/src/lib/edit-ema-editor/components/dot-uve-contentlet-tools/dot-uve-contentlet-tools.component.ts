import { JsonPipe, NgStyle } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Output,
    ViewChild,
    computed,
    effect,
    inject,
    input,
    signal
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Menu, MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { CONTENTLET_CONTROLS_DRAG_ORIGIN } from '../../../shared/consts';
import { ActionPayload, ContentletPayload, VTLFile } from '../../../shared/models';
import { ContentletArea } from '../ema-page-dropzone/types';

// TODO: Add the CSS for this
// const MIN_WIDTH_FOR_CENTERED_BUTTON = 250;

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

    @ViewChild('menu') menu?: Menu;
    @ViewChild('menuVTL') menuVTL?: Menu;

    /**
     * Whether the current environment is Enterprise.
     * When `true`, additional Enterprise-only actions (such as adding forms) are enabled.
     */
    readonly isEnterprise = input<boolean>(false, { alias: 'isEnterprise' });
    /**
     * Positional and contextual data for the currently hovered/selected contentlet.
     * Drives the floating toolbar's placement and the payload for all actions.
     */
    readonly contentletArea = input.required<ContentletArea>({ alias: 'contentletArea' });
    /**
     * Controls whether the delete-content action is allowed.
     * When `false`, the delete button is disabled and a tooltip explaining why is shown.
     */
    readonly allowContentDelete = input<boolean>(true, { alias: 'allowContentDelete' });

    /**
     * Emitted when the user chooses to edit a VTL file associated with the current contentlet.
     */
    @Output() editVTL = new EventEmitter<VTLFile>();
    /**
     * Emitted when the user wants to edit the contentlet itself.
     * Carries the current `ActionPayload` built from `contentletArea`.
     */
    @Output() editContent = new EventEmitter<ActionPayload>();
    /**
     * Emitted when the user requests deletion of the current contentlet.
     * The parent component is responsible for performing and confirming the deletion.
     */
    @Output() deleteContent = new EventEmitter<ActionPayload>();
    /**
     * Emitted when the user invokes the "add content" menu.
     * The `type` indicates what the user wants to add (content, form or widget).
     */
    @Output() addContent = new EventEmitter<{
        type: 'content' | 'form' | 'widget';
        payload: ActionPayload;
    }>();
    /**
     * Emitted when the contentlet is selected from the tools (for example, via a drag handle).
     */
    @Output() selectContent = new EventEmitter<ContentletPayload>();

    protected readonly controlsDragOrigin = CONTENTLET_CONTROLS_DRAG_ORIGIN; // Maybe call this `showCustomDragImage`?

    /**
     * Indicates where newly added contentlets should be inserted relative to the current one.
     * - `'before'`: add above
     * - `'after'`: add below
     */
    protected readonly buttonPosition = signal<'after' | 'before'>('after');

    /**
     * Snapshot of the area payload augmented with the current insert position.
     * Consumers can dispatch the returned object directly.
     */
    readonly contentContext = computed<ActionPayload>(() => ({
        ...this.contentletArea()?.payload,
        position: this.buttonPosition()
    }));

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
     */
    protected readonly menuItems = computed<MenuItem[]>(() => {
        const items = [
            {
                label: this.#dotMessageService.get('content'),
                command: () =>
                    this.addContent.emit({ type: 'content', payload: this.contentContext() })
            },
            {
                label: this.#dotMessageService.get('Widget'),
                command: () =>
                    this.addContent.emit({ type: 'widget', payload: this.contentContext() })
            }
        ];

        if (this.isEnterprise()) {
            items.push({
                label: this.#dotMessageService.get('form'),
                command: () =>
                    this.addContent.emit({ type: 'form', payload: this.contentContext() })
            });
        }

        return items;
    });

    /**
     * Menu items corresponding to the VTL files of the current contentlet.
     * Each item represents a file and triggers the `editVTL` output when clicked.
     */
    protected readonly vtlMenuItems = computed<MenuItem[]>(() => {
        const { vtlFiles } = this.contentContext() ?? {};
        return vtlFiles?.map((file) => ({
            label: file?.name,
            command: () => this.editVTL.emit(file)
        }));
    });

    /**
     * Inline styles that bound the floating toolbar to the visual rectangle of the contentlet.
     * The toolbar is absolutely positioned based on the coordinates in `contentletArea`.
     */
    protected readonly boundsStyles = computed(() => {
        const contentletArea = this.contentletArea();
        return {
            left: `${contentletArea?.x ?? 0}px`,
            top: `${contentletArea?.y ?? 0}px`,
            width: `${contentletArea?.width ?? 0}px`,
            height: `${contentletArea?.height ?? 0}px`
        };
    });

    /**
     * Describes the draggable payload for the current contentlet controls.
     * Returns null-like values when the source data is incomplete, allowing
     * the template to disable the drag affordance gracefully.
     */
    readonly dragPayload = computed(() => {
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
     * Sets up reactive side effects for the tools component.
     * Currently used to hide open menus whenever the hovered contentlet changes.
     */
    constructor() {
        effect(() => {
            // If this changes, we need to hide the menus
            this.contentletArea();
            this.hideMenus();
        });
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
        this.menu?.hide();
        this.menuVTL?.hide();
    }
}
