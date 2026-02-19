import { Observable } from 'rxjs';

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';

import { DotCMSAPIResponse } from '@dotcms/dotcms-models';

import { BundleMap } from './bundle-map.model';

const OSGI_BASE = '/api/v1/osgi';

@Injectable({
    providedIn: 'root'
})
export class DotOsgiService {
    readonly #http = inject(HttpClient);

    getInstalledBundles(ignoreSystemBundles = false): Observable<DotCMSAPIResponse<BundleMap[]>> {
        const params = new HttpParams().set('ignoresystembundles', String(ignoreSystemBundles));
        return this.#http.get<DotCMSAPIResponse<BundleMap[]>>(OSGI_BASE, { params });
    }

    getDotSystemBundles(ignoreSystemBundles = false): Observable<DotCMSAPIResponse<BundleMap[]>> {
        const params = new HttpParams().set('ignoresystembundles', String(ignoreSystemBundles));
        return this.#http.get<DotCMSAPIResponse<BundleMap[]>>(`${OSGI_BASE}/dotsystem`, {
            params
        });
    }

    getAvailablePlugins(): Observable<DotCMSAPIResponse<string[]>> {
        return this.#http.get<DotCMSAPIResponse<string[]>>(`${OSGI_BASE}/available-plugins`);
    }

    uploadBundles(files: File[]): Observable<DotCMSAPIResponse<unknown>> {
        const formData = new FormData();
        files.forEach((file) => formData.append('file', file, file.name));
        return this.#http.post<DotCMSAPIResponse<unknown>>(OSGI_BASE, formData);
    }

    deploy(jar: string): Observable<DotCMSAPIResponse<unknown>> {
        return this.#http.put<DotCMSAPIResponse<unknown>>(
            `${OSGI_BASE}/jar/${encodeURIComponent(jar)}/_deploy`,
            {}
        );
    }

    start(jar: string): Observable<DotCMSAPIResponse<unknown>> {
        return this.#http.put<DotCMSAPIResponse<unknown>>(
            `${OSGI_BASE}/jar/${encodeURIComponent(jar)}/_start`,
            {}
        );
    }

    stop(jar: string): Observable<DotCMSAPIResponse<unknown>> {
        return this.#http.put<DotCMSAPIResponse<unknown>>(
            `${OSGI_BASE}/jar/${encodeURIComponent(jar)}/_stop`,
            {}
        );
    }

    undeploy(jar: string): Observable<DotCMSAPIResponse<unknown>> {
        return this.#http.delete<DotCMSAPIResponse<unknown>>(
            `${OSGI_BASE}/jar/${encodeURIComponent(jar)}`
        );
    }

    processExports(bundle: string): Observable<DotCMSAPIResponse<unknown>> {
        return this.#http.get<DotCMSAPIResponse<unknown>>(
            `${OSGI_BASE}/_processExports/${encodeURIComponent(bundle)}`
        );
    }

    getExtraPackages(): Observable<DotCMSAPIResponse<string>> {
        return this.#http.get<DotCMSAPIResponse<string>>(`${OSGI_BASE}/extra-packages`);
    }

    updateExtraPackages(packages: string): Observable<DotCMSAPIResponse<string>> {
        return this.#http.put<DotCMSAPIResponse<string>>(`${OSGI_BASE}/extra-packages`, {
            packages
        });
    }
}
