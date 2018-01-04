import { Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { ActivatedRoute } from '@angular/router';
import { MessageService } from '../../../../api/services/messages-service';

@Component({
    selector: 'dot-edit-page-nav',
    templateUrl: './dot-edit-page-nav.component.html',
    styleUrls: ['./dot-edit-page-nav.component.scss'],
})
export class DotEditPageNavComponent implements OnInit {
    constructor(public route: ActivatedRoute, public messageService: MessageService) {}

    ngOnInit() {
        this.messageService
            .getMessages([
                'editpage.toolbar.nav.content',
                'editpage.toolbar.nav.layout'
            ])
            .subscribe();
    }
}
