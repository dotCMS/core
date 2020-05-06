import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { DotIframeService } from '@components/_common/iframe/service/dot-iframe/dot-iframe.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotCustomEventHandlerService } from '@services/dot-custom-event-handler/dot-custom-event-handler.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    providers: [],
    selector: 'dot-main-component',
    styleUrls: ['./main-legacy.component.scss'],
    templateUrl: './main-legacy.component.html'
})
export class MainComponentLegacyComponent implements OnInit {
    constructor(
        private dotRouterService: DotRouterService,
        private dotIframeService: DotIframeService,
        private dotCustomEventHandlerService: DotCustomEventHandlerService
    ) {}

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

    /**
     * Handle the custom events emmited by the Create Contentlet
     *
     * @param CustomEvent $event
     * @memberof MainComponentLegacyComponent
     */
    onCustomEvent($event: CustomEvent): void {
        this.dotCustomEventHandlerService.handle($event);
    }

}
