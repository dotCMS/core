import { ComponentStore } from '@ngrx/component-store';

import { HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

import { MessageService } from 'primeng/api';
import { DialogService, DynamicDialogRef } from 'primeng/dynamicdialog';

import { catchError, filter, switchMap, take, tap } from 'rxjs/operators';

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

import { DotLocaleConfirmationDialogComponent } from '../../share/ui/DotLocaleConfirmationDialog/DotLocaleConfirmationDialog.component';
import { getLocaleISOCode } from '../../share/utils';

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

export interface DotLocaleListViewModel extends DotLanguagesISO {
    locales: DotLocaleRow[];
    initialLocales: DotLanguage[];
}

export interface DotLocaleListResolverData {
    localaes: DotLanguage[];
    languages: { code: string; name: string }[];
    countries: { code: string; name: string }[];
}

export const LOCALE_CONFIRM_DIALOG_KEY = 'LOCALE_CONFIRM_DIALOG_KEY';

@Injectable()
export class DotLocalesListStore extends ComponentStore<DotLocalesListState> {
    private readonly dialogService = inject(DialogService);
    private readonly languageService = inject(DotLanguagesService);
    private readonly dotMessageService = inject(DotMessageService);
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
        ({ locales, countries, languages, initialLocales }): DotLocaleListViewModel => ({
            locales,
            initialLocales,
            countries,
            languages
        })
    );

    //Effects
    readonly makeDefaultLocale = this.effect<number>((languageId$) => {
        return languageId$.pipe(
            switchMap((languageId) => this.languageService.makeDefault(languageId)),
            switchMap(() => this.languageService.get()),
            tap((languages) => {
                this.setLocales(languages);
                this.messageService.add({
                    severity: 'success',
                    summary: this.dotMessageService.get('locale.notification.success.title'),
                    detail: this.dotMessageService.get('locale.notification.success.message')
                });
            }),
            catchError((error: HttpErrorResponse) => this.dotHttpErrorManagerService.handle(error))
        );
    });

    readonly deleteLocale = this.effect<number>((languageId$) =>
        languageId$.pipe(
            switchMap((languageId) => this.languageService.delete(languageId)),
            switchMap(() => this.languageService.get()),
            tap((languages) => {
                this.setLocales(languages);
                this.messageService.add({
                    severity: 'info',
                    summary: this.dotMessageService.get(
                        'locale.delete.confirmation.notification.title'
                    ),
                    detail: this.dotMessageService.get(
                        'locale.delete.confirmation.notification.message'
                    )
                });
            }),
            catchError((error: HttpErrorResponse) => this.dotHttpErrorManagerService.handle(error))
        )
    );

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
        locales: DotLanguage[],
        isEnterprise: boolean,
        pushPublishEnvironments: DotEnvironment[]
    ): DotLocaleRow[] {
        return locales.map((locale) => ({
            id: locale.id,
            locale: `${locale.language} (${locale.isoCode})`,
            language: `${locale.language} - ${locale.languageCode}`,
            country: `${locale.country} - ${locale.countryCode}`,
            variables: 'TBD',
            defaultLanguage: locale.defaultLanguage,
            actions: [
                {
                    menuItem: {
                        label: this.dotMessageService.get('locales.edit'),
                        command: () => {
                            // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
                        }
                    }
                },
                {
                    menuItem: {
                        label: this.dotMessageService.get('locales.push.publish'),
                        command: () => {
                            this.dotPushPublishDialogService.open({
                                assetIdentifier: locale.id.toString(),
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
                        label: this.dotMessageService.get('locales.set.as.default'),
                        command: () => {
                            this.callDynamicDialog(
                                locale,
                                'locale.set.default.confirmation.title',
                                'locale.set.default.confirmation.message',
                                'locale.set.default.confirmation.accept.button',
                                () => {
                                    this.makeDefaultLocale(locale.id);
                                }
                            );
                        }
                    },
                    shouldShow: () => !locale.defaultLanguage
                },
                {
                    menuItem: {
                        label: this.dotMessageService.get('locales.delete'),
                        command: () => {
                            this.callDynamicDialog(
                                locale,
                                'locale.delete.confirmation.title',
                                'locale.delete.confirmation.message',
                                'delete',
                                () => {
                                    this.deleteLocale(locale.id);
                                }
                            );
                        }
                    },
                    shouldShow: () => !locale.defaultLanguage
                }
            ]
        }));
    }

    private callDynamicDialog(
        locale: DotLanguage,
        headerLabel: string,
        messageLabel: string,
        acceptLabel: string,
        action: () => void
    ) {
        const dialogRef: DynamicDialogRef = this.dialogService.open(
            DotLocaleConfirmationDialogComponent,
            {
                width: '38rem',
                header: this.dotMessageService.get(
                    headerLabel,
                    `${locale.language} (${getLocaleISOCode(locale)})`
                ),
                data: {
                    acceptLabel: this.dotMessageService.get(acceptLabel),
                    icon: 'pi pi-exclamation-triangle',
                    ISOCode: getLocaleISOCode(locale),
                    locale,
                    message: this.dotMessageService.get(messageLabel)
                }
            }
        );

        dialogRef.onClose
            .pipe(
                take(1),
                filter((isDelete) => isDelete)
            )
            .subscribe(action);
    }
}
