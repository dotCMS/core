import {
    Component,
    OnInit,
    Input,
    EventEmitter,
    Output,
    OnChanges,
    OnDestroy
} from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotLicenseService } from '@services/dot-license/dot-license.service';
import { DotPageRenderState, DotPageMode } from '@portlets/dot-edit-page/shared/models';
import { DotMessageDisplayService } from '@components/dot-message-display/services';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { filter, takeUntil } from 'rxjs/operators';
import { DotEvent } from '@shared/models/dot-event/dot-event';
import { DotMessageSeverity, DotMessageType } from '@components/dot-message-display/model';

@Component({
    selector: 'dot-edit-page-toolbar',
    templateUrl: './dot-edit-page-toolbar.component.html',
    styleUrls: ['./dot-edit-page-toolbar.component.scss']
})
export class DotEditPageToolbarComponent implements OnInit, OnChanges, OnDestroy {
    @Input() pageState: DotPageRenderState;

    @Output() cancel = new EventEmitter<boolean>();

    @Output() actionFired = new EventEmitter<boolean>();

    @Output() whatschange = new EventEmitter<boolean>();

    isEnterpriseLicense$: Observable<boolean>;
    showWhatsChanged: boolean;

    private destroy$: Subject<boolean> = new Subject<boolean>();

    constructor(
        private dotLicenseService: DotLicenseService,
        private dotEventsService: DotEventsService,
        private dotMessageDisplayService: DotMessageDisplayService
    ) {}

    ngOnInit() {
        this.isEnterpriseLicense$ = this.dotLicenseService.isEnterprise();
        this.listenGlobalMessages();
    }

    ngOnChanges(): void {
        this.showWhatsChanged =
            this.pageState.state.mode === DotPageMode.PREVIEW &&
            !('persona' in this.pageState.viewAs);
    }

    ngOnDestroy(): void {
        this.destroy$.next(true);
        this.destroy$.complete();
    }

    /**
     * Hide what's change when state change
     *
     * @memberof DotEditPageToolbarComponent
     */
    stateChange(): void {
        if (this.showWhatsChanged) {
            this.showWhatsChanged = false;
            this.whatschange.emit(this.showWhatsChanged);
        }
    }

    private listenGlobalMessages() {
        this.dotEventsService
            .listen('dot-global-message')
            .pipe(
                filter((event: DotEvent) => !!event.data),
                takeUntil(this.destroy$)
            )
            .subscribe((event: DotEvent) => {
                this.dotMessageDisplayService.push({
                    life: 3000,
                    message: event.data.value,
                    severity: event.data.type || DotMessageSeverity.INFO,
                    type: DotMessageType.SIMPLE_MESSAGE
                });
            });
    }
}
