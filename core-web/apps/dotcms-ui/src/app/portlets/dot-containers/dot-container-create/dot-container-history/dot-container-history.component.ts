import { Component, Input, OnChanges, OnInit, ViewChild } from '@angular/core';
import { IframeComponent } from '@components/_common/iframe/iframe-component';

@Component({
    selector: 'dot-container-history',
    templateUrl: './dot-container-history.component.html',
    styleUrls: ['./dot-container-history.component.scss']
})
export class DotContainerHistoryComponent implements OnChanges, OnInit {
    @Input() containerId: string;
    @ViewChild('historyIframe') historyIframe: IframeComponent;
    historyUrl = '/html/containers/push_history.jsp';

    ngOnInit() {
        this.historyUrl = `/html/containers/push_history.jsp?containerId=${this.containerId}&popup=true`;
    }

    ngOnChanges(): void {
        if (this.historyIframe) {
            this.historyIframe.iframeElement.nativeElement.contentWindow.location.reload();
        }
    }
}
