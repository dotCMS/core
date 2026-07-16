import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotColorIconComponent } from '../dot-color-icon/dot-color-icon.component';

/** A single selectable card in the binary-choice dialog. */
export interface OPTION {
    /** Value emitted via DynamicDialogRef.close() when this option is confirmed. */
    value: string;
    /** i18n key or pre-translated string rendered as the card body text. */
    message: string;
    /** Optional Material Symbols icon name rendered inside dot-color-icon. */
    icon?: string;
    /** i18n key or pre-translated string rendered as the card heading. */
    label: string;
    /** i18n key for the confirm button label; falls back to 'next' when absent. */
    buttonLabel?: string;
}

/** The two mutually exclusive choices presented to the user. */
export interface BINARY_OPTION {
    option1: OPTION;
    option2: OPTION;
}

/** Shape of DynamicDialogConfig.data expected by DotBinaryOptionSelectorComponent. */
export interface BinaryOptionDialogData {
    options: BINARY_OPTION;
    /** Optional i18n key rendered above the option cards via the dm pipe. */
    description?: string;
}

@Component({
    selector: 'dot-binary-selector',
    imports: [DotMessagePipe, ButtonModule, DynamicDialogModule, DotColorIconComponent],
    templateUrl: './dot-binary-option-selector.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryOptionSelectorComponent {
    private readonly ref = inject(DynamicDialogRef);
    private readonly config = inject(DynamicDialogConfig);

    value: string;
    private options: BINARY_OPTION;
    private readonly defaultBtnLabel = 'next';

    description: string | undefined;

    constructor() {
        const { options, description } = (this.config.data || {}) as BinaryOptionDialogData;
        this.options = options;
        this.value = this.options.option1.value;
        this.description = description;
    }

    get firstOption(): OPTION {
        return this.options.option1;
    }

    get secondOption(): OPTION {
        return this.options.option2;
    }

    /**
     * Active option's message — used for the dynamic button label.
     * Per-option descriptions render inline next to each card now,
     * but the button-label fallback path still keys on which option
     * is selected.
     */
    get message(): string {
        return this.value === this.firstOption.value
            ? this.firstOption.message
            : this.secondOption.message;
    }

    get btnLabel() {
        const label =
            this.value === this.firstOption.value
                ? this.firstOption.buttonLabel
                : this.secondOption.buttonLabel;

        return label || this.defaultBtnLabel;
    }

    onSelect() {
        this.ref.close(this.value);
    }
}
