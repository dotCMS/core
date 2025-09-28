import { inject, Injectable } from '@angular/core';
import { from, Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {
  DotCMSPageRequestParams,
  DotCMSComposedPageResponse,
  DotCMSExtendedPageResponse,
} from '@dotcms/types';

import { DotCMSClient } from '@dotcms/angular';

@Injectable({
  providedIn: 'root',
})
export class PageService {
  private readonly client = inject(DotCMSClient);

  /**
   * Get the page and navigation for the given url and params.
   *
   * @param {string} url
   * @param {DotCMSPageRequestParams} params
   * @return {*}  {Observable<DotCMSComposedPageResponse<T>>}
   * @memberof PageService
   */
  getPageAsset<T extends DotCMSExtendedPageResponse>(
    url: string,
    params: DotCMSPageRequestParams,
  ): Observable<DotCMSComposedPageResponse<T>> {
    return from(
      this.client.page.get<T>(url, {
        ...params,
      }),
    ).pipe(
      // To prevent the error from being swallowed by the pipe, we need to catch it
      catchError((error: DotCMSComposedPageResponse<T>) => {
        return of(error);
      }),
    );
  }
}
