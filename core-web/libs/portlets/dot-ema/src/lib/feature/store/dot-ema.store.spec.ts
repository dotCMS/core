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
                    editIframeURL: ''
                });
                done();
            });
        });

        it('should update editFrameURL', (done) => {
            spectator.service.setEditIframeURL('test-url');

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    editor: {
                        page: {
                            title: ''
                        }
                    },
                    language_id: '',
                    url: '',
                    editIframeURL: 'test-url'
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
                    editIframeURL: ''
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
                    editIframeURL: ''
                });
                done();
            });
        });
    });
});
