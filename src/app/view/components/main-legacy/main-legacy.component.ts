import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { DotRouterService } from '../../../api/services/dot-router/dot-router.service';
import { DotIframeService } from '../_common/iframe/service/dot-iframe/dot-iframe.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['./main-legacy.component.scss'],
    templateUrl: './main-legacy.component.html'
})
export class MainComponentLegacyComponent implements OnInit {
    constructor(private dotRouterService: DotRouterService, private dotIframeService: DotIframeService) {}

    ngOnInit(): void {
        document.body.style.backgroundColor = '';
        document.body.style.backgroundImage = '';
    }

    /**
     * Reload content search iframe when contentlet editor close
     *
     * @memberof MainComponentLegacyComponent
     */
    onCloseContentletEditor(): void {
        this.dotIframeService.reloadData(this.dotRouterService.currentPortlet.id);
    }
}
