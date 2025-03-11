/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    from as observableFrom,
    of as observableOf,
    throwError as observableThrowError
} from 'rxjs';

import { DebugElement } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotEventsService, DotMessageService, PaginatorService } from '@dotcms/data-access';
import { LoginService, User } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService, mockUser } from '@dotcms/utils-testing';

import { DotLoginAsComponent } from './dot-login-as.component';

describe('DotLoginAsComponent', () => {
    let comp: DotLoginAsComponent;
    let fixture: ComponentFixture<DotLoginAsComponent>;
    let de: DebugElement;
    let paginatorService: PaginatorService;
    let loginService: LoginService;
    let dotEventsService: DotEventsService;
    let dotNavigationService: DotNavigationService;
    let locationService: Location;

    const users: User[] = [
        {
            admin: true,
            emailAddress: 'a@a.com',
            firstName: 'user_first_name',
            lastName: 'user_lastname',
            name: 'user 1',
            userId: '1'
        }
    ];

    beforeEach(waitForAsync(() => {
        const messageServiceMock = new MockDotMessageService({
            Change: 'Change',
            cancel: 'cancel',
            'login-as': 'login-as',
            'loginas.select.loginas.user': 'loginas.select.loginas.user',
            password: 'password',
            'loginas.error.wrong-credentials': 'wrong password'
        });

        DOTTestBed.configureTestingModule({
            imports: [
                BrowserAnimationsModule,
                InputTextModule,
                ReactiveFormsModule,
                DialogModule,
                DropdownModule,
                RouterTestingModule,
                DotMessagePipe,
                DotLoginAsComponent
            ],
            providers: [
                {
                    provide: LOCATION_TOKEN,
                    useValue: {
                        reload() {}
                    }
                },
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: { params: observableFrom([{ id: '1234' }]) }
                },
                DotNavigationService,
                DotMenuService,
                PaginatorService
            ]
        });

        fixture = DOTTestBed.createComponent(DotLoginAsComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        locationService = de.injector.get(LOCATION_TOKEN);
        paginatorService = de.injector.get(PaginatorService);
        loginService = de.injector.get(LoginService);
        dotEventsService = de.injector.get(DotEventsService);
        dotNavigationService = de.injector.get(DotNavigationService);

        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([...users]));
    }));

    it('should load the first page', () => {
        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.url).toEqual('v1/users/loginAsData');
        expect(paginatorService.filter).toEqual('');
        expect(comp.userCurrentPage).toEqual(users);
    });

    it('should change filter', () => {
        comp.visible = true;
        fixture.detectChanges();

        const dropdown = de.query(By.css('p-dropdown'));
        dropdown.triggerEventHandler('onFilter', {
            filter: 'new filter'
        });

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual('new filter');
    });

    it('should call "loginAs" in "LoginService"', () => {
        spyOn(loginService, 'loginAs').and.callThrough();
        spyOn(dotEventsService, 'notify');

        comp.visible = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser());
        comp.doLoginAs();

        expect(loginService.loginAs).toHaveBeenCalledTimes(1);
    });

    it('should focus on password input after an error happens in "loginAs" in "LoginService"', () => {
        spyOn(loginService, 'loginAs').and.returnValue(observableThrowError({ message: 'Error' }));
        comp.visible = true;
        comp.needPassword = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser());
        comp.form.get('password').setValue('password');
        fixture.detectChanges();

        const passwordInputElem = de.query(By.css('#dot-login-as-password'));
        spyOn(passwordInputElem.nativeElement, 'focus');

        comp.doLoginAs();

        expect(passwordInputElem.nativeElement.focus).toHaveBeenCalled();
    });

    it('should reload the page on login as', async () => {
        spyOn(dotNavigationService, 'goToFirstPortlet').and.returnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );
        spyOn(loginService, 'logoutAs').and.callThrough();
        spyOn(locationService, 'reload');

        comp.visible = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser());
        fixture.detectChanges();

        comp.doLoginAs();

        await fixture.whenStable();

        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
        expect(locationService.reload).toHaveBeenCalledTimes(1);
    });

    it('should show error message', async () => {
        spyOn(loginService, 'loginAs').and.returnValue(observableThrowError({}));

        comp.visible = true;
        comp.needPassword = true;
        fixture.detectChanges();

        // Set form values
        comp.form.get('loginAsUser').setValue(mockUser());
        comp.form.get('password').setValue('password');
        fixture.detectChanges();

        // Verify error message is not present initially
        let error = de.query(By.css('.login-as__error-message'));
        expect(error).toBeNull();

        // Call doLoginAs directly since this triggers the error
        comp.doLoginAs();

        // Wait for async operations
        await fixture.whenStable();
        fixture.detectChanges();

        // Now verify error message is present and has correct text
        error = de.query(By.css('.login-as__error-message'));
        expect(error).not.toBeNull();
        expect(error.nativeElement.textContent.trim()).toBe('wrong password');
    });

    it('should clean error after user selection change', () => {
        comp.visible = true;
        comp.errorMessage = 'Error messsage';
        fixture.detectChanges();

        const dropdown = de.query(By.css('p-dropdown'));
        dropdown.triggerEventHandler('onChange', {
            value: { requestPassword: false }
        });

        expect(comp.errorMessage).toEqual('');
    });
});
