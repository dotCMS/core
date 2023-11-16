import { ComponentStore } from '@ngrx/component-store';
import { EMPTY, Observable } from 'rxjs';

import { Injectable } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotPageApiService } from '../../services/dot-page-api.service';

export interface EditEmaState {
    language_id: string;
    iframeUrl: string;
    url: string;
    editor: {
        page: {
            title: string;
        };
    };
}

@Injectable()
export class EditEmaStore extends ComponentStore<EditEmaState> {
    constructor(private dotPageApiService: DotPageApiService) {
        super({
            language_id: '',
            iframeUrl: '',
            url: '',
            editor: {
                page: {
                    title: ''
                }
            }
        });
    }

    readonly iframeUrl$: Observable<string> = this.select((state) => state.iframeUrl);
    readonly language_id$: Observable<string> = this.select((state) => state.language_id);
    readonly url$: Observable<string> = this.select((state) => state.url);
    readonly pageTitle$: Observable<string> = this.select((state) => state.editor.page.title);

    readonly setContent = this.updater(
        (
            state,
            {
                editor,
                language_id,
                url
            }: { editor: { page: { title: string } }; language_id: string; url: string }
        ) => ({
            ...state,
            editor,
            language_id,
            url,
            iframeUrl: `http://localhost:3000/${url}?language_id=${language_id}`
        })
    );

    readonly load = this.effect((params$: Observable<{ language_id: string; url: string }>) => {
        return params$.pipe(
            switchMap(({ language_id, url }) =>
                this.dotPageApiService.get({ language_id, url }).pipe(
                    tap({
                        next: (editor) => {
                            this.setContent({
                                editor,
                                language_id,
                                url
                            });
                        },
                        error: (e) => {
                            // eslint-disable-next-line no-console
                            console.log(e);
                        }
                    }),
                    catchError(() => EMPTY)
                )
            )
        );
    });

    readonly setLanguage = this.updater((state, language_id: string) => ({
        ...state,
        language_id,
        iframeUrl: `http://localhost:3000/${state.url}?language_id=${language_id}`
    }));

    readonly setUrl = this.updater((state, url: string) => ({
        ...state,
        url,
        iframeUrl: `http://localhost:3000/${url}?language_id=${state.language_id}`
    }));
}
