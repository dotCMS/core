import { ComponentStore } from '@ngrx/component-store';

import { Injectable } from '@angular/core';

import { ComponentStatus, DotLanguage } from '@dotcms/dotcms-models';

export interface DotLanguagesListState {
    status: ComponentStatus;
    languages: DotLanguage[];
}

export interface DotLanguagesListViewModel {
    languages: DotLanguage[];
}

@Injectable()
export class DotLanguagesListStore extends ComponentStore<DotLanguagesListState> {
    // Updaters
    readonly setLanguages = this.updater(
        (state: DotLanguagesListState, languages: DotLanguage[]) => ({
            ...state,
            languages
        })
    );

    readonly vm$ = this.select(
        this.state$,
        ({ languages }): DotLanguagesListViewModel => ({
            languages
        })
    );

    constructor() {
        super({ status: ComponentStatus.IDLE, languages: [] });
    }
}
