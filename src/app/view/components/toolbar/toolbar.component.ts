import {
    Component,
    ViewEncapsulation,
    Output,
    EventEmitter
} from '@angular/core';
import { SiteService, Site } from 'dotcms-js/dotcms-js';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'toolbar-component',
    styleUrls: ['./toolbar.component.scss'],
    templateUrl: './toolbar.component.html'
})
export class ToolbarComponent {
    @Output() mainButtonClick: EventEmitter<MouseEvent> = new EventEmitter();

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
