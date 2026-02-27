// Type declarations for external dependencies used in Stencil build
// These are stubs to allow TypeScript compilation without requiring
// the full Angular/PrimeNG dependencies in the webcomponents build

declare module '@angular/common/http' {
    export interface HttpHeaders {
        get(name: string): string | null;
        set(name: string, value: string | string[]): HttpHeaders;
        delete(name: string): HttpHeaders;
    }
    
    export class HttpHeaders {
        constructor(headers?: string | { [name: string]: string | string[] } | HttpHeaders);
    }
    
    export interface HttpResponse<T> {
        body: T | null;
        headers: HttpHeaders;
        status: number;
        statusText: string;
        url: string | null;
    }
    
    export interface HttpErrorResponse extends HttpResponse<any> {
        error: any;
        message: string;
        name: string;
        ok: boolean;
        status: number;
        statusText: string;
        url: string | null;
    }
    
    export interface HttpRequest<T> {
        body: T | null;
        headers: HttpHeaders;
        method: string;
        params: any;
        reportProgress: boolean;
        responseType: 'arraybuffer' | 'blob' | 'json' | 'text';
        url: string;
        urlWithParams: string;
        withCredentials: boolean;
    }
    
    export class HttpRequest<T> {
        constructor(method: string, url: string, body?: T | null, init?: {
            headers?: HttpHeaders;
            params?: any;
            reportProgress?: boolean;
            responseType?: 'arraybuffer' | 'blob' | 'json' | 'text';
            withCredentials?: boolean;
        });
    }
    
    export interface HttpEvent<T> {
        type: HttpEventType;
    }
    
    export enum HttpEventType {
        Sent = 0,
        UploadProgress = 1,
        ResponseHeader = 2,
        Response = 3,
        User = 4,
        DownloadProgress = 5
    }
    
    export interface HttpParams {
        get(name: string): string | null;
        getAll(name: string): string[];
        has(name: string): boolean;
        keys(): string[];
        set(name: string, value: string | number | boolean): HttpParams;
        append(name: string, value: string | number | boolean): HttpParams;
        delete(name: string): HttpParams;
        toString(): string;
    }
    
    export class HttpParams {
        constructor(options?: { fromString?: string; [param: string]: string | string[] | undefined });
    }
    
    export class HttpClient {
        request<T>(req: HttpRequest<any>): any;
        get<T>(url: string, options?: any): any;
        post<T>(url: string, body: any, options?: any): any;
        put<T>(url: string, body: any, options?: any): any;
        patch<T>(url: string, body: any, options?: any): any;
        delete<T>(url: string, options?: any): any;
        head<T>(url: string, options?: any): any;
        options<T>(url: string, options?: any): any;
    }
}

declare module 'primeng/api' {
    export interface MenuItem {
        label?: string;
        icon?: string;
        command?: (event?: any) => void;
        url?: string;
        routerLink?: any;
        items?: MenuItem[];
        expanded?: boolean;
        disabled?: boolean;
        visible?: boolean;
        target?: string;
        routerLinkActiveOptions?: any;
        separator?: boolean;
        badge?: string;
        badgeStyleClass?: string;
        style?: any;
        styleClass?: string;
        title?: string;
        id?: string;
        automationId?: string;
        data?: any;
    }
    
    export interface MenuItemCommandEvent {
        originalEvent?: Event;
        item?: MenuItem;
    }
    
    export interface SelectItem {
        label?: string;
        value?: any;
        icon?: string;
        title?: string;
        disabled?: boolean;
    }
    
    export interface TreeNode<T = any> {
        checked?: boolean;
        label?: string;
        data?: T;
        icon?: string;
        expandedIcon?: string;
        collapsedIcon?: string;
        children?: TreeNode<T>[];
        leaf?: boolean;
        expanded?: boolean;
        type?: string;
        parent?: TreeNode<T>;
        partialSelected?: boolean;
        style?: any;
        styleClass?: string;
        draggable?: boolean;
        droppable?: boolean;
        selectable?: boolean;
        key?: string;
        loading?: boolean;
    }
}

