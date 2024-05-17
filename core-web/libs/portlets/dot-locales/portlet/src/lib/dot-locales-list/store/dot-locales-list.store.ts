import { ComponentStore } from '@ngrx/component-store';

import { inject, Injectable } from '@angular/core';

import { DialogService } from 'primeng/dynamicdialog';

import { map, switchMap } from 'rxjs/operators';

import { DotLanguagesService } from '@dotcms/data-access';
import { ComponentStatus, DotActionMenuItem, DotLanguage } from '@dotcms/dotcms-models';

import {
    DotLocaleCreateEditComponent,
    DotLocaleCreateEditData
} from '../components/dot-locale-create-edit/dot-locale-create-edit.component';

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
    private readonly dialogService = inject(DialogService);
    private readonly languageService = inject(DotLanguagesService);

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

    //Effects
    readonly loadDialog = this.effect<string | null>((_languageId$) => {
        return _languageId$.pipe(
            map((languageId) =>
                this.dialogService.open(DotLocaleCreateEditComponent, {
                    header: languageId ? 'Edit Locale' : 'Add Locale',
                    width: '31rem',
                    data: {
                        languages: ['Spanish', 'English'],
                        countries: ['Spain', 'USA'],
                        languageId
                    }
                })
            )
        );
    });

    readonly addLocale = this.effect<DotLocaleCreateEditData>((data$) => {
        return data$.pipe(
            switchMap(() =>
                this.languageService.add({
                    languageCode: 'cc',
                    language: 'cc',
                    countryCode: 'cc',
                    country: 'cc'
                })
            ),
            switchMap(() => this.languageService.get()),
            map((languages) => this.setLocales(languages))
        );
    });

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
