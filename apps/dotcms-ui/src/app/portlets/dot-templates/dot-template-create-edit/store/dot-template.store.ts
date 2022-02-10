import { Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ComponentStore } from '@ngrx/component-store';
import { Observable, zip, of } from 'rxjs';
import { pluck, switchMap, take, tap, catchError } from 'rxjs/operators';
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
    title: string;
    type?: 'advanced';
    image?: string;
}

export type DotTemplateItem = DotTemplateItemDesign | DotTemplateItemadvanced;

export interface DotTemplateState {
    original: DotTemplateItem;
    working?: DotTemplateItem;
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
    readonly vm$ = this.select(({ original, apiLink }: DotTemplateState) => {
        return {
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

    readonly updateTemplate = this.updater<DotTemplateItem>(
        (state: DotTemplateState, template: DotTemplateItem) => {
            return {
                ...state,
                working: template,
                original: template
            };
        }
    );

    readonly saveTemplate = this.effect((origin$: Observable<DotTemplateItem>) => {

        return origin$.pipe(
            switchMap((template: DotTemplateItem) => {
                this.dotGlobalMessageService.loading(
                    this.dotMessageService.get('dot.common.message.saving')
                );
              return this.persistTemplate(template)
            }),
            tap((template: DotTemplate) => {
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

    readonly saveProperties = this.effect((origin$: Observable<DotTemplateItem>) => {
        return origin$.pipe(
            switchMap((template: DotTemplateItem) => this.persistTemplate(template)),
            tap((template: DotTemplate) => {
                this.updateTemplate(this.getTemplateItem(template));
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

        zip(template$, type$)
            .pipe(take(1))
            .subscribe(([dotTemplate, type]: [DotTemplate, string]) => {
                const fixType = type as DotTemplateType;
                const isAdvanced = this.getIsAdvanced(fixType, dotTemplate?.drawed);

                const template = dotTemplate
                    ? this.getTemplateItem(dotTemplate)
                    : this.getDefaultTemplate(isAdvanced);

                if (template.type === 'design') {
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

    private persistTemplate(template: DotTemplateItem): Observable<DotTemplate> {
        delete template.type;
        if (template.type === 'design') {
            delete template.containers;
        }
        return this.dotTemplateService.update(template as DotTemplate);
    }
}
