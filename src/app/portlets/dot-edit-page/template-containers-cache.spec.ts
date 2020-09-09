import { TemplateContainersCacheService } from './template-containers-cache.service';
import { TestBed } from '@angular/core/testing';
import { CONTAINER_SOURCE, DotContainerMap } from '@models/container/dot-container.model';

describe('TemplateContainersCacheService', () => {
    let service: TemplateContainersCacheService;
    let containers: DotContainerMap = {};

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [TemplateContainersCacheService],
            imports: []
        });

        service = TestBed.get(TemplateContainersCacheService);
        containers = {
            '/containers/path': {
                container: {
                    identifier: '1',
                    name: 'container 1',
                    type: 'type',
                    source: CONTAINER_SOURCE.FILE,
                    path: '/containers/path',
                    parentPermissionable: {
                        hostname: 'demo.dotcms.com'
                    }
                }
            },
            '2': {
                container: {
                    identifier: '2',
                    name: 'container 2',
                    type: 'type',
                    source: CONTAINER_SOURCE.DB,
                    parentPermissionable: {
                        hostname: 'demo.dotcms.com'
                    }
                }
            }
        };
    });

    it('should return the right container', () => {
        service.set(containers);

        expect(service.get('/containers/path')).toEqual(containers['/containers/path'].container);
        expect(service.get('2')).toEqual(containers[2].container);
        expect(service.get('3')).toBeNull();
    });

    it('should return the right container identifier', () => {
        const fileContainer = service.getContainerReference(containers['/containers/path'].container);
        const dataBaseConstainer = service.getContainerReference(containers['2'].container);

        expect(fileContainer).toEqual(containers['/containers/path'].container.path);
        expect(dataBaseConstainer).toEqual(containers['2'].container.identifier);
    });
});
