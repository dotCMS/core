import { fromEvent, Subject } from 'rxjs';

import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    inject
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { FocusTrapModule } from 'primeng/focustrap';

import { takeUntil } from 'rxjs/operators';

import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-form-dialog',
    imports: [ButtonModule, FocusTrapModule, DotMessagePipe],
    templateUrl: './dot-form-dialog.component.html',
    styleUrls: ['./dot-form-dialog.component.scss']
})
export class DotFormDialogComponent implements OnInit, OnDestroy {
    private dynamicDialog = inject(DynamicDialogRef);
    private el = inject(ElementRef);

    destroy = new Subject<void>();
    destroy$ = this.destroy.asObservable();

    @Input()
    saveButtonDisabled: boolean;

    @Input()
    saveButtonLoading: boolean;

    @Output()
    save: EventEmitter<MouseEvent | KeyboardEvent> = new EventEmitter(null);

    @Output()
    cancel: EventEmitter<MouseEvent> = new EventEmitter(null);

    ngOnInit(): void {
        const content = document.querySelector('p-dynamicdialog .p-dialog-content');

        fromEvent(content, 'scroll')
            .pipe(takeUntil(this.destroy$))
            .subscribe((e: Event) => {
                const pos = this.getYPosition(e);
                const target = e.target as HTMLDivElement;
                target.style.boxShadow = pos > 10 ? 'inset 0px 3px 20px 0 #00000026' : null;
            });

        fromEvent(this.el.nativeElement, 'keydown')
            .pipe(takeUntil(this.destroy$))
            .subscribe((keyboardEvent: KeyboardEvent) => {
                const nodeName = (keyboardEvent.target as Element).nodeName;
                if (
                    !this.saveButtonDisabled &&
                    !this.saveButtonLoading &&
                    nodeName !== 'TEXTAREA' &&
                    keyboardEvent.key === 'Enter' &&
                    (keyboardEvent.metaKey || keyboardEvent.altKey)
                ) {
                    this.save.emit(keyboardEvent);
                }
            });
    }

    ngOnDestroy(): void {
        this.destroy.next();
    }

    /**
     * Handle primary button click
     *
     * @param {MouseEvent} event
     * @memberof DotFormDialogComponent
     */
    onPrimaryClick(event: MouseEvent): void {
        if (!this.saveButtonDisabled && !this.saveButtonLoading) {
            this.save.emit(event);
        }
    }

    /**
     * Handle secondary button click
     *
     * @param {MouseEvent} $event
     * @memberof DotFormDialogComponent
     */
    onSecondaryClick($event: MouseEvent): void {
        this.cancel.emit($event);

        if (!this.cancel.observers.length) {
            this.dynamicDialog.close();
        }
    }

    private getYPosition(e: Event): number {
        return (e.target as Element).scrollTop;
    }
}
