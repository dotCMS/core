import { Injectable } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ComponentStore } from '@ngrx/component-store';
import { Observable, zip } from 'rxjs';
import { pluck, switchMap, take, tap } from 'rxjs/operators';
import * as _ from 'lodash';

import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotContainerMap } from '@models/container/dot-container.model';
import { DotLayout, DotTemplate } from '@models/dot-edit-layout-designer';

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
}

interface DotTemplateItemadvanced {
    body: string;
    drawed?: boolean;
    friendlyName: string;
    identifier: string;
    title: string;
    type?: 'advanced';
}

export type DotTemplateItem = DotTemplateItemDesign | DotTemplateItemadvanced;

export interface DotTemplateState {
    original: DotTemplateItem;
    working?: DotTemplateItem;
    type?: DotTemplateType;
    apiLink: string;
}

const EMPTY_TEMPLATE = {
    identifier: '',
    title: '',
    friendlyName: ''
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
    theme: 'd7b0ebc2-37ca-4a5a-b769-e8a3ff187661', // TODO: use theme selector
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
        let value;

        if (original.type === 'design') {
            value = { ...original };
            delete value.containers;
        }

        return {
            original: value || original,
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

    readonly updateTemplate = this.updater<DotTemplate>(
        (state: DotTemplateState, template: DotTemplate) => {
            const type: DotTemplateType = template.drawed ? 'design' : 'advanced';

            if (type === 'design') {
                this.templateContainersCacheService.set(template.containers);
            }

            const item = this.getTemplateItem(template);

            return {
                ...state,
                working: item,
                original: item
            };
        }
    );

    readonly saveTemplate = this.effect((origin$: Observable<DotTemplateItem>) => {
        return origin$.pipe(
            switchMap((template: DotTemplateItem) => {
                delete template.type;

                if (template.type === 'design') {
                    delete template.containers;
                }
                return this.dotTemplateService.update(template as DotTemplate);
            }),
            tap((template: DotTemplate) => {
                this.updateTemplate(template);
            })
        );
    });

    readonly createTemplate = this.effect<DotTemplateItem>(
        (origin$: Observable<DotTemplateItem>) => {
            return origin$.pipe(
                switchMap((template: DotTemplateItem) => {
                    console.log(template);
                    delete template.type;

                    if (template.type === 'design') {
                        delete template.containers;
                    }
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
        private templateContainersCacheService: DotTemplateContainersCacheService
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

                console.log(template);

                if (template.type === 'design') {
                    this.templateContainersCacheService.set(template.containers);
                }

                this.setState({
                    original: template,
                    working: template,
                    type: fixType,
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
                drawed: true
            };
        } else {
            result = {
                type: 'advanced',
                identifier,
                title,
                friendlyName,
                body: template.body,
                drawed: false
            };
        }

        return result;
    }
}
