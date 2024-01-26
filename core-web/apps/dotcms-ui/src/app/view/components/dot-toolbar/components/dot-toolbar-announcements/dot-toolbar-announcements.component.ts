import { NgClass, NgForOf, CommonModule } from '@angular/common';
import { Component, Input, OnInit, Signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { skip } from 'rxjs/operators';

import {
    Announcement,
    TypesIcons,
    AnnouncementsStore
} from '@components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store';
import { DotMessageService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-toolbar-announcements',
    templateUrl: './dot-toolbar-announcements.component.html',
    styleUrls: ['./dot-toolbar-announcements.component.scss'],
    standalone: true,
    imports: [NgForOf, NgClass, DotMessagePipe, RouterLink, CommonModule],
    providers: [AnnouncementsStore]
})
export class DotToolbarAnnouncementsComponent implements OnInit {
    announcementsStore = inject(AnnouncementsStore);
    dotMessageService = inject(DotMessageService);
    siteService = inject(SiteService);

    @Input() showUnreadAnnouncement: boolean;
    announcements: Signal<Announcement[]> = this.announcementsStore.announcementsSignal;
    utm_parameters = `utm_source=platform&utm_medium=${this.siteService.currentSite.hostname}&utm_campaign=announcement`;

    ngOnInit(): void {
        this.siteService.switchSite$.pipe(skip(1)).subscribe(() => {
            this.announcementsStore.load();
            this.utm_parameters = `utm_source=platform&utm_medium=${this.siteService.currentSite.hostname}&utm_campaign=announcement`;
        });
    }

    knowledgeCenterLinks = [
        {
            label: this.dotMessageService.get('announcements.knowledge.center.documentation'),
            url: `https://www.dotcms.com/docs/latest/table-of-contents?${this.utm_parameters}`
        },
        {
            label: this.dotMessageService.get('announcements.knowledge.center.blog'),
            url: `https://www.dotcms.com/blog/?${this.utm_parameters}`
        },
        {
            label: this.dotMessageService.get('announcements.knowledge.center.knowledge.forum'),
            url: 'https://groups.google.com/g/dotcms'
        }
    ];

    contactLinks = [
        {
            label: this.dotMessageService.get('announcements.contact.customer.support'),
            url: `https://www.dotcms.com/services/support/?${this.utm_parameters}`
        },
        {
            label: this.dotMessageService.get('announcements.contact.professional.services'),
            url: `https://www.dotcms.com/services/professional-services/?${this.utm_parameters}`
        }
    ];

    typesIcons = {
        comment: TypesIcons.Comment,
        release: TypesIcons.Release,
        announcement: TypesIcons.Announcement
    };

    protected linkToDotCms = `https://dotcms.com/?${this.utm_parameters}`;
}
