import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotSystemConfig, SystemConfigResponse } from '@dotcms/dotcms-models';

import { DotSystemConfigService } from './dot-system-config.service';

describe('DotSystemConfigService', () => {
    let spectator: SpectatorHttp<DotSystemConfigService>;
    const createHttp = createHttpFactory(DotSystemConfigService);

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('getSystemConfig', () => {
        it('should fetch and transform system configuration', (done) => {
            const mockResponse: SystemConfigResponse = {
                entity: {
                    config: {
                        logos: {
                            loginScreen: '/test/logo.png',
                            navBar: 'NA'
                        },
                        colors: {
                            primary: '#4e65f1',
                            secondary: '#233f9b',
                            background: '#1b3359'
                        },
                        releaseInfo: {
                            buildDate: 'August 05, 2025 9:54 PM',
                            version: '1.0.0-SNAPSHOT'
                        },
                        systemTimezone: {
                            id: 'Asia/Dubai',
                            label: 'Gulf Standard Time (Asia/Dubai)',
                            offset: 14400000
                        },
                        languages: [
                            {
                                country: 'United States',
                                countryCode: 'US',
                                id: 1,
                                isoCode: 'en-us',
                                language: 'English',
                                languageCode: 'en'
                            }
                        ],
                        license: {
                            displayServerId: '726f1d94',
                            isCommunity: false,
                            level: 500,
                            levelName: 'PLATFORM EDITION'
                        },
                        cluster: {
                            clusterId: 'b03337d7b7',
                            companyKeyDigest:
                                'b0ab76ea89ce7bbcccd2ecaba4e19f4e13a5422350526cb5191c4e17f4e28317'
                        }
                    }
                }
            };

            const expectedConfig: DotSystemConfig = {
                logos: mockResponse.entity.config.logos,
                colors: mockResponse.entity.config.colors,
                releaseInfo: mockResponse.entity.config.releaseInfo,
                systemTimezone: mockResponse.entity.config.systemTimezone,
                languages: mockResponse.entity.config.languages,
                license: mockResponse.entity.config.license,
                cluster: mockResponse.entity.config.cluster
            };

            spectator.service.getSystemConfig().subscribe((config) => {
                expect(config).toEqual(expectedConfig);
                done();
            });

            const req = spectator.expectOne('/api/v1/appconfiguration', HttpMethod.GET);
            expect(req.request.method).toBe('GET');
            req.flush(mockResponse);
        });

        it('should handle errors when fetching system configuration', () => {
            spectator.service.getSystemConfig().subscribe(
                () => fail('Expected an error, but received a response'),
                (error) => {
                    expect(error).toBeDefined();
                }
            );

            const req = spectator.expectOne('/api/v1/appconfiguration', HttpMethod.GET);
            req.flush(null, { status: 500, statusText: 'Server Error' });
        });
    });
});
