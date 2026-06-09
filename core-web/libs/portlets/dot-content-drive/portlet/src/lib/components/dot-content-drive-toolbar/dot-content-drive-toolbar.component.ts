import {
    ChangeDetectionStrategy,
    Component,
    computed,
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

import { DotMessageService } from '@dotcms/data-access';
import { DotUVEPaletteListTypes } from '@dotcms/portlets/dot-ema/ui';
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveContentTypeFilterComponent } from './components/dot-content-drive-content-type-filter/dot-content-drive-content-type-filter.component';
import { DotContentDriveLanguageFieldComponent } from './components/dot-content-drive-language-field/dot-content-drive-language-field.component';
import { DotContentDriveSearchInputComponent } from './components/dot-content-drive-search-input/dot-content-drive-search-input.component';
import { DotContentDriveTreeTogglerComponent } from './components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';
import { DotContentDriveWorkflowActionsComponent } from './components/dot-content-drive-workflow-actions/dot-content-drive-workflow-actions.component';
import { DotContentDriveWorkflowFilterComponent } from './components/dot-content-drive-workflow-filter/dot-content-drive-workflow-filter.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

/**
 * Animation delay in milliseconds - matches the duration of the enter/leave fade
 */
const ANIMATION_DELAY = 135;

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
        DotContentDriveTreeTogglerComponent,
        DotContentDriveContentTypeFilterComponent,
        DotContentDriveSearchInputComponent,
        DotContentDriveLanguageFieldComponent,
        DotContentDriveWorkflowActionsComponent,
        DotContentDriveWorkflowFilterComponent
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
        `
    ]
})
export class DotContentDriveToolbarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);

    $addNewDotAsset = output<void>({ alias: 'addNewDotAsset' });

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

    readonly $treeExpanded = this.#store.isTreeExpanded;
    readonly $showWorkflowActions = computed(() => !!this.#store.selectedItems().length);
    readonly $hasFilters = computed(() => Object.keys(this.#store.filters()).length > 0);

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
     * Convenience computed signals for template readability
     */
    readonly $displayButton = computed(() => this.$animationState().addNewButton);
    readonly $displayActions = computed(() => this.$animationState().workflowActions);

    readonly $togglerStyles = computed(() => ({
        opacity: this.$treeExpanded() ? '0' : '1',
        visibility: this.$treeExpanded() ? 'hidden' : 'visible',
        transition: 'all 0.3s ease-in-out',
        width: this.$treeExpanded() ? '0' : undefined,
        minWidth: this.$treeExpanded() ? '0' : undefined
    }));

    constructor() {
        // Watch for changes in workflow actions state and handle animation sequencing
        effect(() => {
            const shouldShowActions = this.$showWorkflowActions();
            untracked(() => this.#handleAnimationSequence(shouldShowActions));
        });
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
        this.$animationState.set({
            addNewButton: false,
            workflowActions: false
        });

        setTimeout(() => {
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
        this.$animationState.set({
            addNewButton: false,
            workflowActions: false
        });

        setTimeout(() => {
            this.$animationState.set({
                addNewButton: true,
                workflowActions: false
            });
        }, ANIMATION_DELAY);
    }
}
