import {Directive, ElementRef} from '@angular/core';
import {MessageService} from '../../api/services/messages-service';

@Directive({
    selector: '[messagekey]',
    host: {}
})
export class MessageKeyDirective {
    private key: string;
    private messageMapSubscription;

    constructor(private el: ElementRef, private messageService: MessageService) {}

    ngOnInit() {
        this.key = this.el.nativeElement.innerText;

        this.messageMapSubscription = this.messageService.getMessages([this.key]).subscribe(res => {
            this.el.nativeElement.innerText = res[this.key];
        });
    }

    ngOnDestroy() {
        this.messageMapSubscription.unsubscribe();
    }
}