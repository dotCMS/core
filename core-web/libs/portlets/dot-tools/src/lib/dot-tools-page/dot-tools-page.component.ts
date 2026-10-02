import {
    CdkDrag,
    CdkDragHandle,
    CdkDragMove,
    CdkDragPlaceholder,
    CdkDragPreview,
    CdkDropList,
    moveItemInArray
} from '@angular/cdk/drag-drop';
import {
    ChangeDetectionStrategy,
    Component,
    DestroyRef,
    computed,
    effect,
    inject,
    signal,
    untracked
} from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ContextMenuModule } from 'primeng/contextmenu';
import { DialogService } from 'primeng/dynamicdialog';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe, DotStatusToastComponent, STATUS_TOAST_KEY } from '@dotcms/ui';

import { DotToolsStore } from './store/dot-tools.store';

import { CATALOG_LOAD_MORE_STEP } from '../constants/dot-tools.constants';
import { DotToolsSectionDialogComponent } from '../dot-tools-section-dialog/dot-tools-section-dialog.component';
import { DotToolsToolDialogComponent } from '../dot-tools-tool-dialog/dot-tools-tool-dialog.component';
import { DotToolsCatalogEntry, DotToolsSection } from '../models/dot-tools.models';

/** Shape p-contextMenu exposes for programmatic show. */
interface ContextMenuLike {
    show(event: Event): void;
}

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
        ContextMenuModule,
        ProgressSpinnerModule,
        TooltipModule,
        DotMessagePipe,
        DotStatusToastComponent
    ],
    providers: [DotToolsStore, DialogService, ConfirmationService, MessageService],
    templateUrl: './dot-tools-page.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'flex flex-1 min-h-0 block' }
})
export class DotToolsPageComponent {
    protected readonly store = inject(DotToolsStore);

    readonly #dialogService = inject(DialogService);
    readonly #confirmationService = inject(ConfirmationService);
    readonly #messageService = inject(DotMessageService);
    readonly #toastService = inject(MessageService);
    readonly #destroyRef = inject(DestroyRef);

    /**
     * Whether the status toast is currently on screen. A new save landing
     * while this is `true` must NOT clear and re-add — the user would see
     * a flicker — so we only call `add()` on the first save, then arm /
     * re-arm our own hide timer on each save after that.
     */
    #savedToastVisible = false;
    #savedToastHideTimer: ReturnType<typeof setTimeout> | null = null;

    constructor() {
        // Every bump of `toolsSavedAt` is a confirmed section-tool mutation.
        // The initial read is `0`, which the effect ignores so the page does
        // not toast on open.
        effect(() => {
            const savedAt = this.store.toolsSavedAt();
            if (savedAt === 0) {
                return;
            }
            untracked(() => this.#showSavedToast());
        });

        this.#destroyRef.onDestroy(() => {
            if (this.#savedToastHideTimer !== null) {
                clearTimeout(this.#savedToastHideTimer);
            }
            this.#toastService.clear(STATUS_TOAST_KEY);
        });
    }

    /**
     * Shows the "Section saved" toast and keeps it on screen for the base
     * life after the LAST save. Only the first save adds a toast; later
     * saves while that toast is still visible restart the hide timer, so
     * a burst of quick toggles looks like one steady acknowledgement that
     * extends itself rather than a toast that flickers off and on.
     *
     * `sticky: true` on the message disables PrimeNG's own auto-dismiss
     * so we own the full lifetime via `#savedToastHideTimer`.
     */
    #showSavedToast(): void {
        const BASE_LIFE = 2500;

