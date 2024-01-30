import { NgClass, NgForOf, CommonModule } from '@angular/common';
import { Component, Input, OnInit, Signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { skip } from 'rxjs/operators';

import {
    Announcement,
    TypesIcons,
    AnnouncementsStore,
    AnnouncementLink
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
    contactLinks: Signal<AnnouncementLink[]> = this.announcementsStore.selectContactLinks;
    knowledgeCenterLinks: Signal<AnnouncementLink[]> =
        this.announcementsStore.selectKnowledgeCenterLinks;
    linkToDotCms: Signal<string> = this.announcementsStore.selectLinkToDotCms;

    ngOnInit(): void {
        this.announcementsStore.load();

        this.siteService.switchSite$.pipe(skip(1)).subscribe(() => {
            this.announcementsStore.refreshUtmParameters();
            this.announcementsStore.load();
        });
    }

    typesIcons = {
        tip: TypesIcons.Tip,
        release: TypesIcons.Release,
        announcement: TypesIcons.Announcement,
        article: TypesIcons.Article,
        important: TypesIcons.Important
    };
}
