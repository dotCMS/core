import { Observable } from 'rxjs';

import {
    HttpErrorResponse,
    HttpEvent,
    HttpHandler,
    HttpInterceptor,
    HttpRequest
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { catchError, map, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

@Injectable()
export class ServerErrorInterceptor implements HttpInterceptor {
    private httpErrorManagerService = inject(DotHttpErrorManagerService);

    intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
        return next.handle(request).pipe(
            catchError((error: HttpErrorResponse) => {
                return this.httpErrorManagerService.handle(error).pipe(
                    take(1),
                    map(() => null)
                );
            })
        );
    }
}
