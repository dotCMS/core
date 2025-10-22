import { trigger, transition, style, animate } from '@angular/animations';
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
import { DotMessagePipe } from '@dotcms/ui';

import { DotContentDriveBaseTypeSelectorComponent } from './components/dot-content-drive-base-type-selector/dot-content-drive-base-type-selector.component';
import { DotContentDriveContentTypeFieldComponent } from './components/dot-content-drive-content-type-field/dot-content-drive-content-type-field.component';
import { DotContentDriveLanguageFieldComponent } from './components/dot-content-drive-language-field/dot-content-drive-language-field.component';
import { DotContentDriveSearchInputComponent } from './components/dot-content-drive-search-input/dot-content-drive-search-input.component';
import { DotContentDriveTreeTogglerComponent } from './components/dot-content-drive-tree-toggler/dot-content-drive-tree-toggler.component';
import { DotContentDriveWorkflowActionsComponent } from './components/dot-content-drive-workflow-actions/dot-content-drive-workflow-actions.component';

import { DIALOG_TYPE } from '../../shared/constants';
import { DotContentDriveStore } from '../../store/dot-content-drive.store';

/**
 * Animation delay in milliseconds - matches the duration of the fadeAnimation
 */
const ANIMATION_DELAY = 135;

/**
 * Animation duration in milliseconds - matches the duration of the fadeAnimation
 */
const ANIMATION_DURATION = '100ms';

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
        DotContentDriveBaseTypeSelectorComponent,
        DotContentDriveContentTypeFieldComponent,
        DotContentDriveSearchInputComponent,
        DotContentDriveLanguageFieldComponent,
        DotContentDriveWorkflowActionsComponent
    ],
    templateUrl: './dot-content-drive-toolbar.component.html',
    styleUrl: './dot-content-drive-toolbar.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: [
        trigger('slideAnimation', [
            transition(':enter', [
                style({ opacity: 0 }),
                animate(`${ANIMATION_DURATION} ease-out`, style({ opacity: 1 }))
            ]),
            transition(':leave', [animate(`${ANIMATION_DURATION} ease-in`, style({ opacity: 0 }))])
        ])
    ]
})
export class DotContentDriveToolbarComponent {
    readonly #store = inject(DotContentDriveStore);
    readonly #dotMessageService = inject(DotMessageService);

    $addNewDotAsset = output<void>({ alias: 'addNewDotAsset' });

    readonly $items = signal<MenuItem[]>([
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.folder'),
            command: () => {
                this.#store.setDialog({
                    type: DIALOG_TYPE.FOLDER,
                    header: this.#dotMessageService.get('content-drive.dialog.folder.header')
                });
            }
        },
        {
            label: this.#dotMessageService.get('content-drive.add-new.context-menu.asset'),
            command: () => {
                this.$addNewDotAsset.emit();
            }
        }
    ]);

    readonly $treeExpanded = this.#store.isTreeExpanded;
    readonly $showWorkflowActions = computed(() => !!this.#store.selectedItems().length);

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
