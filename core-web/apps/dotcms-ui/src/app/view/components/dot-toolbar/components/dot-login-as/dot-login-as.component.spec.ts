/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    from as observableFrom,
    of as observableOf,
    throwError as observableThrowError
} from 'rxjs';

import { Component, DebugElement, EventEmitter, forwardRef, Input, Output } from '@angular/core';
import { ComponentFixture, waitForAsync } from '@angular/core/testing';
import { NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { InputTextModule } from 'primeng/inputtext';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '@dotcms/app/api/services/dot-menu.service';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DOTTestBed } from '@dotcms/app/test/dot-test-bed';
import { DotEventsService, DotMessageService, PaginatorService } from '@dotcms/data-access';
import { LoginService, User } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService, mockUser } from '@dotcms/utils-testing';

import { DotLoginAsComponent } from './dot-login-as.component';

import { SEARCHABLE_NGFACES_MODULES } from '../../../_common/searchable-dropdown/searchable-dropdown.module';

@Component({
    selector: 'dot-searchable-dropdown',
    template: ``,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSearchableDropdownMockComponent)
        }
    ]
})
class DotSearchableDropdownMockComponent {
    @Input()
    data: string[];

    @Input()
    labelPropertyName: string | string[];

    @Input()
    valuePropertyName: string;

    @Input()
    pageLinkSize = 3;

    @Input()
    rows: number;

    @Input()
    totalRecords: number;

    @Input()
    placeholder = '';

    @Input()
    persistentPlaceholder: boolean;

    @Input()
    width: string;

    @Input()
    multiple: boolean;

    @Output()
    switch: EventEmitter<any> = new EventEmitter();

    @Output()
    filterChange: EventEmitter<string> = new EventEmitter();

    @Output()
    hide: EventEmitter<any> = new EventEmitter();

    @Output()
    pageChange: EventEmitter<any> = new EventEmitter();

    @Output()
    display: EventEmitter<any> = new EventEmitter();

    writeValue() {}

    registerOnChange(): void {}

    registerOnTouched(): void {}
}

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
            declarations: [DotLoginAsComponent, DotSearchableDropdownMockComponent],
            imports: [
                ...SEARCHABLE_NGFACES_MODULES,
                BrowserAnimationsModule,
                InputTextModule,
                ReactiveFormsModule,
                DotDialogModule,
                RouterTestingModule,
                DotMessagePipe
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

    it('should change page', () => {
        comp.visible = true;
        fixture.detectChanges();

        const searchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        searchableDropdown.triggerEventHandler('pageChange', {
            filter: 'filter',
            first: 1
        });

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(1);
        expect(paginatorService.filter).toEqual('filter');
    });

    it('should change filter', () => {
        comp.visible = true;
        fixture.detectChanges();

        const searchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        searchableDropdown.triggerEventHandler('filterChange', 'new filter');

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual('new filter');
    });

    it('should call "loginAs" in "LoginService"', () => {
        spyOn(loginService, 'loginAs').and.callThrough();
        spyOn(dotEventsService, 'notify');

        comp.visible = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser());
        comp.dialogActions.accept.action();

        expect(loginService.loginAs).toHaveBeenCalledTimes(1);
    });

    it('should focus on password input after an error haapens in "loginAs" in "LoginService"', () => {
        spyOn(loginService, 'loginAs').and.returnValue(observableThrowError({ message: 'Error' }));
        comp.visible = true;
        comp.needPassword = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser());
        comp.form.get('password').setValue('password');
        fixture.detectChanges();

        const passwordInputElem = de.query(By.css('#dot-login-as-password'));
        spyOn(passwordInputElem.nativeElement, 'focus');

        comp.dialogActions.accept.action();

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

        comp.dialogActions.accept.action();

        await fixture.whenStable();

        expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
        expect(locationService.reload).toHaveBeenCalledTimes(1);
    });

    it('should show error message', () => {
        spyOn(loginService, 'loginAs').and.returnValue(observableThrowError({}));

        comp.visible = true;
        comp.needPassword = true;
        fixture.detectChanges();

        let error: DebugElement;
        error = de.query(By.css('.login-as__error-message'));
        expect(error).toBeFalsy();

        const form: DebugElement = de.query(By.css('form'));
        form.triggerEventHandler('ngSubmit', {});

        fixture.detectChanges();

        error = de.query(By.css('.login-as__error-message'));
        expect(error.nativeElement.textContent).toBe('wrong password');
    });

    it('should clean error after user selection change', () => {
        comp.visible = true;
        comp.errorMessage = 'Error messsage';
        fixture.detectChanges();

        const searchableDropdown = de.query(By.css('dot-searchable-dropdown'));
        searchableDropdown.triggerEventHandler('switch', {
            requestPassword: false
        });

        expect(comp.errorMessage).toEqual('');
    });
});
