import { Injectable, inject, PLATFORM_ID, TransferState, makeStateKey } from '@angular/core';
import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { HttpClient, HttpHeaders, HttpErrorResponse, HttpContext, HttpContextToken } from '@angular/common/http';
import { Observable, throwError, firstValueFrom, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { BaseHttpClient, DotRequestOptions } from '@dotcms/types';

// Create a context token to mark requests for caching
export const BYPASS_TRANSFER_CACHE = new HttpContextToken<boolean>(() => false);

/**
 * HTTP client implementation using Angular's HttpClient with proper SSR transfer cache support.
 */
@Injectable()
export class AngularHttpClient extends BaseHttpClient {
  private platformId = inject(PLATFORM_ID);
  private transferState = inject(TransferState);
  private isServer = isPlatformServer(this.platformId);
  private isBrowser = isPlatformBrowser(this.platformId);

  constructor(private httpClient: HttpClient) {
    super();
    console.log(`[AngularHttpClient] Running on: ${this.isServer ? 'SERVER' : 'BROWSER'}`);
  }

  /**
   * Sends an HTTP request using Angular's HttpClient with transfer cache support.
   */
  async request<T = unknown>(url: string, options?: DotRequestOptions): Promise<T> {
    const platform = this.isServer ? 'SERVER' : 'BROWSER';

    // Create a unique key for this request in the transfer state
    const transferKey = this.createTransferKey(url, options);

    // Check if we're in the browser and have cached data
    if (this.isBrowser && this.transferState.hasKey(transferKey)) {
      const cachedData = this.transferState.get<T>(transferKey, null as unknown as T);
      if (cachedData !== null) {
        console.log(`[${platform}] Using cached response for:`, url);
        // Remove the key after using it to prevent memory leaks
        this.transferState.remove(transferKey);
        return cachedData;
      }
    }

    console.log(`[${platform}] AngularHttpClient request:`, {
      url,
      method: options?.method || 'GET',
      hasBody: !!options?.body,
      transferKey: transferKey.toString()
    });

    try {
      // Convert DotRequestOptions to Angular HttpClient options
      const httpOptions = this.convertToHttpOptions(options);

      // Create observable and convert to promise
      const observable = this.createRequestObservable<T>(url, httpOptions);

      const result = await firstValueFrom(observable);

      // If we're on the server, store the result in transfer state
      if (this.isServer) {
        console.log(`[SERVER] Storing response in transfer state for:`, url);
        this.transferState.set(transferKey, result);
      }

      console.log(`[${platform}] AngularHttpClient response received for:`, url);

      return result;
    } catch (error) {
      console.error(`[${platform}] AngularHttpClient error for:`, url, error);

      if (error instanceof TypeError) {
        throw this.createNetworkError(error);
      }

      throw error;
    }
  }

  /**
   * Creates a unique transfer state key for the request
   */
  private createTransferKey(url: string, options?: DotRequestOptions): any {
    const method = options?.method || 'GET';
    const body = options?.body ? JSON.stringify(options.body) : '';
    const keyString = `HTTP_${method}_${url}_${this.hashString(body)}`;
    return makeStateKey<any>(keyString);
  }

  /**
   * Simple hash function for creating unique keys
   */
  private hashString(str: string): string {
    let hash = 0;
    if (str.length === 0) return hash.toString();
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32bit integer
    }
    return hash.toString();
  }

  /**
   * Creates an observable for the HTTP request with proper error handling.
   */
  private createRequestObservable<T>(url: string, options: any): Observable<T> {
    const method = options.method || 'GET';
    const { body, ...httpOptions } = options;

    // Configure options for proper transfer cache handling
    const context = new HttpContext();

    const finalOptions = {
      ...httpOptions,
      observe: 'body' as const,
      responseType: httpOptions.responseType || 'json' as const,
      context: context,
      // Force content-type for JSON requests
      headers: httpOptions.headers || new HttpHeaders({
        'Content-Type': 'application/json'
      })
    };

    // Log what we're sending on the server
    if (this.isServer && body) {
      console.log('[SERVER] Request body preview:',
        typeof body === 'string' ? body.substring(0, 200) : JSON.stringify(body).substring(0, 200)
      );
    }

    let request$: Observable<T>;

    switch (method.toUpperCase()) {
      case 'GET':
        request$ = this.httpClient.get<T>(url, finalOptions) as Observable<T>;
        break;
      case 'POST':
        request$ = this.httpClient.post<T>(url, body, finalOptions) as Observable<T>;
        break;
      case 'PUT':
        request$ = this.httpClient.put<T>(url, body, finalOptions) as Observable<T>;
        break;
      case 'DELETE':
        request$ = this.httpClient.delete<T>(url, finalOptions) as Observable<T>;
        break;
      case 'PATCH':
        request$ = this.httpClient.patch<T>(url, body, finalOptions) as Observable<T>;
        break;
      default:
        request$ = this.httpClient.request<T>(method, url, { body, ...finalOptions }) as Observable<T>;
    }

    return request$.pipe(
      tap((response) => {
        const platform = this.isServer ? 'SERVER' : 'BROWSER';
        console.log(`[${platform}] Request completed:`, url);
      }),
      catchError(this.handleError.bind(this))
    );
  }

  /**
   * Converts DotRequestOptions to Angular HttpClient options format.
   */
  private convertToHttpOptions(options?: DotRequestOptions): any {
    if (!options) {
      return { method: 'GET' };
    }

    const httpOptions: any = {};

    // Handle headers
    if (options.headers) {
      const headerObj: { [key: string]: string } = {};
      if (Array.isArray(options.headers)) {
        options.headers.forEach(([key, value]) => {
          headerObj[key] = value;
        });
      } else if (typeof options.headers === 'object') {
        Object.assign(headerObj, options.headers);
      }
      httpOptions.headers = new HttpHeaders(headerObj);
    }

    // Handle method
    if (options.method) {
      httpOptions.method = options.method;
    }

    // Handle body
    if (options.body) {
      httpOptions.body = options.body;
    }

    // Set withCredentials based on credentials option
    if (options.credentials === 'include') {
      httpOptions.withCredentials = true;
    }

    return httpOptions;
  }

  /**
   * Handles HTTP errors and converts them to the expected error format.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    const platform = this.isServer ? 'SERVER' : 'BROWSER';
    console.error(`[${platform}] HTTP Error:`, error);

    let errorBody: string | unknown;

    try {
      if (error.error) {
        errorBody = error.error;
      } else {
        errorBody = error.message || error.statusText;
      }
    } catch {
      errorBody = error.statusText;
    }

    // Convert headers to plain object
    const headers: Record<string, string> = {};
    if (error.headers) {
      error.headers.keys().forEach(key => {
        const value = error.headers.get(key);
        if (value) {
          headers[key] = value;
        }
      });
    }

    // Create and throw the appropriate error
    const httpError = this.createHttpError(
      error.status,
      error.statusText,
      headers,
      errorBody
    );

    return throwError(() => httpError);
  }
}
