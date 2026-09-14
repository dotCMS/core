import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    effect,
    inject,
    input,
    output,
    viewChild
} from '@angular/core';
import {
    AbstractControl,
    NonNullableFormBuilder,
    ReactiveFormsModule,
    ValidationErrors,
    ValidatorFn,
    Validators
} from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';

import { DotMessagePipe } from '../../../dot-message/dot-message.pipe';
import { DotKeyValue } from '../dot-key-value-ng.component';
import { parseKeyValueBlock } from '../dot-key-value-paste.util';

/**
 * The always-available row for adding a new pair, under the column headers.
 * Attached as an attribute so the host IS the `tr` — see
 * {@link DotKeyValueTableRowComponent} for why.
 */
@Component({
    // `libs/ui` narrows this rule to element selectors; see the class doc for why
    // that is not usable here.
    // eslint-disable-next-line @angular-eslint/component-selector
    selector: 'tr[dotKeyValueTableHeaderRow]',
    templateUrl: './dot-key-value-table-header-row.component.html',
    imports: [
        ButtonModule,
        IconFieldModule,
        InputIconModule,
        InputTextModule,
        ReactiveFormsModule,
        DotMessagePipe
    ],
    changeDetection: ChangeDetectionStrategy.Eager
})
export class DotKeyValueTableHeaderRowComponent {
    #fb = inject(NonNullableFormBuilder);

    $keyCell = viewChild.required<ElementRef<HTMLInputElement>>('keyCell');

    $valueCell = viewChild.required<ElementRef<HTMLInputElement>>('valueCell');

    $showHiddenField = input<boolean>(false, { alias: 'showHiddenField' });

    /** Keys already in the list, which this row must not duplicate. */
    $forbiddenkeys = input<Record<string, boolean>>({}, { alias: 'forbiddenkeys' });

    save = output<DotKeyValue>();

    /** A `.env`-style block pasted into the key input, already parsed. */
    saveMany = output<DotKeyValue[]>();

    form = this.#fb.group({
        key: ['', [Validators.required, this.#keyValidator()]],
        value: ['', Validators.required],
        hidden: [false]
    });

    constructor() {
        effect(() => {
            this.$forbiddenkeys();
            this.keyControl.updateValueAndValidity({ emitEvent: false });
        });
    }

    get keyControl() {
        return this.form.controls.key;
    }

    get valueControl() {
        return this.form.controls.value;
    }

    get hiddenControl() {
        return this.form.controls.hidden;
    }

    /**
     * Emits the pair if the form is valid, otherwise flags the offending control.
     */
    saveVariable(): void {
        if (!this.form.valid) {
            this.form.markAllAsTouched();
            this.keyControl.markAsDirty();
            this.valueControl.markAsDirty();

            return;
        }

        this.save.emit(this.form.getRawValue());
        this.resetForm();
    }

    /**
     * Clears the row and returns focus to the key input, so consecutive pairs
     * can be entered without reaching for the pointer (FR-012).
     */
    resetForm(): void {
        this.form.reset();
        this.$keyCell().nativeElement.focus();
    }

    onCancel(event: Event): void {
        event.stopPropagation();
        this.resetForm();
    }

    /**
     * Turns a pasted `KEY=VALUE` block into pairs, the way Vercel's env editor does.
     *
     * Only intercepts when the text actually parses to pairs, so pasting a plain key
     * still behaves like a paste. Keys already in the list are reported rather than
     * overwritten — see {@link parseKeyValueBlock}.
     */
    handleKeyInputPaste(event: ClipboardEvent): void {
        const text = event.clipboardData?.getData('text') ?? '';
        const pairs = parseKeyValueBlock(text, this.$forbiddenkeys());

        // Nothing parsed means this is not a block: let the browser paste it into the
        // input as it normally would, where the user can see it and fix it.
        if (!pairs.length) {
            return;
        }

        event.preventDefault();
        this.saveMany.emit(pairs);
        this.resetForm();
    }

    /** Enter on the key input advances to the value, if the key is usable. */
    handleKeyInputEnter(event: Event): void {
        event.preventDefault();

        if (this.keyControl.valid) {
            this.$valueCell().nativeElement.focus();

            return;
        }

        this.$keyCell().nativeElement.focus();
    }

    /** Enter on the value input adds the pair. */
    handleValueInputEnter(event: Event): void {
        event.preventDefault();
        this.saveVariable();
    }

    /**
     * Rejects a key that is already present in the list.
     */
    #keyValidator(): ValidatorFn {
        return ({ value }: AbstractControl): ValidationErrors | null =>
            this.$forbiddenkeys()[value] ? { duplicatedKey: true } : null;
    }
}
