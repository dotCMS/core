import {
    throwError as observableThrowError,
    of as observableOf,
    from as observableFrom
} from 'rxjs';
import { mockUser, LoginServiceMock } from './../../../test/login-service.mock';
import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement, Injectable, Component, Input, forwardRef, Output, EventEmitter } from '@angular/core';
import { LoginAsComponent } from './login-as';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../test/dot-test-bed';
import { SEARCHABLE_NGFACES_MODULES } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { DotMessageService } from '@services/dot-messages-service';
import { LoginService, User } from 'dotcms-js';
import { PaginatorService } from '@services/paginator';
import { ActivatedRoute } from '@angular/router';
import { InputTextModule } from 'primeng/primeng';
import { ReactiveFormsModule, NG_VALUE_ACCESSOR } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';
import { DotNavigationService } from '../dot-navigation/services/dot-navigation.service';
import { DotEventsService } from '@services/dot-events/dot-events.service';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { RouterTestingModule } from '@angular/router/testing';

@Component({
    selector: 'dot-searchable-dropdown',
    template: ``,
    providers: [
        {
            multi: true,
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DotSearchableDropdownMockComponent)
        }
    ],
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

@Injectable()
class MockDotNavigationService {
    goToFirstPortlet() {}
}

describe('LoginAsComponent', () => {
    let comp: LoginAsComponent;
    let fixture: ComponentFixture<LoginAsComponent>;
    let de: DebugElement;
    let paginatorService: PaginatorService;
    let loginService: LoginService;
    let dotEventsService: DotEventsService;

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
            password: 'password'
        });

        DOTTestBed.configureTestingModule({
            declarations: [LoginAsComponent, DotSearchableDropdownMockComponent],
            imports: [
                ...SEARCHABLE_NGFACES_MODULES,
                BrowserAnimationsModule,
                InputTextModule,
                ReactiveFormsModule,
                DotDialogModule,
                RouterTestingModule
            ],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: LoginService, useClass: LoginServiceMock },
                {
                    provide: DotNavigationService,
                    useClass: MockDotNavigationService
                },
                {
                    provide: ActivatedRoute,
                    useValue: { params: observableFrom([{ id: '1234' }]) }
                },
                PaginatorService,
                IframeOverlayService
            ]
        });

        fixture = DOTTestBed.createComponent(LoginAsComponent);
        comp = fixture.componentInstance;
        de = fixture.debugElement;

        paginatorService = de.injector.get(PaginatorService);
        loginService = de.injector.get(LoginService);
        dotEventsService = de.injector.get(DotEventsService);

        spyOn(paginatorService, 'getWithOffset').and.returnValue(observableOf([...users]));

    }));

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
        searchableDropdown.componentInstance.pageChange.emit({
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
        searchableDropdown.componentInstance.filterChange.emit('new filter');

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.filter).toEqual('new filter');
    });

    it('should call "loginAs" in "LoginService" when login as happens', () => {
        spyOn(loginService, 'loginAs').and.callThrough();
        spyOn(dotEventsService, 'notify');

        comp.visible = true;
        fixture.detectChanges();

        comp.form.get('loginAsUser').setValue(mockUser);
        fixture.detectChanges();

        comp.dialogActions.accept.action();

        expect(loginService.loginAs).toHaveBeenCalledTimes(1);
        expect(dotEventsService.notify).toHaveBeenCalledWith('login-as');
    });

    it('should focus on Password input after an Error haapens in "loginAs" in "LoginService"', () => {
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
});
