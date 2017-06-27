import { Response, Headers } from '@angular/http';

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
export class ResponseView {

    private bodyJsonObject: any;
    private headers: Headers;

    public constructor( private resp: Response ) {
        try {
            this.bodyJsonObject = resp.json();
            this.headers = resp.headers;
        } catch (e) {
            this.bodyJsonObject = {};
        }
    }

    public header(headerName: string): string {
        return this.headers.get(headerName);
    }

    get i18nMessagesMap(): any{
        return this.bodyJsonObject.i18nMessagesMap;
    }

    get entity(): any {
        return this.bodyJsonObject.entity;
    }

    get errorsMessages(): string {
        let errorMessages = '';

        if (this.bodyJsonObject.errors) {
            this.bodyJsonObject.errors.forEach(e => {
                errorMessages += e.message;
            });
        } else {
            errorMessages = this.bodyJsonObject.message;
        }

        return errorMessages;
    }

    get status(): number{
        return this.resp.status;
    }

    get response(): Response {
        return this.resp;
    }

    public existError(errorCode: string): boolean {
        return this.bodyJsonObject.errors &&
            this.bodyJsonObject.errors.filter( e => e.errorCode === errorCode).length > 0;
    }
}
