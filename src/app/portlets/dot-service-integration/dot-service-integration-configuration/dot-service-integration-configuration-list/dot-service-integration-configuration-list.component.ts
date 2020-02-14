import {
    Component,
    OnInit,
    ViewChild,
    ElementRef,
    Input,
    Output,
    EventEmitter
} from '@angular/core';
import { DotServiceIntegrationSites } from '@shared/models/dot-service-integration/dot-service-integration.model';

import { LazyLoadEvent } from 'primeng/primeng';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-service-integration-configuration-list',
    templateUrl: './dot-service-integration-configuration-list.component.html',
    styleUrls: ['./dot-service-integration-configuration-list.component.scss']
})
export class DotServiceIntegrationConfigurationListComponent implements OnInit {
    @ViewChild('searchInput')
    searchInput: ElementRef;

    @Input() disabledLoadDataButton: boolean;
    @Input() itemsPerPage: number;
    @Input() siteConfigurations: DotServiceIntegrationSites[];

    @Output() loadData = new EventEmitter<LazyLoadEvent>();
    @Output() edit = new EventEmitter<DotServiceIntegrationSites>();
    @Output() delete = new EventEmitter<DotServiceIntegrationSites>();

    messagesKey: { [key: string]: string } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['service.integration.configurations.show.more'])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
    }

    /**
     * Emits action to load next configuration page
     *
     * @memberof DotServiceIntegrationConfigurationListComponent
     */
    loadNext() {
        this.loadData.emit({ first: this.siteConfigurations.length, rows: this.itemsPerPage });
    }
}
