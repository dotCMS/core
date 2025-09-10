/* eslint-disable @typescript-eslint/no-empty-function */
/* eslint-disable @typescript-eslint/no-explicit-any */

import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import {
    from as observableFrom,
    of as observableOf,
    throwError as observableThrowError
} from 'rxjs';

import { fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';

import { DotEventsService, DotMessageService, PaginatorService } from '@dotcms/data-access';
import { LoginService, User } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService, mockUser } from '@dotcms/utils-testing';

import { DotLoginAsComponent } from './dot-login-as.component';

import { DotMenuService } from '../../../../../api/services/dot-menu.service';
import { LOCATION_TOKEN } from '../../../../../providers';
import { DotNavigationService } from '../../../dot-navigation/services/dot-navigation.service';

describe('DotLoginAsComponent', () => {
    let spectator: Spectator<DotLoginAsComponent>;
    let component: DotLoginAsComponent;
    let paginatorService: SpyObject<PaginatorService>;
    let dotNavigationService: SpyObject<DotNavigationService>;

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

    const messageServiceMock = new MockDotMessageService({
        Change: 'Change',
        cancel: 'cancel',
        'login-as': 'login-as',
        'loginas.select.loginas.user': 'loginas.select.loginas.user',
        password: 'password',
        'loginas.error.wrong-credentials': 'wrong password'
    });

    const createComponent = createComponentFactory({
        component: DotLoginAsComponent,
        imports: [
            ReactiveFormsModule,
            DialogModule,
            DropdownModule,
            InputTextModule,
            PasswordModule,
            RouterTestingModule,
            DotMessagePipe
        ],
        providers: [
            { provide: DotMessageService, useValue: messageServiceMock },
            { provide: LoginService, useClass: LoginServiceMock },
            {
                provide: ActivatedRoute,
                useValue: { params: observableFrom([{ id: '1234' }]) }
            },
            {
                provide: LOCATION_TOKEN,
                useValue: {
                    reload() {}
                }
            },
            mockProvider(DotNavigationService),
            mockProvider(DotMenuService),
            mockProvider(DotEventsService),
            // Provide a complete mock for PaginatorService
            {
                provide: PaginatorService,
                useValue: {
                    getWithOffset: jest.fn().mockReturnValue(observableOf([...users])),
                    filter: '',
                    url: ''
                }
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        component = spectator.component;

        paginatorService = spectator.inject(PaginatorService) as any;

        dotNavigationService = spectator.inject(
            DotNavigationService
        ) as SpyObject<DotNavigationService>;

        // Configure the spy for goToFirstPortlet to always return a promise
        dotNavigationService.goToFirstPortlet.mockReturnValue(Promise.resolve(true));
    });

    it('should create component', () => {
        expect(component).toBeTruthy();
    });

    it('should load users on initialization', () => {
        spectator.detectChanges();

        expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
        expect(paginatorService.url).toEqual('v1/users/loginAsData');
        expect(paginatorService.filter).toEqual('');
        expect(component.userCurrentPage()).toEqual(users);
    });

    describe('User filtering', () => {
        it('should filter users when user types in dropdown', () => {
            // Arrange
            spectator.setInput('visible', true);
            spectator.detectChanges();

            // Act - Simulate user filtering
            component.handleFilterChange({ filter: 'new filter' });

            // Assert
            expect(paginatorService.getWithOffset).toHaveBeenCalledWith(0);
            expect(paginatorService.filter).toEqual('new filter');
        });
    });

    describe('Login as functionality', () => {
        it('should call login service when user submits form', () => {
            // Arrange
            const testUser = mockUser();
            // Create a new spy for loginAs
            const loginServiceInstance = spectator.inject(LoginService);
            jest.spyOn(loginServiceInstance, 'loginAs').mockReturnValue(observableOf(true));

            // Make sure goToFirstPortlet returns a promise
            dotNavigationService.goToFirstPortlet.mockReturnValue(Promise.resolve(true));

            spectator.setInput('visible', true);
            spectator.detectChanges();

            // Act - Simulate user selecting a user and submitting
            component.form.get('loginAsUser').setValue(testUser);
            spectator.detectChanges();

            // Verify that the form is valid before clicking
            expect(component.form.valid).toBe(true);

            // Call the doLoginAs method directly instead of clicking the button
            component.doLoginAs();

            // Assert
            expect(loginServiceInstance.loginAs).toHaveBeenCalledTimes(1);
            expect(loginServiceInstance.loginAs).toHaveBeenCalledWith({
                user: testUser,
                password: ''
            });
        });

        it('should show password field when selected user requires password', () => {
            // Arrange
            spectator.setInput('visible', true);
            spectator.detectChanges();

            // Act - Simulate user selecting a user that requires password
            component.userSelectedHandler({ requestPassword: true } as User);
            spectator.detectChanges();

            // Assert
            expect(component.needPassword()).toBe(true);
            expect(spectator.query(byTestId('dot-login-as-password-input'))).toBeTruthy();
        });

        it('should focus on password input after login error', () => {
            // Arrange
            // Create a new spy for loginAs
            const loginServiceInstance = spectator.inject(LoginService);
            jest.spyOn(loginServiceInstance, 'loginAs').mockReturnValue(
                observableThrowError({ message: 'Error' })
            );

            spectator.setInput('visible', true);
            component.needPassword.set(true);
            spectator.detectChanges();

            // Set form values as a user would
            component.form.get('loginAsUser').setValue(mockUser());
            component.form.get('password').setValue('password');
            spectator.detectChanges();

            const passwordInputElem = spectator.query(
                byTestId('dot-login-as-password-input')
            ) as HTMLElement;
            jest.spyOn(passwordInputElem, 'focus');

            // Act - Submit form
            component.doLoginAs();

            // Assert
            expect(passwordInputElem.focus).toHaveBeenCalled();
        });
    });

    // Isolate this test in its own describe block to avoid spy conflicts
    describe('Navigation after login', () => {
        let spectator: Spectator<DotLoginAsComponent>;
        let component: DotLoginAsComponent;
        let navigationService: SpyObject<DotNavigationService>;
        let locationService: Location;
        let loginService: LoginService;

        beforeEach(() => {
            spectator = createComponent();
            component = spectator.component;
            navigationService = spectator.inject(
                DotNavigationService
            ) as SpyObject<DotNavigationService>;
            locationService = spectator.inject(LOCATION_TOKEN);
            loginService = spectator.inject(LoginService);

            // Make sure goToFirstPortlet returns a promise
            navigationService.goToFirstPortlet.mockReturnValue(Promise.resolve(true));
        });

        it('should navigate to first portlet after successful login', async () => {
            // Arrange
            // Configure the spy that was already created by mockProvider
            navigationService.goToFirstPortlet.mockReturnValue(Promise.resolve(true));

            // Create a spy for the reload method of the location service
            jest.spyOn(locationService, 'reload');

            // Create a spy for the loginAs method of the login service
            jest.spyOn(loginService, 'loginAs').mockReturnValue(observableOf(true));

            // Set up the component
            spectator.setInput('visible', true);
            spectator.detectChanges();

            // Act - Simulate user login
            component.form.get('loginAsUser').setValue(mockUser());
            spectator.detectChanges();

            component.doLoginAs();

            // Assert
            await spectator.fixture.whenStable();
            expect(navigationService.goToFirstPortlet).toHaveBeenCalledTimes(1);
            expect(locationService.reload).toHaveBeenCalledTimes(1);
        });
    });

    describe('Error handling', () => {
        it('should set error message signal when login fails', fakeAsync(() => {
            // Arrange
            const mockDotMessageService = spectator.inject(DotMessageService);
            jest.spyOn(mockDotMessageService, 'get').mockReturnValue('wrong password');

            // Create a new spy for loginAs
            const loginServiceInstance = spectator.inject(LoginService);
            jest.spyOn(loginServiceInstance, 'loginAs').mockReturnValue(observableThrowError({}));

            // Make sure goToFirstPortlet returns a promise
            dotNavigationService.goToFirstPortlet.mockReturnValue(Promise.resolve(true));

            // Set up the component
            spectator.setInput('visible', true);
            component.needPassword.set(true);
            spectator.detectChanges();

            // Fill the form
            component.form.get('loginAsUser').setValue(mockUser());
            component.form.get('password').setValue('password');
            spectator.detectChanges();

            // Act - Call doLoginAs directly
            component.doLoginAs();

            // Use tick to simulate the passage of time
            tick();

            // Assert - Verify the error message signal has the correct value
            expect(component.errorMessage()).toBe('wrong password');
        }));

        it('should render error message in DOM when errorMessage signal has value', () => {
            // Arrange
            spectator.setInput('visible', true);
            spectator.detectChanges();

            // Act - Set the error message signal
            component.errorMessage.set('test error message');
            spectator.detectChanges();

            // Assert - Verify the error message is displayed in the DOM
            const errorMessageElement = spectator.query(byTestId('dot-login-as-error-message'));
            expect(errorMessageElement).toBeTruthy();
            expect(errorMessageElement.textContent.trim()).toBe('test error message');
        });

        it('should clear error message when user selects a different user', () => {
            // Arrange
            spectator.setInput('visible', true);
            component.errorMessage.set('Error message');
            spectator.detectChanges();

            // Act - Simulate user selecting a different user
            component.userSelectedHandler({ requestPassword: false } as User);
            spectator.detectChanges();

            // Assert
            expect(component.errorMessage()).toEqual('');
            expect(spectator.query(byTestId('dot-login-as-error-message'))).toBeNull();
        });
    });

    describe('Dialog interaction', () => {
        it('should close dialog when cancel button is clicked', () => {
            // Arrange
            jest.spyOn(component, 'close');
            spectator.setInput('visible', true);
            spectator.detectChanges();

            // Act
            spectator.click(byTestId('dot-login-as-cancel-button'));

            // Assert
            expect(component.close).toHaveBeenCalled();
        });
    });
});
