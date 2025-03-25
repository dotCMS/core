import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { ExistingContentStore } from '../../store/existing-content.store';

@Component({
    selector: 'dot-select-existing-content-footer',
    standalone: true,
    imports: [ButtonModule, DotMessagePipe],
    templateUrl: './footer.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class FooterComponent {
    /**
     * A readonly instance of the ExistingContentStore injected into the component.
     * This store is used to manage the state and actions related to the existing content.
     */
    readonly store = inject(ExistingContentStore);
    /**
     * A readonly instance of the DotMessageService injected into the component.
     * This service is used to get localized messages.
     */
    readonly #dotMessage = inject(DotMessageService);

    /**
     * A reference to the dynamic dialog instance.
     * This is a read-only property that is injected using Angular's dependency injection.
     * It provides access to the dialog's methods and properties.
     */
    readonly #dialogRef = inject(DynamicDialogRef);

    /**
     * Determines the label for the apply button based on the number of selected items.
     * @returns The localized message for the apply button
     */
    applyLabel(): string {
        const count = this.store.items().length;

        const messageKey =
            count === 1
                ? 'dot.file.relationship.dialog.apply.one.entry'
                : 'dot.file.relationship.dialog.apply.entries';

        return this.#dotMessage.get(messageKey, count.toString());
    }

    /**
     * A method that closes the existing content dialog.
     * It sets the visibility signal to false, hiding the dialog.
     */
    applyChanges() {
        this.#dialogRef.close(this.store.items());
    }

    /**
     * A method that closes the existing content dialog.
     * It sets the visibility signal to false, hiding the dialog.
     */
    closeDialog() {
        this.#dialogRef.close();
    }
}
