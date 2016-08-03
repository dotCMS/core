import { Response } from '@angular/http'

export class DotCMSHttpResponse{

    private bodyJsonObject:any;

    public constructor( public resp:Response ){
        this.bodyJsonObject = JSON.parse(resp._body)
    }

    get i18nMessagesMap():any{
        return this.bodyJsonObject.i18nMessagesMap;
    }

    get entity():any{
        return this.bodyJsonObject.entity;
    }

    get errorsMessages(): string {
        let errorMessages = '';
        try {
            this.bodyJsonObject.errors.forEach(e => {
                errorMessages += e.message;
            });
        }catch (ex) {
            errorMessages = this.bodyJsonObject.error.split(':')[1];
        }
        return errorMessages;
    }

    get status() : number{
        return this.resp.status;
    }

    get response():Response{
        return this.resp;
    }

    public existError(errorCode:string):boolean{
        return this.bodyJsonObject.errors &&
            this.bodyJsonObject.errors.filter( e => e.errorCode === errorCode).length > 0;
    }
}
