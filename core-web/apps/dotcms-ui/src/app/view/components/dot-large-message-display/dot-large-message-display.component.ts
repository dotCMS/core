import { Observable, Subject } from 'rxjs';

import {
    AfterViewInit,
    Component,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChildren,
    inject
} from '@angular/core';

import { DialogModule, Dialog } from 'primeng/dialog';

import { filter, takeUntil } from 'rxjs/operators';

import { DotcmsEventsService } from '@dotcms/dotcms-js';

import { DotParseHtmlService } from '../../../api/services/dot-parse-html/dot-parse-html.service';

interface DotLargeMessageDisplayParams {
    title: string;
    width: string;
    height: string;
    body: string;
    script?: string;
    code?: {
        lang: string;
        content: string;
    };
}

@Component({
    selector: 'dot-large-message-display',
    templateUrl: './dot-large-message-display.component.html',
    styleUrls: ['./dot-large-message-display.component.scss'],
    imports: [DialogModule],
    providers: [DotParseHtmlService]
})
export class DotLargeMessageDisplayComponent implements OnInit, OnDestroy, AfterViewInit {
    private dotcmsEventsService = inject(DotcmsEventsService);
    private dotParseHtmlService = inject(DotParseHtmlService);

    @ViewChildren(Dialog) dialogs: QueryList<Dialog>;

    messages: DotLargeMessageDisplayParams[] = [];
    messageVisibility: Map<DotLargeMessageDisplayParams, boolean> = new Map();
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private recentlyDialogAdded: boolean;

    getMessageVisibility(message: DotLargeMessageDisplayParams): boolean {
        return this.messageVisibility.get(message) ?? false;
    }

    setMessageVisibility(message: DotLargeMessageDisplayParams, visible: boolean): void {
        this.messageVisibility.set(message, visible);
    }

    ngAfterViewInit() {
        this.dialogs.changes
            .pipe(
                takeUntil(this.destroy$),
                filter(() => this.recentlyDialogAdded)
            )
            .subscribe((dialogs: QueryList<Dialog>) => {
                this.createContent(dialogs.last, this.messages[this.messages.length - 1]);
                this.recentlyDialogAdded = false;
            });
    }

    ngOnInit() {
        this.getMessages()
            .pipe(takeUntil(this.destroy$))
            .subscribe((content: DotLargeMessageDisplayParams) => {
                this.recentlyDialogAdded = true;
                this.messages.push(content);
                this.messageVisibility.set(content, !!content.title);
            });
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Close dialog's component by clearing messages from service
     *
     * @param {DotLargeMessageDisplayParams} messageToRemove
     * @memberof DotLargeMessageDisplayComponent
     */
    close(messageToRemove: DotLargeMessageDisplayParams) {
        this.messageVisibility.delete(messageToRemove);
        this.messages.splice(this.messages.indexOf(messageToRemove), 1);
    }

    onVisibilityChange(message: DotLargeMessageDisplayParams, visible: boolean): void {
        this.setMessageVisibility(message, visible);
        if (!visible) {
            this.close(message);
        }
    }

    private createContent(dialogComponent: Dialog, content: DotLargeMessageDisplayParams): void {
        // Access the dialog container element - container is already the native element
        const dialogElement = dialogComponent.container as HTMLElement;
        const target = dialogElement?.querySelector('.dialog-message__body') as HTMLElement;
        if (target) {
            this.dotParseHtmlService.parse(content.body, target, true);
            if (content.script) {
                this.dotParseHtmlService.parse(`<script>${content.script}</script>`, target, false);
            }
        }
    }

    private getMessages(): Observable<DotLargeMessageDisplayParams> {
        return this.dotcmsEventsService
            .subscribeTo<DotLargeMessageDisplayParams>('LARGE_MESSAGE')
            .pipe(filter((data: DotLargeMessageDisplayParams) => !!data));
    }
}
