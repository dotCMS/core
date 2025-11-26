import { createComponentFactory, Spectator, byTestId, mockProvider } from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';

import { CheckboxModule, Checkbox } from 'primeng/checkbox';

import { DotMessageService, DotRouterService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterComponent } from './dot-starter.component';

import { DotAccountService } from '../../api/services/dot-account-service';

const messages = {
    'starter.title': 'Welcome!',
    'starter.description':
        'You are logged in as {0}. To help you get started building with dotCMS we provided some quick links.',
    'starter.dont.show': `Don't show this again`,
    'starter.main.link.data.model.title': 'Create data model',
    'starter.main.link.data.model.description': 'Create data model description',
    'starter.main.link.add.content.title': 'Add content',
    'starter.main.link.add.content.description': 'Add content description',
    'starter.main.link.design.layout.title': 'Design a layout',
    'starter.main.link.design.layout.description': 'Design a layout description',
    'starter.main.link.create.page.title': 'Create a page',
    'starter.main.link.create.page.description': 'Create a page description',
    'starter.side.title': 'APIs and Services',
    'starter.side.link.graphQl.title': 'GraphQL API',
    'starter.side.link.graphQl.description': 'GraphQL API description',
    'starter.side.link.content.title': 'Content API',
    'starter.side.link.content.description': 'Content API description',
    'starter.side.link.image.processing.title': 'Image Resizing and Processing',
    'starter.side.link.image.processing.description': 'Image Resizing and Processing description',
    'starter.side.link.page.layout.title': 'Page Layout API (Layout as a Service)',
    'starter.side.link.page.layout.description': 'Page Layout API description',
    'starter.side.link.generate.key.title': 'Generate API Token',
    'starter.side.link.generate.key.description': 'Generate API Token description',
    'starter.footer.link.documentation.title': 'Documentation',
    'starter.footer.link.documentation.description': 'Documentation description',
    'starter.footer.link.examples.title': 'Examples',
    'starter.footer.link.examples.description': 'Examples description',
    'starter.footer.link.community.title': 'Community',
    'starter.footer.link.community.description': 'Community description',
    'starter.footer.link.training.title': 'Training Videos',
    'starter.footer.link.training.description': 'Training Videos description',
    'starter.footer.link.review.title': 'Write A Review',
    'starter.footer.link.review.description': 'Write A Review description',
    'starter.footer.link.feedback.title': 'Feedback',
    'starter.footer.link.feedback.description': 'Feedback description'
};

const routeDataMock = {
    userData: {
        user: {
            email: 'admin@dotcms.com',
            givenName: 'Admin',
            roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
            surname: 'User',
            userId: 'testId'
        },
        permissions: {
            STRUCTURES: { canRead: true, canWrite: true },
            HTMLPAGES: { canRead: true, canWrite: true },
            TEMPLATES: { canRead: true, canWrite: true },
            CONTENTLETS: { canRead: true, canWrite: true }
        }
    }
};

const routeDataWithoutPermissionsMock = {
    userData: {
        user: {
            email: 'admin@dotcms.com',
            givenName: 'Admin',
            roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
            surname: 'User',
            userId: 'testId'
        },
        permissions: {
            STRUCTURES: { canRead: true, canWrite: false },
            HTMLPAGES: { canRead: true, canWrite: false },
            TEMPLATES: { canRead: true, canWrite: false },
            CONTENTLETS: { canRead: true, canWrite: false }
        }
    }
};

class ActivatedRouteMock {
    get data() {
        return of(routeDataMock);
    }
}

