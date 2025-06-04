import { DatePipe, LowerCasePipe } from '@angular/common';
import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import {
    TypesIcons,
    AnnouncementsStore,
    AnnouncementLink
} from '@components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store';
import { SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';

import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';

@Component({
    selector: 'dot-toolbar-announcements',
    templateUrl: './dot-toolbar-announcements.component.html',
    styleUrls: ['./dot-toolbar-announcements.component.scss'],
    standalone: true,
    imports: [DotMessagePipe, LowerCasePipe, DatePipe, DotToolbarBtnOverlayComponent],
    providers: [AnnouncementsStore]
})
export class DotToolbarAnnouncementsComponent implements OnInit {
    announcementsStore = inject(AnnouncementsStore);
    siteService = inject(SiteService);

    $showUnreadAnnouncement = this.announcementsStore.showUnreadAnnouncement;
    $announcements = this.announcementsStore.announcementsSignal;
    $contactLinks = this.announcementsStore.selectContactLinks;
    $knowledgeCenterLinks = this.announcementsStore.selectKnowledgeCenterLinks;
    $linkToDotCms = this.announcementsStore.selectLinkToDotCms;

    $overlayPanel = viewChild.required<DotToolbarBtnOverlayComponent>('overlayPanel');

    $aboutLinks = signal<{ title: string; items: AnnouncementLink[] }[]>([]);

    constructor() {
        this.siteService.switchSite$.pipe(takeUntilDestroyed()).subscribe(() => {
            this.announcementsStore.load();
            this.$aboutLinks.set(this.getAboutLinks());
        });
    }

    ngOnInit(): void {
        this.announcementsStore.load();
        this.$aboutLinks.set(this.getAboutLinks());
    }

    /**
     * Get the about links
     * @returns About links
     */
    getAboutLinks(): { title: string; items: AnnouncementLink[] }[] {
        return [
            { title: 'announcements.knowledge.center', items: this.$knowledgeCenterLinks() },
            { title: 'announcements.knowledge.contact.us', items: this.$contactLinks() }
        ];
    }

    hideOverlayPanel(): void {
        this.$overlayPanel().hide();
    }

    markAnnouncementsAsRead(): void {
        this.announcementsStore.markAnnouncementsAsRead();
    }

    typesIcons = {
        tip: TypesIcons.Tip,
        release: TypesIcons.Release,
        announcement: TypesIcons.Announcement,
        article: TypesIcons.Article,
        important: TypesIcons.Important
    };
}
