import { fromEvent, Subscription } from 'rxjs';

import {
    Component,
    ElementRef,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
    inject
} from '@angular/core';

import { filter } from 'rxjs/operators';

import { DialogButton, DotDialogActions } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-dialog',
    templateUrl: './dot-dialog.component.html',
    styleUrls: ['./dot-dialog.component.scss'],
    standalone: false
})
export class DotDialogComponent implements OnChanges {
    private el = inject(ElementRef);

    @ViewChild('dialog') dialog: ElementRef;

    @Input()
    @HostBinding('class.active')
    visible: boolean;

    @Input() header = '';

    @Input() actions: DotDialogActions;

    @Input() closeable = true;

    @Input() cssClass: string;

    @Input()
    contentStyle: {
        [key: string]: string;
    };

    @Input()
    isSaving = false;

    @Input()
    headerStyle: {
        [key: string]: string;
    };

    @Input() width: string;

    @Input() height: string;

    @Input() hideButtons: boolean;

    @Input() appendToBody = false;

    @Input() bindEvents = true;

    @Output() hide: EventEmitter<unknown> = new EventEmitter();

    @Output()
    beforeClose: EventEmitter<{
        close: () => void;
    }> = new EventEmitter();

    @Output() visibleChange: EventEmitter<unknown> = new EventEmitter();

    isContentScrolled: boolean;

    private subscription: Subscription[] = [];

    ngOnChanges(changes: SimpleChanges) {
        if (this.isVisible(changes)) {
            if (this.bindEvents) {
                this.bindKeydown();
            }

            this.appendContainer();
        }
    }

    /**
     * Accept button handler
     *
     * @memberof DotDialogComponent
     */
    acceptAction(): void {
        if (this.actions && this.canTriggerAction(this.actions.accept)) {
            this.actions.accept.action(this);
            this.unBindEvents();
        }
    }

    /**
     * Cancel button handler
     *
     * @memberof DotDialogComponent
     */
    cancelAction(): void {
        if (this.actions && this.canTriggerAction(this.actions.cancel)) {
            this.actions.cancel.action(this);
        } else {
            this.close();
        }
    }

    /**
     * Close dialog
     *
     * @memberof DotDialogComponent
     */
    close($event?: MouseEvent): void {
        if (this.beforeClose.observers.length) {
            this.beforeClose.emit({
                close: () => {
                    this.handleClose();
                }
            });
        } else {
            this.handleClose();
        }

        if ($event) {
            $event.preventDefault();
        }
    }

    /**
     * Handle scroll event in the content
     *
     * @param {{ target: HTMLInputElement }} event
     * @memberof DotDialogComponent
     */
    onContentScroll(event: { target: HTMLInputElement }) {
        /*
            Absolute positioned overlay panels (in dropdowns, menus, etc...) inside the
            dialog content need to be appended to the body, this click is to hide them on
            scroll because they maintain their position relative to the body [appendTo="body"].
        */
        event.target.click();

        this.isContentScrolled = event.target.scrollTop > 0;
    }

    private bindKeydown(): void {
        this.subscription.push(
            fromEvent(document, 'keydown').subscribe(this.handleKeyboardEvents.bind(this))
        );

        this.subscription.push(
            fromEvent(this.el.nativeElement, 'click')
                .pipe(
                    filter((event: MouseEvent) => {
                        const el = <HTMLElement>event.target;

                        return el.localName !== 'dot-dialog' && el.classList.contains('active');
                    })
                )
                .subscribe(this.close.bind(this))
        );
    }

    private canTriggerAction(item: DialogButton): boolean {
        return item && !item.disabled && !!item.action;
    }

    private handleClose(): void {
        this.visibleChange.emit(false);
        this.hide.emit();
        this.unBindEvents();
    }

    private handleKeyboardEvents(event: KeyboardEvent): void {
        if (event.code === 'Escape') {
            this.cancelAction();
        } else if (event.code === 'Enter' && (event.metaKey || event.altKey)) {
            this.acceptAction();
        }
    }

    private unBindEvents(): void {
        this.subscription.forEach((sub: Subscription) => {
            sub.unsubscribe();
        });
    }

    private appendContainer() {
        if (this.appendToBody) {
            document.body.appendChild(this.el.nativeElement);
        }
    }

    private isVisible(changes: SimpleChanges): boolean {
        return changes.visible && changes.visible.currentValue;
    }
}
