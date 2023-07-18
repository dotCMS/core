import { of } from 'rxjs';

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';

import { Checkbox, CheckboxModule } from 'primeng/checkbox';

import { DotAccountService } from '@dotcms/app/api/services/dot-account-service';
import { DotRouterService } from '@dotcms/app/api/services/dot-router/dot-router.service';
import { DotMessageService } from '@dotcms/data-access';
import { CoreWebService } from '@dotcms/dotcms-js';
import { DotMessagePipe } from '@dotcms/ui';
import {
    CoreWebServiceMock,
    MockDotMessageService,
    MockDotRouterService
} from '@dotcms/utils-testing';

import { DotStarterResolver } from './dot-starter-resolver.service';
import { DotStarterComponent } from './dot-starter.component';

const messages = {
    'starter.title': 'Welcome!',
    'starter.description': 'You are logged in as <em>{0}</em>.',
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

const routeDatamock = {
    userData: {
        user: {
            email: 'admin@dotcms.com',
            givenName: 'Admin',
            roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
            surnaname: 'User',
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

class ActivatedRouteMock {
    get data() {
        return {};
    }
}

class DotAccountServiceMock {
    addStarterPage() {
        return of(true);
    }

    removeStarterPage() {
        return of(true);
    }
}

describe('DotStarterComponent', () => {
    let fixture: ComponentFixture<DotStarterComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService(messages);
    let dotAccountService: DotAccountService;
    let activatedRoute: ActivatedRoute;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [DotMessagePipe, CheckboxModule, HttpClientTestingModule],
            declarations: [DotStarterComponent],
            providers: [
                { provide: DotMessageService, useValue: messageServiceMock },
                {
                    provide: ActivatedRoute,
                    useClass: ActivatedRouteMock
                },
                { provide: CoreWebService, useClass: CoreWebServiceMock },
                { provide: DotRouterService, useClass: MockDotRouterService },
                DotStarterResolver,
                { provide: DotAccountService, useClass: DotAccountServiceMock }
            ]
        });

        fixture = TestBed.createComponent(DotStarterComponent);

        de = fixture.debugElement;
        dotAccountService = TestBed.inject(DotAccountService);
        activatedRoute = TestBed.inject(ActivatedRoute);
    }));

    describe('With user permissions', () => {
        beforeEach(() => {
            spyOnProperty(activatedRoute, 'data').and.returnValue(of(routeDatamock));
            fixture.detectChanges();
        });

        it('should set proper labels to the main container', () => {
            expect(de.query(By.css('.dot-starter-title')).nativeElement.innerText).toContain(
                messageServiceMock.get('starter.title')
            );
            expect(de.query(By.css('.dot-starter-description')).nativeElement.innerText).toContain(
                'You are logged in as Admin'
            );
            expect(
                de.query(By.css('[data-testId="starter.main.link.data.model"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.data.model.title'));
            expect(
                de.query(By.css('[data-testId="starter.main.link.data.model"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.data.model.description'));

            expect(
                de.query(By.css('[data-testId="starter.main.link.content"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.add.content.title'));
            expect(
                de.query(By.css('[data-testId="starter.main.link.content"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.add.content.description'));

            expect(
                de.query(By.css('[data-testId="starter.main.link.design.layout"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.design.layout.title'));
            expect(
                de.query(By.css('[data-testId="starter.main.link.design.layout"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.design.layout.description'));

            expect(
                de.query(By.css('[data-testId="starter.main.link.create.page"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.create.page.title'));
            expect(
                de.query(By.css('[data-testId="starter.main.link.create.page"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.main.link.create.page.description'));
        });

        it('should set proper labels to the side container', () => {
            expect(
                de.query(By.css('.dot-starter-top-secondary__section h3')).nativeElement.innerText
            ).toContain(messageServiceMock.get('starter.side.title'));
            expect(
                de.query(By.css('[data-testId="starter.side.link.graphQl"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.graphQl.title'));
            expect(
                de.query(By.css('[data-testId="starter.side.link.graphQl"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.graphQl.description'));

            expect(
                de.query(By.css('[data-testId="starter.side.link.content"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.content.title'));
            expect(
                de.query(By.css('[data-testId="starter.side.link.content"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.content.description'));

            expect(
                de.query(By.css('[data-testId="starter.side.link.image.processing"] h4'))
                    .nativeElement.innerText
            ).toContain(messageServiceMock.get('starter.side.link.image.processing.title'));
            expect(
                de.query(By.css('[data-testId="starter.side.link.image.processing"] p'))
                    .nativeElement.innerText
            ).toContain(messageServiceMock.get('starter.side.link.image.processing.description'));

            expect(
                de.query(By.css('[data-testId="starter.side.link.page.layout"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.page.layout.title'));
            expect(
                de.query(By.css('[data-testId="starter.side.link.page.layout"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.page.layout.description'));

            expect(
                de.query(By.css('[data-testId="starter.side.link.generate.key"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.generate.key.title'));
            expect(
                de.query(By.css('[data-testId="starter.side.link.generate.key"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.side.link.generate.key.description'));
        });

        it('should set proper labels to the footer container', () => {
            expect(
                de.query(By.css('[data-testId="starter.footer.link.documentation"] h4'))
                    .nativeElement.innerText
            ).toContain(messageServiceMock.get('starter.footer.link.documentation.title'));
            expect(
                de.query(By.css('[data-testId="starter.footer.link.documentation"] p'))
                    .nativeElement.innerText
            ).toContain(messageServiceMock.get('starter.footer.link.documentation.description'));

            expect(
                de.query(By.css('[data-testId="starter.footer.link.examples"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.examples.title'));
            expect(
                de.query(By.css('[data-testId="starter.footer.link.examples"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.examples.description'));

            expect(
                de.query(By.css('[data-testId="starter.footer.link.community"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.community.title'));
            expect(
                de.query(By.css('[data-testId="starter.footer.link.community"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.community.description'));

            expect(
                de.query(By.css('[data-testId="starter.footer.link.training"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.training.title'));
            expect(
                de.query(By.css('[data-testId="starter.footer.link.training"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.training.description'));

            expect(
                de.query(By.css('[data-testId="starter.footer.link.review"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.review.title'));
            expect(
                de.query(By.css('[data-testId="starter.footer.link.review"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.review.description'));

            expect(
                de.query(By.css('[data-testId="starter.footer.link.feedback"] h4')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.feedback.title'));
            expect(
                de.query(By.css('[data-testId="starter.footer.link.feedback"] p')).nativeElement
                    .innerText
            ).toContain(messageServiceMock.get('starter.footer.link.feedback.description'));
        });

        it('should have right links to internal portlets', () => {
            expect(
                de.query(By.css('[data-testId="starter.main.link.data.model"]')).nativeElement
                    .attributes['routerLink'].value
            ).toEqual('/content-types-angular/create/content');

            expect(
                de.query(By.css('[data-testId="starter.main.link.content"]')).nativeElement
                    .attributes['routerLink'].value
            ).toEqual('/c/content/new/webPageContent');

            expect(
                de.query(By.css('[data-testId="starter.main.link.design.layout"]')).nativeElement
                    .attributes['routerLink'].value
            ).toEqual('/templates/new/designer');

            expect(
                de.query(By.css('[data-testId="starter.main.link.create.page"]')).nativeElement
                    .attributes['routerLink'].value
            ).toEqual('/c/content/new/htmlpageasset');
        });

        it('should call the endpoint to hide/show the portlet', () => {
            const checkBox: Checkbox = de.query(By.css('p-checkbox')).componentInstance;
            const boxEl = fixture.nativeElement.querySelector('.p-checkbox-box');

            spyOn(dotAccountService, 'addStarterPage').and.callThrough();
            spyOn(dotAccountService, 'removeStarterPage').and.callThrough();

            expect(checkBox.label).toEqual(messageServiceMock.get('starter.dont.show'));
            boxEl.click();
            expect(dotAccountService.removeStarterPage).toHaveBeenCalledTimes(1);
            boxEl.click();
            expect(dotAccountService.addStarterPage).toHaveBeenCalledTimes(1);
        });
    });

    describe('Without user permissions', () => {
        beforeEach(() => {
            spyOnProperty(activatedRoute, 'data').and.returnValue(
                of({
                    userData: {
                        user: {
                            email: 'admin@dotcms.com',
                            givenName: 'Admin',
                            roleId: 'e7d23sde-5127-45fc-8123-d424fd510e3',
                            surnaname: 'User',
                            userId: 'testId'
                        },
                        permissions: {
                            STRUCTURES: { canRead: true, canWrite: false },
                            HTMLPAGES: { canRead: true, canWrite: false },
                            TEMPLATES: { canRead: true, canWrite: false },
                            CONTENTLETS: { canRead: true, canWrite: false }
                        }
                    }
                })
            );
            fixture.detectChanges();
        });

        it('should hide links from the main container', () => {
            expect(de.query(By.css('[data-testId="starter.main.link.data.model"]'))).toBeFalsy();
            expect(de.query(By.css('[data-testId="starter.main.link.content"]'))).toBeFalsy();
            expect(de.query(By.css('[data-testId="starter.main.link.design.layout"]'))).toBeFalsy();
            expect(de.query(By.css('[data-testId="starter.main.link.create.page"]'))).toBeFalsy();
        });
    });
});
