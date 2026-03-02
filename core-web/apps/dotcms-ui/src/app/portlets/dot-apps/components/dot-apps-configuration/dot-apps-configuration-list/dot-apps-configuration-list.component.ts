import { NgClass } from '@angular/common';
import { Component, ElementRef, input, output, viewChild } from '@angular/core';

import { LazyLoadEvent } from 'primeng/api';
import { ButtonModule } from 'primeng/button';

import { DotAppsSite } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotAppsConfigurationItemComponent } from './dot-apps-configuration-item/dot-apps-configuration-item.component';

@Component({
    selector: 'dot-apps-configuration-list',
    templateUrl: './dot-apps-configuration-list.component.html',
    styleUrls: ['./dot-apps-configuration-list.component.scss'],
    imports: [NgClass, ButtonModule, DotAppsConfigurationItemComponent, DotMessagePipe]
})
export class DotAppsConfigurationListComponent {
    searchInput = viewChild<ElementRef>('searchInput');

    hideLoadDataButton = input<boolean>();
    itemsPerPage = input<number>();
    siteConfigurations = input<DotAppsSite[]>();

    loadData = output<LazyLoadEvent>();
    edit = output<DotAppsSite>();
    export = output<DotAppsSite>();
    delete = output<DotAppsSite>();

    /**
     * Emits action to load next configuration page
     *
     * @memberof DotAppsConfigurationListComponent
     */
    loadNext() {
        this.loadData.emit({ first: this.siteConfigurations().length, rows: this.itemsPerPage() });
    }
}
