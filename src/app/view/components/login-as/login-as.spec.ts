import { By } from '@angular/platform-browser';
import { ComponentFixture, async } from '@angular/core/testing';
import { DebugElement } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { LoginAsComponent } from './login-as';
import { MockDotMessageService } from '../../../test/dot-message-service.mock';
import { DOTTestBed } from '../../../test/dot-test-bed';
import {
    SEARCHABLE_NGFACES_MODULES,
    SearchableDropDownModule
} from '../_common/searchable-dropdown/searchable-dropdown.module';
import { DotMessageService } from '../../../api/services/dot-messages-service';
import { LoginService, User } from 'dotcms-js/dotcms-js';
import { PaginatorService } from '../../../api/services/paginator';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs/Rx';
import { InputTextModule } from 'primeng/primeng';
import { ReactiveFormsModule } from '@angular/forms';
import { DotRouterService } from '../../../api/services/dot-router-service';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { IframeOverlayService } from '../_common/iframe/service/iframe-overlay.service';

describe('LoginAsComponent', () => {
    let comp: LoginAsComponent;
    let fixture: ComponentFixture<LoginAsComponent>;
    let de: DebugElement;
    let el: HTMLElement;

    beforeEach(
        async(() => {
            const messageServiceMock = new MockDotMessageService({
                Change: 'Change',
                cancel: 'cancel',
                'login-as': 'login-as',
                'loginas.select.loginas.user': 'loginas.select.loginas.user',
                password: 'password'
            });

            DOTTestBed.configureTestingModule({
                declarations: [LoginAsComponent],
                imports: [
                    ...SEARCHABLE_NGFACES_MODULES,
                    BrowserAnimationsModule,
                    InputTextModule,
                    ReactiveFormsModule,
                    SearchableDropDownModule
                ],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    { provide: LoginService, useValue: {} },
                    { provide: DotRouterService, useValue: {} },
                    {
                        provide: ActivatedRoute,
                        useValue: { params: Observable.from([{ id: '1234' }]) }
                    },
                    PaginatorService,
                    IframeOverlayService
                ]
            });

            fixture = DOTTestBed.createComponent(LoginAsComponent);
            comp = fixture.componentInstance;
            de = fixture.debugElement;
            el = de.nativeElement;
        })
    );

    it('Should load the first page', () => {
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
        const paginatorService: PaginatorService = fixture.debugElement.injector.get(PaginatorService);
        spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of(users));

        comp.ngOnInit();
        fixture.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.url).toEqual('v2/users/loginAsData');
        expect(paginatorService.filter).toEqual('');
        expect(comp.userCurrentPage).toEqual(users);
    });

    it(
        'Should change page',
        fakeAsync(() => {
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

            const paginatorService: PaginatorService = fixture.debugElement.injector.get(PaginatorService);
            spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of(users));

            const searchableDropdown = de.query(By.css('searchable-dropdown'));
            searchableDropdown.componentInstance.pageChange.emit({
                filter: 'filter',
                first: 1
            });

            tick();

            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(1);
            expect(paginatorService.filter).toEqual('filter');
        })
    );

    it(
        'Should change filter',
        fakeAsync(() => {
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

            const paginatorService: PaginatorService = fixture.debugElement.injector.get(PaginatorService);
            spyOn(paginatorService, 'getWithOffset').and.returnValue(Observable.of(users));

            const searchableDropdown = de.query(By.css('searchable-dropdown'));
            searchableDropdown.componentInstance.filterChange.emit('new filter');

            tick();

            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
            expect(paginatorService.filter).toEqual('new filter');
        })
    );
});
