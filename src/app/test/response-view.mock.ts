import { HttpHeaders, HttpErrorResponse } from '@angular/common/http';

export const mockResponseView = (status: number, url?: string, headers?: HttpHeaders, body?: any) =>
    new HttpErrorResponse({
        error: body || null,
        status: status,
        headers: headers || null,
        url: url || '/test/test'
    });
