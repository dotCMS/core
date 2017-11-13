import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotEditPageToolbarModule } from './dot-edit-page-toolbar.module';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { MessageService } from '../../../../../api/services/messages-service';
import { MockMessageService } from '../../../../../test/message-service.mock';

describe('DotEditPageToolbarComponent', () => {
    let component: DotEditPageToolbarComponent;
    let fixture: ComponentFixture<DotEditPageToolbarComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockMessageService({
        'editpage.toolbar.primary.action': 'Hello',
        'editpage.toolbar.secondary.action': 'World'
    });

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                imports: [DotEditPageToolbarModule],
                providers: [
                    {provide: MessageService, useValue: messageServiceMock}
                ]
            }).compileComponents();
        })
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageToolbarComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should have a toolbar element', () => {
        expect(de.query(By.css('p-toolbar'))).toBeTruthy();
    });

    it('should have a page title', () => {
        expect(de.query(By.css('.edit-page-toolbar__page-title'))).toBeTruthy();
    });

    it('should receive page title as a param', () => {
        component.pageTitle = 'Hello World';
        const pageTitleEl: HTMLElement = de.query(By.css('.edit-page-toolbar__page-title')).nativeElement;
        fixture.detectChanges();

        expect(pageTitleEl.textContent).toEqual('Hello World');
    });

    it('should have a primary action button', () => {
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__primary-action'));
        expect(primaryAction).toBeTruthy();

        const primaryActionEl: HTMLElement = primaryAction.nativeElement;
        expect(primaryActionEl.textContent).toEqual('Hello');
    });

    it('should have a primary secondary button', () => {
        const secondaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__secondary-action'));
        expect(secondaryAction).toBeTruthy();

        const secondaryActionEl: HTMLElement = secondaryAction.nativeElement;
        expect(secondaryActionEl.textContent).toEqual('World');
    });

    it('should emit save event on primary action button click', () => {
        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__primary-action'));

        let res;
        component.save.subscribe(event => {
            res = event;
        });

        primaryAction.nativeElement.click();

        expect(res).toBeDefined();
    });

    it('should emit cancel event on secondary action button click', () => {
        const secondaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__secondary-action'));

        let res;
        component.cancel.subscribe(event => {
            res = event;
        });

        secondaryAction.nativeElement.click();

        expect(res).toBeDefined();
    });
});
