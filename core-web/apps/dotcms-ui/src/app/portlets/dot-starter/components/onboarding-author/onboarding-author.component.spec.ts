import { createComponentFactory, Spectator, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { DotCurrentUserService, DotMessageService } from '@dotcms/data-access';
import { PermissionsType } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotOnboardingAuthorComponent } from './onboarding-author.component';

import { DotAccountService } from '../../../../api/services/dot-account-service';

const userMock = {
    admin: true,
    email: 'admin@dotcms.com',
    givenName: 'Admin',
    roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
    surname: 'User',
    userId: 'testId'
};

const permissionsWithWrite = {
    [PermissionsType.STRUCTURES]: { canRead: true, canWrite: true },
    [PermissionsType.HTMLPAGES]: { canRead: true, canWrite: true },
    [PermissionsType.TEMPLATES]: { canRead: true, canWrite: true },
    [PermissionsType.CONTENTLETS]: { canRead: true, canWrite: true }
};

const permissionsWithoutWrite = {
    [PermissionsType.STRUCTURES]: { canRead: true, canWrite: false },
    [PermissionsType.HTMLPAGES]: { canRead: true, canWrite: false },
    [PermissionsType.TEMPLATES]: { canRead: true, canWrite: false },
    [PermissionsType.CONTENTLETS]: { canRead: true, canWrite: false }
};

const messages = {
    'starter.description': 'You are logged in as {0}.',
    'starter.dont.show': `Don't show this again`,
    'starter.main.link.data.model.title': 'Create data model',
    'starter.main.link.add.content.title': 'Add content',
    'starter.main.link.design.layout.title': 'Design a layout',
    'starter.main.link.create.page.title': 'Create a page',
    'starter.side.title': 'APIs and Services',
    'starter.side.resources.title': 'Resources'
};

describe('DotOnboardingAuthorComponent', () => {
    describe('With user write permissions', () => {
        let spectator: Spectator<DotOnboardingAuthorComponent>;
        const messageServiceMock = new MockDotMessageService(messages);

        const createComponent = createComponentFactory({
            component: DotOnboardingAuthorComponent,
            imports: [DotMessagePipe],
            componentProviders: [
                mockProvider(DotAccountService, {
                    addStarterPage: jest.fn().mockReturnValue(of('')),
                    removeStarterPage: jest.fn().mockReturnValue(of(''))
                })
            ],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                provideRouter([]),
                { provide: DotMessageService, useValue: messageServiceMock },
                mockProvider(DotCurrentUserService, {
                    getCurrentUser: () => of(userMock),
                    getUserPermissions: () => of(permissionsWithWrite)
                })
            ]
        });

        beforeEach(() => {
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should create', () => {
            expect(spectator.component).toBeTruthy();
        });

        it('should call removeStarterPage when handleVisibility(true)', () => {
            const dotAccountService =
                spectator.fixture.debugElement.injector.get(DotAccountService);
            spectator.component.handleVisibility(true);
            expect(dotAccountService.removeStarterPage).toHaveBeenCalledTimes(1);
        });

        it('should call addStarterPage when handleVisibility(false)', () => {
            const dotAccountService =
                spectator.fixture.debugElement.injector.get(DotAccountService);
            spectator.component.handleVisibility(false);
            expect(dotAccountService.addStarterPage).toHaveBeenCalledTimes(1);
        });

        it('should emit reset-user-profile on resetUserProfile', () => {
            const emitSpy = jest.spyOn(spectator.component.eventEmitter, 'emit');
            spectator.component.resetUserProfile();
            expect(emitSpy).toHaveBeenCalledWith('reset-user-profile');
        });

        it('should show main links when user has write permissions', (done) => {
            spectator.component.userData$.subscribe((user) => {
                expect(user.showCreateDataModelLink).toBe(true);
                expect(user.showCreateContentLink).toBe(true);
                expect(user.showCreateTemplateLink).toBe(true);
                expect(user.showCreatePageLink).toBe(true);
                expect(user.username).toBe('Admin');
                done();
            });
        });
    });

    describe('Without user write permissions', () => {
        let spectator: Spectator<DotOnboardingAuthorComponent>;

        const createComponent = createComponentFactory({
            component: DotOnboardingAuthorComponent,
            imports: [DotMessagePipe],
            componentProviders: [
                mockProvider(DotAccountService, {
                    addStarterPage: jest.fn().mockReturnValue(of('')),
                    removeStarterPage: jest.fn().mockReturnValue(of(''))
                })
            ],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                provideRouter([]),
                { provide: DotMessageService, useValue: new MockDotMessageService({}) },
                mockProvider(DotCurrentUserService, {
                    getCurrentUser: () => of(userMock),
                    getUserPermissions: () => of(permissionsWithoutWrite)
                })
            ]
        });

        beforeEach(() => {
            spectator = createComponent();
            spectator.detectChanges();
        });

        it('should not show main links when user lacks write permissions', (done) => {
            spectator.component.userData$.subscribe((user) => {
                expect(user.showCreateDataModelLink).toBe(false);
                expect(user.showCreateContentLink).toBe(false);
                expect(user.showCreateTemplateLink).toBe(false);
                expect(user.showCreatePageLink).toBe(false);
                done();
            });
        });
    });
});
