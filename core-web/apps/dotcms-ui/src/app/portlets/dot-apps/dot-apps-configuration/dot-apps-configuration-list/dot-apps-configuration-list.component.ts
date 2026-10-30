import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';

import { DotAppsSite } from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-apps-configuration-list',
    templateUrl: './dot-apps-configuration-list.component.html',
    styleUrls: ['./dot-apps-configuration-list.component.scss'],
    standalone: false
})
export class DotAppsConfigurationListComponent {
    @ViewChild('searchInput') searchInput: ElementRef;

    @Input() hideLoadDataButton: boolean;
    @Input() itemsPerPage: number;
    @Input() siteConfigurations: DotAppsSite[];

    @Output() loadData = new EventEmitter<LazyLoadEvent>();
    @Output() edit = new EventEmitter<DotAppsSite>();
    @Output() export = new EventEmitter<DotAppsSite>();
    @Output() delete = new EventEmitter<DotAppsSite>();

    /**
     * Emits action to load next configuration page
     *
     * @memberof DotAppsConfigurationListComponent
     */
    loadNext() {
        this.loadData.emit({ first: this.siteConfigurations.length, rows: this.itemsPerPage });
    }
}
