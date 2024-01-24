import { ComponentStore } from '@ngrx/component-store';
import { EMPTY } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';

import { catchError, pluck, tap } from 'rxjs/operators';

import { LocalStoreService } from '@dotcms/dotcms-js';

export type Announcement = {
    title: string;
    type: string;
    announcementDateAsISO8601: string;
    identifier: string;
    inode: string;
    url: string;
};

export interface DotAnnouncementsState {
    announcements: Announcement[];
    showUnreadAnnouncement: boolean;
}

export enum TypesIcons {
    Comment = 'pi pi-comment',
    Release = 'pi pi-book',
    Announcement = 'pi pi-megaphone'
}

@Injectable()
export class AnnouncementsStore extends ComponentStore<DotAnnouncementsState> {
    private http = inject(HttpClient);
    private localStoreService = inject(LocalStoreService);
    private announcementsUrl = '/api/v1/announcements';

    constructor() {
        super({
            announcements: [],
            showUnreadAnnouncement: false
        });
    }

    readonly loadAnnouncements = this.effect(() => {
        return this.http.get<Announcement[]>(this.announcementsUrl).pipe(
            pluck('entity'),
            tap((announcements: Announcement[]) => {
                const modifiedAnnouncements = announcements.map((announcement) => {
                    return {
                        ...announcement,
                        url: `${announcement.url}?utm_source=dotcms&utm_medium=application&utm_campaign=announcement_menu`
                    };
                });

                this.setState({
                    announcements: modifiedAnnouncements,
                    showUnreadAnnouncement: this.hasUnreadAnnouncements(announcements)
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
