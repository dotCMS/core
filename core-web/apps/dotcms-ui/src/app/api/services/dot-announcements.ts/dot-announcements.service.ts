import { HttpClient } from '@angular/common/http';
import { Injectable, Signal, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { catchError, pluck } from 'rxjs/operators';

export type Announcement = {
    title: string;
    type: string;
    announcementDateAsISO8601: string;
    identifier: string;
    url: string;
};

export enum TypesIcons {
    Comment = 'pi pi-comment',
    Release = 'pi pi-book',
    Announcement = 'pi pi-megaphone'
}

@Injectable()
export class AnnouncementsService {
    private http = inject(HttpClient);
    private announcementsUrl = '/api/v1/announcements';

    private announcementsData$ = this.http.get<Announcement[]>(this.announcementsUrl).pipe(
        pluck('entity'),
        catchError(() => [])
    );

    announcements: Signal<Announcement[]> = toSignal(this.announcementsData$, {
        initialValue: [] as Announcement[]
    });

    unreadAnnouncements: Signal<boolean> = computed(() => {
        const storedAnnouncementsJson = localStorage.getItem('announcementsData');
        const storedAnnouncements: Announcement[] = storedAnnouncementsJson
            ? JSON.parse(storedAnnouncementsJson)
            : [];

        const newAnnouncements = this.announcements();
        const newAnnouncementIds = newAnnouncements.map((announcement) => announcement.identifier);
        const storedAnnouncementIds = storedAnnouncements.map(
            (announcement) => announcement.identifier
        );

        const isNewAnnouncement = newAnnouncementIds.some(
            (id) => !storedAnnouncementIds.includes(id)
        );

        localStorage.setItem('announcementsData', JSON.stringify(newAnnouncements));

        return isNewAnnouncement;
    });
}
