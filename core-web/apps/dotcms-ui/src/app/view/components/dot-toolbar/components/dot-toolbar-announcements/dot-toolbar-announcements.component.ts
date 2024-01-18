import { NgClass, NgForOf, CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AnnouncementsService } from '@dotcms/app/api/services/dot-announcements.ts/dot-announcements.service';
import { DotMessagePipe } from '@dotcms/ui';

export type Announcement = {
    title: string;
    type: string;
    announcementDateAsISO8601: string;
};

@Component({
    selector: 'dot-toolbar-announcements',
    templateUrl: './dot-toolbar-announcements.component.html',
    styleUrls: ['./dot-toolbar-announcements.component.scss'],
    standalone: true,
    imports: [NgForOf, NgClass, DotMessagePipe, RouterLink, CommonModule],
    providers: [AnnouncementsService]
})
export class DotToolbarAnnouncementsComponent {
    @Output() hideOverlayPanel = new EventEmitter<string>();

    annoucementsService = inject(AnnouncementsService);

    announcements = this.annoucementsService.announcements;

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
        comment: 'pi pi-comment',
        release: 'pi pi-book',
        announcement: 'pi pi-megaphone'
    };

    protected linkToAddDevice = '/c/starter';

    close(): void {
        this.hideOverlayPanel.emit();
    }
}
