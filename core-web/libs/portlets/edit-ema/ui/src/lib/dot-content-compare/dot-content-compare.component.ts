import { Observable } from 'rxjs';

import { Component, EventEmitter, Input, Output, inject, input } from '@angular/core';

import { DotAlertConfirmService, DotMessageService, DotIframeService } from '@dotcms/data-access';
import { DotContentCompareEvent } from '@dotcms/dotcms-models';

import { DotContentCompareState, DotContentCompareStore } from './store/dot-content-compare.store';

@Component({
    selector: 'dot-content-compare',
    templateUrl: './dot-content-compare.component.html',
    styleUrls: ['./dot-content-compare.component.scss'],
    providers: [DotContentCompareStore],
    standalone: false
})
export class DotContentCompareComponent {
    store = inject(DotContentCompareStore);
    private dotAlertConfirmService = inject(DotAlertConfirmService);
    private dotMessageService = inject(DotMessageService);
    private dotIframeService = inject(DotIframeService);

    @Input() set data(data: DotContentCompareEvent) {
        if (data != null) {
            this.store.loadData(data);
        }
    }
    $showActions = input<boolean>(true, { alias: 'showActions' });
    @Output() shutdown = new EventEmitter<boolean>();
    @Output() letMeBringBack = new EventEmitter<{ name: string; args: string[] }>();
    vm$: Observable<DotContentCompareState> = this.store.vm$;

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
