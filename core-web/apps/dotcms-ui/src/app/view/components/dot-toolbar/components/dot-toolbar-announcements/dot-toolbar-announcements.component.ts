import { NgClass, NgForOf, CommonModule } from '@angular/common';
import { Component, OnInit, Signal, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
    Announcement,
    TypesIcons,
    AnnouncementsService
} from '@dotcms/app/api/services/dot-announcements.ts/dot-announcements.service';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-toolbar-announcements',
    templateUrl: './dot-toolbar-announcements.component.html',
    styleUrls: ['./dot-toolbar-announcements.component.scss'],
    standalone: true,
    imports: [NgForOf, NgClass, DotMessagePipe, RouterLink, CommonModule],
    providers: [AnnouncementsService]
})
export class DotToolbarAnnouncementsComponent implements OnInit {
    announcementsService = inject(AnnouncementsService);

    announcements: Signal<Announcement[]> = this.announcementsService.announcements;

    unreadAnnouncements: Signal<boolean> = signal(false);

    ngOnInit(): void {
        this.unreadAnnouncements = this.announcementsService.unreadAnnouncements;
    }

    knowledgeCenterLinks = [
        {
            label: 'Documentation',
            url: 'https://www.dotcms.com/docs/latest/table-of-contents?utm_source=dotcms&utm_medium=application&utm_campaign=announcement_menu'
        },
        {
            label: 'Blog',
            url: 'https://www.dotcms.com/blog/?utm_source=dotcms&utm_medium=application&utm_campaign=announcement_menu'
        },
        { label: 'User Forums', url: 'https://groups.google.com/g/dotcms' }
    ];

    contactLinks = [
        {
            label: 'Customer Support',
            url: 'https://www.dotcms.com/services/support/?utm_source=dotcms&utm_medium=application&utm_campaign=announcement_menu'
        },
        {
            label: 'Professional Services',
            url: 'https://www.dotcms.com/services/professional-services/?utm_source=dotcms&utm_medium=application&utm_campaign=announcement_menu'
        }
    ];

    typesIcons = {
        comment: TypesIcons.Comment,
        release: TypesIcons.Release,
        announcement: TypesIcons.Announcement
    };

    protected linkToDotCms =
        'https://dotcms.com/?utm_source=dotcms&utm_medium=application&utm_campaign=announcement_menu';
}
