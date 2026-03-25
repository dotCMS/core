import { ChangeDetectionStrategy, Component, inject, input, OnDestroy } from '@angular/core';

import { CardModule } from 'primeng/card';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';

import { DotRulesDialogComponent } from './components/rules-dialog/rules-dialog.component';

/**
 * Tab content component for the Rules section in the edit content sidebar.
 * Renders a clickable card that opens the rules modal.
 */
@Component({
    selector: 'dot-edit-content-sidebar-rules',
    imports: [CardModule, DotMessagePipe],
    templateUrl: './dot-edit-content-sidebar-rules.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEditContentSidebarRulesComponent implements OnDestroy {
    readonly #dialogService = inject(DialogService);
    readonly #dotMessageService = inject(DotMessageService);

    #rulesDialogRef: DynamicDialogRef | undefined;

    /**
     * Contentlet identifier for the rules engine.
     */
    readonly $identifier = input<string>('', { alias: 'identifier' });

    /**
     * Contentlet language id.
     */
    readonly $languageId = input<number>(0, { alias: 'languageId' });

    ngOnDestroy(): void {
        this.#rulesDialogRef?.close();
    }

    /**
     * Opens the rules dialog for the current contentlet.
     * Prevents opening multiple instances if the user clicks the card repeatedly.
     */
    openRulesDialog(): void {
        if (this.#rulesDialogRef) return;

        const id = this.$identifier();
        if (!id) return;

        const header = this.#dotMessageService.get('edit.content.sidebar.rules.title');
        this.#rulesDialogRef = this.#dialogService.open(DotRulesDialogComponent, {
            header,
            width: 'min(92vw, 75rem)',
            data: { identifier: id },
            modal: true,
            appendTo: 'body',
            closeOnEscape: false,
            closable: true,
            draggable: false,
            keepInViewport: false,
            resizable: false,
            position: 'center'
        });
        this.#rulesDialogRef.onClose.subscribe({
            next: () => {
                this.#rulesDialogRef = undefined;
            }
        });
    }
}
