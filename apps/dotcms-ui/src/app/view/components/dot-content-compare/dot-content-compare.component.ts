import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
    DotContentCompareState,
    DotContentCompareStore
} from '@components/dot-content-compare/store/dot-content-compare.store';
import { Observable } from 'rxjs';
import { catchError, filter, map, take } from 'rxjs/operators';
import { DotAlertConfirmService } from '@services/dot-alert-confirm';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import {
    DotVersionable,
    DotVersionableService
} from '@services/dot-verionable/dot-versionable.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { HttpErrorResponse } from '@angular/common/http';

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
    @Output() close = new EventEmitter<boolean>();
    vm$: Observable<DotContentCompareState> = this.store.vm$;

    constructor(
        private store: DotContentCompareStore,
        private dotAlertConfirmService: DotAlertConfirmService,
        private dotVersionableService: DotVersionableService,
        private dotRouterService: DotRouterService,
        private dotMessageService: DotMessageService,
        private dotHttpErrorManagerService: DotHttpErrorManagerService
    ) {}

    /**
     * Confirm if the user want to bring back to specific version.
     ** @param string inode
     * @memberof DotContentCompareComponent
     */
    bringBack(inode: string) {
        this.dotAlertConfirmService.confirm({
            accept: () => {
                this.dotVersionableService
                    .bringBack(inode)
                    .pipe(
                        take(1),
                        catchError((err: HttpErrorResponse) => {
                            return this.dotHttpErrorManagerService
                                .handle(err)
                                .pipe(map(() => null));
                        }),
                        filter((version: DotVersionable) => version != null)
                    )
                    .subscribe((version: DotVersionable) => {
                        this.dotRouterService.goToURL(`/c/content/${version.inode}`);
                        this.close.emit(true);
                    });
            },
            reject: () => {},
            header: this.dotMessageService.get('Confirm'),
            message: this.dotMessageService.get('folder.replace.contentlet.working.version')
        });
    }
}
