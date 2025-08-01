import { TestBed } from '@angular/core/testing';

import { CONTAINER_SOURCE, DotContainerMap } from '@dotcms/dotcms-models';

import { DotTemplateContainersCacheService } from './dot-template-containers-cache.service';

describe('TemplateContainersCacheService', () => {
    let service: DotTemplateContainersCacheService;
    let containers: DotContainerMap = {};

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [DotTemplateContainersCacheService],
            imports: []
        });

        service = TestBed.inject(DotTemplateContainersCacheService);
        containers = {
            '/containers/path': {
                identifier: '1',
                name: 'container 1',
                type: 'type',
                source: CONTAINER_SOURCE.FILE,
                path: '/containers/path',
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            },
            '2': {
                identifier: '2',
                name: 'container 2',
                type: 'type',
                source: CONTAINER_SOURCE.DB,
                parentPermissionable: {
                    hostname: 'demo.dotcms.com'
                }
            }
        };
    });

    it('should return the right container', () => {
        service.set(containers);

        expect(service.get('/containers/path')).toEqual(containers['/containers/path']);
        expect(service.get('2')).toEqual(containers[2]);
        expect(service.get('3')).toBeUndefined();
    });

    it('should return the right container identifier', () => {
        const fileContainer = service.getContainerReference(containers['/containers/path']);
        const dataBaseConstainer = service.getContainerReference(containers['2']);

        expect(fileContainer).toEqual(containers['/containers/path'].path);
        expect(dataBaseConstainer).toEqual(containers['2'].identifier);
    });
});
