import {Directive, ElementRef, OnDestroy, OnInit} from '@angular/core';
import { MessageService } from '../../../api/services/messages-service';

@Directive({
    host: {},
    selector: '[messagekey]'
})
export class MessageKeyDirective implements OnInit, OnDestroy{
    private key: string;
    private messageMapSubscription;

    constructor(private el: ElementRef, private messageService: MessageService) {}

    ngOnInit(): void {
        this.key = this.el.nativeElement.innerText;

        this.messageMapSubscription = this.messageService.getMessages([this.key]).subscribe(res => {
            this.el.nativeElement.innerText = res[this.key];
        });
    }

    ngOnDestroy(): void {
        this.messageMapSubscription.unsubscribe();
    }
}
