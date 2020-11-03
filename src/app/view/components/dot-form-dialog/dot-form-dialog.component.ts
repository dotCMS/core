import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { DynamicDialogRef } from 'primeng/dynamicdialog';
import { fromEvent, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
    selector: 'dot-form-dialog',
    templateUrl: './dot-form-dialog.component.html',
    styleUrls: ['./dot-form-dialog.component.scss']
})
export class DotFormDialogComponent implements OnInit, OnDestroy {
    destroy = new Subject();
    destroy$ = this.destroy.asObservable();

    @Input()
    saveButtonDisabled: boolean;

    @Output()
    save: EventEmitter<MouseEvent> = new EventEmitter(null);

    @Output()
    cancel: EventEmitter<MouseEvent> = new EventEmitter(null);

    constructor(private dynamicDialog: DynamicDialogRef) {}

    ngOnInit(): void {
        const content = document.querySelector('p-dynamicdialog .p-dialog-content');

        fromEvent(content, 'scroll')
            .pipe(takeUntil(this.destroy$))
            .subscribe((e: Event) => {
                const pos = this.getYPosition(e);
                const target = e.target as HTMLDivElement;
                target.style.boxShadow = pos > 10 ? 'inset 0px 3px 20px 0 #00000026' : null;
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
        this.save.emit(event);
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
