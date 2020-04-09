import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';
import { DotApps } from '@shared/models/dot-apps/dot-apps.model';

@Component({
    selector: 'dot-apps-card',
    templateUrl: './dot-apps-card.component.html',
    styleUrls: ['./dot-apps-card.component.scss']
})
export class DotAppsCardComponent implements OnInit {
    @Input() app: DotApps;
    @Output() actionFired = new EventEmitter<string>();

    messagesKey: { [key: string]: string } = {};
    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages([
                'apps.configurations',
                'apps.no.configurations'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
    }
}
