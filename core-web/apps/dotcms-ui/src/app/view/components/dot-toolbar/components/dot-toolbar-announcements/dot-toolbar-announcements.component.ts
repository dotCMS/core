import { NgClass, NgForOf, CommonModule } from '@angular/common';
import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnInit,
    Output,
    Signal,
    ViewChild,
    inject,
    signal
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { OverlayPanel, OverlayPanelModule } from 'primeng/overlaypanel';

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
    imports: [NgForOf, NgClass, DotMessagePipe, RouterLink, CommonModule, OverlayPanelModule],
    providers: [AnnouncementsStore]
})
export class DotToolbarAnnouncementsComponent implements OnInit, OnChanges {
    announcementsStore = inject(AnnouncementsStore);
    dotMessageService = inject(DotMessageService);
    siteService = inject(SiteService);
    @ViewChild('toolbarAnnouncements', { static: true }) toolbarAnnouncements: OverlayPanel;
    @Output() hideMenu = new EventEmitter();

    @Input() showUnreadAnnouncement: boolean;
    announcements: Signal<Announcement[]> = this.announcementsStore.announcementsSignal;
    contactLinks: Signal<AnnouncementLink[]> = this.announcementsStore.selectContactLinks;
    knowledgeCenterLinks: Signal<AnnouncementLink[]> =
        this.announcementsStore.selectKnowledgeCenterLinks;
    linkToDotCms: Signal<string> = this.announcementsStore.selectLinkToDotCms;
    showMask = signal<boolean>(false);

    aboutLinks: { title: string; items: AnnouncementLink[] }[] = [];

    ngOnInit(): void {
        this.announcementsStore.load();

        this.siteService.switchSite$.pipe(skip(1)).subscribe(() => {
            this.announcementsStore.refreshUtmParameters();
            this.announcementsStore.load();
            this.aboutLinks = [
                { title: 'announcements.knowledge.center', items: this.knowledgeCenterLinks() },
                { title: 'announcements.knowledge.contact.us', items: this.contactLinks() }
            ];
        });
    }

    ngOnChanges(changes): void {
        if (!changes.showUnreadAnnouncement.currentValue) {
            this.announcementsStore.markAnnouncementsAsRead();
        }
    }

    /**
     * Toggle the dialog
     * @param event
     */
    toggleDialog(event): void {
        this.showMask.update((value) => !value);
        this.toolbarAnnouncements.toggle(event);
    }

    /**
     * On hide dialog mark announcements as read
     * @param event
     */
    hideDialog(): void {
        this.hideMenu.emit();
        this.showMask.set(false);
    }

    typesIcons = {
        tip: TypesIcons.Tip,
        release: TypesIcons.Release,
        announcement: TypesIcons.Announcement,
        article: TypesIcons.Article,
        important: TypesIcons.Important
    };
}
