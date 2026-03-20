import { throwError } from 'rxjs';

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, switchMap, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

/**
 * Global HTTP interceptor that routes errors through DotHttpErrorManagerService
 * for centralized UI feedback (dialogs, 401 → login redirect, etc.),
 * then re-throws so callers can also react (e.g. stop loading spinners).
 */
export const serverErrorInterceptor: HttpInterceptorFn = (req, next) => {
    const httpErrorManagerService = inject(DotHttpErrorManagerService);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            return httpErrorManagerService.handle(error).pipe(
                take(1),
                switchMap(() => throwError(() => error))
            );
        })
    );
};
