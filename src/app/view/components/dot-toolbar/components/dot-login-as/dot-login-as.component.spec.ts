import {
    throwError as observableThrowError,
    of as observableOf,
    from as observableFrom
} from 'rxjs';
import { mockUser, LoginServiceMock } from '../../../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Component, Input, forwardRef, Output, EventEmitter } from '@angular/core';
import { DotLoginAsComponent } from './dot-login-as.component';
import { MockDotMessageService } from '../../../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../../../test/dot-test-bed';
import { SEARCHABLE_NGFACES_MODULES } from '../../../_common/searchable-dropdown/searchable-dropdown.module';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { LoginService, User } from 'dotcms-js';
import { PaginatorService } from '@services/paginator';
import { ActivatedRoute } from '@angular/router';
import { InputTextModule } from 'primeng/primeng';
import { ReactiveFormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { RouterTestingModule } from '@angular/router/testing';
import { LOCATION_TOKEN } from 'src/app/providers';
import { DotNavigationService } from '@components/dot-navigation/services/dot-navigation.service';
import { DotMenuService } from '@services/dot-menu.service';

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
    change: EventEmitter<any> = new EventEmitter();

    @Output()
    filterChange: EventEmitter<string> = new EventEmitter();

    @Output()
    hide: EventEmitter<any> = new EventEmitter();

    @Output()
    pageChange: EventEmitter<any> = new EventEmitter();

    @Output()
    show: EventEmitter<any> = new EventEmitter();

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
            emailAddress: 'a@a.com',
            firstName: 'user_first_name',
            lastName: 'user_lastname',
            loggedInDate: 1,
            name: 'user 1',
            userId: '1'
        }
    ];

    beforeEach(async(() => {
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
                RouterTestingModule
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

    afterAll(() => {
        comp.visible = false;
        fixture.detectChanges();
    });

    it('should load the first page', () => {
        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.url).toEqual('v2/users/loginAsData');
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

        comp.form.get('loginAsUser').setValue(mockUser);
        comp.dialogActions.accept.action();

        expect(loginService.loginAs).toHaveBeenCalledTimes(1);
    });

    it('should focus on password input after an error haapens in "loginAs" in "LoginService"', () => {
        spyOn(loginService, 'loginAs').and.returnValue(observableThrowError({ message: 'Error' }));
        comp.visible = true;
        comp.needPassword = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser);
        comp.form.get('password').setValue('password');
        fixture.detectChanges();

        const passwordInputElem = de.query(By.css('#dot-login-as-password'));
        spyOn(passwordInputElem.nativeElement, 'focus');

        comp.dialogActions.accept.action();

        expect(passwordInputElem.nativeElement.focus).toHaveBeenCalled();
    });

    it('should reload the page on login as', () => {
        spyOn(dotNavigationService, 'goToFirstPortlet').and.returnValue(
            new Promise((resolve) => {
                resolve(true);
            })
        );
        spyOn(loginService, 'logoutAs').and.callThrough();
        spyOn(locationService, 'reload');

        comp.visible = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser);
        fixture.detectChanges();

        comp.dialogActions.accept.action();

        fixture.whenStable().then(() => {
            expect(dotNavigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
            expect(locationService.reload).toHaveBeenCalledTimes(1);
        });
    });

    it('should show error message', () => {
        spyOn(loginService, 'loginAs').and.returnValue(
            observableThrowError({ })
        );

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
        searchableDropdown.triggerEventHandler('change', {
            requestPassword: false
        });

        expect(comp.errorMessage).toEqual('');
    });
});