describe('DotStarterComponent', () => {
    describe('With user permissions', () => {
        let spectator: Spectator<DotStarterComponent>;
        const messageServiceMock = new MockDotMessageService(messages);

        const createComponent = createComponentFactory({
            component: DotStarterComponent,
            imports: [DotMessagePipe, CheckboxModule],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: messageServiceMock },
                { provide: ActivatedRoute, useClass: ActivatedRouteMock },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                DotStarterResolver,
                mockProvider(DotAccountService, {
                    addStarterPage: jest.fn().mockReturnValue(of(true)),
                    removeStarterPage: jest.fn().mockReturnValue(of(true))
                })
            ]
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        describe('With user permissions', () => {
            it('should set proper labels to the main container', () => {
                expect(spectator.query('.dot-starter-description')).toHaveText(
                    'You are logged in as Admin. To help you get started building with dotCMS we provided some quick links.'
                );

                expect(spectator.query(byTestId('starter.main.link.data.model'))).toHaveText(
                    messageServiceMock.get('starter.main.link.data.model.title')
                );
                expect(spectator.query(byTestId('starter.main.link.data.model'))).toHaveText(
                    messageServiceMock.get('starter.main.link.data.model.description')
                );

                expect(spectator.query(byTestId('starter.main.link.content'))).toHaveText(
                    messageServiceMock.get('starter.main.link.add.content.title')
                );
                expect(spectator.query(byTestId('starter.main.link.content'))).toHaveText(
                    messageServiceMock.get('starter.main.link.add.content.description')
                );

                expect(spectator.query(byTestId('starter.main.link.design.layout'))).toHaveText(
                    messageServiceMock.get('starter.main.link.design.layout.title')
                );
                expect(spectator.query(byTestId('starter.main.link.design.layout'))).toHaveText(
                    messageServiceMock.get('starter.main.link.design.layout.description')
                );

                expect(spectator.query(byTestId('starter.main.link.create.page'))).toHaveText(
                    messageServiceMock.get('starter.main.link.create.page.title')
                );
                expect(spectator.query(byTestId('starter.main.link.create.page'))).toHaveText(
                    messageServiceMock.get('starter.main.link.create.page.description')
                );
            });

            it('should set proper labels to the side container', () => {
                expect(spectator.query(byTestId('dot-side-title'))).toHaveText(
                    messageServiceMock.get('starter.side.title')
                );

                expect(spectator.query(byTestId('starter.side.link.graphQl'))).toHaveText(
                    messageServiceMock.get('starter.side.link.graphQl.title')
                );
                expect(spectator.query(byTestId('starter.side.link.graphQl'))).toHaveText(
                    messageServiceMock.get('starter.side.link.graphQl.description')
                );

                expect(spectator.query(byTestId('starter.side.link.content'))).toHaveText(
                    messageServiceMock.get('starter.side.link.content.title')
                );
                expect(spectator.query(byTestId('starter.side.link.content'))).toHaveText(
                    messageServiceMock.get('starter.side.link.content.description')
                );

                expect(spectator.query(byTestId('starter.side.link.image.processing'))).toHaveText(
                    messageServiceMock.get('starter.side.link.image.processing.title')
                );
                expect(spectator.query(byTestId('starter.side.link.image.processing'))).toHaveText(
                    messageServiceMock.get('starter.side.link.image.processing.description')
                );

                expect(spectator.query(byTestId('starter.side.link.page.layout'))).toHaveText(
                    messageServiceMock.get('starter.side.link.page.layout.title')
                );
                expect(spectator.query(byTestId('starter.side.link.page.layout'))).toHaveText(
                    messageServiceMock.get('starter.side.link.page.layout.description')
                );

                expect(spectator.query(byTestId('starter.side.link.generate.key'))).toHaveText(
                    messageServiceMock.get('starter.side.link.generate.key.title')
                );
                expect(spectator.query(byTestId('starter.side.link.generate.key'))).toHaveText(
                    messageServiceMock.get('starter.side.link.generate.key.description')
                );
            });

            it('should set proper labels to the footer container', () => {
                expect(spectator.query(byTestId('starter.footer.link.documentation'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.documentation.title')
                );
                expect(spectator.query(byTestId('starter.footer.link.documentation'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.documentation.description')
                );

                expect(spectator.query(byTestId('starter.footer.link.examples'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.examples.title')
                );
                expect(spectator.query(byTestId('starter.footer.link.examples'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.examples.description')
                );

                expect(spectator.query(byTestId('starter.footer.link.community'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.community.title')
                );
                expect(spectator.query(byTestId('starter.footer.link.community'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.community.description')
                );

                expect(spectator.query(byTestId('starter.footer.link.training'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.training.title')
                );
                expect(spectator.query(byTestId('starter.footer.link.training'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.training.description')
                );

                expect(spectator.query(byTestId('starter.footer.link.review'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.review.title')
                );
                expect(spectator.query(byTestId('starter.footer.link.review'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.review.description')
                );

                expect(spectator.query(byTestId('starter.footer.link.feedback'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.feedback.title')
                );
                expect(spectator.query(byTestId('starter.footer.link.feedback'))).toHaveText(
                    messageServiceMock.get('starter.footer.link.feedback.description')
                );
            });

            it('should have right links to internal portlets', () => {
                expect(spectator.query(byTestId('starter.main.link.data.model'))).toHaveAttribute(
                    'routerLink',
                    '/content-types-angular/create/content'
                );

                expect(spectator.query(byTestId('starter.main.link.content'))).toHaveAttribute(
                    'routerLink',
                    '/c/content/new/webPageContent'
                );

                expect(
                    spectator.query(byTestId('starter.main.link.design.layout'))
                ).toHaveAttribute('routerLink', '/templates/new/designer');

                expect(spectator.query(byTestId('starter.main.link.create.page'))).toHaveAttribute(
                    'routerLink',
                    '/c/content/new/htmlpageasset'
                );
            });

            it('should call the endpoint to hide/show the portlet', () => {
                const dotAccountService = spectator.inject(DotAccountService);
                const checkbox = spectator.query(Checkbox);

                expect(checkbox.label).toBe(messageServiceMock.get('starter.dont.show'));

                spectator.component.handleVisibility(true);
                expect(dotAccountService.removeStarterPage).toHaveBeenCalledTimes(1);

                spectator.component.handleVisibility(false);
                expect(dotAccountService.addStarterPage).toHaveBeenCalledTimes(1);
            });
        });
    });

    describe('Without user permissions', () => {
        let spectator: Spectator<DotStarterComponent>;
        const messageServiceMock = new MockDotMessageService(messages);

        const createComponent = createComponentFactory({
            component: DotStarterComponent,
            imports: [DotMessagePipe, CheckboxModule],
            providers: [
                provideHttpClient(),
                provideHttpClientTesting(),
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useValue: { data: of(routeDataWithoutPermissionsMock) }
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                DotStarterResolver,
                mockProvider(DotAccountService, {
                    addStarterPage: jest.fn().mockReturnValue(of(true)),
                    removeStarterPage: jest.fn().mockReturnValue(of(true))
                })
            ]
        });

        beforeEach(() => {
            spectator = createComponent();
        });

        it('should hide links from the main container', () => {
            spectator.detectChanges();

            expect(spectator.query(byTestId('starter.main.link.data.model'))).toBeFalsy();
            expect(spectator.query(byTestId('starter.main.link.content'))).toBeFalsy();
            expect(spectator.query(byTestId('starter.main.link.design.layout'))).toBeFalsy();
            expect(spectator.query(byTestId('starter.main.link.create.page'))).toBeFalsy();
        });
    });
});
