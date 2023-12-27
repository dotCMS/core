// http-error-interceptor.ts
import { Observable, throwError } from 'rxjs';

import {
    HttpErrorResponse,
    HttpEvent,
    HttpHandler,
    HttpInterceptor,
    HttpRequest
} from '@angular/common/http';
import { Injectable } from '@angular/core';

import { catchError } from 'rxjs/operators';

import { BlockEditorErrorHandlerService } from '../services/block-editor-error-handler/block-editor-error-handler.service';

@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
    constructor(private errorHandlerService: BlockEditorErrorHandlerService) {}

    intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        return next.handle(request).pipe(
            catchError((error: HttpErrorResponse) => {
                if (error instanceof HttpErrorResponse) {
                    this.errorHandlerService.handleError(error);
                } else {
                    console.error('Frontend error handler');
                }

                return throwError(error);
            })
        );
    }
}
