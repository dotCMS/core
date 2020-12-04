import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { of } from 'rxjs';

import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotTemplateStore } from './dot-template.store';
import { DotRouterService } from '@services/dot-router/dot-router.service';

function getTemplate({ identifier, name, body }) {
    return {
        body: body || '',
        canPublish: true,
        canRead: true,
        canWrite: true,
        categoryId: '715ac8f7-10af-4a9c-90a1-56f27039be55',
        containers: {},
        countAddContainer: 0,
        countContainers: 0,
        deleted: false,
        drawed: false,
        drawedBody: null,
        footer: null,
        friendlyName: '',
        hasLiveVersion: false,
        headCode: null,
        header: null,
        identifier: identifier,
        image: null,
        inode: '715ac8f7-10af-4a9c-90a1-56f27039be55',
        layout: null,
        live: false,
        locked: false,
        lockedBy: null,
        modDate: 1606337558564,
        modUser: 'dotcms.org.1',
        name: name,
        new: false,
        owner: 'dotcms.org.1',
        selectedimage: '',
        showOnMenu: true,
        sortOrder: 0,
        theme: null,
        themeName: null,
        title: name,
        working: true
    };
}

const cacheSetSpy = jasmine.createSpy();

const BASIC_PROVIDERS = [
    DotTemplateStore,
    {
        provide: DotTemplatesService,
        useValue: {
            create: jasmine.createSpy().and.returnValue(
                of(
                    getTemplate({
                        identifier: '222-3000-333---30303-394',
                        name: 'Created template',
                        body: '<h4>Hi you</h1>'
                    })
                )
            ),
            update: jasmine.createSpy().and.returnValue(
                of(
                    getTemplate({
                        identifier: '222-3000-333---30303-394',
                        name: 'Updated template',
                        body: '<h4>Hi you</h1>'
                    })
                )
            )
        }
    },
    {
        provide: DotRouterService,
        useValue: {
            goToEditTemplate: jasmine.createSpy(),
            gotoPortlet: jasmine.createSpy()
        }
    },
    {
        provide: DotTemplateContainersCacheService,
        useValue: {
            set: cacheSetSpy
        }
    }
];

describe('DotTemplateStore', () => {
    let service: DotTemplateStore;
    let dotTemplateContainersCacheService: DotTemplateContainersCacheService;
    let dotRouterService: DotRouterService;
    let dotTemplatesService: DotTemplatesService;

    afterEach(() => {
        cacheSetSpy.calls.reset();
    });

    describe('create', () => {
        beforeEach(() => {
            TestBed.configureTestingModule({
                providers: [
                    ...BASIC_PROVIDERS,
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            data: of({
                                template: undefined
                            }),
                            params: of({
                                type: 'design'
                            })
                        }
                    }
                ]
            });
            service = TestBed.inject(DotTemplateStore);
            dotTemplateContainersCacheService = TestBed.inject(DotTemplateContainersCacheService);
            dotRouterService = TestBed.inject(DotRouterService);
            dotTemplatesService = TestBed.inject(DotTemplatesService);
        });

        it('should have basic state', (done) => {
            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    original: {
                        containers: {},
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
                        drawed: true,
                        selectedimage: ''
                    },
                    apiLink: ''
                });
                done();
            });
        });

        it('should call set in DotTemplateContainersCacheService', () => {
            expect(dotTemplateContainersCacheService.set).toHaveBeenCalledWith({});
        });

        describe('effects', () => {
            it('should create template', () => {
                service.createTemplate({
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });
                expect(dotRouterService.goToEditTemplate).toHaveBeenCalledWith(
                    '222-3000-333---30303-394'
                );
                expect<any>(dotTemplatesService.create).toHaveBeenCalledWith({
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });
            });
        });
    });

    describe('edit', () => {
        beforeEach(() => {
            TestBed.configureTestingModule({
                providers: [
                    ...BASIC_PROVIDERS,
                    {
                        provide: ActivatedRoute,
                        useValue: {
                            data: of({
                                template: getTemplate({
                                    identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                                    name: 'Advaced',
                                    body: ''
                                })
                            }),
                            params: of({
                                type: undefined
                            })
                        }
                    }
                ]
            });
            service = TestBed.inject(DotTemplateStore);
            dotTemplateContainersCacheService = TestBed.inject(DotTemplateContainersCacheService);
            dotRouterService = TestBed.inject(DotRouterService);
            dotTemplatesService = TestBed.inject(DotTemplatesService);
        });

        it('should have basic state', (done) => {
            service.vm$.subscribe((res) => {
                expect(res).toEqual({
                    original: {
                        identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                        title: 'Advaced',
                        friendlyName: '',
                        type: 'advanced',
                        drawed: false,
                        body: '',
                        selectedimage: ''
                    },
                    apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                });
                done();
            });
        });

        it('should call set in DotTemplateContainersCacheService', () => {
            expect(dotTemplateContainersCacheService.set).not.toHaveBeenCalled();
        });

        describe('selectors', () => {
            it('should update the didTemplateChanged$', () => {
                service.updateBody('Hello');

                service.didTemplateChanged$.subscribe((res) => {
                    expect(res).toBe(true);
                });
            });
        });

        describe('updaters', () => {
            it('should update the template', () => {
                service.updateTemplate({
                    identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                    title: 'New advaced',
                    friendlyName: '',
                    type: 'advanced',
                    drawed: false,
                    body: '<h1>Hello</h1>'
                });

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: {
                            type: 'advanced',
                            identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                            title: 'New advaced',
                            friendlyName: '',
                            drawed: false,
                            body: '<h1>Hello</h1>'
                        },
                        original: {
                            type: 'advanced',
                            identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                            title: 'New advaced',
                            friendlyName: '',
                            drawed: false,
                            body: '<h1>Hello</h1>'
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });
            it('should update the body', () => {
                service.updateBody('<h3>Hola Mundo</h3>');

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: {
                            identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                            title: 'Advaced',
                            friendlyName: '',
                            type: 'advanced',
                            drawed: false,
                            body: '<h3>Hola Mundo</h3>',
                            selectedimage: ''
                        },
                        original: {
                            identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                            title: 'Advaced',
                            friendlyName: '',
                            type: 'advanced',
                            drawed: false,
                            body: '',
                            selectedimage: ''
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });
        });

        describe('effects', () => {
            it('should update template and update the state', () => {
                service.saveTemplate({
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });

                expect<any>(dotTemplatesService.update).toHaveBeenCalledWith({
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Updated template',
                            friendlyName: '',
                            drawed: false,
                            body: '<h4>Hi you</h1>',
                            selectedimage: ''
                        },
                        original: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Updated template',
                            friendlyName: '',
                            drawed: false,
                            body: '<h4>Hi you</h1>',
                            selectedimage: ''
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });
        });
    });
});
