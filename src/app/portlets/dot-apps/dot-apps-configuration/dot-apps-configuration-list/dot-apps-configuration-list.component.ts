import { Component, ViewChild, ElementRef, Input, Output, EventEmitter } from '@angular/core';
import { DotAppsSites } from '@shared/models/dot-apps/dot-apps.model';

import { LazyLoadEvent } from 'primeng/primeng';

@Component({
    selector: 'dot-apps-configuration-list',
    templateUrl: './dot-apps-configuration-list.component.html',
    styleUrls: ['./dot-apps-configuration-list.component.scss']
})
export class DotAppsConfigurationListComponent {
    @ViewChild('searchInput') searchInput: ElementRef;

    @Input() hideLoadDataButton: boolean;
    @Input() itemsPerPage: number;
    @Input() siteConfigurations: DotAppsSites[];

    @Output() loadData = new EventEmitter<LazyLoadEvent>();
    @Output() edit = new EventEmitter<DotAppsSites>();
    @Output() delete = new EventEmitter<DotAppsSites>();

    constructor() {}

    /**
     * Emits action to load next configuration page
     *
     * @memberof DotAppsConfigurationListComponent
     */
    loadNext() {
        this.loadData.emit({ first: this.siteConfigurations.length, rows: this.itemsPerPage });
    }
}
