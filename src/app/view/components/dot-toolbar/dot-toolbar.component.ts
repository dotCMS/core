import { Component, ViewEncapsulation, Output, EventEmitter, Input } from '@angular/core';
import { SiteService, Site } from 'dotcms-js/dotcms-js';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html'
})
export class ToolbarComponent {
    @Input() collapsed;
    @Output() mainButtonClick: EventEmitter<MouseEvent> = new EventEmitter();

    constructor(public iframeOverlayService: IframeOverlayService, private siteService: SiteService) {}

    siteChange(site: Site): void {
        this.siteService.switchSite(site);
    }

    handleMainButtonClick($event): void {
        this.mainButtonClick.emit($event);
    }
}
