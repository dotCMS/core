import {
    Component,
    ViewEncapsulation,
    ViewChild,
    Output,
    EventEmitter
} from '@angular/core';
import { IframeOverlayService } from '../../../api/services/iframe-overlay-service';
import { SiteService, Site } from '../../../api/services/site-service';
import { SiteSelectorComponent } from '../_common/site-selector/site-selector.component';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'toolbar-component',
    styles: [require('./toolbar.component.scss')],
    templateUrl: './toolbar.component.html'
})
export class ToolbarComponent {
    @Output() mainButtonClick: EventEmitter<MouseEvent> = new EventEmitter();
    @ViewChild('siteSelector') siteSelectorComponent: SiteSelectorComponent;

    constructor(
        public iframeOverlayService: IframeOverlayService,
        private siteService: SiteService
    ) {}

    siteChange(site: Site): void {
        this.siteService.switchSite(site);
    }

    handleMainButtonClick($event): void {
        this.mainButtonClick.emit($event);
    }
}
