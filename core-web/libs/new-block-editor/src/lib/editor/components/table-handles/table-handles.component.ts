import { autoUpdate, computePosition, shift } from '@floating-ui/dom';

import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    NgZone,
    OnDestroy,
    computed,
    effect,
    inject,
    input
} from '@angular/core';

import { Editor } from '@tiptap/core';

import { DotMessageService } from '@dotcms/data-access';

import { EditorPopoverService } from '../../services/editor-popover.service';
import { ActiveCell, TableHandlesStore } from '../../services/table-handles.store';

interface PositionedHandle {
    el: HTMLElement;
    placement: 'top' | 'left';
}

/**
 * Renders two floating handles — column (top of column) and row (left of row) — anchored
 * to the cell containing the editor cursor. Driven by {@link TableHandlesStore.activeCell},
 * which {@link TableSelectionPlugin} updates whenever the cursor moves between cells.
 *
 * Each handle button uses `mousedown.preventDefault()` so clicking it does NOT move the
 * editor selection — the cursor stays in the cell, the popover opens, and after the popover
 * closes the user can keep typing without re-clicking the cell.
 *
 * Phase 3: dropped the third (table-actions) handle and the hover-driven lock state. Table
 * a11y properties are now reached from the toolbar's `table_edit` button instead.
 */
@Component({
    selector: 'dot-table-handles',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        '[style.display]': 'isVisible() ? "contents" : "none"'
    },
    template: `
        <button
            #columnHandle
            type="button"
            data-testid="table-column-handle"
            class="table-handle table-handle--column"
            [attr.aria-label]="columnAriaLabel"
            (mousedown)="$event.preventDefault(); openColumn(columnHandle)">
            <span aria-hidden="true" class="material-symbols-outlined">more_horiz</span>
        </button>
        <button
            #rowHandle
            type="button"
            data-testid="table-row-handle"
            class="table-handle table-handle--row"
            [attr.aria-label]="rowAriaLabel"
            (mousedown)="$event.preventDefault(); openRow(rowHandle)">
            <span aria-hidden="true" class="material-symbols-outlined">more_vert</span>
        </button>
    `
})
export class TableHandlesComponent implements OnDestroy {
    readonly editor = input.required<Editor>();

    private readonly store = inject(TableHandlesStore);
    private readonly popovers = inject(EditorPopoverService);
    private readonly el = inject(ElementRef<HTMLElement>);
    private readonly zone = inject(NgZone);
    private readonly dotMessageService = inject(DotMessageService);

    protected readonly columnAriaLabel = this.dotMessageService.get(
        'dot.block.editor.table.handle.column.aria-label'
    );
    protected readonly rowAriaLabel = this.dotMessageService.get(
        'dot.block.editor.table.handle.row.aria-label'
    );

    protected readonly isVisible = computed(() => this.store.activeCell() !== null);

    private autoUpdateDispose: Array<() => void> = [];

    constructor() {
        effect(() => {
            const active = this.store.activeCell();
            this.teardownAutoUpdate();
            if (!active) return;

            for (const handle of this.collectHandles()) {
                const refEl = pickReference(handle, active);
                if (!refEl) continue;
                this.autoUpdateDispose.push(
                    autoUpdate(
                        refEl,
                        handle.el,
                        () => this.applyPosition(handle.el, refEl, handle.placement),
                        { ancestorScroll: true, ancestorResize: true, elementResize: true }
                    )
                );
            }
        });
    }

    ngOnDestroy(): void {
        this.teardownAutoUpdate();
    }

    private collectHandles(): PositionedHandle[] {
        const root = this.el.nativeElement as HTMLElement;
        const buttons = root.querySelectorAll('.table-handle');
        if (buttons.length < 2) return [];
        return [
            { el: buttons[0] as HTMLElement, placement: 'top' },
            { el: buttons[1] as HTMLElement, placement: 'left' }
        ];
    }

    private applyPosition(el: HTMLElement, refEl: HTMLElement, placement: 'top' | 'left'): void {
        void computePosition(refEl, el, {
            placement: placement === 'top' ? 'top' : 'left',
            strategy: 'fixed',
            middleware: [shift({ padding: 4 })]
        }).then(({ x, y }) => {
            this.zone.run(() => {
                el.style.position = 'fixed';
                el.style.left = `${x}px`;
                el.style.top = `${y}px`;
            });
        });
    }

    private teardownAutoUpdate(): void {
        for (const dispose of this.autoUpdateDispose) dispose();
        this.autoUpdateDispose = [];
    }

    // ── Popover openers ──────────────────────────────────────────────────────

    protected openColumn(anchor: HTMLElement): void {
        const active = this.store.activeCell();
        if (!active) return;
        this.popovers.openTableColumn(() => anchor.getBoundingClientRect(), {
            cellPos: active.cellPos,
            isHeader: active.isHeader,
            headerScope: this.readScope(active)
        });
    }

    protected openRow(anchor: HTMLElement): void {
        const active = this.store.activeCell();
        if (!active) return;
        this.popovers.openTableRow(() => anchor.getBoundingClientRect(), {
            cellPos: active.cellPos
        });
    }

    private readScope(active: ActiveCell): string {
        const node = this.editor().state.doc.nodeAt(active.cellPos);
        if (!node || node.type.name !== 'tableHeader') return '';
        return (node.attrs['scope'] as string | null) ?? '';
    }
}

function pickReference(handle: PositionedHandle, active: ActiveCell): HTMLElement | null {
    if (handle.placement === 'top' && handle.el.classList.contains('table-handle--column')) {
        return active.columnHeadEl;
    }
    if (handle.placement === 'left' && handle.el.classList.contains('table-handle--row')) {
        return active.rowHeadEl;
    }
    return null;
}
