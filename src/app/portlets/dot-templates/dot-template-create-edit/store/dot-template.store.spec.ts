import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { of } from 'rxjs';

import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotTemplateStore } from './dot-template.store';
import { DotRouterService } from '@services/dot-router/dot-router.service';

describe('DotTemplateStore', () => {
    let service: DotTemplateStore;
    let dotTemplateContainersCacheService: DotTemplateContainersCacheService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                DotTemplateStore,
                {
                    provide: DotTemplatesService,
                    useValue: {
                        get: jasmine.createSpy()
                    }
                },
                {
                    provide: DotRouterService,
                    useValue: {
                        gotoPortlet: jasmine.createSpy()
                    }
                },
                {
                    provide: DotTemplateContainersCacheService,
                    useValue: {
                        set: jasmine.createSpy()
                    }
                },
                {
                    provide: ActivatedRoute,
                    useValue: {
                        data: of({
                            template: undefined
                        }),
                        params: of({
                            type: 'create'
                        })
                    }
                }
            ]
        });
        service = TestBed.inject(DotTemplateStore);
        dotTemplateContainersCacheService = TestBed.inject(DotTemplateContainersCacheService);
    });

    describe('create', () => {
        it('should have basic state', (done) => {
            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    original: {
                        identifier: '',
                        title: '',
                        friendlyName: '',
                        type: 'design',
                        layout: {
                            header: true,
                            footer: true,
                            body: { rows: [] },
                            sidebar: null,
                            title: '',
                            width: null
                        },
                        theme: 'd7b0ebc2-37ca-4a5a-b769-e8a3ff187661',
                        drawed: true
                    },
                    apiLink: ''
                });
                done();
            });
        });

        it('should call set in DotTemplateContainersCacheService', () => {
            expect(dotTemplateContainersCacheService.set).toHaveBeenCalledWith({});
        });
    });
});
