import { EMPTY, throwError } from 'rxjs';

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject, Injector } from '@angular/core';

import { catchError } from 'rxjs/operators';

import { HttpCode, LoginService, LOGOUT_URL } from '@dotcms/dotcms-js';

export const serverErrorInterceptor: HttpInterceptorFn = (req, next) => {
    const injector = inject(Injector);

    return next(req).pipe(
        catchError((error: HttpErrorResponse) => {
            if (error.status === HttpCode.UNAUTHORIZED) {
                const loginService = injector.get(LoginService);

                if (loginService.auth?.user) {
                    window.location.href = `${LOGOUT_URL}?r=${new Date().getTime()}`;

                    return EMPTY;
                }
            }

            return throwError(() => error);
        })
    );
};
