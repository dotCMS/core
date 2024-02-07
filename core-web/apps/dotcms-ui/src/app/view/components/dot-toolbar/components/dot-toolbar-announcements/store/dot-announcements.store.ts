import { ComponentStore } from '@ngrx/component-store';
import { EMPTY } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';

import { catchError, pluck, tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import { LocalStoreService, SiteService } from '@dotcms/dotcms-js';

export type Announcement = {
    title: string;
    type: string;
    announcementDateAsISO8601: string;
    identifier: string;
    inode: string;
    url: string;
};

export type AnnouncementLink = {
    label: string;
    url: string;
    id: string;
};

export interface DotAnnouncementsState {
    announcements: Announcement[];
    showUnreadAnnouncement: boolean;
    utmParameters?: string;
}

export enum TypesIcons {
    Release = 'pi pi-box',
    Announcement = 'pi pi-megaphone',
    Article = 'pi pi-book',
    Important = 'pi pi-bolt',
    Tip = 'pi pi-comment'
}

@Injectable()
export class AnnouncementsStore extends ComponentStore<DotAnnouncementsState> {
    private http = inject(HttpClient);
    private localStoreService = inject(LocalStoreService);
    private siteService = inject(SiteService);
    private dotMessageService = inject(DotMessageService);

    private announcementsUrl = '/api/v1/announcements';

    constructor() {
        super({
            announcements: [],
            showUnreadAnnouncement: false,
            utmParameters: ''
        });
    }

    readonly load = () =>
        this.effect(() => {
            return this.http.get<Announcement[]>(this.announcementsUrl).pipe(
                pluck('entity'),
                tap((announcements: Announcement[]) => {
                    const modifiedAnnouncements = this.appendUtmParameters(announcements);

                    this.setState({
                        announcements: modifiedAnnouncements,
                        showUnreadAnnouncement: this.hasUnreadAnnouncements(announcements),
                        utmParameters: this.generateUtmQueryString()
                    });
                }),
                catchError(() => EMPTY)
            );
        });

    readonly announcementsSignal: Signal<Announcement[]> = this.selectSignal(
        (state) => state.announcements
    );

    readonly showUnreadAnnouncement: Signal<boolean> = this.selectSignal(
        (state) => state.showUnreadAnnouncement
    );

    readonly selectKnowledgeCenterLinks: Signal<AnnouncementLink[]> = this.selectSignal((state) => {
        return [
            {
                id: '1',
                url: `https://www.dotcms.com/announcement-menu-documentation?${state.utmParameters}`,
                label: this.dotMessageService.get('announcements.knowledge.center.documentation')
            },
            {
                url: `https://www.dotcms.com/announcement-menu-user-forum?${state.utmParameters}`,
                id: '2',
                label: this.dotMessageService.get('announcements.knowledge.center.forum')
            },
            {
                url: `https://www.dotcms.com/announcement-menu-online-training?${state.utmParameters}`,
                id: '3',
                label: this.dotMessageService.get('announcements.knowledge.center.training')
            },
            {
                id: '4',
                label: this.dotMessageService.get('announcements.knowledge.center.blog'),
                url: `https://www.dotcms.com/announcement-menu-dotcms-blog?${state.utmParameters}`
            },
            {
                url: `https://www.dotcms.com/announcement-menu-github-repository?${state.utmParameters}`,
                id: '5',
                label: this.dotMessageService.get('announcements.knowledge.center.github')
            }
        ];
    });

    readonly selectContactLinks: Signal<AnnouncementLink[]> = this.selectSignal((state) => {
        return [
            {
                label: this.dotMessageService.get('announcements.contact.customer.support'),
                url: `https://www.dotcms.com/announcement-menu-customer-support?${state.utmParameters}`,
                id: '1'
            },
            {
                id: '2',
                label: this.dotMessageService.get('announcements.contact.professional.services'),
                url: `https://www.dotcms.com/announcement-menu-professional-services?${state.utmParameters}`
            },
            {
                label: this.dotMessageService.get('announcements.contact.request.feature'),
                url: `https://www.dotcms.com/announcement-menu-request-a-feature?${state.utmParameters}`,
                id: '3'
            },
            {
                id: '4',
                label: this.dotMessageService.get('announcements.contact.report.bug'),
                url: `https://www.dotcms.com/announcement-menu-report-a-bug?${state.utmParameters}`
            }
        ];
    });

    readonly selectLinkToDotCms: Signal<string> = this.selectSignal((state) => {
        return `https://www.dotcms.com/announcement-menu-show-all?${state.utmParameters}`;
    });

    readonly markAnnouncementsAsRead = this.updater((state) => {
        this.localStoreService.storeValue(
            'dotAnnouncementsData',
            JSON.stringify(state.announcements)
        );

        return {
            ...state,
            showUnreadAnnouncement: this.hasUnreadAnnouncements(state.announcements)
        };
    });

    readonly refreshUtmParameters = this.updater((state) => {
        return {
            ...state,
            utmParameters: this.generateUtmQueryString()
        };
    });

    private generateUtmQueryString(): string {
        return `utm_source=platform&utm_medium=announcement&utm_campaign=${this.siteService.currentSite.hostname}`;
    }

    private appendUtmParameters(announcements: Announcement[]): Announcement[] {
        const modifiedAnnouncements = announcements.map((announcement) => {
            return {
                ...announcement,
                url: `${announcement.url}?${this.generateUtmQueryString()}`
            };
        });

        return modifiedAnnouncements;
    }

    private hasUnreadAnnouncements(announcements: Announcement[]): boolean {
        const storedAnnouncementsJson = this.localStoreService.getValue('dotAnnouncementsData');
        const storedAnnouncements: Announcement[] = storedAnnouncementsJson
            ? JSON.parse(storedAnnouncementsJson)
            : [];

        const newAnnouncements = announcements || [];
        const newAnnouncementIds = newAnnouncements.map((announcement) => announcement.inode);
        const storedAnnouncementIds = storedAnnouncements.map((announcement) => announcement.inode);

        const isNewAnnouncement = newAnnouncementIds.some(
            (id) => !storedAnnouncementIds.includes(id)
        );

        return isNewAnnouncement;
    }
}
