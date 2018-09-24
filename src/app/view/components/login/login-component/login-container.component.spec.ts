import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotLoadingIndicatorService } from '../../_common/iframe/dot-loading-indicator/dot-loading-indicator.service';
import { RouterTestingModule } from '@angular/router/testing';
import { DotRouterService } from '@services/dot-router/dot-router.service';
import { DotLoadingIndicatorComponent } from '../../_common/iframe/dot-loading-indicator/dot-loading-indicator.component';
import { MdInputTextModule } from '../../../directives/md-inputtext/md-input-text.module';
import { LoginContainerComponent } from './login-container.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { async } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { By } from '@angular/platform-browser';
import { DOTTestBed } from '../../../../test/dot-test-bed';
import { LoginService } from 'dotcms-js/core/login.service';
import { LoginServiceMock } from '../../../../test/login-service.mock';

describe('LoginContainerComponent', () => {
    let component: LoginContainerComponent;
    let loginService: LoginService;
    let dotRouterService: DotRouterService;
    let fixture: ComponentFixture<LoginContainerComponent>;

    beforeEach(async(() => {
        DOTTestBed.configureTestingModule({
            imports: [MdInputTextModule, RouterTestingModule, BrowserAnimationsModule],
            declarations: [LoginContainerComponent, LoginComponent, DotLoadingIndicatorComponent],
            providers: [DotLoadingIndicatorService, { provide: LoginService, useClass: LoginServiceMock }]
        }).compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(LoginContainerComponent);
        component = fixture.componentInstance;
        loginService = fixture.componentRef.injector.get(LoginService);
        dotRouterService = fixture.componentRef.injector.get(DotRouterService);
        fixture.detectChanges();
    });

    it('should have dot-login-component', () => {
        expect(fixture.debugElement.query(By.css('dot-login-component'))).not.toBeNull();
    });

    describe('loginUser()', () => {
        beforeEach(() => {
            spyOn(loginService, 'loginUser').and.callThrough();
            spyOn(component, 'logInUser').and.callThrough();
            spyOn(dotRouterService, 'goToMain');
            const loginComponent: LoginComponent = fixture.debugElement.query(By.css('dot-login-component')).componentInstance;

            loginComponent.login.emit({
                login: 'admin@dotcms.com',
                password: 'admin',
                rememberMe: false,
                language: 'en'
            });
        });

        it('should call login user method on login component emit', () => {
            expect(component.logInUser).toHaveBeenCalledWith({
                login: 'admin@dotcms.com',
                password: 'admin',
                rememberMe: false,
                language: 'en'
            });
        });

        it('should call login service correctly', () => {
            expect(loginService.loginUser).toHaveBeenCalledWith('admin@dotcms.com', 'admin', false, 'en');
        });

        it('should redirect correctly after login', () => {
            expect(dotRouterService.goToMain).toHaveBeenCalledWith('redirect/to');
        });
    });
});
