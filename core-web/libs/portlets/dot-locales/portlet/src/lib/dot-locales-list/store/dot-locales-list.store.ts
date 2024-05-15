import { ComponentStore } from '@ngrx/component-store';

import { Injectable } from '@angular/core';

import { ComponentStatus, DotActionMenuItem, DotLanguage } from '@dotcms/dotcms-models';

/**
 * Interface for language row data
 */
export interface DotLocaleRow {
    locale: string;
    language: string;
    country: string;
    variables: string;
    defaultLanguage: boolean | undefined;
    actions: DotActionMenuItem[];
}

export interface DotLocalesListState {
    status: ComponentStatus;
    locales: DotLocaleRow[];
}

export interface DotLocaleListViewModel {
    locales: DotLocaleRow[];
}

@Injectable()
export class DotLocalesListStore extends ComponentStore<DotLocalesListState> {
    // Updaters
    readonly setLocales = this.updater((state: DotLocalesListState, languages: DotLanguage[]) => ({
        ...state,
        locales: this.processLanguages(languages)
    }));

    readonly vm$ = this.select(
        this.state$,
        ({ locales }): DotLocaleListViewModel => ({
            locales
        })
    );

    constructor() {
        super({ status: ComponentStatus.IDLE, locales: [] });
    }

    /**
     * Private function to process the languages into the format needed for the state
     */
    private processLanguages(languages: DotLanguage[]): DotLocaleRow[] {
        return languages.map((language) => ({
            locale: `${language.language} (${language.isoCode})`,
            language: `${language.language} - ${language.languageCode}`,
            country: `${language.country} - ${language.countryCode}`,
            variables: 'TBD',
            defaultLanguage: language.defaultLanguage,
            actions: [
                {
                    menuItem: {
                        label: 'Edit',
                        command: () => {
                            //TODO: Implement
                        }
                    },
                    shouldShow: () => true
                },
                {
                    menuItem: {
                        label: 'Delete',
                        command: () => {
                            //TODO: Implement
                        }
                    },
                    shouldShow: () => true
                }
            ]
        }));
    }
}
