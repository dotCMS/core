import { HttpErrorResponse, HttpStatusCode } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { ConfirmationService } from 'primeng/api';

import { DotMessageService } from '@dotcms/data-access';

const DEFAULT_ICON_CLASS = 'pi pi-info-circle';

@Injectable({
    providedIn: 'root'
})
export class BlockEditorErrorHandlerService {
    private readonly errorHandlers;
    private confirmationService: ConfirmationService = inject(ConfirmationService);
    private dotMessageService: DotMessageService = inject(DotMessageService);

    constructor() {
        if (!this.errorHandlers) {
            this.errorHandlers = {};
            this.errorHandlers[HttpStatusCode.InternalServerError] = {
                handler: this.defaultHandleServerError.bind(this),
                defaultTitle: this.dotMessageService.get('dot.common.http.error.500.header'),
                defaultMessage: this.dotMessageService.get('dot.common.http.error.500.message')
            };
        }
    }
    public handleError(error: HttpErrorResponse): void {
        this.errorHandlers[error.status].handler(error);
    }

    private defaultHandleServerError(response?: HttpErrorResponse): void {
        const message =
            this.getErrorMessage(response) || this.errorHandlers[response.status].defaultMessage;
        this.showErrorMessage(message, this.errorHandlers[response.status].defaultTitle);
    }

    private showErrorMessage(message: string, header?: string): void {
        this.confirmationService.confirm({
            message,
            header,
            icon: DEFAULT_ICON_CLASS
        });
    }

    private getErrorMessage(response: HttpErrorResponse): string {
        let msg: string;
        if (Array.isArray(response['error']) || Array.isArray(response.error?.errors)) {
            msg = response.error[0]?.message || response.error?.errors[0]?.message;
        } else {
            const error = response['error'];
            msg = error?.message || error?.error;
        }

        return msg;
    }
}
