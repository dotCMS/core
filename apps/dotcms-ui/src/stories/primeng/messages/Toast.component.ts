import { Component } from '@angular/core';
import { MessageService } from 'primeng/api';

@Component({
    selector: 'app-p-toast',
    template: `
        <p-toast></p-toast>
        <p-toast position="top-left" key="tl"></p-toast>
        <p-toast position="top-center" key="tc"></p-toast>
        <p-toast position="bottom-center" key="bc"></p-toast>
        <p>
            <button type="button" pButton pRipple (click)="showSuccess()" label="Success"></button>
        </p>
        <p>
            <button type="button" pButton pRipple (click)="showError()" label="Error"></button>
        </p>
        <p>
            <button type="button" pButton pRipple (click)="showInfo()" label="Info"></button>
        </p>
        <p>
            <button type="button" pButton pRipple (click)="showWarn()" label="Warning"></button>
        </p>
    `
})
export class ToastComponent {
    constructor(private messageService: MessageService) {}

    ngOnInit() {
        console.log('object');
    }

    showSuccess() {
        this.messageService.add({
            severity: 'success',
            detail: 'The action "Publish" was executed succesfully'
        });
    }

    showInfo() {
        this.messageService.add({
            severity: 'info',
            detail: 'Make sure you add the width of the element'
        });
    }

    showWarn() {
        this.messageService.add({
            severity: 'warn',
            detail: 'Make sure you add the width of the element'
        });
    }

    showError() {
        this.messageService.add({
            severity: 'error',
            detail: 'Something went wrong, please try again.'
        });
    }

    showTopLeft() {
        this.messageService.add({
            key: 'tl',
            severity: 'info',
            detail: 'Message Content'
        });
    }

    showTopCenter() {
        this.messageService.add({
            key: 'tc',
            severity: 'info',
            detail: 'Message Content'
        });
    }

    showBottomCenter() {
        this.messageService.add({
            key: 'bc',
            severity: 'info',
            detail: 'Message Content'
        });
    }

    showConfirm() {
        this.messageService.clear();
        this.messageService.add({
            key: 'c',
            sticky: true,
            severity: 'warn',
            summary: 'Are you sure?',
            detail: 'Confirm to proceed'
        });
    }

    showMultiple() {
        this.messageService.addAll([
            { severity: 'info', summary: 'Message 1', detail: 'Message Content' },
            { severity: 'info', summary: 'Message 2', detail: 'Message Content' },
            { severity: 'info', summary: 'Message 3', detail: 'Message Content' }
        ]);
    }

    showSticky() {
        this.messageService.add({
            severity: 'info',
            summary: 'Sticky',
            detail: 'Message Content',
            sticky: true
        });
    }

    onConfirm() {
        this.messageService.clear('c');
    }

    onReject() {
        this.messageService.clear('c');
    }

    clear() {
        this.messageService.clear();
    }
}
