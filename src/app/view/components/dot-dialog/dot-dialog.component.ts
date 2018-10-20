import {
    Component,
    Input,
    EventEmitter,
    Output,
    HostBinding,
    ViewChild,
    ElementRef
} from '@angular/core';
import { trigger, transition, style, animate, state, AnimationEvent } from '@angular/animations';
import { Observable, fromEvent, Subscription } from 'rxjs';
import { filter, map, tap } from 'rxjs/operators';

@Component({
    selector: 'dot-dialog',
    templateUrl: './dot-dialog.component.html',
    styleUrls: ['./dot-dialog.component.scss'],
    animations: [
        trigger('animation', [
            state(
                'show',
                style({
                    opacity: 1
                })
            ),
            state(
                'void',
                style({
                    opacity: 0
                })
            ),
            transition('* => *', animate('300ms ease-in'))
        ])
    ]
})
export class DotDialogComponent {
    @ViewChild('dialog')
    dialog: ElementRef;

    @ViewChild('content')
    content: ElementRef;

    @Input()
    @HostBinding('class.active')
    visible: boolean;

    @Input()
    header = '';

    @Input()
    actions: DotDialogActions;

    @Input()
    closeable = true;

    @Input()
    contentStyle: {
        [key: string]: string;
    };

    @Input()
    headerStyle: {
        [key: string]: string;
    };

    @Output()
    hide: EventEmitter<any> = new EventEmitter();

    @Output()
    beforeClose: EventEmitter<{
        close: () => void;
    }> = new EventEmitter();

    @Output()
    visibleChange: EventEmitter<any> = new EventEmitter();

    isContentScrolled$: Observable<boolean>;

    private subscription: Subscription[] = [];

    constructor(private el: ElementRef) {}

    /**
     * Accept button handler
     *
     * @memberof DotDialogComponent
     */
    acceptAction(): void {
        if (this.actions && this.canTriggerAction(this.actions.accept)) {
            this.actions.accept.action();
        }
    }

    /**
     * Cancel button handler
     *
     * @memberof DotDialogComponent
     */
    cancelAction(): void {
        this.close();

        if (this.actions && this.canTriggerAction(this.actions.cancel)) {
            this.actions.cancel.action();
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
                    this.visibleChange.emit(false);
                }
            });
        } else {
            this.visibleChange.emit(false);
        }

        if ($event) {
            $event.preventDefault();
        }
    }

    /**
     * Handle animation start
     *
     * @param {AnimationEvent} $event
     * @memberof DotDialogComponent
     */
    onAnimationStart($event: AnimationEvent): void {
        switch ($event.toState) {
            case 'visible':
                this.bindEvents();
                break;
            case 'void':
                this.hide.emit();
                this.unBindEvents();
                break;
        }
    }

    private bindEvents(): void {
        this.isContentScrolled$ = this.isContentScrolled();

        this.subscription.push(
            fromEvent(document, 'keydown').subscribe(this.handleKeyboardEvents.bind(this))
        );

        this.subscription.push(
            fromEvent(this.el.nativeElement, 'click')
                .pipe(
                    filter((event: MouseEvent) => {
                        const el = <HTMLElement>event.target;
                        return el.localName === 'dot-dialog' && el.classList.contains('active');
                    })
                )
                .subscribe(this.close.bind(this))
        );
    }

    private canTriggerAction(item: DialogButton): boolean {
        return item && !item.disabled && !!item.action;
    }

    private handleKeyboardEvents(event: KeyboardEvent): void {
        switch (event.code) {
            case 'Escape':
                this.cancelAction();
                break;
            case 'Enter':
                this.acceptAction();
                break;
            default:
                break;
        }
    }

    private isContentScrolled(): Observable<boolean> {
        return this.content ? fromEvent(this.content.nativeElement, 'scroll').pipe(
            tap((e: { target: HTMLInputElement }) => {
                /*
                    Absolute positioned overlays panels (in dropdowns, menus, etc...) inside the
                    dialog content needs to be append to the body, this click is to hide them on
                    scroll because they mantain their position relative to the body.
                */
                e.target.click();
            }),
            map((e: { target: HTMLInputElement }) => {
                return e.target.scrollTop > 0;
            })
        ) : null;
    }

    private unBindEvents(): void {
        this.isContentScrolled$ = null;

        this.subscription.forEach((sub: Subscription) => {
            sub.unsubscribe();
        });
    }
}

interface DialogButton {
    action?: (dialog?: any) => void;
    disabled?: boolean;
    label: string;
}

export interface DotDialogActions {
    accept?: DialogButton;
    cancel?: DialogButton;
}
