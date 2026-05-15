import { Observable } from 'rxjs';

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { map } from 'rxjs/operators';

import { DotCMSContentlet, DotCMSResponse } from '@dotcms/dotcms-models';

export interface DotReportIssuePayload {
    description: string;
    metadata: Record<string, string>;
    screenshot?: File | null;
}

export interface DotReportIssueUserMetadata {
    email: string;
    fullName: string;
    userId: string;
}

export interface DotReportIssueMetadata {
    browser?: string;
    dotcmsBuildDate?: string;
    dotcmsVersion?: string;
    platform?: string;
    referer?: string;
    referrer?: string;
    remoteAddress?: string;
    requestUrl?: string;
    serverName?: string;
    submittedAt?: string;
    url?: string;
    user?: DotReportIssueUserMetadata;
    userAgent?: string;
    viewport?: string;
    [key: string]: unknown;
}

export interface DotReportIssueScreenshotMetadata {
    contentType: string;
    editableAsText: boolean;
    fileSize: number;
    height?: number;
    isImage: boolean;
    length: number;
    modDate: number;
    name: string;
    sha256: string;
    title: string;
    version: number;
    width?: number;
}

export interface DotReportIssueContentlet extends DotCMSContentlet {
    metadata?: DotReportIssueMetadata;
    screenshot?: string;
    screenshotContentAsset?: string;
    screenshotMetaData?: DotReportIssueScreenshotMetadata;
    screenshotVersion?: string;
}

@Injectable()
export class DotReportIssueService {
    private readonly http = inject(HttpClient);

    reportIssue(payload: DotReportIssuePayload): Observable<DotReportIssueContentlet> {
        const formData = new FormData();

        formData.append('description', payload.description);
        formData.append('metadata', JSON.stringify(payload.metadata));

        if (payload.screenshot) {
            formData.append('screenshot', payload.screenshot);
        }

        return this.http
            .post<DotCMSResponse<DotReportIssueContentlet>>('/api/v1/report-issue', formData)
            .pipe(map((response) => response.entity));
    }
}
