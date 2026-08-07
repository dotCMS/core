import { HttpHeaders, HttpResponse } from '@angular/common/http';

import { DotCMSResponse } from '@dotcms/dotcms-models';

/**
 * @deprecated Use DotCMSResponse from @dotcms/dotcms-models and Angular HttpClient directly instead.
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
    // `HttpResponse.body` is nullable, so the parsed body genuinely can be absent.
    private bodyJsonObject: DotCMSResponse<T> | null;
    private headers: HttpHeaders;

    public constructor(private resp: HttpResponse<DotCMSResponse<T>>) {
        this.bodyJsonObject = resp.body;
        this.headers = resp.headers;
    }

    public header(headerName: string): string | null {
        return this.headers.get(headerName);
    }

    get i18nMessagesMap(): { [key: string]: string } {
        return this.bodyJsonObject?.i18nMessagesMap ?? {};
    }

    get contentlets(): T | undefined {
        return this.bodyJsonObject?.contentlets;
    }

    get entity(): T | undefined {
        return this.bodyJsonObject?.entity;
    }

    get tempFiles(): T | undefined {
        return this.bodyJsonObject?.tempFiles;
    }

    get errorsMessages(): string {
        let errorMessages = '';

        if (this.bodyJsonObject?.errors) {
            this.bodyJsonObject.errors.forEach((e: any) => {
                errorMessages += e.message;
            });
        } else {
            errorMessages = this.bodyJsonObject?.messages.toString() ?? '';
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
            !!this.bodyJsonObject?.errors &&
            this.bodyJsonObject.errors.filter((e: any) => e.errorCode === errorCode).length > 0
        );
    }
}
