import { Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ComponentStore } from '@ngrx/component-store';
import { Observable, zip, of } from 'rxjs';
import {
    pluck,
    switchMap,
    take,
    tap,
    catchError,
    debounceTime,
    withLatestFrom,
    filter,
    map
} from 'rxjs/operators';
import * as _ from 'lodash';

import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotContainerMap } from '@models/container/dot-container.model';
import { DotLayout, DotTemplate } from '@models/dot-edit-layout-designer';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

import { HttpErrorResponse } from '@angular/common/http';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { DotEditLayoutService } from '@services/dot-edit-layout/dot-edit-layout.service';

type DotTemplateType = 'design' | 'advanced';

interface DotTemplateItemDesign {
    containers?: DotContainerMap;
    drawed?: boolean;
    friendlyName: string;
    identifier: string;
    layout: DotLayout;
    live?: boolean;
    theme: string;
    title: string;
    type?: 'design';
    image?: string;
}

interface DotTemplateItemadvanced {
    body: string;
    drawed?: boolean;
    friendlyName: string;
    identifier: string;
    live?: boolean;
    title: string;
    type?: 'advanced';
    image?: string;
}

export type DotTemplateItem = DotTemplateItemDesign | DotTemplateItemadvanced;

export interface DotTemplateState {
    original: DotTemplateItem;
    working: DotTemplateItem;
    apiLink: string;
}

const EMPTY_TEMPLATE = {
    identifier: '',
    title: '',
    friendlyName: '',
    image: ''
};

export const EMPTY_TEMPLATE_DESIGN: DotTemplateItemDesign = {
    ...EMPTY_TEMPLATE,
    type: 'design',
    layout: {
        header: true,
        footer: true,
        body: {
            rows: []
        },
        sidebar: null,
        title: '',
        width: null
    },
    theme: '',
    containers: {},
    drawed: true
};

export const EMPTY_TEMPLATE_ADVANCED: DotTemplateItemadvanced = {
    ...EMPTY_TEMPLATE,
    type: 'advanced',
    body: '',
    drawed: false
};

@Injectable()
export class DotTemplateStore extends ComponentStore<DotTemplateState> {
    readonly vm$ = this.select(({ working, original, apiLink }: DotTemplateState) => {
        return {
            working,
            original,
            apiLink
        };
    });

    readonly didTemplateChanged$: Observable<boolean> = this.select(
        ({ original, working }: DotTemplateState) => !_.isEqual(original, working)
    );

    readonly updateBody = this.updater<string>((state: DotTemplateState, body: string) => ({
        ...state,
        working: {
            ...state.working,
            body
        }
    }));

    readonly updateWorkingTemplate = this.updater<DotTemplateItem>(
        (state: DotTemplateState, template: DotTemplateItem) => {
            return {
                ...state,
                working: {
                    ...state.working,
                    ...template
                }
            };
        }
    );

    readonly updateTemplate = this.updater<DotTemplateItem>(
        (state: DotTemplateState, template: DotTemplateItem) => {
            return {
                ...state,
                working: template,
                original: template
            };
        }
    );

    readonly saveAndPublishTemplate = this.effect((origin$: Observable<DotTemplateItem>) => {
        return origin$.pipe(
            switchMap((template: DotTemplateItem) => {
                this.dotGlobalMessageService.loading(this.dotMessageService.get('publishing'));

                return this.dotTemplateService.saveAndPublish(this.cleanTemplateItem(template));
            }),
            tap((template: DotTemplate) => {
                this.dotGlobalMessageService.success(
                    this.dotMessageService.get('message.template.published')
                );
                this.updateTemplateState(template);
            }),
            catchError((err: HttpErrorResponse) => {
                this.dotGlobalMessageService.error(err.statusText);
                this.dotHttpErrorManagerService.handle(err).subscribe(() => {
                    this.dotEditLayoutService.changeDesactivateState(true);
                });

                return of(null);
            })
        );
    });

    readonly updateProperties = this.updater<DotTemplateItem>(
        (state: DotTemplateState, template: DotTemplateItem) => {
            const working = this.updateTemplateProperties(state.working, template);
            const original = this.updateTemplateProperties(state.original, template);

            return {
                ...state,
                working: working,
                original: original
            };
        }
    );

