import { createHttpFactory, HttpMethod, SpectatorHttp } from '@ngneat/spectator/jest';

import { BUNDLE_STATE, BundleMap } from './bundle-map.model';
import { DotOsgiService } from './dot-osgi.service';

function mockBundle(overrides: Partial<BundleMap> = {}): BundleMap {
    return {
        bundleId: 1,
        symbolicName: 'test',
        location: 'file:plugins/test.jar',
        jarFile: 'test.jar',
        state: BUNDLE_STATE.ACTIVE,
        version: '1.0.0',
        separator: '|',
        isSystem: false,
        ...overrides
    };
}

describe('DotOsgiService', () => {
    let spectator: SpectatorHttp<DotOsgiService>;

    const createHttp = createHttpFactory({
        service: DotOsgiService
    });

    beforeEach(() => {
        spectator = createHttp();
    });

    describe('getInstalledBundles', () => {
        it('should GET with ignoresystembundles=false by default', () => {
            const entity = [mockBundle()];
            spectator.service.getInstalledBundles().subscribe((res) => {
                expect(res.entity).toEqual(entity);
            });
            const req = spectator.expectOne(
                '/api/v1/osgi?ignoresystembundles=false',
                HttpMethod.GET
            );
            req.flush({ entity });
        });

        it('should GET with ignoresystembundles=false when passed explicitly', () => {
            spectator.service.getInstalledBundles(false).subscribe();
            spectator.expectOne('/api/v1/osgi?ignoresystembundles=false', HttpMethod.GET).flush({});
        });

        it('should GET with ignoresystembundles=true when requested', () => {
            spectator.service.getInstalledBundles(true).subscribe();
            spectator.expectOne('/api/v1/osgi?ignoresystembundles=true', HttpMethod.GET).flush({});
        });
    });

    describe('getDotSystemBundles', () => {
        it('should GET dotsystem with ignoresystembundles=false by default', () => {
            spectator.service.getDotSystemBundles().subscribe();
            spectator
                .expectOne('/api/v1/osgi/dotsystem?ignoresystembundles=false', HttpMethod.GET)
                .flush({});
        });

        it('should GET dotsystem with ignoresystembundles=true when requested', () => {
            spectator.service.getDotSystemBundles(true).subscribe();
            spectator
                .expectOne('/api/v1/osgi/dotsystem?ignoresystembundles=true', HttpMethod.GET)
                .flush({});
        });
    });

    describe('getAvailablePlugins', () => {
        it('should GET available-plugins', () => {
            const entity = ['plugin1.jar', 'plugin2.jar'];
            spectator.service.getAvailablePlugins().subscribe((res) => {
                expect(res.entity).toEqual(entity);
            });
            const req = spectator.expectOne('/api/v1/osgi/available-plugins', HttpMethod.GET);
            req.flush({ entity });
        });
    });

    describe('uploadBundles', () => {
        it('should POST FormData with each file under key "file"', () => {
            const fileA = new File(['a'], 'a.jar', { type: 'application/java-archive' });
            const fileB = new File(['b'], 'b.jar', { type: 'application/java-archive' });

            spectator.service.uploadBundles([fileA, fileB]).subscribe();

            const req = spectator.expectOne('/api/v1/osgi', HttpMethod.POST);
            expect(req.request.body).toBeInstanceOf(FormData);
            const body = req.request.body as FormData;
            expect(body.getAll('file')).toEqual([fileA, fileB]);
            req.flush({});
        });
    });

    describe('deploy', () => {
        it('should PUT deploy URL with plain jar name', () => {
            spectator.service.deploy('my.jar').subscribe();
            const req = spectator.expectOne('/api/v1/osgi/jar/my.jar/_deploy', HttpMethod.PUT);
            expect(req.request.body).toEqual({});
            req.flush({});
        });

        it('should encode jar name in deploy path', () => {
            spectator.service.deploy('my plugin.jar').subscribe();
            spectator
                .expectOne('/api/v1/osgi/jar/my%20plugin.jar/_deploy', HttpMethod.PUT)
                .flush({});
        });
    });

    describe('start', () => {
        it('should PUT start URL', () => {
            spectator.service.start('my.jar').subscribe();
            const req = spectator.expectOne('/api/v1/osgi/jar/my.jar/_start', HttpMethod.PUT);
            expect(req.request.body).toEqual({});
            req.flush({});
        });

        it('should encode jar name in start path', () => {
            spectator.service.start('a/b.jar').subscribe();
            spectator.expectOne('/api/v1/osgi/jar/a%2Fb.jar/_start', HttpMethod.PUT).flush({});
        });
    });

    describe('stop', () => {
        it('should PUT stop URL', () => {
            spectator.service.stop('my.jar').subscribe();
            const req = spectator.expectOne('/api/v1/osgi/jar/my.jar/_stop', HttpMethod.PUT);
            expect(req.request.body).toEqual({});
            req.flush({});
        });
    });

    describe('undeploy', () => {
        it('should DELETE jar URL', () => {
            spectator.service.undeploy('my.jar').subscribe();
            spectator.expectOne('/api/v1/osgi/jar/my.jar', HttpMethod.DELETE).flush({});
        });

        it('should encode jar name in undeploy path', () => {
            spectator.service.undeploy('x+y.jar').subscribe();
            spectator.expectOne('/api/v1/osgi/jar/x%2By.jar', HttpMethod.DELETE).flush({});
        });
    });

    describe('processExports', () => {
        it('should GET processExports with encoded bundle symbolic name', () => {
            spectator.service.processExports('com.example.plugin').subscribe();
            spectator
                .expectOne('/api/v1/osgi/_processExports/com.example.plugin', HttpMethod.GET)
                .flush({});
        });

        it('should encode special characters in bundle name', () => {
            spectator.service.processExports('bundle name').subscribe();
            spectator
                .expectOne('/api/v1/osgi/_processExports/bundle%20name', HttpMethod.GET)
                .flush({});
        });
    });

    describe('getExtraPackages', () => {
        it('should GET extra-packages', () => {
            const entity = 'pkg1;version=1\npkg2;version=2';
            spectator.service.getExtraPackages().subscribe((res) => {
                expect(res.entity).toEqual(entity);
            });
            const req = spectator.expectOne('/api/v1/osgi/extra-packages', HttpMethod.GET);
            req.flush({ entity });
        });
    });

    describe('updateExtraPackages', () => {
        it('should PUT extra-packages with body', () => {
            const packages = 'com.foo;version=1';
            spectator.service.updateExtraPackages(packages).subscribe((res) => {
                expect(res.entity).toEqual(packages);
            });
            const req = spectator.expectOne('/api/v1/osgi/extra-packages', HttpMethod.PUT);
            expect(req.request.body).toEqual({ packages });
            req.flush({ entity: packages });
        });
    });

    describe('restart', () => {
        it('should PUT _restart with empty body', () => {
            spectator.service.restart().subscribe();
            const req = spectator.expectOne('/api/v1/osgi/_restart', HttpMethod.PUT);
            expect(req.request.body).toEqual({});
            req.flush({});
        });
    });
});
