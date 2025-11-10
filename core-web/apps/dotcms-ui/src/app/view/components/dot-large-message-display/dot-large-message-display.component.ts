import { Observable, Subject } from 'rxjs';

import { CommonModule } from '@angular/common';
import {
    AfterViewInit,
    Component,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChildren,
    inject
} from '@angular/core';

import { filter, takeUntil } from 'rxjs/operators';

import { DotcmsEventsService } from '@dotcms/dotcms-js';
import { DotDialogComponent } from '@dotcms/ui';

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
    imports: [CommonModule, DotDialogComponent],
    providers: [DotParseHtmlService]
})
export class DotLargeMessageDisplayComponent implements OnInit, OnDestroy, AfterViewInit {
    private dotcmsEventsService = inject(DotcmsEventsService);
    private dotParseHtmlService = inject(DotParseHtmlService);

    @ViewChildren(DotDialogComponent) dialogs: QueryList<DotDialogComponent>;

    messages: DotLargeMessageDisplayParams[] = [];
    private destroy$: Subject<boolean> = new Subject<boolean>();
    private recentlyDialogAdded: boolean;

    ngAfterViewInit() {
        this.dialogs.changes
            .pipe(
                takeUntil(this.destroy$),
                filter(() => this.recentlyDialogAdded)
            )
            .subscribe((dialogs: QueryList<DotDialogComponent>) => {
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
        this.messages.splice(this.messages.indexOf(messageToRemove), 1);
    }

    private createContent(
        dialogComponent: DotDialogComponent,
        content: DotLargeMessageDisplayParams
    ): void {
        const target = dialogComponent.dialog.nativeElement.querySelector('.dialog-message__body');
        this.dotParseHtmlService.parse(content.body, target, true);
        if (content.script) {
            this.dotParseHtmlService.parse(`<script>${content.script}</script>`, target, false);
        }
    }

    private getMessages(): Observable<DotLargeMessageDisplayParams> {
        return this.dotcmsEventsService
            .subscribeTo<DotLargeMessageDisplayParams>('LARGE_MESSAGE')
            .pipe(filter((data: DotLargeMessageDisplayParams) => !!data));
    }
}
