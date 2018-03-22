import { DebugElement } from '@angular/core/src/debug/debug_node';
import { By } from '@angular/platform-browser';
import { DotEditPageNavComponent } from './dot-edit-page-nav.component';
import { DotMessageService } from '../../../../api/services/dot-messages-service';
import { DotRenderedPageState } from '../../shared/models/dot-rendered-page-state.model';
import { MockDotMessageService } from '../../../../test/dot-message-service.mock';
import { RouterTestingModule } from '@angular/router/testing';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { mockDotRenderedPage } from './../../../../test/dot-rendered-page.mock';
import { mockUser } from './../../../../test/login-service.mock';

describe('DotEditPageNavComponent', () => {
    let component: DotEditPageNavComponent;
    let fixture: ComponentFixture<DotEditPageNavComponent>;

    const messageServiceMock = new MockDotMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.layout': 'Layout',
        'editpage.toolbar.nav.code': 'Code'
    });

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                imports: [RouterTestingModule],
                declarations: [DotEditPageNavComponent],
                providers: [{ provide: DotMessageService, useValue: messageServiceMock }]
            }).compileComponents();
        })
    );

    describe('basic setup', () => {
        beforeEach(() => {
            fixture = TestBed.createComponent(DotEditPageNavComponent);
            component = fixture.componentInstance;
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
            fixture.detectChanges();
        });

        it('should have menu list', () => {
            const menuList = fixture.debugElement.query(By.css('.edit-page-nav'));
            expect(menuList).not.toBeNull();
        });

        it('should have basic menu items', () => {
            component.pageState = new DotRenderedPageState(mockUser, mockDotRenderedPage);
            fixture.detectChanges();
            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems.length).toEqual(2);

            const labels = ['Content', 'Layout'];
            const icons = ['fa fa-file-text', 'fa fa-th-large'];
            menuListItems.forEach((item, index) => {
                const iconClass = item.query(By.css('i')).nativeElement.classList.value;
                expect(iconClass).toEqual(icons[index]);
                expect(item.nativeElement.textContent).toContain(labels[index]);
            });
        });
    });

    describe('advanced template', () => {
        beforeEach(() => {
            fixture = TestBed.createComponent(DotEditPageNavComponent);
            component = fixture.componentInstance;
        });

        it('should have menu items: Content and Code', () => {
            component.pageState = new DotRenderedPageState(
                mockUser,
                {
                    ...mockDotRenderedPage,
                    template: {
                        ...mockDotRenderedPage.template,
                        drawed: false
                    }
                },
                null
            );
            fixture.detectChanges();
            const menuListItems: DebugElement[] = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems.length).toEqual(2);

            const labels = ['Content', 'Code'];
            const icons = ['fa fa-file-text', 'fa fa-code'];
            menuListItems.forEach((item: DebugElement, index: number) => {
                const iconClass = item.query(By.css('i')).nativeElement.classList.value;
                expect(iconClass).toEqual(icons[index]);
                expect(item.nativeElement.textContent).toContain(labels[index]);
            });
        });

        it('should have code option disabled because user can\'t edit the page thus the layout or template', () => {
            component.pageState = new DotRenderedPageState(
                mockUser,
                {
                    ...mockDotRenderedPage,
                    page: {
                        ...mockDotRenderedPage.page,
                        canEdit: false
                    }
                },
                null
            );
            fixture.detectChanges();

            const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
            expect(menuListItems[1].nativeElement.classList).toContain('edit-page-nav__item--disabled');

            const labels = ['Content', 'Layout'];
            const icons = ['fa fa-file-text', 'fa fa-th-large'];
            menuListItems.forEach((item, index) => {
                const iconClass = item.query(By.css('i')).nativeElement.classList.value;
                expect(iconClass).toEqual(icons[index]);
                expect(item.nativeElement.textContent).toContain(labels[index]);
            });
        });
    });
});
