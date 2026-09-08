import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostListener,
    Input,
    Output,
    inject
} from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dotcms-remove-confirm-dialog',
    templateUrl: './remove-confirm-dialog.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ConfirmPopupModule, ButtonModule],
    providers: [ConfirmationService, DotMessagePipe]
})
export class RemoveConfirmDialogComponent {
    private confirmationService = inject(ConfirmationService);
    private dotMessagePipe = inject(DotMessagePipe);

    @Input() skipConfirmation = false;
    @Output() deleteConfirmed: EventEmitter<void> = new EventEmitter();
    @Output() deleteRejected: EventEmitter<void> = new EventEmitter();
    /** Null when no popup is open; `onEscapePress` clears it back to null. */
    private currentPopup: ConfirmationService | null = null;

    @HostListener('document:keydown.escape')
    onEscapePress() {
        if (this.currentPopup) {
            this.currentPopup.close();
            this.deleteRejected.emit();
            this.currentPopup = null;
        }
    }

    openConfirmationDialog(event: Event): void {
        if (this.skipConfirmation) {
            this.deleteConfirmed.emit();
        } else {
            this.currentPopup = this.confirmationService.confirm({
                closeOnEscape: true,
                target: event.target ?? undefined,
                message: this.dotMessagePipe.transform(
                    'dot.template.builder.comfirmation.popup.message'
                ),
                icon: 'pi pi-info-circle',
                accept: () => {
                    this.deleteConfirmed.emit();
                },
                reject: () => {
                    this.deleteRejected.emit();
                }
            });
        }
    }
}
