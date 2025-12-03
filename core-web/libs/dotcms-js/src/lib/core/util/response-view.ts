import { HttpResponse, HttpHeaders } from '@angular/common/http';

import { DotCMSResponse } from '../core-web.service';

/**
 *
 *
 * <code>
 * {
 *   "errors":[],
 *   "entity":{},
 *   "messages":[],
 *   "i18nMessagesMap":{}
 * }
 * </code>
 */
export class ResponseView<T = any> {
    private _bodyJsonObject: DotCMSResponse<T>;
    private headers: HttpHeaders;

    public constructor(private resp: HttpResponse<DotCMSResponse<T>>) {
        try {
            this._bodyJsonObject = resp.body;
            this.headers = resp.headers;
        } catch (e) {
            this._bodyJsonObject = null;
        }
    }

    public header(headerName: string): string {
        return this.headers.get(headerName);
    }

    get bodyJsonObject(): DotCMSResponse<T> {
        return this._bodyJsonObject;
    }

    get i18nMessagesMap(): { [key: string]: string } {
        return this._bodyJsonObject.i18nMessagesMap;
    }

    get contentlets(): T {
        return this._bodyJsonObject.contentlets;
    }

    get entity(): T {
        return this._bodyJsonObject.entity;
    }

    get tempFiles(): T {
        return this._bodyJsonObject.tempFiles;
    }

    get errorsMessages(): string {
        let errorMessages = '';

        if (this._bodyJsonObject.errors) {
            this._bodyJsonObject.errors.forEach((e: any) => {
                errorMessages += e.message;
            });
        } else {
            errorMessages = this._bodyJsonObject.messages.toString();
        }

        return errorMessages;
    }

    get status(): number {
        return this.resp.status;
    }

    get response(): HttpResponse<DotCMSResponse<T>> {
        return this.resp;
    }

    public existError(errorCode: string): boolean {
        return (
            this._bodyJsonObject.errors &&
            this._bodyJsonObject.errors.filter((e: any) => e.errorCode === errorCode).length > 0
        );
    }
}
