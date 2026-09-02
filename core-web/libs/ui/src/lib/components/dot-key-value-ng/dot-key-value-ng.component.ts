import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    linkedSignal,
    output
} from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotMessageService } from '@dotcms/data-access';

import { DotKeyValueTableHeaderRowComponent } from './dot-key-value-table-header-row/dot-key-value-table-header-row.component';
import { DotKeyValueTableRowComponent } from './dot-key-value-table-row/dot-key-value-table-row.component';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

export interface DotKeyValue {
    key: string;
    hidden?: boolean;
    value: string;
}

/**
 * Editor for an ordered list of key/value pairs, shared by three consumers:
 * the Edit Content key/value field, the Content Type Field Variables tab, and
 * the Apps custom-properties panel.
 *
 * This component owns the list and the table frame; the entry row and each data
 * row are separate components attached by attribute selector, so their hosts are
 * the `tr` elements themselves.
 */
@Component({
    selector: 'dot-key-value-ng',
    templateUrl: './dot-key-value-ng.component.html',
    imports: [
        ButtonModule,
        TableModule,
        DotKeyValueTableHeaderRowComponent,
        DotKeyValueTableRowComponent,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotKeyValueComponent {
    #confirmationService = inject(ConfirmationService);
    #dotMessageService = inject(DotMessageService);

    $showHiddenField = input<boolean>(false, { alias: 'showHiddenField' });

    $variables = input<DotKeyValue[]>([], { alias: 'variables' });

    /**
     * Renders the list without any way to change it.
     *
     * For fields whose pairs are produced elsewhere — a file asset's metadata is
     * regenerated on every save — where adding, editing, reordering or clearing would
     * offer control the user does not actually have. Paging and the hidden indicator
     * stay: they help read a long list without pretending it can be edited.
     */
    $readOnly = input<boolean>(false, { alias: 'readOnly' });

    updatedList = output<DotKeyValue[]>();

    delete = output<DotKeyValue>();

    save = output<DotKeyValue>();

    update = output<{
        variable: DotKeyValue;
        oldVariable: DotKeyValue;
    }>();

    /**
     * Working copy of the list. Seeded from `$variables` and re-seeded whenever
     * the consumer supplies a new one.
     */
    $variableList = linkedSignal(() => this.$variables());

    /**
     * Existing keys, so the entry row can reject a duplicate before it is added.
     */
    $forbiddenkeys = computed(() =>
        this.$variableList().reduce(
            (acc, variable) => {
                acc[variable.key] = true;

                return acc;
            },
            {} as Record<string, boolean>
        )
    );

    /**
     * Column count for the full-width rows.
     *
     * Read-only drops the actions column — reordering stays, so the drag column does
     * too — leaving three for the empty state and the foot to span.
     */
    $colspan = computed(() => (this.$readOnly() ? 3 : 4));

    /** Rows revealed per step, matching the site/folder selector's page size. */
    static readonly PAGE_SIZE = 40;

    /**
     * How many rows are currently rendered.
     *
     * Re-seeded whenever the consumer supplies a new list, so reopening a field
     * never starts part-way down a previous one.
     */
    $visibleCount = linkedSignal<DotKeyValue[], number>({
        source: this.$variables,
        computation: () => DotKeyValueComponent.PAGE_SIZE
    });

    /** Rows still hidden below the last rendered one. */
    $remaining = computed(() => Math.max(0, this.$variableList().length - this.$visibleCount()));

    /**
     * Reveals the next page.
     *
     * Purely a rendering limit: the whole list is already in memory, so unlike the
     * site/folder selector this fetches nothing. Rows are withheld from the DOM,
     * never from the data — every operation below still indexes the full list.
     */
    loadMore(): void {
        this.$visibleCount.update((count) => count + DotKeyValueComponent.PAGE_SIZE);
    }

    saveVariable(variable: DotKeyValue): void {
        this.$variableList.update((variables) => [variable, ...variables]);
        this.save.emit(variable);
        this.updatedList.emit(this.$variableList());
    }

    /**
     * Adds a whole pasted block at once.
     *
     * The block keeps its own order and goes on top as one group, so pasting
     * `A,B,C` reads `A,B,C` and not reversed — which is what adding them one by one
     * through {@link saveVariable} would produce.
     *
     * `save` is emitted per pair on purpose: Field Variables persists row by row
     * through that output, so a block paste has to reach it as individual writes.
     * `updatedList` fires once, since its consumers take the whole array.
     */
    saveVariables(variables: DotKeyValue[]): void {
        if (!variables.length) {
            return;
        }

        this.$variableList.update((current) => [...variables, ...current]);
        variables.forEach((variable) => this.save.emit(variable));
        this.updatedList.emit(this.$variableList());
    }

    updateKeyValue(variable: DotKeyValue, index: number): void {
        const oldVariable = this.$variableList()[index];
        this.$variableList.update((variables) =>
            variables.map((item, i) => (i === index ? variable : item))
        );
        this.update.emit({ variable, oldVariable });
        this.updatedList.emit(this.$variableList());
    }

    deleteVariable(index: number): void {
        const deletedVariable = this.$variableList()[index];
        this.$variableList.update((variables) => variables.filter((_, i) => i !== index));
        this.delete.emit(deletedVariable);
        this.updatedList.emit(this.$variableList());
    }

    /**
     * Empties the list, behind a confirmation.
     *
     * Confirmed through `ConfirmationService`, so the dialog each host already renders
     * is the one that appears — this component brings none of its own. Its options
     * match the unsaved-changes prompt in Edit Content: a header and no icons. The
     * reject is a plain text button, as `dot-tags-list` does.
     *
     * `delete` is emitted per pair because Field Variables persists row by row through
     * it; `updatedList` fires once for the consumers that take the whole array.
     */
    confirmClearAll(): void {
        const cleared = this.$variableList();

        if (!cleared.length) {
            return;
        }

        this.#confirmationService.confirm({
            header: this.#dotMessageService.get('keyValue.clear_all.title'),
            message: this.#dotMessageService.get('keyValue.clear_all.message'),
            acceptLabel: this.#dotMessageService.get('keyValue.clear_all.accept'),
            rejectLabel: this.#dotMessageService.get('keyValue.clear_all.reject'),
            acceptIcon: 'hidden',
            rejectIcon: 'hidden',
            rejectButtonStyleClass: 'p-button-text',
            accept: () => {
                this.$variableList.set([]);
                cleared.forEach((variable) => this.delete.emit(variable));
                this.updatedList.emit([]);
            }
        });
    }

    /**
     * Publishes the list after a drag-and-drop reorder.
     *
     * PrimeNG has already moved the item by this point — `onRowDrop` calls
     * `reorderArray` on the very array bound to `[value]`, which is the one this
     * signal holds. So the move must NOT be applied again here; all that is left
     * is to hand out a new reference, since an in-place mutation leaves the
     * signal comparing equal and never notifying.
     */
    onRowReorder(): void {
        const reordered = [...this.$variableList()];

        this.$variableList.set(reordered);
        this.updatedList.emit(reordered);
    }
}
