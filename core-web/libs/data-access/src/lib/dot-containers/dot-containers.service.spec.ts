import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { DotContainer, DotConfigurationVariables, CONTAINER_SOURCE } from '@dotcms/dotcms-models';

import { CONTAINER_API_URL, DotContainersService } from './dot-containers.service';

import { DotPropertiesService } from '../dot-properties/dot-properties.service';

describe('DotContainersService', () => {
    let spectator: SpectatorHttp<DotContainersService>;
    let dotPropertiesService: DotPropertiesService;

    const mockContainer: DotContainer = {
        identifier: 'test-container-id',
        name: 'Test Container',
        type: 'containers',
        source: CONTAINER_SOURCE.FILE,
        live: true,
        working: true,
        deleted: false,
        locked: false,
        title: 'Test Container Title',
        path: '/test-container',
        archived: false,
        categoryId: 'test-category',
        parentPermissionable: {
            hostname: 'test-host'
        }
    };

    const mockSystemContainer: DotContainer = {
        ...mockContainer,
        title: 'System Container',
        name: 'System Container'
    };

    const mockContainers: DotContainer[] = [mockContainer];

    const createHttp = createHttpFactory({
        service: DotContainersService,
        providers: [
            {
                provide: DotPropertiesService,
                useValue: {
                    getKey: jest.fn().mockReturnValue(of('null'))
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createHttp();
        dotPropertiesService = spectator.inject(DotPropertiesService);
        // Reset the mock to return null by default
        jest.spyOn(dotPropertiesService, 'getKey').mockReturnValue(of('null'));
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    describe('Constructor and Initialization', () => {
        it('should call DotPropertiesService.getKey with DEFAULT_CONTAINER', () => {
            expect(dotPropertiesService.getKey).toHaveBeenCalledWith(
                DotConfigurationVariables.DEFAULT_CONTAINER
            );
        });

        it('should initialize service successfully', () => {
            expect(spectator.service).toBeTruthy();
            expect(spectator.service.defaultContainer$).toBeDefined();
        });
    });

    describe('defaultContainer$ Observable', () => {
        it('should be defined and return an observable', () => {
            expect(spectator.service.defaultContainer$).toBeDefined();
            expect(typeof spectator.service.defaultContainer$.subscribe).toBe('function');
        });

        it('should filter out initial values', () => {
            const spy = jest.fn();
            spectator.service.defaultContainer$.subscribe(spy);

            // The observable filters out the initial value (isInitial: true)
            // but emits null when configuration is null, so it should be called once with null
            expect(spy).toHaveBeenCalledTimes(1);
            expect(spy).toHaveBeenCalledWith(null);
        });
    });

    describe('getFiltered', () => {
        it('should call the correct endpoint with default parameters', () => {
            const filter = 'test';
            const perPage = 10;

            spectator.service.getFiltered(filter, perPage).subscribe((containers) => {
                expect(containers).toEqual(mockContainers);
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}&system=false`,
                HttpMethod.GET
            );
            expect(req.request.method).toBe('GET');
            req.flush({ entity: mockContainers });
        });

        it('should call the correct endpoint with system containers enabled', () => {
            const filter = 'system';
            const perPage = 5;
            const fetchSystemContainers = true;

            spectator.service
                .getFiltered(filter, perPage, fetchSystemContainers)
                .subscribe((containers) => {
                    expect(containers).toEqual(mockContainers);
                });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}&system=true`,
                HttpMethod.GET
            );
            expect(req.request.method).toBe('GET');
            req.flush({ entity: mockContainers });
        });

        it('should handle empty filter string', () => {
            const filter = '';
            const perPage = 20;

            spectator.service.getFiltered(filter, perPage).subscribe((containers) => {
                expect(containers).toEqual(mockContainers);
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}&system=false`,
                HttpMethod.GET
            );
            req.flush({ entity: mockContainers });
        });

        it('should handle special characters in filter', () => {
            const filter = 'test@#$%';
            const perPage = 15;

            spectator.service.getFiltered(filter, perPage).subscribe((containers) => {
                expect(containers).toEqual(mockContainers);
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}&system=false`,
                HttpMethod.GET
            );
            req.flush({ entity: mockContainers });
        });
    });

    describe('getContainerByTitle', () => {
        it('should call the correct endpoint with default system parameter', () => {
            const title = 'Test Container';

            spectator.service.getContainerByTitle(title).subscribe((container) => {
                expect(container).toEqual(mockContainer);
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${title}&perPage=1&system=false`,
                HttpMethod.GET
            );
            expect(req.request.method).toBe('GET');
            req.flush({ entity: [mockContainer] });
        });

        it('should call the correct endpoint with system parameter set to true', () => {
            const title = 'System Container';
            const system = true;

            spectator.service.getContainerByTitle(title, system).subscribe((container) => {
                expect(container).toEqual(mockSystemContainer);
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${title}&perPage=1&system=true`,
                HttpMethod.GET
            );
            expect(req.request.method).toBe('GET');
            req.flush({ entity: [mockSystemContainer] });
        });

        it('should return the first container from the response array', () => {
            const title = 'Multiple Containers';
            const multipleContainers = [mockContainer, mockSystemContainer];

            spectator.service.getContainerByTitle(title).subscribe((container) => {
                expect(container).toEqual(mockContainer); // Should return the first one
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${title}&perPage=1&system=false`,
                HttpMethod.GET
            );
            req.flush({ entity: multipleContainers });
        });

        it('should return undefined when no containers are found', () => {
            const title = 'Non-existent Container';

            spectator.service.getContainerByTitle(title).subscribe((container) => {
                expect(container).toBeUndefined();
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${title}&perPage=1&system=false`,
                HttpMethod.GET
            );
            req.flush({ entity: [] });
        });

        it('should handle empty title string', () => {
            const title = '';

            spectator.service.getContainerByTitle(title).subscribe((container) => {
                expect(container).toBeUndefined();
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${title}&perPage=1&system=false`,
                HttpMethod.GET
            );
            req.flush({ entity: [] });
        });
    });

    describe('Error Handling', () => {
        it('should handle HTTP errors in getFiltered', () => {
            const filter = 'test';
            const perPage = 10;
            const errorResponse = { status: 500, statusText: 'Internal Server Error' };

            spectator.service.getFiltered(filter, perPage).subscribe({
                next: () => fail('Should have failed'),
                error: (error) => {
                    expect(error.status).toBe(500);
                }
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${filter}&perPage=${perPage}&system=false`,
                HttpMethod.GET
            );
            req.flush('Server Error', errorResponse);
        });

        it('should handle HTTP errors in getContainerByTitle', () => {
            const title = 'Test Container';
            const errorResponse = { status: 404, statusText: 'Not Found' };

            spectator.service.getContainerByTitle(title).subscribe({
                next: () => fail('Should have failed'),
                error: (error) => {
                    expect(error.status).toBe(404);
                }
            });

            const req = spectator.expectOne(
                `${CONTAINER_API_URL}?filter=${title}&perPage=1&system=false`,
                HttpMethod.GET
            );
            req.flush('Not Found', errorResponse);
        });
    });
});
