import { ChangeDetectionStrategy, Component, computed, EventEmitter, inject, input, Output } from '@angular/core';

import { TreeNode } from 'primeng/api';
import { TabViewChangeEvent, TabViewModule } from 'primeng/tabview';
import { TooltipModule } from 'primeng/tooltip';
import { TreeModule, TreeNodeSelectEvent } from 'primeng/tree';

import { DEFAULT_VARIANT_ID } from '@dotcms/dotcms-models';
import { StyleEditorFormSchema } from '@dotcms/uve';

import { DotUvePaletteListComponent } from './components/dot-uve-palette-list/dot-uve-palette-list.component';
import { DotUveStyleEditorFormComponent } from './components/dot-uve-style-editor-form/dot-uve-style-editor-form.component';
import { DotUVEPaletteListTypes } from './models';

import { UVEStore } from '../../../store/dot-uve.store';
import { UVE_PALETTE_TABS } from '../../../store/features/editor/models';

/**
 * Standalone palette component used by the EMA editor to display and switch
 * between different UVE-related resources (content types, components, styles, etc.).
 *
 * It exposes inputs to control the current page, language, variant and active tab,
 * and emits events when the active tab changes.
 */
@Component({
    selector: 'dot-uve-palette',
    imports: [
        TabViewModule,
        DotUvePaletteListComponent,
        TooltipModule,
        DotUveStyleEditorFormComponent,
        TreeModule
    ],
    templateUrl: './dot-uve-palette.component.html',
    styleUrl: './dot-uve-palette.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotUvePaletteComponent {
    /**
     * Absolute path of the page currently being edited.
     */
    $pagePath = input.required<string>({ alias: 'pagePath' });

    /**
     * Identifier of the language in which the page is being edited.
     */
    $languageId = input.required<number>({ alias: 'languageId' });

    /**
     * Variant identifier of the page/contentlet; defaults to `DEFAULT_VARIANT_ID`.
     */
    $variantId = input<string>(DEFAULT_VARIANT_ID, { alias: 'variantId' });

    /**
     * Currently active palette tab.
     */
    $activeTab = input<UVE_PALETTE_TABS>(UVE_PALETTE_TABS.CONTENT_TYPES, { alias: 'activeTab' });

    /**
     * Whether the style editor tab should be shown in the palette.
     */
    $showStyleEditorTab = input<boolean>(false, { alias: 'showStyleEditorTab' });

    /**
     * The Style Schema to use for the current selected contentlet.
     */
    $styleSchema = input<StyleEditorFormSchema>(undefined, { alias: 'styleSchema' });

    /**
     * Emits whenever the active tab in the palette changes.
     */
    @Output() onTabChange = new EventEmitter<UVE_PALETTE_TABS>();

    /**
     * Emits when a tree node is selected to scroll to the corresponding element.
     */
    @Output() onNodeSelect = new EventEmitter<{ selector: string; type: string }>();

    protected readonly uveStore = inject(UVEStore);
    protected readonly TABS_MAP = UVE_PALETTE_TABS;
    protected readonly DotUVEPaletteListTypes = DotUVEPaletteListTypes;

    /**
     * Computed signal that transforms the page layout structure into TreeNode format
     * for the PrimeNG Tree component. Structure: rows > columns > containers > contentlets
     */
    readonly $layoutTree = computed<TreeNode[]>(() => {
        const pageResponse = this.uveStore.pageAPIResponse();

        if (!pageResponse?.layout?.body?.rows) {
            return [];
        }

        const rows = pageResponse.layout.body.rows;
        const containers = pageResponse.containers || {};

        const treeNodes: TreeNode[] = rows.map((row, rowIndex) => {
            const columnNodes: TreeNode[] = (row.columns || []).map((column, columnIndex) => {
                const containerNodes: TreeNode[] = (column.containers || []).map((container, containerIndex) => {
                    const containerData = containers[container.identifier];
                    const containerInfo = containerData?.container;
                    const containerLabel = containerInfo?.name ||
                                           containerInfo?.friendlyName ||
                                           containerInfo?.title ||
                                           container.identifier ||
                                           `Container ${containerIndex + 1}`;

                    // Get contentlets for this container using uuid from layout
                    const contentletUuid = `uuid-${container.uuid}`;
                    const contentlets = containerData?.contentlets?.[contentletUuid] || [];

                    const contentletNodes: TreeNode[] = contentlets.map((contentlet, contentletIndex) => {
                        return {
                            key: `row-${rowIndex}-column-${columnIndex}-container-${containerIndex}-contentlet-${contentletIndex}`,
                            label: contentlet.title || `Contentlet ${contentletIndex + 1}`,
                            selectable: false,
                            data: {
                                ...contentlet,
                                type: 'contentlet',
                                selector: `[data-dot-identifier="${contentlet.identifier}"]`
                            }
                        };
                    });

                    return {
                        key: `row-${rowIndex}-column-${columnIndex}-container-${containerIndex}`,
                        label: containerLabel,
                        selectable: false,
                        data: {
                            ...container,
                            containerInfo: containerInfo,
                            type: 'container',
                            selector: `[data-dot-identifier="${container.identifier}"][data-dot-uuid="${container.uuid}"]`
                        },
                        children: contentletNodes.length > 0 ? contentletNodes : undefined
                    };
                });

                return {
                    key: `row-${rowIndex}-column-${columnIndex}`,
                    label: `Column ${columnIndex + 1}`,
                    selectable: false,
                    children: containerNodes.length > 0 ? containerNodes : undefined
                };
            });

            return {
                key: `row-${rowIndex}`,
                label: `Row ${rowIndex + 1}`,
                selectable: true,
                data: {
                    type: 'row',
                    selector: `#section-${rowIndex + 1}`
                },
                children: columnNodes.length > 0 ? columnNodes : undefined
            };
        });

        return treeNodes;
    });

    /**
     * Called whenever the tab changes, either by user interaction or via the `activeIndex` property.
     *
     * @param event TabView change event containing the new active index.
     */
    protected handleTabChange(event: TabViewChangeEvent) {
        this.onTabChange.emit(event.index);
    }

    /**
     * Handles tree node selection and emits event to scroll to the corresponding element.
     * Only row nodes are selectable.
     *
     * @param event PrimeNG tree node select event
     */
    protected handleNodeSelect(event: TreeNodeSelectEvent): void {
        const node = event.node;
        if (!node?.data || node.data.type !== 'row') {
            return;
        }

        const selector = node.data.selector;
        if (!selector) {
            return;
        }

        // Emit event to parent component to handle scrolling
        this.onNodeSelect.emit({
            selector: selector,
            type: node.data.type
        });
    }
}
