import {
    ChangeDetectionStrategy,
    Component,
    computed,
    DestroyRef,
    effect,
    inject,
    output,
    signal,
    untracked
} from '@angular/core';

import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenuModule } from 'primeng/menu';
import { ToolbarModule } from 'primeng/toolbar';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotCMSContentTypeField } from '@dotcms/dotcms-models';
import {
    DotFolderTreeNodeContentData,
    LOAD_MORE_NODE_TYPE
} from '@dotcms/portlets/content-drive/ui';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe, DotUploadButtonComponent } from '@dotcms/ui';

import { DotContentDriveContentTypeFilterComponent } from './components/dot-content-drive-content-type-filter/dot-content-drive-content-type-filter.component';
import { DotContentDriveFieldFilterComponent } from './components/dot-content-drive-field-filter/dot-content-drive-field-filter.component';
import { DotContentDriveFieldFilterMenuComponent } from './components/dot-content-drive-field-filter-menu/dot-content-drive-field-filter-menu.component';
import { DotContentDriveLanguageFieldComponent } from './components/dot-content-drive-language-field/dot-content-drive-language-field.component';
import { DotContentDriveSearchInputComponent } from './components/dot-content-drive-search-input/dot-content-drive-search-input.component';
import { DotContentDriveSharedAssetsFilterComponent } from './components/dot-content-drive-shared-assets-filter/dot-content-drive-shared-assets-filter.component';
import { DotContentDriveStatusFilterComponent } from './components/dot-content-drive-status-filter/dot-content-drive-status-filter.component';
import { DotContentDriveTreeTogglerComponent } from './components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';
import { DotContentDriveWorkflowActionsComponent } from './components/dot-content-drive-workflow-actions/dot-content-drive-workflow-actions.component';
import { DotContentDriveWorkflowFilterComponent } from './components/dot-content-drive-workflow-filter/dot-content-drive-workflow-filter.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';
import { excludeFolders } from '../../utils/action-center';
import { hasNonDefaultFilters } from '../../utils/functions';

/**
 * Animation delay in milliseconds - matches the duration of the enter/leave fade
 */
const ANIMATION_DELAY = 135;

/** Characters that would let an interpolated value become markup. */
const HTML_ESCAPES: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
};

/**
 * Escapes a value that will be interpolated into a message rendered as HTML.
 *
 * Needed only because `content-drive.action-center.applying` carries its own `<b>`, which forces the
 * label to be bound with `[innerHTML]` rather than interpolated. Everything substituted into such a
 * message has to be escaped, or the message stops being the only source of markup in it.
 */
