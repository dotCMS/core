import { ComponentStore } from '@ngrx/component-store';

import { inject, Injectable } from '@angular/core';

import { ConfirmationService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { map, switchMap } from 'rxjs/operators';

import { DotLanguagesService, DotMessageService } from '@dotcms/data-access';
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
    private readonly messageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);

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
    readonly loadDialog = this.effect<number | null>((_languageId$) => {
        return _languageId$.pipe(
            map((languageId) =>
                this.dialogService.open(DotLocaleCreateEditComponent, {
                    header: this.messageService.get(
                        languageId ? 'locales.edit.locale' : 'locales.add.locale'
                    ),
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
            switchMap((data) =>
                this.languageService.add({
                    languageCode: data.languageCode,
                    language: data.language,
                    countryCode: data.countryCode,
                    country: data.country
                })
            ),
            switchMap(() => this.languageService.get()),
            map((languages) => this.setLocales(languages))
        );
    });

    readonly makeDefaultLocale = this.effect<number>((languageId$) => {
        return languageId$.pipe(
            switchMap((languageId) => this.languageService.makeDefault(languageId)),
            switchMap(() => this.languageService.get()),
            map((languages) => this.setLocales(languages))
        );
    });

    readonly deleteLocale = this.effect<number>((languageId$) => {
        return languageId$.pipe(
            switchMap((languageId) => this.languageService.delete(languageId)),
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
                        label: this.messageService.get('locales.edit'),
                        command: () => {
                            this.loadDialog(language.id);
                        }
                    }
                },
                {
                    menuItem: {
                        label: this.messageService.get('locales.make.default'),
                        command: () => {
                            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                        }
                    },
                    shouldShow: () => !language.defaultLanguage
                },
                {
                    menuItem: {
                        label: this.messageService.get('locales.delete'),
                        command: () => {
                            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                        }
                    }
                }
            ]
        }));
    }
}
