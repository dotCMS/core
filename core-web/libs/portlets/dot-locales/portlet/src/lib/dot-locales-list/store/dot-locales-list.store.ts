import { ComponentStore } from '@ngrx/component-store';
import { of } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { ConfirmationService, MessageService } from 'primeng/api';
import { DialogService } from 'primeng/dynamicdialog';

import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService
} from '@dotcms/data-access';
import { DotPushPublishDialogService } from '@dotcms/dotcms-js';
import {
    ComponentStatus,
    DotActionMenuItem,
    DotEnvironment,
    DotLanguage,
    DotLanguagesISO
} from '@dotcms/dotcms-models';
import { DotLocalesListResolverData } from '@dotcms/portlets/dot-locales/portlet/data-access';

import {
    DotLocaleCreateEditComponent,
    DotLocaleCreateEditData
} from '../components/dot-locale-create-edit/dot-locale-create-edit.component';

/**
 * Interface for language row data
 */
export interface DotLocaleRow {
    id: number;
    locale: string;
    language: string;
    country: string;
    variables: string;
    defaultLanguage: boolean | undefined;
    actions: DotActionMenuItem[];
}

export interface DotLocalesListState extends DotLanguagesISO {
    status: ComponentStatus;
    locales: DotLocaleRow[];
    initialLocales: DotLanguage[];
    isEnterprise: boolean;
    pushPublishEnvironments: DotEnvironment[];
}

export interface DotLocaleListViewModel {
    locales: DotLocaleRow[];
}

export interface DotLocaleListResolverData {
    localaes: DotLanguage[];
    languages: { code: string; name: string }[];
    countries: { code: string; name: string }[];
}

@Injectable()
export class DotLocalesListStore extends ComponentStore<DotLocalesListState> {
    private readonly dialogService = inject(DialogService);
    private readonly languageService = inject(DotLanguagesService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly confirmationService = inject(ConfirmationService);
    private readonly messageService = inject(MessageService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);
    private readonly dotPushPublishDialogService = inject(DotPushPublishDialogService);

    // Updaters

    readonly setResolvedData = this.updater(
        (
            state: DotLocalesListState,
            routeResolved: {
                data: DotLocalesListResolverData;
                isEnterprise: boolean;
                pushPublishEnvironments: DotEnvironment[];
            }
        ) => ({
            ...state,
            initialLocales: routeResolved.data.locales,
            locales: this.processLanguages(
                routeResolved.data.locales,
                routeResolved.isEnterprise,
                routeResolved.pushPublishEnvironments
            ),
            countries: routeResolved.data.countries,
            languages: routeResolved.data.languages,
            isEnterprise: routeResolved.isEnterprise,
            pushPublishEnvironments: routeResolved.pushPublishEnvironments
        })
    );

    readonly setLocales = this.updater((state: DotLocalesListState, locales: DotLanguage[]) => ({
        ...state,
        initialLocales: locales,
        locales: this.processLanguages(locales, state.isEnterprise, state.pushPublishEnvironments)
    }));

    readonly vm$ = this.select(
        this.state$,
        ({ locales }): DotLocaleListViewModel => ({
            locales
        })
    );

    //Effects
    readonly loadDialog = this.effect<number | null>((_languageId$) =>
        _languageId$.pipe(
            withLatestFrom(this.state$),
            map(([languageId, { languages, countries, initialLocales }]) => {
                const localeToEdit = initialLocales.find((l) => l.id === languageId);

                this.dialogService.open(DotLocaleCreateEditComponent, {
                    header: this.dotMessageService.get(
                        localeToEdit ? 'locales.edit.locale' : 'locales.add.locale'
                    ),
                    width: '31rem',
                    data: {
                        languages,
                        countries,
                        locale: localeToEdit
                    }
                });
            })
        )
    );

    readonly addLocale = this.effect<DotLocaleCreateEditData>((data$) => {
        return data$.pipe(
            switchMap((data) => {
                const { languageCode, language, countryCode, country } = data.locale as DotLanguage;

                return this.languageService.add({ languageCode, language, countryCode, country });
            }),
            map(() => {
                this.messageService.add({
                    severity: 'success',
                    summary: this.dotMessageService.get('message.success'),
                    detail: this.dotMessageService.get('locales.add.success')
                });
            }),
            switchMap(() => this.languageService.get()),
            map((languages) => this.setLocales(languages)),
            catchError((error: HttpErrorResponse) => {
                this.dotHttpErrorManagerService.handle(error);

                return of(null);
            })
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
        super({
            status: ComponentStatus.IDLE,
            locales: [],
            initialLocales: [],
            countries: [],
            languages: [],
            isEnterprise: false,
            pushPublishEnvironments: []
        });
    }

    /**
     * Private function to process the languages into the format needed for the state
     */
    private processLanguages(
        languages: DotLanguage[],
        isEnterprise: boolean,
        pushPublishEnvironments: DotEnvironment[]
    ): DotLocaleRow[] {
        return languages.map((language) => ({
            id: language.id,
            locale: `${language.language} (${language.isoCode})`,
            language: `${language.language} - ${language.languageCode}`,
            country: `${language.country} - ${language.countryCode}`,
            variables: 'TBD',
            defaultLanguage: language.defaultLanguage,
            actions: [
                {
                    menuItem: {
                        label: this.dotMessageService.get('locales.edit'),
                        command: () => {
                            this.loadDialog(language.id);
                        }
                    }
                },
                {
                    menuItem: {
                        label: this.dotMessageService.get('locales.push.publish'),
                        command: () => {
                            this.dotPushPublishDialogService.open({
                                assetIdentifier: language.id.toString(),
                                title: this.dotMessageService.get(
                                    'contenttypes.content.push_publish'
                                )
                            });
                        }
                    },
                    shouldShow: () => isEnterprise && pushPublishEnvironments.length > 0
                },
                {
                    menuItem: {
                        label: this.dotMessageService.get('locales.make.default'),
                        command: () => {
                            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                        }
                    },
                    shouldShow: () => !language.defaultLanguage
                },
                {
                    menuItem: {
                        label: this.dotMessageService.get('locales.delete'),
                        command: () => {
                            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                        }
                    }
                }
            ]
        }));
    }
}
