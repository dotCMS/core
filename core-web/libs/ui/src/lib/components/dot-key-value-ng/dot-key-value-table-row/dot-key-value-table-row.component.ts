import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    computed,
    effect,
    inject,
    input,
    output,
    signal,
    viewChild
} from '@angular/core';
import {
    AbstractControl,
    FormBuilder,
    ReactiveFormsModule,
    ValidationErrors
} from '@angular/forms';

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

    /** Renders the pair as plain text, with nothing that could change it. */
    $readOnly = input<boolean>(false, { alias: 'readOnly' });

    $index = input.required<number>({ alias: 'index' });

    $variable = input.required<DotKeyValue>({ alias: 'variable' });

    /**
     * The value input, which only exists while this row is being edited.
     */
    $valueInput = viewChild<ElementRef<HTMLInputElement>>('valueInput');

    /** The key input, which only exists while the key is being edited. */
    $keyInput = viewChild<ElementRef<HTMLInputElement>>('keyInput');

    /**
     * Which cell of this row is being edited, if any. At rest both are plain text;
     * activating one swaps in its input.
     */
    $editing = signal<'key' | 'value' | null>(null);

    $isEditing = computed(() => this.$editing() === 'value');

    $isEditingKey = computed(() => this.$editing() === 'key');

    /**
     * The text being edited, whichever cell is open.
     *
     * Carries the same validators the entry row applies to the same field, so adding
     * `name` and renaming a row to `name` are refused for the same reason and say so
     * in the same words. Which validators are attached depends on the cell, and is
     * decided in {@link #beginEditing}.
     */
    editControl = this.#fb.nonNullable.control('');

    /** Keys held by other rows, so a rename cannot collide with one. */
    $forbiddenkeys = input<Record<string, boolean>>({}, { alias: 'forbiddenkeys' });

    /** Text captured when editing started, so Escape can put it back. */
    #textBeforeEdit = '';

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
            } else if (this.$isEditingKey()) {
                this.$keyInput()?.nativeElement.focus();
            }
        });
    }

    startEdit(): void {
        this.#beginEditing('value', this.$variable().value);
    }

    startEditKey(): void {
        this.#beginEditing('key', this.$variable().key);
    }

    #beginEditing(cell: 'key' | 'value', text: string): void {
        if (this.$readOnly()) {
            return;
        }

        this.#textBeforeEdit = text;
        this.editControl.setValidators(
            cell === 'key' ? [this.#nonBlank, this.#keyValidator] : [this.#nonBlank]
        );
        this.editControl.setValue(text);
        // Pristine, so the row opens without an error on text the user has not touched.
        this.editControl.markAsPristine();
        this.$editing.set(cell);
    }

    /**
     * Commits the edit and returns the row to its at-rest presentation. Emits
     * only when the text actually changed, so merely tabbing through a row
     * does not look like an edit to the consumer.
     *
     * An invalid edit — a blank key or value, or a rename onto a key another row
     * already holds — keeps the input open with its message rather than closing.
     * Closing would discard what the user typed without ever saying why it was
     * refused, which is what this used to do. Escape still abandons the edit.
     *
     * Keys commit trimmed: surrounding space in a key is never meaningful, and two
     * keys differing only by it would read as duplicates. Values commit exactly as
     * typed, and are compared the same way, so deliberately adding a trailing space
     * to a value is a change like any other — trimming only decides whether the
     * value counts as blank.
     */
    commitEdit(event?: Event): void {
        event?.preventDefault();

        const cell = this.$editing();

        if (!cell) {
            return;
        }

        if (this.editControl.invalid) {
            this.editControl.markAsDirty();

            return;
        }

        const raw = this.editControl.value;
        const text = cell === 'key' ? raw.trim() : raw;

        this.$editing.set(null);

        if (text === this.#textBeforeEdit) {
            return;
        }

        this.save.emit(
            cell === 'key'
                ? { ...this.$variable(), key: text }
                : { ...this.$variable(), value: text }
        );
    }

    /**
     * Discards the edit. Leaving edit mode removes the input, which fires
     * `blur` — so that must happen before anything else, or `commitEdit` would
     * save the text being discarded.
     */
    cancelEdit(event?: Event): void {
        event?.preventDefault();
        this.$editing.set(null);
    }

    /**
     * Required, reported under the same `required` error the entry row uses so both
     * rows can render the same message. A run of spaces is not a value.
     */
    #nonBlank = ({ value }: AbstractControl<string>): ValidationErrors | null =>
        value?.trim() ? null : { required: true };

    /**
     * Rejects a rename onto a key another row already holds.
     *
     * This row's own key is in the map too, so it has to be excluded — otherwise the
     * row would report itself as a duplicate the moment editing opened.
     */
    #keyValidator = ({ value }: AbstractControl<string>): ValidationErrors | null => {
        const key = value?.trim() ?? '';

        return key !== this.$variable().key && this.$forbiddenkeys()[key]
            ? { duplicatedKey: true }
            : null;
    };
}
