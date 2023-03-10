import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, Output } from '@angular/core';

import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import {
    DotContentCompareState,
    DotContentCompareStore
} from '@components/dot-content-compare/store/dot-content-compare.store';
import { DotAlertConfirmService, DotMessageService } from '@dotcms/data-access';

export interface DotContentCompareEvent {
    inode: string;
    identifier: string;
    language: string;
}

@Component({
    selector: 'dot-content-compare',
    templateUrl: './dot-content-compare.component.html',
    styleUrls: ['./dot-content-compare.component.scss'],
    providers: [DotContentCompareStore]
})
export class DotContentCompareComponent {
    @Input() set data(data: DotContentCompareEvent) {
        if (data != null) {
            this.store.loadData(data);
        }
    }
    @Output() shutdown = new EventEmitter<boolean>();
    vm$: Observable<DotContentCompareState> = this.store.vm$;

    constructor(
        private store: DotContentCompareStore,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotMessageService: DotMessageService,
        private dotIframeService: DotIframeService
    ) {}

    /**
     * Confirm if the user want to bring back to specific version.
     ** @param string inode
     * @memberof DotContentCompareComponent
     */
    bringBack(inode: string) {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotIframeService.run({ name: 'getVersionBack', args: [inode] });
                this.shutdown.emit(true);
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('Confirm'),
            message: this.dotMessageService.get('folder.replace.contentlet.working.version')
        });
    }
}
