import { Component, Input } from '@angular/core';

import { MessageService } from 'primeng/api';

@Component({
    selector: 'dot-p-toast',
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
    @Input() severity: string = 'success';
    @Input() summary: string = 'Success Message';
    @Input() detail: string = 'The action "Publish" was executed succesfully';
    @Input() position: string = 'top-right';
    @Input() life: number = 2000;
    @Input() icon: string = 'pi-check-circle';

    constructor(private messageService: MessageService) {}

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
