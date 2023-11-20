import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { EditEmaStore } from './dot-ema.store';

import { DotPageApiService } from '../../services/dot-page-api.service';

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
                            title: ''
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
                            title: ''
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
                            title: ''
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
                            title: ''
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
                            title: ''
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
                            title: ''
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
                            title: ''
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
    });

    describe('effects', () => {
        it('should handle successful data loading', (done) => {
            const dotPageApiService = spectator.inject(DotPageApiService);
            const mockResponse = {
                page: {
                    title: 'Test Page'
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
                            title: 'Test Page'
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
    });
});
