import { Component, ViewEncapsulation, Output, EventEmitter, Input, OnInit } from '@angular/core';
import { SiteService, Site, DotcmsEventsService } from 'dotcms-js/dotcms-js';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'dot-toolbar',
    styleUrls: ['./dot-toolbar.component.scss'],
    templateUrl: './dot-toolbar.component.html'
})
export class ToolbarComponent implements OnInit {
    @Input() collapsed: boolean;
    @Output() mainButtonClick: EventEmitter<MouseEvent> = new EventEmitter();

    constructor(
        public iframeOverlayService: IframeOverlayService,
        private siteService: SiteService,
        private dotcmsEventsService: DotcmsEventsService
    ) {}

    ngOnInit(): void {
        this.dotcmsEventsService.subscribeTo('ARCHIVE_SITE').subscribe((site) => {
            if (site.data.hostname === this.siteService.currentSite.hostname && site.data.archived) {
                this.siteService.switchToDefaultSite().subscribe((defaultSite: Site) => {
                    this.siteChange(defaultSite);
                });
            }
        });
    }

    siteChange(site: Site): void {
        this.siteService.switchSite(site);
    }

    handleMainButtonClick($event): void {
        $event.stopPropagation();
        this.mainButtonClick.emit($event);
    }
}
