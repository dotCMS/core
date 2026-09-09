import {
    CdkDrag,
    CdkDragDrop,
    CdkDragHandle,
    CdkDragPlaceholder,
    CdkDragPreview,
    CdkDropList,
    moveItemInArray
} from '@angular/cdk/drag-drop';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MenuModule } from 'primeng/menu';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotToolsStore } from './store/dot-tools.store';

import { DotToolsSectionDialogComponent } from '../dot-tools-section-dialog/dot-tools-section-dialog.component';
import { DotToolsToolDialogComponent } from '../dot-tools-tool-dialog/dot-tools-tool-dialog.component';
import {
    DotToolsCatalogEntry,
    DotToolsSection,
    DotToolsSectionForm,
    DotToolsToolForm
} from '../models/dot-tools.models';

@Component({
    selector: 'dot-tools-page',
    standalone: true,
    imports: [
        FormsModule,
        CdkDrag,
        CdkDragHandle,
        CdkDragPlaceholder,
        CdkDragPreview,
        CdkDropList,
        ButtonModule,
        CheckboxModule,
        ConfirmDialogModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        MenuModule,
        TooltipModule
    ],
    providers: [DotToolsStore, DialogService, ConfirmationService],
    templateUrl: './dot-tools-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-1 min-h-0 block' }
})
export class DotToolsPageComponent {
    protected readonly store = inject(DotToolsStore);

    readonly #dialogService = inject(DialogService);
    readonly #confirmationService = inject(ConfirmationService);

    // The overflow menu on a section row is a single p-menu instance
    // reparented to whichever row's trigger was clicked. Storing the target
    // section on the component keeps the template simple.
    protected readonly $menuSection = signal<DotToolsSection | null>(null);
    protected readonly $menuTool = signal<DotToolsCatalogEntry | null>(null);

    protected readonly sectionMenuItems = computed<MenuItem[]>(() => {
        const section = this.$menuSection();
        if (!section) {
            return [];
        }

        return [
            {
                label: 'Edit',
                icon: 'pi pi-pencil',
                command: () => this.openEditSectionDialog(section)
            },
            {
                label: 'Delete',
                icon: 'pi pi-trash',
                styleClass: 'text-red-600',
                command: () => this.confirmDeleteSection(section)
            }
        ];
    });

    protected readonly toolMenuItems = computed<MenuItem[]>(() => {
        const tool = this.$menuTool();
        if (!tool) {
            return [];
        }

        return [
            {
                label: 'Edit',
                icon: 'pi pi-pencil',
                command: () => this.openEditToolDialog(tool)
            },
            {
                label: 'Delete',
                icon: 'pi pi-trash',
                styleClass: 'text-red-600',
                command: () => this.confirmDeleteTool(tool)
            }
        ];
    });

    protected onCatalogFilterChange(value: string): void {
        this.store.setCatalogFilter(value);
    }

    protected onSelectSection(id: string): void {
        this.store.selectSection(id);
    }

    protected onToggleTool(toolId: string): void {
        this.store.toggleToolInSelectedSection(toolId);
    }

    protected onRemoveTool(toolId: string): void {
        this.store.removeToolFromSelectedSection(toolId);
    }

    protected onLoadMore(): void {
        this.store.loadMoreCatalog();
    }

    protected onSectionDrop(event: CdkDragDrop<DotToolsSection[]>): void {
        if (event.previousIndex === event.currentIndex) {
            return;
        }
        const ids = this.store.sections().map((section) => section.id);
        moveItemInArray(ids, event.previousIndex, event.currentIndex);
        this.store.reorderSections(ids);
    }

    protected onToolDrop(event: CdkDragDrop<DotToolsCatalogEntry[]>): void {
        if (event.previousIndex === event.currentIndex) {
            return;
        }
        const section = this.store.selectedSection();
        if (!section) {
            return;
        }
        const ids = [...section.portletIds];
        moveItemInArray(ids, event.previousIndex, event.currentIndex);
        this.store.reorderSelectedSectionTools(ids);
    }

    protected openNewSectionDialog(): void {
        const ref = this.#dialogService.open(DotToolsSectionDialogComponent, {
            header: 'New Section',
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe((form: DotToolsSectionForm | undefined) => {
            if (form) {
                this.store.createSection(form);
            }
        });
    }

    protected openNewToolDialog(): void {
        const ref = this.#dialogService.open(DotToolsToolDialogComponent, {
            header: 'New Tool',
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe((form: DotToolsToolForm | undefined) => {
            if (form) {
                this.store.createCustomTool(form);
            }
        });
    }

    protected setSectionMenuTarget(section: DotToolsSection): void {
        this.$menuSection.set(section);
    }

    protected setToolMenuTarget(tool: DotToolsCatalogEntry): void {
        this.$menuTool.set(tool);
    }

    protected isToolCustom(tool: DotToolsCatalogEntry): boolean {
        return tool.isCustom;
    }

    #openEditSectionDialogRef(section: DotToolsSection) {
        return this.#dialogService.open(DotToolsSectionDialogComponent, {
            header: 'Edit Section',
            width: '700px',
            data: { section },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
    }

    private openEditSectionDialog(section: DotToolsSection): void {
        const ref = this.#openEditSectionDialogRef(section);

        ref?.onClose.pipe(take(1)).subscribe((form: DotToolsSectionForm | undefined) => {
            if (form) {
                this.store.updateSection(section.id, form);
            }
        });
    }

    private openEditToolDialog(tool: DotToolsCatalogEntry): void {
        // TODO(#37353): once `GET /v1/portlet/custom/{id}` lands, fetch the
        // current config and pass it as `prefill` so the dialog reopens with
        // the stored base types, content types and view mode. Until then the
        // dialog opens with defaults and the user has to re-pick.
        const ref = this.#dialogService.open(DotToolsToolDialogComponent, {
            header: 'Edit Tool',
            width: '700px',
            data: { tool },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

        ref?.onClose.pipe(take(1)).subscribe((form: DotToolsToolForm | undefined) => {
            if (form) {
                this.store.updateCustomTool(form);
            }
        });
    }

    private confirmDeleteSection(section: DotToolsSection): void {
        const toolCount = section.portletIds.length;
        const body =
            toolCount > 0
                ? `The section and its ${toolCount === 1 ? '1 tool assignment' : toolCount + ' tool assignments'} will be removed from the navigation. This action cannot be undone.`
                : 'The section will be removed from the navigation. This action cannot be undone.';

        this.#confirmationService.confirm({
            header: `Delete section "${section.name}"?`,
            message: body,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Delete',
            rejectLabel: 'Cancel',
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            accept: () => this.store.deleteSection(section.id)
        });
    }

    private confirmDeleteTool(tool: DotToolsCatalogEntry): void {
        const sectionCount = this.store.toolSectionCount()(tool.id);
        const body =
            sectionCount > 0
                ? `The tool will be deleted and removed from ${sectionCount === 1 ? '1 section' : sectionCount + ' sections'}. This action cannot be undone.`
                : 'The tool will be deleted from the catalog. This action cannot be undone.';

        this.#confirmationService.confirm({
            header: `Delete tool "${tool.title}"?`,
            message: body,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: 'Delete',
            rejectLabel: 'Cancel',
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            accept: () => this.store.deleteCustomTool(tool.id)
        });
    }
}
