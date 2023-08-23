import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

export interface OPTION {
    value: string;
    message: string;
    icon: string;
    label: string;
    buttonLabel?: string;
}

export interface BINARY_OPTION {
    option1: OPTION;
    option2: OPTION;
}

@Component({
    selector: 'dot-binary-selector',
    standalone: true,
    imports: [CommonModule, FormsModule, DotMessagePipe, ButtonModule, DynamicDialogModule],
    templateUrl: './dot-binary-option-selector.component.html',
    styleUrls: ['./dot-binary-option-selector.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBinaryOptionSelectorComponent {
    value: string;
    private options: BINARY_OPTION;
    private readonly defaultBtnLabel = 'next';

    constructor(
        private readonly ref: DynamicDialogRef,
        private readonly config: DynamicDialogConfig
    ) {
        const { options } = this.config.data || {};
        this.options = options;
        this.value = this.options.option1.value;
    }

    get firstOption(): OPTION {
        return this.options.option1;
    }

    get secondOption(): OPTION {
        return this.options.option2;
    }

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
