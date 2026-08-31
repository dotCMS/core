import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    computed,
    effect,
    inject,
    input,
    model,
    output,
    signal,
    viewChild
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';

/**
 * One existing key/value pair.
 *
 * Attached as an attribute so the host IS the `tr`: PrimeNG themes its table with
 * direct-child combinators (`.p-datatable-tbody > tr > td`), and an element wrapper
 * between the tbody and the tr silently defeats every table style the theme provides.
 */
@Component({
    // `libs/ui` narrows this rule to element selectors; see the class doc for why
    // that is not usable here.
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'tr[dotKeyValueTableRow]',
    templateUrl: './dot-key-value-table-row.component.html',
    // `group` drives the hover reveal of this row's actions. The row's own class is
    // how e2e locates a row from a cell inside it, and predates the redesign.
    host: { class: 'group dot-key-value-table-row' },
    imports: [ButtonModule, InputTextModule, ReactiveFormsModule, TableModule, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotKeyValueTableRowComponent {
    #fb = inject(FormBuilder);

    save = output<DotKeyValue>();

    delete = output<void>();

    $showHiddenField = input.required<boolean>({ alias: 'showHiddenField' });

    $index = input.required<number>({ alias: 'index' });

    $variable = model.required<DotKeyValue>({ alias: 'variable' });

    /**
     * The value input, which only exists while this row is being edited.
     */
    $valueInput = viewChild<ElementRef<HTMLInputElement>>('valueInput');

    /**
     * Whether this row's value is being edited. At rest the value is plain
     * text; activating it swaps in the input.
     */
    $isEditing = signal(false);

    editControl = this.#fb.nonNullable.control('');

    /** Value captured when editing started, so Escape can put it back. */
    #valueBeforeEdit = '';

    /**
     * Whether this row's value is withheld, in which case the row states that and
     * shows nothing else — no value, no input, no control.
     *
     * Gated on the capability, not just the flag: only the consumer that deals in
     * secrets should render the withheld state (FR-024).
     */
    $isHiddenField = computed(() => this.$showHiddenField() && !!this.$variable()?.hidden);

    constructor() {
        // Focus the input as soon as it is rendered, so activating a value and
        // typing is one uninterrupted motion for keyboard users.
        effect(() => {
            if (this.$isEditing()) {
                this.$valueInput()?.nativeElement.focus();
            }
        });
    }

    startEdit(): void {
        this.#valueBeforeEdit = this.$variable().value;
        this.editControl.setValue(this.#valueBeforeEdit);
        this.$isEditing.set(true);
    }

    /**
     * Commits the edit and returns the row to its at-rest presentation. Emits
     * only when the value actually changed, so merely tabbing through a row
     * does not look like an edit to the consumer.
     */
    commitEdit(event?: Event): void {
        event?.preventDefault();

        if (!this.$isEditing()) {
            return;
        }

        this.$isEditing.set(false);

        if (this.editControl.value !== this.#valueBeforeEdit) {
            this.save.emit({ ...this.$variable(), value: this.editControl.value });
        }
    }

    /**
     * Discards the edit. Leaving edit mode removes the input, which fires
     * `blur` — so that must happen before anything else, or `commitEdit` would
     * save the value being discarded.
     */
    cancelEdit(event?: Event): void {
        event?.preventDefault();
        this.$isEditing.set(false);
    }
}
