import { async, ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DotEditPageToolbarComponent } from './dot-edit-page-toolbar.component';
import { DotEditPageToolbarModule } from './dot-edit-page-toolbar.module';
import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';

describe('DotEditPageToolbarComponent', () => {
    let component: DotEditPageToolbarComponent;
    let fixture: ComponentFixture<DotEditPageToolbarComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.primary.action': 'Hello',
    });

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                imports: [DotEditPageToolbarModule],
                providers: [
                    {provide: DotMessageService, useValue: messageServiceMock}
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

    it('should emit save event on primary action button click', () => {
        component.canSave = true;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__primary-action'));

        let res;
        component.save.subscribe(event => {
            res = event;
        });
        primaryAction.nativeElement.click();

        expect(res).toBeDefined();
    });

    it('should disabled save button', () => {
        component.canSave = false;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__primary-action'));
        expect(primaryAction.nativeElement.disabled).toBeTruthy('the save button have to be disabled');
    });

    it('should enabled save button', () => {
        component.canSave = true;

        fixture.detectChanges();

        const primaryAction: DebugElement = de.query(By.css('.edit-page-toolbar__primary-action'));
        expect(primaryAction.nativeElement.disabled).toBeFalsy('the save button have to be enable');
    });
});
