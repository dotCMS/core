import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, Output } from '@angular/core';

import { DotAlertConfirmService, DotMessageService, DotIframeService } from '@dotcms/data-access';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';

import { DotContentCompareState, DotContentCompareStore } from './store/dot-content-compare.store';

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
    @Output() letMeBringBack = new EventEmitter<{ name: string; args: string[] }>();
    vm$: Observable<DotContentCompareState> = this.store.vm$;

    constructor(
        public store: DotContentCompareStore,
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
                this.letMeBringBack.emit({ name: 'getVersionBack', args: [inode] });
            },
            reject: () => {
                //
            },
            header: this.dotMessageService.get('Confirm'),
            message: this.dotMessageService.get('folder.replace.contentlet.working.version')
        });
    }
}
