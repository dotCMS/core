import { Component, OnInit, Input, EventEmitter, Output, OnChanges } from '@angular/core';
import { Observable } from 'rxjs';
import { take } from 'rxjs/operators';

import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotMessageService } from '@services/dot-messages-service';
import { DotPageRenderState, DotPageMode } from '@portlets/dot-edit-page/shared/models';

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges {
    @Input()
    pageState: DotPageRenderState;

    @Output()
    cancel = new EventEmitter<boolean>();

    @Output()
    actionFired = new EventEmitter<boolean>();

    @Output()
    whatschange = new EventEmitter<boolean>();

    isEnterpriseLicense$: Observable<boolean>;
    showWhatsChanged: boolean;
    messagesKey: { [key: string]: string } = {};

    constructor(
        public dotMessageService: DotMessageService,
        private dotLicenseService: DotLicenseService
    ) {}

    ngOnInit() {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.dotMessageService
            .getMessages([
                'dot.common.whats.changed',
                'dot.common.cancel'
            ])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
    }

    ngOnChanges(): void {
        this.showWhatsChanged = this.pageState.state.mode === DotPageMode.PREVIEW;
    }
}
