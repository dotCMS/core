import { createServiceFactory, SpectatorService } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { EditEmaStore } from './dot-ema.store';

import { DotPageApiService } from '../../services/dot-page-api.service';
import { DEFAULT_PERSONA, EDIT_CONTENTLET_URL } from '../../shared/consts';

describe('EditEmaStore', () => {
    let spectator: SpectatorService<EditEmaStore>;
    const createService = createServiceFactory({
        service: EditEmaStore,
        mocks: [DotPageApiService]
    });

    beforeEach(() => (spectator = createService()));

    describe('selectors', () => {
        it('should return editorState', (done) => {
            const dotPageApiService = spectator.inject(DotPageApiService);
            const mockResponse = {
                page: {
                    title: 'Test Page',
                    identifier: '123'
                },
                viewAs: {
                    language: {
                        id: 1,
                        language: '',
                        countryCode: '',
                        languageCode: '',
                        country: ''
                    },
                    persona: {
                        ...DEFAULT_PERSONA
                    }
                }
            };
            dotPageApiService.get.andReturn(of(mockResponse));

            spectator.service.load({ language_id: '1', url: 'test-url', persona_id: '123' });

            spectator.service.editorState$.subscribe((state) => {
                expect(state as unknown).toEqual({
                    apiURL: 'http://localhost/api/v1/page/json/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona',
                    editor: {
                        page: { identifier: '123', title: 'Test Page' },
                        viewAs: {
                            language: {
                                country: '',
                                countryCode: '',
                                id: 1,
                                language: '',
                                languageCode: ''
                            },
                            persona: {
                                ...DEFAULT_PERSONA
                            }
                        }
                    },
                    iframeURL:
                        'http://localhost:3000/test-url?language_id=1&com.dotmarketing.persona.id=modes.persona.no.persona'
                });
                done();
            });
        });

        it('should return the dialogState', () => {
            spectator.service.setDialogIframeURL('test-url');
            spectator.service.setDialogVisible(true);
            spectator.service.setDialogHeader('test');
            spectator.service.setDialogIframeLoading(true);

            spectator.service.dialogState$.subscribe((state) => {
                expect(state).toEqual({
                    dialogIframeURL: 'test-url',
                    dialogVisible: true,
                    dialogHeader: 'test',
                    dialogIframeLoading: true
                });
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
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
                        }
                    },
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
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
                        }
                    },
                    url: '',
                    dialogIframeURL: 'test-url',
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
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
                        }
                    },
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
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
                        }
                    },
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
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
                        }
                    },
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
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
                        }
                    },
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
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
                        }
                    },
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
                },
                viewAs: {
                    language: {
                        id: 1,
                        language: '',
                        countryCode: '',
                        languageCode: '',
                        country: ''
                    }
                }
            };
            dotPageApiService.get.andReturn(of(mockResponse));

            spectator.service.load({ language_id: 'en', url: 'test-url', persona_id: '123' });

            spectator.service.state$.subscribe((state) => {
                expect(state).toEqual({
                    url: 'test-url',
                    editor: {
                        page: {
                            title: 'Test Page',
                            identifier: '123'
                        },
                        viewAs: {
                            language: {
                                id: 1,
                                language: '',
                                countryCode: '',
                                languageCode: '',
                                country: ''
                            }
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

            spectator.service.load({ language_id: 'en', url: 'test-url', persona_id: '123' });
            spectator.service.savePage({
                pageContainers: [],
                container: {
                    uuid: '123',
                    identifier: 'test',
                    contentletsId: [],
                    acceptTypes: 'test'
                },
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
                pageID: '789'
            });
        });
    });
});
