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
    private bodyJsonObject: DotCMSResponse<T>;
    private headers: HttpHeaders;

    public constructor(private resp: HttpResponse<DotCMSResponse<T>>) {
        try {
            this.bodyJsonObject = resp.body;
            this.headers = resp.headers;
        } catch (e) {
            this.bodyJsonObject = null;
        }
    }

    public header(headerName: string): string {
        return this.headers.get(headerName);
    }

    get i18nMessagesMap(): { [key: string]: string } {
        return this.bodyJsonObject.i18nMessagesMap;
    }

    get contentlets(): T {
        return this.bodyJsonObject.contentlets;
    }

    get entity(): T {
        return this.bodyJsonObject.entity;
    }

    get tempFiles(): T {
        return this.bodyJsonObject.tempFiles;
    }

    get errorsMessages(): string {
        let errorMessages = '';

        if (this.bodyJsonObject.errors) {
            this.bodyJsonObject.errors.forEach((e: any) => {
                errorMessages += e.message;
            });
        } else {
            errorMessages = this.bodyJsonObject.messages.toString();
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
            this.bodyJsonObject.errors &&
            this.bodyJsonObject.errors.filter((e: any) => e.errorCode === errorCode).length > 0
        );
    }
}