        if (!this.#savedToastVisible) {
            this.#savedToastVisible = true;
            this.#toastService.add({
                key: STATUS_TOAST_KEY,
                severity: 'success',
                summary: this.#messageService.get('tools.toast.section-saved'),
                sticky: true
            });
        }

        if (this.#savedToastHideTimer !== null) {
            clearTimeout(this.#savedToastHideTimer);
        }
        this.#savedToastHideTimer = setTimeout(() => {
            this.#toastService.clear(STATUS_TOAST_KEY);
            this.#savedToastVisible = false;
            this.#savedToastHideTimer = null;
        }, BASE_LIFE);
    }

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
            // Non-interactive context header so the user sees at a glance
            // whether the menu belongs to a section or a tool. `disabled`
            // keeps PrimeNG from treating it as a click target.
            {
                label: this.#messageService.get('tools.menu.section-header'),
                disabled: true,
                styleClass: 'text-xs font-bold text-gray-900 uppercase tracking-wider'
            },
            {
                label: this.#messageService.get('tools.menu.edit'),
                command: () => this.openEditSectionDialog(section)
            },
            { separator: true },
            {
                label: this.#messageService.get('tools.menu.delete'),
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
                label: this.#messageService.get('tools.menu.tool-header'),
                disabled: true,
                styleClass: 'text-xs font-bold text-gray-900 uppercase tracking-wider'
            },
            {
                label: this.#messageService.get('tools.menu.edit'),
                command: () => this.openEditToolDialog(tool)
            },
            { separator: true },
            {
                label: this.#messageService.get('tools.menu.delete'),
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

    // ----- Drag-and-drop --------------------------------------------------
    //
    // The lists run in `cdkDropListSortingDisabled` mode: the original row
    // stays visible in place (rendered by its placeholder template at reduced
    // opacity) and siblings don't shift while dragging. Instead we compute a
    // "drop index" from the pointer position on every `cdkDragMoved` event
    // and render a 2px horizontal line in the target gap. On drop we ignore
    // CDK's `currentIndex` (unreliable when sorting is off) and use the
    // tracked drop index.

    protected readonly $sectionDropIndex = signal<number | null>(null);
    protected readonly $toolDropIndex = signal<number | null>(null);

    protected onSectionDragMoved(event: CdkDragMove, listEl: HTMLElement): void {
        this.$sectionDropIndex.set(this.#computeDropIndex(event, listEl));
    }

    protected onToolDragMoved(event: CdkDragMove, listEl: HTMLElement): void {
        this.$toolDropIndex.set(this.#computeDropIndex(event, listEl));
    }

    protected onSectionDragEnded(previousIndex: number): void {
        const target = this.$sectionDropIndex();
        this.$sectionDropIndex.set(null);
        if (target === null) {
            return;
        }

        const ids = this.store.sections().map((section) => section.id);
        const currentIndex = this.#normalizeInsertionIndex(previousIndex, target, ids.length);
        if (currentIndex === previousIndex) {
            return;
        }

        moveItemInArray(ids, previousIndex, currentIndex);
        this.store.reorderSections(ids);
    }

    protected onToolDragEnded(previousIndex: number): void {
        const target = this.$toolDropIndex();
        this.$toolDropIndex.set(null);
        if (target === null) {
            return;
        }

        const section = this.store.selectedSection();
        if (!section) {
            return;
        }

        const ids = [...section.portletIds];
        const currentIndex = this.#normalizeInsertionIndex(previousIndex, target, ids.length);
        if (currentIndex === previousIndex) {
            return;
        }

        moveItemInArray(ids, previousIndex, currentIndex);
        this.store.reorderSelectedSectionTools(ids);
    }

    /**
     * Walks the row elements inside a drop list (marked with `data-row`, which
     * lives on both the real row li AND its placeholder replacement so the
     * count stays N during a drag) and returns the gap index the pointer is
     * closest to. Gap N sits above row N; gap `rows.length` is after the last
     * row.
     */
    #computeDropIndex(event: CdkDragMove, listEl: HTMLElement): number {
        const rows = Array.from(listEl.querySelectorAll<HTMLElement>('[data-row]'));
        if (rows.length === 0) {
            return 0;
        }

        const pointerY = event.pointerPosition.y;
        for (let i = 0; i < rows.length; i++) {
            const rect = rows[i].getBoundingClientRect();
            if (pointerY < rect.top + rect.height / 2) {
                return i;
            }
        }

        return rows.length;
    }

    /**
     * Converts a "gap index" (drop-line position) to the index we pass to
     * `moveItemInArray`. Dropping into the gap immediately below the original
     * row is a no-op; dropping into any gap after that needs to be shifted
     * down by one because the source is being removed first.
     */
    #normalizeInsertionIndex(previousIndex: number, gapIndex: number, total: number): number {
        if (gapIndex <= previousIndex) {
            return gapIndex;
        }

        return Math.min(gapIndex - 1, total - 1);
    }

    protected openNewSectionDialog(): void {
        // Section dialog owns its own submit (per libs/portlets/CLAUDE.md ›
        // Dialogs whose submit can fail into the form) so we do not subscribe
        // to onClose for the form value — the store is already updated by the
        // time the dialog closes on success.
        this.#dialogService.open(DotToolsSectionDialogComponent, {
            header: this.#messageService.get('tools.new-section'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
    }

    protected openNewToolDialog(): void {
        // Tool dialog owns its submit for the same reason the section
        // dialog does (duplicate id → 400 renders inline).
        this.#dialogService.open(DotToolsToolDialogComponent, {
            header: this.#messageService.get('tools.new-tool'),
            width: '700px',
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
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

    /**
     * Right-click opens the Edit / Delete menu only when the tool is
     * custom — first-party tools have no menu, so the browser's native
     * context menu is left alone there.
     */
    protected onToolRowContextMenu(
        event: MouseEvent,
        tool: DotToolsCatalogEntry,
        menu: ContextMenuLike
    ): void {
        if (!tool.isCustom) {
            return;
        }
        this.setToolMenuTarget(tool);
        this.showContextMenu(event, menu);
    }

    /**
     * p-contextMenu positions at `event.pageX / pageY` and does not always
     * fit itself inside the viewport when the cursor is near the right
     * or bottom edge, so we clamp both coordinates to leave room for the
     * overlay. Dimensions are an estimate — p-contextMenu reads the
     * synthetic `pageX` / `pageY` after `.show()` and that's all it
     * needs; preventDefault/stopPropagation are stubbed because the
     * component expects to be able to call them.
     */
    protected showContextMenu(event: MouseEvent, menu: ContextMenuLike): void {
        event.preventDefault();
        event.stopPropagation();

        const estimatedWidth = 220;
        const estimatedHeight = 100;
        const margin = 8;
        const pageX = Math.max(
            margin,
            Math.min(event.pageX, window.innerWidth - estimatedWidth - margin)
        );
        const pageY = Math.max(
            margin,
            Math.min(event.pageY, window.innerHeight - estimatedHeight - margin)
        );

        const noop = (): void => undefined;
        menu.show({
            pageX,
            pageY,
            preventDefault: noop,
            stopPropagation: noop
        } as unknown as Event);
    }

    private openEditSectionDialog(section: DotToolsSection): void {
        // Same "dialog owns the submit" pattern as openNewSectionDialog.
        this.#dialogService.open(DotToolsSectionDialogComponent, {
            header: this.#messageService.get('tools.edit-section'),
            width: '700px',
            data: { section },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
        });
    }

    private openEditToolDialog(tool: DotToolsCatalogEntry): void {
        // The tool dialog fetches its own prefill from
        // GET /v1/portlet/custom/{id} and owns the update submit so a
        // rejected write (e.g. duplicate id) can render inline.
        this.#dialogService.open(DotToolsToolDialogComponent, {
            header: this.#messageService.get('tools.edit-tool'),
            width: '700px',
            data: { tool },
            closable: true,
            closeOnEscape: true,
            draggable: false,
            position: 'center'
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
