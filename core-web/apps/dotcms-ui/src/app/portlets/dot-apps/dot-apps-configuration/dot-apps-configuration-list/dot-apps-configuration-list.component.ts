import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { DotAppsSites } from '@dotcms/dotcms-models';

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
    @Output() export = new EventEmitter<DotAppsSites>();
    @Output() delete = new EventEmitter<DotAppsSites>();

    /**
     * Emits action to load next configuration page
     *
     * @memberof DotAppsConfigurationListComponent
     */
    loadNext() {
        this.loadData.emit({ first: this.siteConfigurations.length, rows: this.itemsPerPage });
    }
}
