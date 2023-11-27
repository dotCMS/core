import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { EditEmaStore } from './dot-ema.store';

import { DotPageApiService } from '../../services/dot-page-api.service';
import { EDIT_CONTENTLET_URL } from '../../shared/consts';

describe('EditEmaStore', () => {
    let spectator: SpectatorService<EditEmaStore>;
    const createService = createServiceFactory({
        service: EditEmaStore,
        mocks: [DotPageApiService]
    });

    beforeEach(() => (spectator = createService()));

    describe('selectors', () => {
        it('should return iframe url', (done) => {
            const dotPageApiService = spectator.inject(DotPageApiService);
            const mockResponse = {
                page: {
                    title: 'Test Page'
                }
            };
            dotPageApiService.get.andReturn(of(mockResponse));

            spectator.service.load({ language_id: 'en', url: 'test-url' });

            spectator.service.iframeUrl$.subscribe((url) => {
                expect(url).toEqual('http://localhost:3000/test-url?language_id=en');
                done();
            });
        });
    });

    describe('updaters', () => {
        it('should update url', (done) => {
            spectator.service.setURL('test-url');

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '',
                    url: 'test-url',
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: false
                });
                done();
            });
        });

        it('should update editFrameURL', (done) => {
            spectator.service.setDialogIframeURL('test-url');

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '',
                    url: '',
                    dialogIframeURL: 'test-url',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: false
                });
                done();
            });
        });

        it('should update language_id', (done) => {
            spectator.service.setLanguage('1');

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '1',
                    url: '',
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: false
                });
                done();
            });
        });

        it('should update dialogVisible', (done) => {
            spectator.service.setDialogVisible(true);

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '',
                    url: '',
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: true
                });
                done();
            });
        });

        it('should update dialogHeader', (done) => {
            spectator.service.setDialogHeader('test');

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '',
                    url: '',
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: 'test',
                    dialogVisible: false
                });
                done();
            });
        });

        it('should update editIframeLoading', (done) => {
            spectator.service.setDialogIframeLoading(true);

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '',
                    url: '',
                    dialogIframeURL: '',
                    dialogIframeLoading: true,
                    dialogHeader: '',
                    dialogVisible: false
                });
                done();
            });
        });

        it('should reset editIframe properties', (done) => {
            spectator.service.setDialogHeader('test');
            spectator.service.setDialogVisible(true);
            spectator.service.setDialogIframeLoading(true);

            spectator.service.resetDialog();

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '',
                    url: '',
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: false
                });
                done();
            });
        });

        it('should initialize editIframe properties', (done) => {
            spectator.service.initActionEdit({
                inode: '123',
                title: 'test'
            });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: '',
                            identifier: ''
                        }
                    },
                    language_id: '',
                    url: '',
                    dialogIframeURL: EDIT_CONTENTLET_URL + '123',
                    dialogIframeLoading: true,
                    dialogHeader: 'test',
                    dialogVisible: true
                });
                done();
            });
        });
    });

    describe('effects', () => {
        it('should handle successful data loading', (done) => {
            const dotPageApiService = spectator.inject(DotPageApiService);
            const mockResponse = {
                page: {
                    title: 'Test Page',
                    identifier: '123'
                }
            };
            dotPageApiService.get.andReturn(of(mockResponse));

            spectator.service.load({ language_id: 'en', url: 'test-url' });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    language_id: 'en',
                    url: 'test-url',
                    editor: {
                        page: {
                            title: 'Test Page',
                            identifier: '123'
                        }
                    },
                    dialogIframeURL: '',
                    dialogIframeLoading: false,
                    dialogHeader: '',
                    dialogVisible: false
                });
                done();
            });
        });

        it("should call save method from dotPageApiService when 'save' action is dispatched", () => {
            const dotPageApiService = spectator.inject(DotPageApiService);
            const mockResponse = {
                page: {
                    title: 'Test Page'
                }
            };
            dotPageApiService.get.andReturn(of(mockResponse));

            spectator.service.load({ language_id: 'en', url: 'test-url' });
            spectator.service.savePage({
                pageContainers: [],
                container: {
                    uuid: '123',
                    identifier: 'test',
                    contentletsId: [],
                    acceptTypes: 'test'
                },
                contentletID: '456',
                pageID: '789'
            });

            expect(dotPageApiService.save).toHaveBeenCalledWith({
                pageContainers: [],
                container: {
                    uuid: '123',
                    identifier: 'test',
                    contentletsId: [],
                    acceptTypes: 'test'
                },
                contentletID: '456',
                pageID: '789'
            });
        });
    });
});