    readonly saveTemplate = this.effect((origin$: Observable<DotTemplateItem>) => {
        return origin$.pipe(
            switchMap((template: DotTemplateItem) => {
                this.dotGlobalMessageService.loading(
                    this.dotMessageService.get('dot.common.message.saving')
                );

                return this.dotTemplateService.update(this.cleanTemplateItem(template));
            }),
            tap((template: DotTemplate) => this.onSaveTemplate(template)),
            catchError((err: HttpErrorResponse) => this.onSaveTemplateError(err))
        );
    });

    readonly saveTemplateDebounce = this.effect((origin$: Observable<DotTemplateItem>) => {
        return origin$.pipe(
            debounceTime(5000),
            // If this observable is called due to a template change and then
            // we save template properties, there is not simple way to cancel
            // the debounceTime and avoid a double save.
            // So we can implement the following code.
            // More Information: https://stackoverflow.com/questions/17745478/filter-an-observable-using-values-from-another-observable
            withLatestFrom(this.didTemplateChanged$),
            filter(([, didTemplateChanged]: [DotTemplateItem, boolean]) => didTemplateChanged),
            map(([template]: [DotTemplateItem, boolean]) => template),
            switchMap((template: DotTemplateItem) => {
                this.dotGlobalMessageService.loading(
                    this.dotMessageService.get('dot.common.message.saving')
                );

                return this.dotTemplateService.update(this.cleanTemplateItem(template));
            }),
            tap((template: DotTemplate) => this.onSaveTemplate(template)),
            catchError((err: HttpErrorResponse) => this.onSaveTemplateError(err))
        );
    });

    readonly saveWorkingTemplate = this.effect((working$: Observable<DotTemplateItem>) => {
        return working$.pipe(
            tap((template: DotTemplateItem) => {
                if (template.type === 'design') {
                    // Design templates need to be save 10 seconds after the last change.
                    this.saveTemplateDebounce(template);
                }

                this.updateWorkingTemplate(template);
            })
        );
    });

    readonly saveProperties = this.effect((origin$: Observable<DotTemplateItem>) => {
        return origin$.pipe(
            switchMap((template: DotTemplateItem) =>
                this.dotTemplateService.update(this.cleanTemplateItem(template))
            ),
            tap((template: DotTemplate) => {
                this.updateProperties(this.getTemplateItem(template));
            })
        );
    });

    readonly createTemplate = this.effect<DotTemplateItem>(
        (origin$: Observable<DotTemplateItem>) => {
            return origin$.pipe(
                switchMap((template: DotTemplateItem) => {
                    if (template.type === 'design') {
                        delete template.containers;
                    }

                    delete template.type;

                    return this.dotTemplateService.create(template as DotTemplate);
                }),
                tap(({ identifier }: DotTemplate) => {
                    this.dotRouterService.goToEditTemplate(identifier);
                })
            );
        }
    );

    constructor(
        private dotTemplateService: DotTemplatesService,
        private dotRouterService: DotRouterService,
        private activatedRoute: ActivatedRoute,
        private dotHttpErrorManagerService: DotHttpErrorManagerService,
        private dotEditLayoutService: DotEditLayoutService,
        private templateContainersCacheService: DotTemplateContainersCacheService,
        private dotGlobalMessageService: DotGlobalMessageService,
        private dotMessageService: DotMessageService
    ) {
        super(null);

        const template$ = this.activatedRoute.data.pipe(pluck('template'));
        const type$ = this.activatedRoute.params.pipe(pluck('type'));

        // If it is a system template, redirect to template
        const templateId = this.activatedRoute.snapshot.params['id'];
        if (templateId === 'SYSTEM_TEMPLATE') {
            this.goToTemplateList();

            return;
        }

        zip(template$, type$)
            .pipe(take(1))
            .subscribe(([dotTemplate, type]: [DotTemplate, string]) => {
                const fixType = type as DotTemplateType;
                const isAdvanced = this.getIsAdvanced(fixType, dotTemplate?.drawed);

                const template = dotTemplate
                    ? this.getTemplateItem(dotTemplate)
                    : this.getDefaultTemplate(isAdvanced);

                if (template.type === 'design') {
                    this.canRouteBeDesativated();
                    this.templateContainersCacheService.set(template.containers);
                }

                this.setState({
                    original: template,
                    working: template,
                    apiLink: this.getApiLink(template?.identifier)
                });
            });
    }

