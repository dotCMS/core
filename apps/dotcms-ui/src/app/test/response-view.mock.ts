import { HttpHeaders, HttpErrorResponse } from '@angular/common/http';

export const mockResponseView = (
    status: number,
    url?: string,
    headers?: HttpHeaders,
    body?: Record<string, unknown>[] | Record<string, unknown>
) =>
    new HttpErrorResponse({
        error: body || null,
        status: status,
        headers: headers || null,
        url: url || '/test/test'
    });
