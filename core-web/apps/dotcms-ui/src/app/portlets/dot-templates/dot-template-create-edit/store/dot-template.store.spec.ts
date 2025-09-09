/* eslint-disable @typescript-eslint/no-explicit-any */

import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import {
    DotHttpErrorManagerService,
    DotMessageService,
    DotRouterService,
    DotGlobalMessageService
} from '@dotcms/data-access';
import {
    MockDotMessageService,
    MockDotRouterService,
    mockResponseView
} from '@dotcms/utils-testing';

import { DotTemplateItem, DotTemplateStore } from './dot-template.store';

import { DotTemplateContainersCacheService } from '../../../../api/services/dot-template-containers-cache/dot-template-containers-cache.service';
import { DotTemplatesService } from '../../../../api/services/dot-templates/dot-templates.service';

const messageServiceMock = new MockDotMessageService({
    'dot.common.message.saved': 'saved',
    'dot.common.message.saving': 'saving',
    publishing: 'publishing',
    'message.template.published': 'published'
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

const cacheSetSpy = jest.fn();

const BASIC_PROVIDERS = [
    DotTemplateStore,
    {
        provide: DotHttpErrorManagerService,
        useValue: {
            handle: jest.fn().mockReturnValue(of({}))
        }
    },
    {
        provide: DotTemplatesService,
        useValue: {
            create: jest.fn().mockReturnValue(
                of(
                    getTemplate({
                        identifier: '222-3000-333---30303-394',
                        name: 'Created template',
                        body: '<h4>Hi you</h1>'
                    })
                )
            ),
            update: jest.fn().mockReturnValue(
                of(
                    getTemplate({
                        identifier: '222-3000-333---30303-394',
                        name: 'Updated template',
                        body: '<h4>Hi you</h1>'
                    })
                )
            ),
            saveAndPublish: jest.fn().mockReturnValue(
                of(
                    getTemplate({
                        identifier: '222-3000-333---30303-394',
                        name: 'Saved and published template',
                        body: '<h4>Hi you</h1>'
                    })
                )
            )
        }
    },
    {
        provide: DotRouterService,
        useValue: new MockDotRouterService()
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
            loading: jest.fn(),
            success: jest.fn(),
            error: jest.fn()
        }
    }
];

describe('DotTemplateStore', () => {
    let service: DotTemplateStore;
    let dotTemplateContainersCacheService: DotTemplateContainersCacheService;
    let dotRouterService: DotRouterService;
    let dotTemplatesService: DotTemplatesService;
    let dotGlobalMessageService: DotGlobalMessageService;
    let dotHttpErrorManagerService: DotHttpErrorManagerService;

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
                            snapshot: { params: { id: '222-3000-333---30303-394' } },
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
            dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
            dotTemplatesService.update = jest.fn().mockReturnValue(
                of(
                    getTemplate({
                        identifier: '222-3000-333---30303-394',
                        name: 'Updated template',
                        body: '<h4>Hi you</h1>'
                    })
                )
            );
        });

        it('should have basic state', (done) => {
            const template: DotTemplateItem = {
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
            };

            const state = {
                original: template,
                working: template,
                apiLink: '',
                didTemplateChanged: false
            };

            service.vm$.subscribe((res) => {
                expect(res).toEqual(state);
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
                                    name: 'Advanced',
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
            dotHttpErrorManagerService = TestBed.inject(DotHttpErrorManagerService);
            dotTemplatesService.update = jest.fn().mockReturnValue(
                of(
                    getTemplate({
                        identifier: '222-3000-333---30303-394',
                        name: 'Updated template',
                        body: '<h4>Hi you</h1>'
                    })
                )
            );
        });

        it('should have basic state', (done) => {
            const template: DotTemplateItem = {
                identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                title: 'Advanced',
                friendlyName: '',
                type: 'advanced',
                drawed: false,
                body: '',
                image: ''
            };

            const state = {
                original: template,
                working: template,
                apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working',
                didTemplateChanged: false
            };

            service.vm$.subscribe((res) => {
                expect(res).toEqual(state);
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
                    live: true,
                    body: '<h1>Hello</h1>'
                });

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: {
                            type: 'advanced',
                            identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                            title: 'New advaced',
                            friendlyName: '',
                            live: true,
                            drawed: false,
                            body: '<h1>Hello</h1>'
                        },
                        original: {
                            type: 'advanced',
                            identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                            title: 'New advaced',
                            friendlyName: '',
                            live: true,
                            drawed: false,
                            body: '<h1>Hello</h1>'
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });

            it('should update only the wokring template', () => {
                service.updateWorkingTemplate({
                    identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                    title: 'New advaced',
                    friendlyName: '',
                    type: 'advanced',
                    drawed: false,
                    body: '<h1>Hello</h1>',
                    image: ''
                });

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: {
                            type: 'advanced',
                            identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                            title: 'New advaced',
                            friendlyName: '',
                            drawed: false,
                            body: '<h1>Hello</h1>',
                            image: ''
                        },
                        original: {
                            type: 'advanced',
                            identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                            title: 'Advanced',
                            friendlyName: '',
                            drawed: false,
                            body: '',
                            image: ''
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });

            it('should update template properties', () => {
                service.updateTemplate({
                    identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                    title: 'New advaced',
                    friendlyName: '',
                    type: 'advanced',
                    drawed: false,
                    image: 'image',
                    body: ''
                });

                const tamplate: DotTemplateItem = {
                    type: 'advanced',
                    identifier: '23423-234as-sd-w3sd-sd-srzcxsd',
                    title: 'New advaced',
                    friendlyName: '',
                    drawed: false,
                    image: 'image',
                    body: ''
                };

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: tamplate,
                        original: tamplate,
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
                            title: 'Advanced',
                            friendlyName: '',
                            type: 'advanced',
                            drawed: false,
                            body: '<h3>Hola Mundo</h3>',
                            image: ''
                        },
                        original: {
                            identifier: '2d87af36-a935-4689-b427-dea75e9d84cf',
                            title: 'Advanced',
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

            it('should update template and update the state after 10 seconds if template has changed', fakeAsync(() => {
                const newTemplate = {
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                };

                service.updateWorkingTemplate(newTemplate);
                service.saveTemplateDebounce(newTemplate);

                tick(10000);

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
            }));

            it('should save and publish template and update the state', () => {
                service.saveAndPublishTemplate({
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });

                expect<any>(dotTemplatesService.saveAndPublish).toHaveBeenCalledWith({
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });

                expect(dotGlobalMessageService.loading).toHaveBeenCalledWith('publishing');
                expect(dotGlobalMessageService.success).toHaveBeenCalledWith('published');
                expect(dotRouterService.goToEditTemplate).toHaveBeenCalledWith(
                    '222-3000-333---30303-394'
                );

                service.state$.subscribe((res) => {
                    expect(res).toEqual({
                        working: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Saved and published template',
                            friendlyName: '',
                            drawed: false,
                            body: '<h4>Hi you</h1>',
                            image: ''
                        },
                        original: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Saved and published template',
                            friendlyName: '',
                            drawed: false,
                            body: '<h4>Hi you</h1>',
                            image: ''
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });

            it('should call updateWorkingTemplate and call saveTemplateDebounce when is a design template', () => {
                jest.spyOn(service, 'updateWorkingTemplate');
                jest.spyOn(service, 'saveTemplateDebounce');
                service.saveWorkingTemplate({
                    type: 'design',
                    layout: {
                        header: true,
                        footer: true,
                        body: { rows: [] },
                        sidebar: null,
                        title: '',
                        width: null
                    },
                    theme: '123',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });

                expect(service.updateWorkingTemplate).toHaveBeenCalled();
                expect(service.saveTemplateDebounce).toHaveBeenCalled();
            });
            it('should call updateWorkingTemplate and not call saveTemplateDebounce when is a advanced template', () => {
                jest.spyOn(service, 'updateWorkingTemplate');
                jest.spyOn(service, 'saveTemplateDebounce');
                service.saveWorkingTemplate({
                    type: 'advanced',
                    body: '',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });

                expect(service.updateWorkingTemplate).toHaveBeenCalled();
                expect(service.saveTemplateDebounce).not.toHaveBeenCalled();
            });

            it('should handle error on update template', (done) => {
                const error = throwError(new HttpErrorResponse(mockResponseView(400)));
                dotTemplatesService.update = jest.fn().mockReturnValue(error);
                service.saveTemplate({
                    body: 'string',
                    friendlyName: 'string',
                    identifier: 'string',
                    title: 'string'
                });
                expect(dotGlobalMessageService.error).toHaveBeenCalledWith('Unknown Error');
                expect(dotHttpErrorManagerService.handle).toHaveBeenCalledTimes(1);
                dotRouterService.canDeactivateRoute$.subscribe((resp) => {
                    expect(resp).toBeTruthy();
                    done();
                });
            });

            it('should not update template body when updates props', () => {
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
                            body: '',
                            image: ''
                        },
                        original: {
                            type: 'advanced',
                            identifier: '222-3000-333---30303-394',
                            title: 'Updated template',
                            friendlyName: '',
                            drawed: false,
                            body: '',
                            image: ''
                        },
                        apiLink: '/api/v1/templates/2d87af36-a935-4689-b427-dea75e9d84cf/working'
                    });
                });
            });
        });
    });

    describe('redirect', () => {
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
                            snapshot: { params: { id: 'SYSTEM_TEMPLATE' } },
                            params: of({
                                id: 'SYSTEM_TEMPLATE',
                                type: 'design'
                            })
                        }
                    }
                ]
            });
            service = TestBed.inject(DotTemplateStore);
            dotRouterService = TestBed.inject(DotRouterService);
        });

        it('Should redirect to templates listing when trying to edit a SYSTEM_TEMPALTE', () => {
            expect(dotRouterService.gotoPortlet).toHaveBeenCalledWith('templates');
        });
    });
});