    /**
     * Redirect to template listing
     *
     * @memberof DotTemplateStore
     */
    goToTemplateList = () => {
        this.dotRouterService.gotoPortlet('templates');
    };

    /**
     * Redirect to edit specific version of a template.
     *
     * @memberof DotTemplateStore
     */
    goToEditTemplate = (id, inode) => {
        this.dotRouterService.goToEditTemplate(id, inode);
    };

    private onSaveTemplate(template: DotTemplate) {
        if (template.drawed) {
            this.templateContainersCacheService.set(template.containers);
        }

        this.updateTemplate(this.getTemplateItem(template));
        this.dotGlobalMessageService.success(
            this.dotMessageService.get('dot.common.message.saved')
        );
        if (this.activatedRoute?.snapshot?.params['inode']) {
            this.dotRouterService.goToEditTemplate(template.identifier);
        }
    }

    private onSaveTemplateError(err: HttpErrorResponse) {
        this.dotGlobalMessageService.error(err.statusText);
        this.dotHttpErrorManagerService.handle(err).subscribe(() => {
            this.dotEditLayoutService.changeDesactivateState(true);
        });

        return of(null);
    }

    private getApiLink(identifier: string): string {
        return identifier ? `/api/v1/templates/${identifier}/working` : '';
    }

    private getDefaultTemplate(isAdvanced: boolean): DotTemplateItem {
        return isAdvanced ? EMPTY_TEMPLATE_ADVANCED : EMPTY_TEMPLATE_DESIGN;
    }

    private getIsAdvanced(type: DotTemplateType, drawed: boolean): boolean {
        return type === 'advanced' || drawed === false;
    }

    private getTemplateItem(template: DotTemplate): DotTemplateItem {
        const { identifier, title, friendlyName } = template;

        let result: DotTemplateItem;

        if (template.drawed) {
            result = {
                type: 'design',
                identifier,
                title,
                friendlyName,
                layout: template.layout || EMPTY_TEMPLATE_DESIGN.layout,
                theme: template.theme,
                containers: template.containers,
                drawed: true,
                live: template.live,
                image: template.image
            };
        } else {
            result = {
                type: 'advanced',
                identifier,
                title,
                friendlyName,
                body: template.body,
                drawed: false,
                image: template.image
            };
        }

        return result;
    }

    /**
     * Let the user leave the route only when changes have been saved on design template.
     *
     * @private
     * @memberof DotTemplateStore
     */
    private canRouteBeDesativated(): void {
        this.didTemplateChanged$.subscribe((didTemplateChanged: boolean) =>
            this.dotEditLayoutService.changeDesactivateState(!didTemplateChanged)
        );
    }

    /**
     *
     * When we save the properties, we do not want to save the body/layout.
     * Therefore, we keep the same body/layout of the working template
     *
     * @private
     * @param {DotTemplateItem} currentTemplate
     * @param {DotTemplateItem} template
     * @return {*}  {DotTemplateItem}
     * @memberof DotTemplateStore
     */
    private updateTemplateProperties(
        templateState: DotTemplateItem,
        newPropertiesTemplate: DotTemplateItem
    ): DotTemplateItem {
        if (newPropertiesTemplate.type === 'design') {
            return {
                ...newPropertiesTemplate,
                layout: (templateState as DotTemplateItemDesign).layout
            };
        }

        return {
            ...newPropertiesTemplate,
            body: (templateState as DotTemplateItemadvanced).body
        };
    }

    private cleanTemplateItem(template: DotTemplateItem): DotTemplate {
        delete template.type;
        if (template.type === 'design') {
            delete template.containers;
        }

        return template as DotTemplate;
    }

    private updateTemplateState(template: DotTemplate): void {
        if (template.drawed) {
            this.templateContainersCacheService.set(template.containers);
        }

        this.updateTemplate(this.getTemplateItem(template));
        if (this.activatedRoute?.snapshot?.params['inode']) {
            this.dotRouterService.goToEditTemplate(template.identifier);
        }
    }
}
