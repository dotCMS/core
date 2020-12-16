import { of } from 'rxjs';
import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ActivatedRoute } from '@angular/router';
import { CoreWebService } from 'dotcms-js';
import { CoreWebServiceMock } from '@tests/core-web.service.mock';
import { DotStarterComponent } from './dot-starter.component';
import { DotStarterResolver } from './dot-starter-resolver.service';
import { By } from '@angular/platform-browser';
import { DebugElement } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotToolGroupService } from '@services/dot-tool-group/dot-tool-group.service';
import { Checkbox, CheckboxModule } from 'primeng/checkbox';
import { HttpClientTestingModule } from '@angular/common/http/testing';

const messages = {
    'starter.title': 'Welcome!',
    'starter.description': 'You are logged in as <em>{0}</em>.',
    'starter.dont.show': `Don't show this again`,
    'starter.main.link.data.model.title': 'Create data model',
    'starter.main.link.data.model.description': 'Lorem, ipsum dolor sit amet...',
    'starter.main.link.add.content.title': 'Add content',
    'starter.main.link.add.content.description': 'Lorem, ipsum dolor sit amet...',
    'starter.main.link.design.layout.title': 'Design a layout',
    'starter.main.link.design.layout.description': 'Lorem, ipsum dolor sit amet...',
    'starter.main.link.create.page.title': 'Create a page',
    'starter.main.link.create.page.description': 'Lorem, ipsum dolor sit amet...',
    'starter.side.title': 'APIs and Services',
    'starter.side.link.graphQl.title': 'GraphQL API',
    'starter.side.link.graphQl.description': 'GraphQL is an open query language...',
    'starter.side.link.content.title': 'Content API',
    'starter.side.link.content.description': 'GraphQL is an open query language...',
    'starter.side.link.image.processing.title': 'Image Resizing and Processing',
    'starter.side.link.image.processing.description': 'GraphQL is an open query .',
    'starter.side.link.page.layout.title': 'Page Layout API (Layout as a Service)',
    'starter.side.link.page.layout.description': 'The Page REST API enables you...',
    'starter.side.link.generate.key.title': 'Generate API Key',
    'starter.side.link.generate.key.description': 'The Page REST API enables you...',
    'starter.footer.link.documentation.title': 'Documentation',
    'starter.footer.link.documentation.description': 'Lorem, ipsum dolor sit amet...',
    'starter.footer.link.examples.title': 'Examples',
    'starter.footer.link.examples.description': 'Lorem, ipsum dolor sit amet...',
    'starter.footer.link.community.title': 'Community',
    'starter.footer.link.community.description': 'Lorem, ipsum dolor sit amet...',
    'starter.footer.link.training.title': 'Training Videos',
    'starter.footer.link.training.description': 'Lorem, ipsum dolor sit amet...',
    'starter.footer.link.review.title': 'Write A Review',
    'starter.footer.link.review.description': 'Lorem, ipsum dolor sit amet...',
    'starter.footer.link.feedback.title': 'Feedback',
    'starter.footer.link.feedback.description': 'Lorem, ipsum dolor sit amet...'
};

const routeDatamock = {
    username: 'Admin'
};
class ActivatedRouteMock {
    get data() {
        return of(routeDatamock);
    }
}

