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
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';

import { take } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotToolsStore } from './store/dot-tools.store';

import { CATALOG_LOAD_MORE_STEP } from '../constants/dot-tools.constants';
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
        ProgressSpinnerModule,
        TooltipModule,
        DotMessagePipe
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
    readonly #messageService = inject(DotMessageService);

    // Exposed so the "Load N more" button label stays in sync with the store's
    // pagination step without hardcoding "40" in a translation string.
    protected readonly loadMoreStep = signal(CATALOG_LOAD_MORE_STEP);

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
                label: this.#messageService.get('tools.menu.edit'),
                icon: 'pi pi-pencil',
                command: () => this.openEditSectionDialog(section)
            },
            {
                label: this.#messageService.get('tools.menu.delete'),
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
                label: this.#messageService.get('tools.menu.edit'),
                icon: 'pi pi-pencil',
                command: () => this.openEditToolDialog(tool)
            },
            {
                label: this.#messageService.get('tools.menu.delete'),
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

    protected onRetry(): void {
        this.store.loadAll();
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
            header: this.#messageService.get('tools.new-section'),
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
            header: this.#messageService.get('tools.new-tool'),
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

    private openEditSectionDialog(section: DotToolsSection): void {
        const ref = this.#dialogService.open(DotToolsSectionDialogComponent, {
            header: this.#messageService.get('tools.edit-section'),
            width: '700px',
            data: { section },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });

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
            header: this.#messageService.get('tools.edit-tool'),
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
        const message =
            toolCount > 0
                ? this.#messageService.get(
                      'tools.confirm.delete-section.body.with-tools',
                      this.#toolAssignmentPhrase(toolCount)
                  )
                : this.#messageService.get('tools.confirm.delete-section.body');

        this.#confirmationService.confirm({
            header: this.#messageService.get('tools.confirm.delete-section.header', section.name),
            message,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.#messageService.get('tools.confirm.accept'),
            rejectLabel: this.#messageService.get('tools.confirm.reject'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            accept: () => this.store.deleteSection(section.id)
        });
    }

    private confirmDeleteTool(tool: DotToolsCatalogEntry): void {
        const sectionCount = this.store.toolSectionCount()(tool.id);
        const message =
            sectionCount > 0
                ? this.#messageService.get(
                      'tools.confirm.delete-tool.body.with-sections',
                      this.#sectionCountPhrase(sectionCount)
                  )
                : this.#messageService.get('tools.confirm.delete-tool.body');

        this.#confirmationService.confirm({
            header: this.#messageService.get('tools.confirm.delete-tool.header', tool.title),
            message,
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.#messageService.get('tools.confirm.accept'),
            rejectLabel: this.#messageService.get('tools.confirm.reject'),
            acceptButtonStyleClass: 'p-button-danger',
            rejectButtonStyleClass: 'p-button-text',
            accept: () => this.store.deleteCustomTool(tool.id)
        });
    }

    // Two-key pluralization: the resolved phrase gets interpolated into the
    // "body.with-*" string. Keeps grammar right without a real i18n plural
    // library while staying inside the existing DotMessageService helpers.
    #toolAssignmentPhrase(count: number): string {
        return count === 1
            ? this.#messageService.get('tools.confirm.delete-section.tool-count.single')
            : this.#messageService.get(
                  'tools.confirm.delete-section.tool-count.multi',
                  count.toString()
              );
    }

    #sectionCountPhrase(count: number): string {
        return count === 1
            ? this.#messageService.get('tools.confirm.delete-tool.section-count.single')
            : this.#messageService.get(
                  'tools.confirm.delete-tool.section-count.multi',
                  count.toString()
              );
    }
}
