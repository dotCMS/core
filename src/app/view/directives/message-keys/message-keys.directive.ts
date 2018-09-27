import { Directive, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';

@Directive({
    selector: '[dotMessagekey]'
})
export class MessageKeyDirective implements OnInit, OnDestroy {
    private key: string;
    private messageMapSubscription;

    constructor(private el: ElementRef, private dotMessageService: DotMessageService) {}

    ngOnInit(): void {
        this.key = this.el.nativeElement.innerText;

        this.messageMapSubscription = this.dotMessageService
            .getMessages([this.key])
            .subscribe((res) => {
                this.el.nativeElement.innerText = res[this.key];
            });
    }

    ngOnDestroy(): void {
        this.messageMapSubscription.unsubscribe();
    }
}
