import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, firstValueFrom } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { BaseHttpClient, DotRequestOptions } from '@dotcms/types';

/**
 * HTTP client implementation using Angular's HttpClient.
 *
 * Extends BaseHttpClient to provide a standard interface for making HTTP requests.
 * Handles JSON and non-JSON responses, error parsing, and network error handling.
 */
@Injectable({
  providedIn: 'root'
})
export class AngularHttpClient extends BaseHttpClient {
  constructor(private httpClient: HttpClient) {
    super();
  }

  /**
   * Sends an HTTP request using Angular's HttpClient.
   *
   * @template T - The expected response type.
   * @param {string} url - The URL to send the request to.
   * @param {DotRequestOptions} [options] - Optional HTTP options (headers, method, body, etc).
   * @returns {Promise<T>} - Resolves with the parsed response or throws an error.
   * @throws {HttpError} - Throws if the response is not ok (status 4xx/5xx).
   * @throws {NetworkError} - Throws if a network error occurs.
   */
  async request<T = unknown>(url: string, options?: DotRequestOptions): Promise<T> {
    try {
      // Convert DotRequestOptions to Angular HttpClient options
      const httpOptions = this.convertToHttpOptions(options);


      // Create observable and convert to promise
      const observable = this.createRequestObservable<T>(url, httpOptions);

      return await firstValueFrom(observable);
    } catch (error) {
      // Handle network errors
      if (error instanceof TypeError) {
        throw this.createNetworkError(error);
      }

      throw error;
    }
  }

  /**
   * Creates an observable for the HTTP request with proper error handling.
   */
  private createRequestObservable<T>(url: string, options: any): Observable<T> {
    const method = options.method || 'GET';
    const { body, ...httpOptions } = options;
    const finalOptions = { ...httpOptions, observe: 'body' as const };

    switch (method.toUpperCase()) {
      case 'GET':
        return this.httpClient.get<T>(url, finalOptions).pipe(
          catchError(this.handleError.bind(this))
        ) as Observable<T>;
      case 'POST':
        return this.httpClient.post<T>(url, body, finalOptions).pipe(
          catchError(this.handleError.bind(this))
        ) as Observable<T>;
      case 'PUT':
        return this.httpClient.put<T>(url, body, finalOptions).pipe(
          catchError(this.handleError.bind(this))
        ) as Observable<T>;
      case 'DELETE':
        return this.httpClient.delete<T>(url, finalOptions).pipe(
          catchError(this.handleError.bind(this))
        ) as Observable<T>;
      case 'PATCH':
        return this.httpClient.patch<T>(url, body, finalOptions).pipe(
          catchError(this.handleError.bind(this))
        ) as Observable<T>;
      default:
        return this.httpClient.request<T>(method, url, { body, ...finalOptions }).pipe(
          catchError(this.handleError.bind(this))
        ) as Observable<T>;
    }
  }

  /**
   * Converts DotRequestOptions to Angular HttpClient options format.
   */
  private convertToHttpOptions(options?: DotRequestOptions): any {
    if (!options) {
      return {};
    }

    const httpOptions: any = {};

    // Handle headers
    if (options.headers) {
      // Convert HeadersInit to the format expected by HttpHeaders
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

    // Handle other fetch options that Angular HttpClient supports
    if (options.signal) {
      // Angular HttpClient doesn't support AbortSignal directly
      // You might need to implement this differently or use a different approach
      console.warn('AbortSignal is not directly supported by Angular HttpClient');
    }

    return httpOptions;
  }

  /**
   * Handles HTTP errors and converts them to the expected error format.
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorBody: string | unknown;

    try {
      // Try to parse error response
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
