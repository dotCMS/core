import { TemplateContainersCacheService } from './template-containers-cache.service';
import { TestBed } from '@angular/core/testing';
import { CONTAINER_SOURCE } from '@models/container/dot-container.model';

describe('TemplateContainersCacheService', () => {
    let service: TemplateContainersCacheService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [TemplateContainersCacheService],
            imports: []
        });

        service = TestBed.get(TemplateContainersCacheService);
    });

    it('should return the right container', () => {
        const containers = {
            '1': {
                container: {
                    identifier: '1',
                    name: 'container 1',
                    type: 'type',
                    source: CONTAINER_SOURCE.FILE,
                    path: '/containers/path'
                }
            },
            '2': {
                container: {
                    identifier: '2',
                    name: 'container 2',
                    type: 'type',
                    source: CONTAINER_SOURCE.DB
                }
            }
        };

        service.set(containers);

        expect(service.get('/containers/path')).toEqual(containers[1].container);
        expect(service.get('2')).toEqual(containers[2].container);
        expect(service.get('3')).toBeNull();
    });
});
