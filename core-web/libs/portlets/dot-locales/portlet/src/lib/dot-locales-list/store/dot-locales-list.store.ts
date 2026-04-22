import { ComponentStore } from '@ngrx/component-store';
import { tapResponse } from '@ngrx/operators';
import { forkJoin } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';

import { switchMap, take, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotLanguagesService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    DotAddLanguage,
    DotEnvironment,
    DotISOItem,
    DotLanguage,
    DotLanguagesISO
} from '@dotcms/dotcms-models';

export type DotLocaleRow = DotLanguage;

export interface DotLocalesListState extends DotLanguagesISO {
    status: ComponentStatus;
    locales: DotLocaleRow[];
    isEnterprise: boolean;
    pushPublishEnvironments: DotEnvironment[];
}

export interface DotLocaleListViewModel extends DotLanguagesISO {
    locales: DotLocaleRow[];
    isEnterprise: boolean;
    pushPublishEnvironments: DotEnvironment[];
}

export const LOCALE_CONFIRM_DIALOG_KEY = 'LOCALE_CONFIRM_DIALOG_KEY';

@Injectable()
export class DotLocalesListStore extends ComponentStore<DotLocalesListState> {
    private readonly languageService = inject(DotLanguagesService);
    private readonly dotMessageService = inject(DotMessageService);
    private readonly messageService = inject(MessageService);
    private readonly dotHttpErrorManagerService = inject(DotHttpErrorManagerService);

    // Updaters
    readonly setLocales = this.updater((state: DotLocalesListState, locales: DotLanguage[]) => ({
        ...state,
        locales
    }));

    readonly setEnterprise = this.updater((state: DotLocalesListState, isEnterprise: boolean) => ({
        ...state,
        isEnterprise
    }));

    readonly setPushPublishEnvironments = this.updater(
        (state: DotLocalesListState, pushPublishEnvironments: DotEnvironment[]) => ({
            ...state,
            pushPublishEnvironments
        })
    );

    readonly setCountries = this.updater((state: DotLocalesListState, countries: DotISOItem[]) => ({
        ...state,
        countries
    }));

    readonly setLanguages = this.updater((state: DotLocalesListState, languages: DotISOItem[]) => ({
        ...state,
        languages
    }));

    readonly setStatus = this.updater((state: DotLocalesListState, status: ComponentStatus) => ({
        ...state,
        status
    }));

    readonly vm$ = this.select(
        this.state$,
        ({
            locales,
            countries,
            languages,
            isEnterprise,
            pushPublishEnvironments
        }): DotLocaleListViewModel => ({
            locales,
            countries,
            languages,
            isEnterprise,
            pushPublishEnvironments
        })
    );

    // Effects
    readonly loadLocales = this.effect<{
        pushPublishEnvironments: DotEnvironment[];
        isEnterprise: boolean;
    }>((data$) => {
        return data$.pipe(
            tap(() => this.setStatus(ComponentStatus.LOADING)),
            switchMap(({ isEnterprise, pushPublishEnvironments }) =>
                forkJoin([this.languageService.get(), this.languageService.getISO()]).pipe(
                    tap(([languages, ISOData]) => {
                        this.setCountries(ISOData.countries);
                        this.setLanguages(ISOData.languages);
                        this.setEnterprise(isEnterprise);
                        this.setPushPublishEnvironments(pushPublishEnvironments);
                        this.setLocales(languages);
                        this.setStatus(ComponentStatus.IDLE);
                    })
                )
            )
        );
    });

    readonly addLocale = this.effect<DotAddLanguage>((locale$) => {
        return locale$.pipe(
            tap(() => this.setStatus(ComponentStatus.LOADING)),
            switchMap((locale) =>
                this.languageService.add(locale).pipe(
                    take(1),
                    tapResponse({
                        next: () => this.updateListAndNotify(),
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error)
                    })
                )
            )
        );
    });

    readonly updateLocale = this.effect<DotLanguage>((locale$) => {
        return locale$.pipe(
            tap(() => this.setStatus(ComponentStatus.LOADING)),
            switchMap((locale) =>
                this.languageService.update(locale).pipe(
                    take(1),
                    tapResponse({
                        next: () => this.updateListAndNotify(),
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error)
                    })
                )
            )
        );
    });

    readonly makeDefaultLocale = this.effect<number>((localeId$) => {
        return localeId$.pipe(
            tap(() => this.setStatus(ComponentStatus.LOADING)),
            switchMap((localeId) =>
                this.languageService.makeDefault(localeId).pipe(
                    take(1),
                    tapResponse({
                        next: () => this.updateListAndNotify(),
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error)
                    })
                )
            )
        );
    });

    readonly deleteLocale = this.effect<number>((languageId$) =>
        languageId$.pipe(
            tap(() => this.setStatus(ComponentStatus.LOADING)),
            switchMap((languageId) =>
                this.languageService.delete(languageId).pipe(
                    take(1),
                    tapResponse({
                        next: () => this.updateListAndNotify(true),
                        error: (error: HttpErrorResponse) =>
                            this.dotHttpErrorManagerService.handle(error)
                    })
                )
            )
        )
    );

    constructor() {
        super({
            status: ComponentStatus.IDLE,
            locales: [],
            countries: [],
            languages: [],
            isEnterprise: false,
            pushPublishEnvironments: []
        });
    }

    private updateListAndNotify(isDelete = false) {
        this.languageService
            .get()
            .pipe(
                take(1),
                tapResponse({
                    next: (languages) => {
                        this.setLocales(languages);
                        if (isDelete) {
                            this.messageService.add({
                                severity: 'info',
                                summary: this.dotMessageService.get(
                                    'locale.delete.confirmation.notification.title'
                                ),
                                detail: this.dotMessageService.get(
                                    'locale.delete.confirmation.notification.message'
                                )
                            });
                        } else {
                            this.messageService.add({
                                severity: 'success',
                                summary: this.dotMessageService.get(
                                    'locale.notification.success.title'
                                ),
                                detail: this.dotMessageService.get(
                                    'locale.notification.success.message'
                                )
                            });
                        }
                        this.setStatus(ComponentStatus.IDLE);
                    },
                    error: (error: HttpErrorResponse) =>
                        this.dotHttpErrorManagerService.handle(error)
                })
            )
            .subscribe();
    }
}
