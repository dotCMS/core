import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogConfig, DynamicDialogModule, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';
import { DotColorIconComponent } from '../dot-color-icon/dot-color-icon.component';

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

    /**
     * Design-token overrides for the option cards. We use an outlined
     * secondary button so the border tokens are part of the variant
     * (text-buttons have no border by design). Push the colors we want:
     * gray-300 default, primary on hover, primary-50 on hover-bg → looks
     * close to a soft "selectable card." Conditional active styling
     * (when this card is the chosen value) is handled in the template
     * via Tailwind utilities since `dt` can't depend on input state.
     */
    protected readonly cardDt = {
        outlined: {
            secondary: {
                // Tailwind border classes lose the specificity fight against
                // PrimeNG's outlined-secondary-borderColor token (applied via
                // CSS variable on the inner button). Push the colors we want
                // through the same channel so they actually take effect.
                // The "selected" state still uses Tailwind's
                // border-primary-500! override (! = !important) on top.
                borderColor: 'var(--p-content-border-color)',
                color: 'var(--p-text-color)',
                hoverBackground: 'transparent',
                activeBackground: 'transparent'
            }
        }
    };

    constructor() {
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
