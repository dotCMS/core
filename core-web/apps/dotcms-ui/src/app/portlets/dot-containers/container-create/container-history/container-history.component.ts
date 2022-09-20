import { Component, OnChanges, OnInit, ViewChild } from '@angular/core';
import { IframeComponent } from '@components/_common/iframe/iframe-component';

@Component({
    selector: 'dot-container-history',
    templateUrl: './container-history.component.html',
    styleUrls: ['./container-history.component.scss']
})
export class ContainerHistoryComponent implements OnInit, OnChanges {
    @ViewChild('historyIframe') historyIframe: IframeComponent;

    historyUrl = '';

    ngOnInit() {
        this.historyUrl = `/html/templates/permissions.jsp?templateId=7acdb856-4bbc-41c5-8695-a39c2e4a913f&popup=true&in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=templates`;
    }

    ngOnChanges(): void {
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }
}