describe('DotStarterComponent', () => {
    let component: DotStarterComponent;
    let fixture: ComponentFixture<DotStarterComponent>;
    let de: DebugElement;
    const messageServiceMock = new MockDotMessageService(messages);
    let dotToolGroupService: DotToolGroupService;

    beforeEach(
        waitForAsync(() => {
            TestBed.configureTestingModule({
                imports: [DotMessagePipeModule, CheckboxModule, HttpClientTestingModule],
                declarations: [DotStarterComponent],
                providers: [
                    { provide: DotMessageService, useValue: messageServiceMock },
                    {
                        provide: ActivatedRoute,
                        useClass: ActivatedRouteMock
                    },
                    { provide: CoreWebService, useClass: CoreWebServiceMock },
                    DotStarterResolver,
                    DotToolGroupService
                ]
            });

            fixture = TestBed.createComponent(DotStarterComponent);
            component = fixture.debugElement.componentInstance;

            fixture.detectChanges();
            de = fixture.debugElement;
            dotToolGroupService = TestBed.inject(DotToolGroupService);
        })
    );

    it('should set username from resolver', () => {
        expect(component.username).toBe('Admin');
    });

    it('should set proper labels to the main container', () => {
        expect(de.query(By.css('.dot-starter-title')).nativeElement.innerText).toContain(
            messageServiceMock.get('starter.title')
        );
        expect(de.query(By.css('.dot-starter-description')).nativeElement.innerText).toContain(
            'You are logged in as Admin'
        );
        expect(
            de.query(By.css('[data-testid="starter.main.link.data.model"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.main.link.data.model.title'));
        expect(
            de.query(By.css('[data-testid="starter.main.link.data.model"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.main.link.data.model.description'));

        expect(
            de.query(By.css('[data-testid="starter.main.link.content"] h4')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.main.link.add.content.title'));
        expect(
            de.query(By.css('[data-testid="starter.main.link.content"] p')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.main.link.add.content.description'));

        expect(
            de.query(By.css('[data-testid="starter.main.link.design.layout"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.main.link.design.layout.title'));
        expect(
            de.query(By.css('[data-testid="starter.main.link.design.layout"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.main.link.design.layout.description'));

        expect(
            de.query(By.css('[data-testid="starter.main.link.create.page"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.main.link.create.page.title'));
        expect(
            de.query(By.css('[data-testid="starter.main.link.create.page"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.main.link.create.page.description'));
    });

    it('should set proper labels to the side container', () => {
        expect(
            de.query(By.css('.dot-starter-top-secondary__section h3')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.side.title'));
        expect(
            de.query(By.css('[data-testid="starter.side.link.graphQl"] h4')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.side.link.graphQl.title'));
        expect(
            de.query(By.css('[data-testid="starter.side.link.graphQl"] p')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.side.link.graphQl.description'));

        expect(
            de.query(By.css('[data-testid="starter.side.link.content"] h4')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.side.link.content.title'));
        expect(
            de.query(By.css('[data-testid="starter.side.link.content"] p')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.side.link.content.description'));

        expect(
            de.query(By.css('[data-testid="starter.side.link.image.processing"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.side.link.image.processing.title'));
        expect(
            de.query(By.css('[data-testid="starter.side.link.image.processing"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.side.link.image.processing.description'));

        expect(
            de.query(By.css('[data-testid="starter.side.link.page.layout"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.side.link.page.layout.title'));
        expect(
            de.query(By.css('[data-testid="starter.side.link.page.layout"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.side.link.page.layout.description'));

        expect(
            de.query(By.css('[data-testid="starter.side.link.generate.key"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.side.link.generate.key.title'));
        expect(
            de.query(By.css('[data-testid="starter.side.link.generate.key"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.side.link.generate.key.description'));
    });

    it('should set proper labels to the footer container', () => {
        expect(
            de.query(By.css('[data-testid="starter.footer.link.documentation"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.documentation.title'));
        expect(
            de.query(By.css('[data-testid="starter.footer.link.documentation"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.documentation.description'));

        expect(
            de.query(By.css('[data-testid="starter.footer.link.examples"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.examples.title'));
        expect(
            de.query(By.css('[data-testid="starter.footer.link.examples"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.examples.description'));

        expect(
            de.query(By.css('[data-testid="starter.footer.link.community"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.community.title'));
        expect(
            de.query(By.css('[data-testid="starter.footer.link.community"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.community.description'));

        expect(
            de.query(By.css('[data-testid="starter.footer.link.training"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.training.title'));
        expect(
            de.query(By.css('[data-testid="starter.footer.link.training"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.training.description'));

        expect(
            de.query(By.css('[data-testid="starter.footer.link.review"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.review.title'));
        expect(
            de.query(By.css('[data-testid="starter.footer.link.review"] p')).nativeElement.innerText
        ).toContain(messageServiceMock.get('starter.footer.link.review.description'));

        expect(
            de.query(By.css('[data-testid="starter.footer.link.feedback"] h4')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.feedback.title'));
        expect(
            de.query(By.css('[data-testid="starter.footer.link.feedback"] p')).nativeElement
                .innerText
        ).toContain(messageServiceMock.get('starter.footer.link.feedback.description'));
    });

    it('should call the endpoint to hide/show the portlet', () => {
        const checkBox: Checkbox = de.query(By.css('p-checkbox')).componentInstance;
        const boxEl = fixture.nativeElement.querySelector('.p-checkbox-box');

        spyOn(dotToolGroupService, 'show').and.returnValue(of({ message: 'test' }));
        spyOn(dotToolGroupService, 'hide').and.returnValue(of({ message: 'test' }));

        expect(checkBox.label).toEqual(messageServiceMock.get('starter.dont.show'));
        boxEl.click();
        expect(dotToolGroupService.hide).toHaveBeenCalledWith('gettingstarted');
        boxEl.click();
        expect(dotToolGroupService.show).toHaveBeenCalledWith('gettingstarted');
    });
});
