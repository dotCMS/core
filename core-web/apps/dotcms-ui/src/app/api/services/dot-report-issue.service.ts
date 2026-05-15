import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSResponse } from '@dotcms/dotcms-models';

export interface DotReportIssuePayload {
    description: string;
    metadata: Record<string, string>;
    screenshot?: File | null;
}

@Injectable()
export class DotReportIssueService {
    private readonly http = inject(HttpClient);

    reportIssue(payload: DotReportIssuePayload): Observable<unknown> {
        const formData = new FormData();

        formData.append('description', payload.description);
        formData.append('metadata', JSON.stringify(payload.metadata));

        if (payload.screenshot) {
            formData.append('screenshot', payload.screenshot);
        }

        return this.http
            .post<DotCMSResponse<unknown>>('/api/v1/report-issue', formData)
            .pipe(map((response) => response.entity));
    }
}
