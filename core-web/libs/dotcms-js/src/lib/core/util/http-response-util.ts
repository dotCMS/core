import type { HttpRequest, HttpErrorResponse, HttpResponse } from '@angular/common/http';

import { HttpCode } from './http-code';

export const NETWORK_CONNECTION_ERROR = 1;

export const UNKNOWN_RESPONSE_ERROR = 2;

export const SERVER_RESPONSE_ERROR = 3;

export const CLIENTS_ONLY_MESSAGES = {
    1: 'Could not connect to server.'
};

export class CwError {
    constructor(
        public code: number,
        public message: string,
        public request?: HttpRequest<any>,
        public response?: HttpErrorResponse,
        public source?: any
    ) {}
}

export interface ResponseError {
    response: HttpResponse<any>;
    msg: string;
}

export function isSuccess(resp: HttpResponse<any>): boolean {
    return resp.status > 199 && resp.status < 300;
}

export function hasContent(resp: HttpResponse<any>): boolean {
    return isSuccess(resp) && resp.status !== HttpCode.NO_CONTENT;
}
