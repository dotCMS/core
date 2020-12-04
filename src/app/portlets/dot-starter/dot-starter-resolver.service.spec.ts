import { waitForAsync, TestBed } from '@angular/core/testing';
import { DotStarterResolver } from './dot-starter-resolver.service';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '@tests/login-service.mock';

describe('DotStarterResolver', () => {
    let dotStarterResolver: DotStarterResolver;

    beforeEach(
        waitForAsync(() => {
            const testbed = TestBed.configureTestingModule({
                providers: [
                    DotStarterResolver,
                    {
                        provide: LoginService,
                        useClass: LoginServiceMock
                    }
                ]
            });
            dotStarterResolver = testbed.inject(DotStarterResolver);
        })
    );

    it('should get and return apps with configurations', () => {
        dotStarterResolver.resolve().subscribe((username: string) => {
            expect(username).toEqual('Admin');
        });
    });
});
