import { ComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { Injectable } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

export interface EditEmaState {
    language_id: string;
    iframeUrl: SafeResourceUrl;
    url: string;
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(public sanitizer: DomSanitizer) {
        super({
            language_id: '',
            iframeUrl: '',
            url: ''
        });
    }

    readonly iframeUrl$: Observable<SafeResourceUrl> = this.select((state) => state.iframeUrl);
    readonly language_id$: Observable<SafeResourceUrl> = this.select((state) => state.language_id);
    readonly url$: Observable<SafeResourceUrl> = this.select((state) => state.url);

    readonly load = this.updater(
        (state, { language_id, url }: { language_id: string; url: string }) => ({
            ...state,
            language_id,
            url,
            iframeUrl: this.sanitizer.bypassSecurityTrustResourceUrl(
                `http://localhost:3000/${url}?language_id=${language_id}`
            )
        })
    );

    readonly setLanguage = this.updater((state, language_id: string) => ({
        ...state,
        language_id,
        iframeUrl: this.sanitizer.bypassSecurityTrustResourceUrl(
            `http://localhost:3000/${state.url}?language_id=${language_id}`
        )
    }));

    readonly setUrl = this.updater((state, url: string) => ({
        ...state,
        url,
        iframeUrl: this.sanitizer.bypassSecurityTrustResourceUrl(
            `http://localhost:3000/${url}?language_id=${state.language_id}`
        )
    }));
}
