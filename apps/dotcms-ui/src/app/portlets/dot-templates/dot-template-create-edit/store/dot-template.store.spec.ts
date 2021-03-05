import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { of } from 'rxjs';

import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotTemplatesService } from '@services/dot-templates/dot-templates.service';
import { DotTemplateStore } from './dot-template.store';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotGlobalMessageService } from '@components/_common/dot-global-message/dot-global-message.service';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.saved': 'saved',
    'dot.common.message.saving': 'saving'
});

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
        image: '',
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
    },
    {
        provide: DotMessageService,
        useValue: messageServiceMock
    },
    {
        provide: DotGlobalMessageService,
        useValue: {
            loading: jasmine.createSpy(),
            success: jasmine.createSpy()
        }
    }
];

describe('DotTemplateStore', () => {
    let service: DotTemplateStore;
    let dotTemplateContainersCacheService: DotTemplateContainersCacheService;
    let dotRouterService: DotRouterService;
    let dotTemplatesService: DotTemplatesService;
    let dotGlobalMessageService: DotGlobalMessageService;

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
                        theme: '',
                        drawed: true,
                        image: ''
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
                            }),
                            snapshot: {
                                params: {
                                    inode: 'test'
                                }
                            }
                        }
                    }
                ]
            });
            service = TestBed.inject(DotTemplateStore);
            dotTemplateContainersCacheService = TestBed.inject(DotTemplateContainersCacheService);
            dotRouterService = TestBed.inject(DotRouterService);
            dotTemplatesService = TestBed.inject(DotTemplatesService);
            dotGlobalMessageService = TestBed.inject(DotGlobalMessageService);
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
                        image: ''
                    },
                    apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                });
                done();
            });
        });

        it('should call set in DotTemplateContainersCacheService', () => {
            expect(dotTemplateContainersCacheService.set).not.toHaveBeenCalled();
        });

        it('should redirect to edit template', () => {
            service.goToEditTemplate('1', '2');
            expect(dotRouterService.goToEditTemplate).toHaveBeenCalledWith('1', '2');
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
                            image: ''
                        },
                        original: {
                            identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                            title: 'Advaced',
                            friendlyName: '',
                            type: 'advanced',
                            drawed: false,
                            body: '',
                            image: ''
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

                expect(dotGlobalMessageService.loading).toHaveBeenCalledWith('saving');
                expect(dotGlobalMessageService.success).toHaveBeenCalledWith('saved');
                expect(dotRouterService.goToEditTemplate).toHaveBeenCalledWith(
                    '222-3000-333---30303-394'
                );

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Updated template',
                            friendlyName: '',
                            drawed: false,
                            body: '<h4>Hi you</h1>',
                            image: ''
                        },
                        original: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Updated template',
                            friendlyName: '',
                            drawed: false,
                            body: '<h4>Hi you</h1>',
                            image: ''
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });
            it('should update template and update the state when updates props', () => {
                service.saveProperties({
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
                            image: ''
                        },
                        original: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Updated template',
                            friendlyName: '',
                            drawed: false,
                            body: '<h4>Hi you</h1>',
                            image: ''
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });
        });
    });
});
