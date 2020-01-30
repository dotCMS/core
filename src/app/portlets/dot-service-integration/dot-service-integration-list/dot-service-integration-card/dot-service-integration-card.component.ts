import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';
import { DotServiceIntegration } from '@shared/models/dot-service-integration/dot-service-integration.model';

@Component({
    selector: 'dot-service-integration-card',
    templateUrl: './dot-service-integration-card.component.html',
    styleUrls: ['./dot-service-integration-card.component.scss']
})
export class DotServiceIntegrationCardComponent implements OnInit {
    @Input() serviceIntegration: DotServiceIntegration;
    @Output() actionFired = new EventEmitter<string>();

    messagesKey: { [key: string]: string } = {};
    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'service.integration.configurations',
                'service.integration.no.configurations'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
    }
}
