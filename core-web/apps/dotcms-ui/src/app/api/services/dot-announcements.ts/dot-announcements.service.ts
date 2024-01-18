import { of } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { catchError, pluck } from 'rxjs/operators';

export type Announcement = {
    title: string;
    type: string;
    announcementDateAsISO8601: string;
};

@Injectable()
export class AnnouncementsService {
    private http = inject(HttpClient);
    private announcementsUrl = '/api/v1/announcements';

    private announcementsData$ = this.http.get<Announcement[]>(this.announcementsUrl).pipe(
        pluck('entity'),
        catchError(() => of([] as Announcement[]))
    );

    announcements = toSignal(this.announcementsData$, {
        initialValue: [] as Announcement[]
    });
}
