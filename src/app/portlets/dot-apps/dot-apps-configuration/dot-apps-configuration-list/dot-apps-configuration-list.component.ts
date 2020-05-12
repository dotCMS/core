import {
    Component,
    OnInit,
    ViewChild,
    ElementRef,
    Input,
    Output,
    EventEmitter
} from '@angular/core';
import { DotAppsSites } from '@shared/models/dot-apps/dot-apps.model';

import { LazyLoadEvent } from 'primeng/primeng';
import { DotMessageService } from '@services/dot-messages-service';
import { take } from 'rxjs/operators';

@Component({
    selector: 'dot-apps-configuration-list',
    templateUrl: './dot-apps-configuration-list.component.html',
    styleUrls: ['./dot-apps-configuration-list.component.scss']
})
export class DotAppsConfigurationListComponent implements OnInit {
    @ViewChild('searchInput')
    searchInput: ElementRef;

    @Input() hideLoadDataButton: boolean;
    @Input() itemsPerPage: number;
    @Input() siteConfigurations: DotAppsSites[];

    @Output() loadData = new EventEmitter<LazyLoadEvent>();
    @Output() edit = new EventEmitter<DotAppsSites>();
    @Output() delete = new EventEmitter<DotAppsSites>();

    messagesKey: { [key: string]: string } = {};

    constructor(public dotMessageService: DotMessageService) {}

    ngOnInit() {
        this.dotMessageService
            .getMessages(['apps.configurations.show.more'])
            .pipe(take(1))
            .subscribe((messages: { [key: string]: string }) => {
                this.messagesKey = messages;
            });
    }

    /**
     * Emits action to load next configuration page
     *
     * @memberof DotAppsConfigurationListComponent
     */
    loadNext() {
        this.loadData.emit({ first: this.siteConfigurations.length, rows: this.itemsPerPage });
    }
}
