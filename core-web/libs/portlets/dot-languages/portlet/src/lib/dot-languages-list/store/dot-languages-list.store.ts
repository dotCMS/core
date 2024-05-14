import { ComponentStore } from '@ngrx/component-store';

import { Injectable } from '@angular/core';

import { ComponentStatus, DotActionMenuItem, DotLanguage } from '@dotcms/dotcms-models';

/**
 * Interface for language row data
 */
export interface DotLanguageRow {
    locale: string;
    language: string;
    country: string;
    variables: string;
    defaultLanguage: boolean | undefined;
    actions: DotActionMenuItem[];
}

export interface DotLanguagesListState {
    status: ComponentStatus;
    languages: DotLanguageRow[];
}

export interface DotLanguagesListViewModel {
    languages: DotLanguageRow[];
}

@Injectable()
export class DotLanguagesListStore extends ComponentStore<DotLanguagesListState> {
    // Updaters
    readonly setLanguages = this.updater(
        (state: DotLanguagesListState, languages: DotLanguage[]) => ({
            ...state,
            languages: this.processLanguages(languages)
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

    /**
     * Private function to process the languages into the format needed for the state
     */
    private processLanguages(languages: DotLanguage[]): DotLanguageRow[] {
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
