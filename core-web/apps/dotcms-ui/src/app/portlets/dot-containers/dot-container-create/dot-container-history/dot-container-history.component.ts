import { Component, OnChanges, OnInit, ViewChild } from '@angular/core';
import { IframeComponent } from '@components/_common/iframe/iframe-component';

@Component({
    selector: 'dot-container-history',
    templateUrl: './dot-container-history.component.html',
    styleUrls: ['./dot-container-history.component.scss']
})
export class DotContainerHistoryComponent implements OnInit, OnChanges {
    @ViewChild('historyIframe') historyIframe: IframeComponent;
    historyUrl = '/html/containers/push_history.jsp';

    ngOnChanges(): void {
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }
}
