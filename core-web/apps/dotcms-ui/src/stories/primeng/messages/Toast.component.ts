import { Component, Input, inject } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';

@Component({
    selector: 'dot-p-toast',
    standalone: true,
    imports: [ToastModule, ButtonModule],
    template: `
        <p-toast></p-toast>
        <p-toast [position]="position" [key]="position"></p-toast>
        <p>
            <button
                (click)="triggerToast()"
                type="button"
                pButton
                pRipple
                label="Click Me!"></button>
        </p>
    `
})
export class ToastComponent {
    private messageService = inject(MessageService);

    @Input() severity = 'success';
    @Input() summary = 'Success Message';
    @Input() detail = 'The action "Publish" was executed succesfully';
    @Input() position = 'top-right';
    @Input() life = 2000;
    @Input() icon = 'pi-check-circle';

    triggerToast() {
        this.messageService.add({
            key: this.position,
            severity: this.severity,
            summary: this.summary,
            detail: this.detail,
            life: this.life,
            icon: 'pi ' + this.icon
        });
    }
}