const escapeHtml = (value: string): string =>
    value.replace(/[&<>"']/g, (char) => HTML_ESCAPES[char]);

/**
 * Base-type options in the "New" menu (all base types except FORM, which is deprecated).
 * Each maps to a precise palette list type and a Material Symbols icon (rendered via the
 * menu's custom item template, following the design's icon pattern).
 */
const BASE_TYPE_MENU_OPTIONS: {
    labelKey: string;
    icon: string;
    listType: DotUVEPaletteListTypes;
}[] = [
    {
        labelKey: 'content-drive.base-type.content',
        icon: 'description',
        listType: DotUVEPaletteListTypes.ALL_CONTENT
    },
    {
        labelKey: 'content-drive.base-type.widget',
        icon: 'widgets',
        listType: DotUVEPaletteListTypes.ALL_WIDGET
    },
    {
        labelKey: 'content-drive.base-type.fileasset',
        icon: 'draft',
        listType: DotUVEPaletteListTypes.ALL_FILEASSET
    },
    {
        labelKey: 'content-drive.base-type.dotasset',
        icon: 'deployed_code',
        listType: DotUVEPaletteListTypes.ALL_DOTASSET
    },
    {
        labelKey: 'content-drive.base-type.persona',
        icon: 'person',
        listType: DotUVEPaletteListTypes.ALL_PERSONA
    },
    {
        labelKey: 'content-drive.base-type.vanity_url',
        icon: 'link',
        listType: DotUVEPaletteListTypes.ALL_VANITY_URL
    },
    {
        labelKey: 'content-drive.base-type.key_value',
        icon: 'key',
        listType: DotUVEPaletteListTypes.ALL_KEY_VALUE
    },
    {
        labelKey: 'content-drive.base-type.htmlpage',
        icon: 'article',
        listType: DotUVEPaletteListTypes.ALL_HTMLPAGE
    }
];

/**
 * Interface for managing animation states of toolbar elements
 */
interface ToolbarAnimationState {
    addNewButton: boolean;
    workflowActions: boolean;
}

@Component({
    selector: 'dot-content-drive-toolbar',
    imports: [
        ToolbarModule,
        ButtonModule,
        MenuModule,
        DotMessagePipe,
        DotUploadButtonComponent,
        DotContentDriveTreeTogglerComponent,
        DotContentDriveContentTypeFilterComponent,
        DotContentDriveSearchInputComponent,
        DotContentDriveLanguageFieldComponent,
        DotContentDriveWorkflowActionsComponent,
        DotContentDriveWorkflowFilterComponent,
        DotContentDriveFieldFilterComponent,
        DotContentDriveFieldFilterMenuComponent,
        DotContentDriveSharedAssetsFilterComponent,
        DotContentDriveStatusFilterComponent,
        TooltipModule
    ],
    templateUrl: './dot-content-drive-toolbar.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'block transition-all duration-300 ease-in-out',
        '[style.min-height]': '"7.125rem"'
    },
    styles: [
        `
            .toolbar-enter {
                animation: toolbar-fade-in 100ms ease-out;
            }
            @keyframes toolbar-fade-in {
                from {
                    opacity: 0;
                }
                to {
                    opacity: 1;
                }
            }
            .toolbar-leave {
                opacity: 0;
                transition: opacity 100ms ease-in;
            }
            .field-filter-enter {
                overflow: hidden;
                animation: field-filter-in 180ms ease-out;
            }
            @keyframes field-filter-in {
                from {
                    opacity: 0;
                    max-width: 0;
                    transform: scale(0.96);
                }
                to {
                    opacity: 1;
                    max-width: 16rem;
                    transform: none;
                }
            }
            .field-filter-leave {
                overflow: hidden;
                animation: field-filter-out 150ms ease-in forwards;
            }
            @keyframes field-filter-out {
                from {
                    opacity: 1;
                    max-width: 16rem;
                    transform: none;
                }
                to {
                    opacity: 0;
                    max-width: 0;
                    transform: scale(0.96);
                }
            }
        `
    ]
})
export class DotContentDriveToolbarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);

    // Emits the click event so the shell can anchor the upload-type popover to the button.
    $upload = output<MouseEvent>({ alias: 'upload' });

    /**
     * Base type the current folder pins uploads to, if any. `dot-upload-button` turns it into the
     * folder-aware label ("Upload Asset" / "Upload File" / "Upload").
     */
    protected readonly $uploadBaseType = computed(() => {
        const data = this.#store.selectedNode()?.data;

        return data && data.type !== LOAD_MORE_NODE_TYPE
            ? ((data as DotFolderTreeNodeContentData).defaultBaseType ?? null)
            : null;
    });

    readonly $items = signal<MenuItem[]>([
        {
            label: this.#dotMessageService.get('content-drive.add-new.all-content-types'),
            icon: 'grid_view',
            command: () => this.#openContentTypeSelector(DotUVEPaletteListTypes.ALL_CONTENT_TYPES)
        },
        { separator: true },
        ...BASE_TYPE_MENU_OPTIONS.map((option) => ({
            label: this.#dotMessageService.get(option.labelKey),
            icon: option.icon,
            command: () => this.#openContentTypeSelector(option.listType)
        })),
        { separator: true },
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.folder'),
            icon: 'folder',
            command: () => {
                this.#store.setDialog({
                    type: DIALOG_TYPE.FOLDER,
                    header: this.#dotMessageService.get('content-drive.dialog.folder.header')
                });
            }
        }
    ]);

    /**
     * Opens the content-type selector dialog for the given palette list type.
     */
    #openContentTypeSelector(listType: DotUVEPaletteListTypes): void {
        this.#store.setDialog({
            type: DIALOG_TYPE.CONTENT_TYPE_SELECTOR,
            header: this.#dotMessageService.get(
                'content-drive.dialog.content-type-selector.header'
            ),
            payload: { listType }
        });
    }

    readonly $showWorkflowActions = computed(() => !!this.#store.selectedItems().length);
    /**
     * Drives the "Clear all" button. Counting filter keys would keep it on screen permanently: the
     * default language and the shared-assets toggle are always seeded, so there is always something
     * in the bag. What matters is whether anything differs from its default and is therefore worth
     * clearing.
     */
    readonly $hasFilters = computed(() =>
        hasNonDefaultFilters(this.#store.filters(), this.#store.defaultLanguageId())
    );

    /**
     * The action currently being applied, surfaced here because the run outlives the Action Center
     * dialog. Once the user closes that dialog the toolbar is the only place still reporting the run,
     * so without this the work would continue with no indication until the completion toast fired.
     */
    readonly $actionExecution = this.#store.actionExecution;

    /**
     * Resolved indicator label. Built here rather than in the template because `DotMessagePipe` takes
     * `string[]` arguments and the item count is a number.
     *
     * The action name is escaped because this label is bound with `[innerHTML]` — the message itself
     * carries a `<b>`, which is the only reason it is not plain interpolation. For a workflow action
     * that name is `WorkflowAction.name` straight from the backend, so without this a name containing
     * markup becomes real DOM. Angular's sanitizer already drops event-handler attributes, so this is
     * not an XSS fix; what it stops is structural injection that survives sanitizing — an `<img>`
     * pointing at an arbitrary URL, a link, or markup that simply breaks the toolbar's layout.
     */
    readonly $actionExecutionLabel = computed(() => {
        const execution = this.$actionExecution();

        return execution
            ? this.#dotMessageService.get(
                  'content-drive.action-center.applying',
                  escapeHtml(execution.actionName),
                  String(execution.total)
              )
            : '';
    });

    /**
     * Active field-filter chips, in the order the user added them (the store keeps `userSearchableActive`
     * in add order). Each variable is resolved to its field metadata, so chips render only once the
     * content type's fields have loaded — which also covers URL restore.
     */
    readonly $activeFieldFilters = computed(() => {
        const fieldByVariable = new Map(
            this.#store.userSearchableFields().map((field) => [field.variable, field])
        );

        return this.#store
            .userSearchableActive()
            .map((variable) => fieldByVariable.get(variable))
            .filter((field): field is DotCMSContentTypeField => field !== undefined);
    });

    onClearAll(): void {
        this.#store.clearFilters();
    }

    /**
     * Controls visibility of toolbar elements to prevent overlap during animations
     */
    readonly $animationState = signal<ToolbarAnimationState>({
        addNewButton: true,
        workflowActions: false
    });

    /**
     * Convenience computed signals for template readability.
     *
     * `$displayButton` gates the creation actions (Upload + "Add New"): both are hidden while a
     * selection is active so they don't compete with the workflow/bulk actions. This keeps the
     * Upload button from offering an upload in a selection context, where the target folder would
     * be ambiguous.
     */
    readonly $displayButton = computed(() => this.$animationState().addNewButton);
    readonly $displayActions = computed(() => this.$animationState().workflowActions);

    /**
     * The "Action Center" button is offered from the first selected contentlet, *alongside* the flat
     * action buttons rather than instead of them. The flat buttons cover the common per-item
     * actions; the dialog adds what they cannot express — per-action eligibility counts and the
     * workflow actions grouped by scheme — and that is just as useful for one item as for many.
     *
     * Folders are excluded from the count: every bulk endpoint takes contentlet inodes and ignores
     * folders, so a folder-only selection offers no Action Center.
     */
    readonly $displayActionCenter = computed(
        () => this.$displayActions() && excludeFolders(this.#store.selectedItems()).length > 0
    );

    /**
     * Opens the Action Center dialog for the current selection.
     */
    protected onOpenActionCenter(): void {
        // The button is disabled during a run, but the guard lives here too: a disabled attribute
        // is a UI affordance, not a lock on the store.
        if (this.$actionExecution()) {
            return;
        }

        this.#store.setDialog({
            type: DIALOG_TYPE.ACTION_CENTER,
            header: this.#dotMessageService.get('content-drive.action-center.header')
        });
    }

    /**
     * Why the Workflow Center is unavailable, or `''` when it is available.
     *
     * The dialog is refused outright during a run rather than opened in a useless state. Reopening
     * mid-run used to give a dialog with every row, both footers and Done disabled — because
     * `$executing` gates all of them — which then closed itself the moment the run settled. Firing a
     * second action over a fresh selection was never possible either way: the store rejects a second
     * run while one is in flight.
     */
    readonly $actionCenterTooltip = computed(() =>
        this.$actionExecution() ? 'content-drive.action-center.busy' : ''
    );

    /** Pending animation-sequencing timer; cleared on each transition so rapid toggles don't race. */
    #animationTimeout: ReturnType<typeof setTimeout> | undefined;

    constructor() {
        // Watch for changes in workflow actions state and handle animation sequencing
        effect(() => {
            const shouldShowActions = this.$showWorkflowActions();
            untracked(() => this.#handleAnimationSequence(shouldShowActions));
        });

        // Cancel a pending transition timer if the toolbar is destroyed mid-animation,
        // so the callback can't mutate a signal on a torn-down component.
        inject(DestroyRef).onDestroy(() => clearTimeout(this.#animationTimeout));
    }

    /**
     * Handles the animation sequence when switching between "Add New" button and workflow actions
     * Ensures animations don't overlap by sequencing them with a delay
     *
     * @param shouldShowActions - Whether workflow actions should be displayed
     */
    #handleAnimationSequence(shouldShowActions: boolean): void {
        if (shouldShowActions) {
            this.#transitionToWorkflowActions();
        } else {
            this.#transitionToAddNewButton();
        }
    }

    /**
     * Transition from "Add New" button to workflow actions
     * 1. Hide button immediately (triggers leave animation)
     * 2. Wait for animation to complete
     * 3. Show workflow actions (triggers enter animation)
     */
    #transitionToWorkflowActions(): void {
        clearTimeout(this.#animationTimeout);
        this.$animationState.set({
            addNewButton: false,
            workflowActions: false
        });

        this.#animationTimeout = setTimeout(() => {
            this.$animationState.set({
                addNewButton: false,
                workflowActions: true
            });
        }, ANIMATION_DELAY);
    }

    /**
     * Transition from workflow actions to "Add New" button
     * 1. Hide workflow actions immediately (triggers leave animation)
     * 2. Wait for animation to complete
     * 3. Show button (triggers enter animation)
     */
    #transitionToAddNewButton(): void {
        clearTimeout(this.#animationTimeout);
        this.$animationState.set({
            addNewButton: false,
            workflowActions: false
        });

        this.#animationTimeout = setTimeout(() => {
            this.$animationState.set({
                addNewButton: true,
                workflowActions: false
            });
        }, ANIMATION_DELAY);
    }
}
