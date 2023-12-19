import { NgClass, NgForOf } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-toolbar-announcements',
    templateUrl: './dot-toolbar-announcements.component.html',
    styleUrls: ['./dot-toolbar-announcements.component.scss'],
    standalone: true,
    imports: [NgForOf, NgClass, DotMessagePipe, RouterLink]
})
export class DotToolbarAnnouncementsComponent {
    @Output() hideOverlayPanel = new EventEmitter<string>();

    announcementsData = [
        {
            iconClass: 'pi pi-comment',
            label: 'Get more out of the Block Editor',
            date: '20 Jul 2023'
        },
        {
            iconClass: 'pi pi-book',
            label: '12 Reasons You Should Migrate to dotCMS Cloud.',
            date: '27 Jul 2023'
        },
        {
            iconClass: 'pi pi-megaphone',
            label: 'Release 22.03 Designated as LTS Release',
            date: '17 Jul 2023'
        },
        {
            iconClass: 'pi pi-comment',
            label: 'Which page rendering strategy is right for you?',
            date: '10 Feb 2023'
        },
        { iconClass: 'pi pi-megaphone', label: 'New in Release 22.01', date: '07 Jan 2023' }
    ];

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
    protected linkToAddDevice = '/c/starter';

    close(): void {
        this.hideOverlayPanel.emit();
    }
}
