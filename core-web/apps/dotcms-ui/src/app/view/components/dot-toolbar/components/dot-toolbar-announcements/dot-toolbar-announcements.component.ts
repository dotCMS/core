import { DatePipe, LowerCasePipe } from '@angular/common';
import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { SiteService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';

import { AnnouncementLink, AnnouncementsStore, TypesIcons } from './store/dot-announcements.store';

import { DotToolbarBtnOverlayComponent } from '../dot-toolbar-overlay/dot-toolbar-btn-overlay.component';

@Component({
    selector: 'dot-toolbar-announcements',
    templateUrl: './dot-toolbar-announcements.component.html',
    imports: [DotMessagePipe, LowerCasePipe, DatePipe, DotToolbarBtnOverlayComponent],
    providers: [AnnouncementsStore]
})
export class DotToolbarAnnouncementsComponent implements OnInit {
    /** Store for managing announcements state and data */
    announcementsStore = inject(AnnouncementsStore);

    /** Service for site-related operations */
    siteService = inject(SiteService);

    /** Signal indicating whether to show unread announcement indicator */
    $showUnreadAnnouncement = this.announcementsStore.showUnreadAnnouncement;

    /** Signal containing all announcements data */
    $announcements = this.announcementsStore.announcementsSignal;

    /** Signal containing contact links */
    $contactLinks = this.announcementsStore.selectContactLinks;

    /** Signal containing knowledge center links */
    $knowledgeCenterLinks = this.announcementsStore.selectKnowledgeCenterLinks;

    /** Signal containing DotCMS related links */
    $linkToDotCms = this.announcementsStore.selectLinkToDotCms;

    /** ViewChild reference to the overlay panel component */
    $overlayPanel = viewChild.required<DotToolbarBtnOverlayComponent>('overlayPanel');

    /** Signal containing organized about links with titles and items */
    $aboutLinks = signal<{ title: string; items: AnnouncementLink[] }[]>([]);

    /**
     * Component constructor.
     * Sets up site switching subscription to reload announcements when site changes.
     */
    constructor() {
        this.siteService.currentSite$.pipe(takeUntilDestroyed()).subscribe(() => {
            this.setAnnouncementsStore();
        });
    }

    /**
     * Angular lifecycle hook called after component initialization.
     * Loads announcements and sets up about links.
     */
    ngOnInit(): void {
        this.announcementsStore.load();
        this.$aboutLinks.set(this.getAboutLinks());
    }

    /**
     * Organizes and returns about links grouped by category.
     * Creates structured data for knowledge center and contact us sections.
     *
     * @returns Array of objects containing title and items for each about section
     * @example
     * ```typescript
     * const aboutLinks = this.getAboutLinks();
     * // Returns:
     * // [
     * //   { title: 'announcements.knowledge.center', items: [...] },
     * //   { title: 'announcements.knowledge.contact.us', items: [...] }
     * // ]
     * ```
     */
    getAboutLinks() {
        return [
            { title: 'announcements.knowledge.center', items: this.$knowledgeCenterLinks() },
            { title: 'announcements.knowledge.contact.us', items: this.$contactLinks() }
        ];
    }

    /**
     * Hides the overlay panel component.
     * Used to close the announcements dropdown when user clicks outside or completes an action.
     */
    hideOverlayPanel(): void {
        this.$overlayPanel().hide();
    }

    /**
     * Marks all announcements as read.
     * Updates the store state to remove unread indicators and persist read status.
     */
    markAnnouncementsAsRead(): void {
        this.announcementsStore.markAnnouncementsAsRead();
    }

    /**
     * Sets the announcements store.
     */
    private setAnnouncementsStore(): void {
        this.announcementsStore.load();
        this.$aboutLinks.set(this.getAboutLinks());
    }

    /**
     * Mapping of announcement types to their corresponding icons.
     * Used in templates to display appropriate icons for different announcement types.
     *
     * @readonly
     */
    typesIcons = {
        tip: TypesIcons.Tip,
        release: TypesIcons.Release,
        announcement: TypesIcons.Announcement,
        article: TypesIcons.Article,
        important: TypesIcons.Important
    };
}
