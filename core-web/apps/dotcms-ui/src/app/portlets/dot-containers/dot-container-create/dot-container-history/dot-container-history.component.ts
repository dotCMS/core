import { Component, inject, Input, OnChanges, ViewChild } from '@angular/core';

import { DotRouterService } from '@dotcms/data-access';

import { IframeComponent } from '../../../../view/components/_common/iframe/iframe-component/iframe.component';
import { DotPortletBoxComponent } from '../../../../view/components/dot-portlet-base/components/dot-portlet-box/dot-portlet-box.component';

@Component({
    selector: 'dot-container-history',
    templateUrl: './dot-container-history.component.html',
    styleUrls: ['./dot-container-history.component.scss'],
    imports: [DotPortletBoxComponent, IframeComponent]
})
export class DotContainerHistoryComponent implements OnChanges {
    @Input() containerId: string;
    @ViewChild('historyIframe') historyIframe: IframeComponent;

    protected historyUrl = '/html/containers/push_history.jsp';
    private readonly dotRouterService = inject(DotRouterService);

    ngOnChanges(): void {
        this.historyUrl = `/html/containers/push_history.jsp?containerId=${this.containerId}&popup=true`;
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }

    onCustomEvent($event: CustomEvent): void {
        const { data, name } = $event.detail;

        if (name === 'bring-back-version') {
            this.dotRouterService.goToEditContainer(data.id);
        }
    }
}
