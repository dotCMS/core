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
        this.historyUrl = `/html/containers/push_history.jsp`;
    }

    ngOnChanges(): void {
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }
}
