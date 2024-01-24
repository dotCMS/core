import { NgClass, NgForOf, CommonModule } from '@angular/common';
import { Component, Input, Signal, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import {
    Announcement,
    TypesIcons,
    AnnouncementsStore
} from '@components/dot-toolbar/components/dot-toolbar-announcements/store/dot-announcements.store';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-toolbar-announcements',
    templateUrl: './dot-toolbar-announcements.component.html',
    styleUrls: ['./dot-toolbar-announcements.component.scss'],
    standalone: true,
    imports: [NgForOf, NgClass, DotMessagePipe, RouterLink, CommonModule],
    providers: [AnnouncementsStore]
})
export class DotToolbarAnnouncementsComponent {
    announcementsStore = inject(AnnouncementsStore);

    @Input() showUnreadAnnouncement: boolean;
    announcements: Signal<Announcement[]> = this.announcementsStore.announcementsSignal;

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
