import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { DotOsgiService } from './dot-osgi.service';

describe('DotOsgiService', () => {
    let spectator: SpectatorHttp<DotOsgiService>;

    const createHttp = createHttpFactory({
        service: DotOsgiService
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    it('should get installed bundles', () => {
        const entity = [{ bundleId: 1, symbolicName: 'test', state: 32, jarFile: 'a.jar' }];
        spectator.service.getInstalledBundles(false).subscribe((res) => {
            expect(res.entity).toEqual(entity);
        });
        const req = spectator.expectOne('/api/v1/osgi?ignoresystembundles=false', HttpMethod.GET);
        req.flush({ entity });
    });

    it('should get available plugins', () => {
        const entity = ['plugin1.jar', 'plugin2.jar'];
        spectator.service.getAvailablePlugins().subscribe((res) => {
            expect(res.entity).toEqual(entity);
        });
        const req = spectator.expectOne('/api/v1/osgi/available-plugins', HttpMethod.GET);
        req.flush({ entity });
    });

    it('should deploy jar', () => {
        spectator.service.deploy('my.jar').subscribe();
        const req = spectator.expectOne('/api/v1/osgi/jar/my.jar/_deploy', HttpMethod.PUT);
        req.flush({});
    });

    it('should start jar', () => {
        spectator.service.start('my.jar').subscribe();
        const req = spectator.expectOne('/api/v1/osgi/jar/my.jar/_start', HttpMethod.PUT);
        req.flush({});
    });

    it('should undeploy jar', () => {
        spectator.service.undeploy('my.jar').subscribe();
        const req = spectator.expectOne('/api/v1/osgi/jar/my.jar', HttpMethod.DELETE);
        req.flush({});
    });
});
