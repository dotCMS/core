import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessagePipe } from '@dotcms/ui';

/**
 * Confirms a company key regeneration. Regenerate Key stays disabled until the user ticks that
 * they understand the impact. Closes with `true` to regenerate and with nothing otherwise.
 */
@Component({
    selector: 'dot-configuration-regenerate-key-dialog',
    imports: [FormsModule, ButtonModule, CheckboxModule, DotMessagePipe],
    templateUrl: './dot-configuration-regenerate-key-dialog.component.html',
    styleUrls: ['./dot-configuration-regenerate-key-dialog.component.scss']
})
export class DotConfigurationRegenerateKeyDialogComponent {
    readonly #ref = inject(DynamicDialogRef);

    protected readonly $acknowledged = signal(false);

    onAcknowledgedChange(acknowledged: boolean): void {
        this.$acknowledged.set(acknowledged);
    }

    confirm(): void {
        if (this.$acknowledged()) {
            this.#ref.close(true);
        }
    }

    cancel(): void {
        this.#ref.close();
    }
}
