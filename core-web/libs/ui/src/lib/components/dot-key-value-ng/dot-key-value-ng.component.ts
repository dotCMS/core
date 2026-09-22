import {
    ChangeDetectionStrategy,
    Component,
    computed,
    inject,
    input,
    linkedSignal,
    output,
    signal
} from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
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
        ConfirmDialogModule,
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
     * offer control the user does not actually have. The Show all toggle and the hidden
     * indicator stay: they help read a long list without pretending it can be edited.
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
     * Read-only drops the actions column, leaving three for the empty state and the
     * foot to span. The gutter column stays either way — read-only empties it rather
     * than removing it, so the header and the body keep the same shape.
     */
    $colspan = computed(() => (this.$readOnly() ? 3 : 4));

    /**
     * Rows rendered while the list is collapsed.
     *
     * Matches `dot-relationship-field`, which shows the same two-state toggle over the
     * same number of rows — one threshold across the two, so an editor learns the
     * affordance once.
     */
    static readonly PAGE_SIZE = 40;

    /**
     * Scopes the confirmation to this editor's own dialog.
     *
     * The editor carries a `p-confirmDialog` because its hosts do not agree on one:
     * Edit Content's layout renders one unconditionally, while dotcms-ui's only exists
     * while `DotAlertConfirmService` has a model — so an unkeyed `confirm()` from here
     * reached nothing at all in Field Variables and Apps, and Clear All did nothing.
     *
     * The key keeps the two apart in both directions: Edit Content's unkeyed dialog
     * ignores this confirmation, and this one ignores everyone else's.
     */
    protected readonly CLEAR_ALL_KEY = 'dot-key-value-clear-all';

    /**
     * Whether the list is showing every pair or only the first {@link PAGE_SIZE}.
     *
     * A flag rather than a count: the control is a two-state toggle — "Show all (N)"
     * and "Show less" — not an incremental reveal, so there is no intermediate amount
     * to track. Revealing a long list is one click at any length, which is what the
     * incremental version was asked to stop making people repeat.
     *
     * **Deliberately state, not derived from `$variables`.** Two of the three consumers
     * hand back a fresh array on every edit, so reacting to that input collapsed the
     * table the moment anything changed after expanding — 95 rows back down to 40 on a
     * single delete.
     *
     * A field opened afresh still starts collapsed, because each consumer builds a new
     * editor for it: a new dialog in Field Variables, a new panel in Apps, a new field
     * component in Edit Content.
     */
    $showingAll = signal(false);

    /**
     * How many rows are currently rendered: all of them, or the first page.
     */
    $visibleCount = computed(() =>
        this.$showingAll() ? this.$variableList().length : DotKeyValueComponent.PAGE_SIZE
    );

    /**
     * Whether the toggle is worth rendering at all: only once the list outgrows one
     * page. Below that there is nothing to expand and nothing to collapse.
     */
    $canToggleAll = computed(() => this.$variableList().length > DotKeyValueComponent.PAGE_SIZE);

    /**
     * Expands the list to every row, or collapses it back to the first page.
     *
     * Purely a rendering limit: the whole list is already in memory, so unlike the
     * site/folder selector this fetches nothing. Rows are withheld from the DOM,
     * never from the data — every operation below still indexes the full list.
     */
    toggleShowAll(): void {
        this.$showingAll.update((showingAll) => !showingAll);
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
     * `save` is emitted per pair on purpose: it is a per-pair output, and a consumer
     * that acts on it — as Edit Content's did before this component grew `updatedList`
     * — has to see every pair a block paste added, not just the last. `updatedList`
     * fires once, since its consumers take the whole array.
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
     * Confirmed through `ConfirmationService`, against this component's own keyed
     * dialog — see {@link CLEAR_ALL_KEY} for why it cannot rely on the host's. Its
     * options match the unsaved-changes prompt in Edit Content: a header and no
     * icons. The reject is a plain text button, as `dot-tags-list` does.
     *
     * `delete` is emitted per pair to keep that output's contract — one event per pair
     * removed, however they were removed; `updatedList` fires once for the consumers
     * that take the whole array.
     */
    confirmClearAll(): void {
        const cleared = this.$variableList();

        if (!cleared.length) {
            return;
        }

        this.#confirmationService.confirm({
            key: this.CLEAR_ALL_KEY,
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
     *
     * Read-only never gets this far — the rows are bound `pReorderableRowDisabled`
     * and render no handle — but the guard stays: this is the one path that would
     * publish a change from a list the consumer was told cannot produce any.
     */
    onRowReorder(): void {
        if (this.$readOnly()) {
            return;
        }

        const reordered = [...this.$variableList()];

        this.$variableList.set(reordered);
        this.updatedList.emit(reordered);
    }
}
