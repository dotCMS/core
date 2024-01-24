import { ComponentStore } from '@ngrx/component-store';
import { EMPTY } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';

import { catchError, pluck, tap } from 'rxjs/operators';

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
    readAnnouncements: boolean;
}

export enum TypesIcons {
    Comment = 'pi pi-comment',
    Release = 'pi pi-book',
    Announcement = 'pi pi-megaphone'
}

@Injectable()
export class AnnouncementsStore extends ComponentStore<DotAnnouncementsState> {
    private http = inject(HttpClient);
    private announcementsUrl = '/api/v1/announcements';

    constructor() {
        super({
            announcements: [],
            readAnnouncements: false
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
                    readAnnouncements: this.hasUnreadAnnouncements(announcements)
                });

                return announcements;
            }),
            catchError(() => EMPTY)
        );
    });

    readonly announcementsSignal: Signal<Announcement[]> = this.selectSignal(
        (state) => state.announcements
    );

    readonly readAnnouncements: Signal<boolean> = this.selectSignal(
        (state) => state.readAnnouncements
    );

    unreadAnnouncements = this.updater((state) => {
        return {
            ...state,
            readAnnouncements: this.hasUnreadAnnouncements(state.announcements)
        };
    });

    private hasUnreadAnnouncements(announcements: Announcement[]): boolean {
        const storedAnnouncementsJson = localStorage.getItem('announcementsData');
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

    saveAnnouncements(announcements: Announcement[]): void {
        localStorage.setItem('announcementsData', JSON.stringify(announcements));
    }
}
