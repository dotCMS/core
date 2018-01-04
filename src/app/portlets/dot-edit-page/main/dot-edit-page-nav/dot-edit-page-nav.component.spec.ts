import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { DotEditPageNavComponent } from './dot-edit-page-nav.component';
import { RouterTestingModule } from '@angular/router/testing';
import { MessageService } from '../../../../api/services/messages-service';
import { MockMessageService } from '../../../../test/message-service.mock';
import { By } from '@angular/platform-browser';

describe('DotEditPageNavComponent', () => {
    let component: DotEditPageNavComponent;
    let fixture: ComponentFixture<DotEditPageNavComponent>;

    const messageServiceMock = new MockMessageService({
        'editpage.toolbar.nav.content': 'Content',
        'editpage.toolbar.nav.layout': 'Layout'
    });

    beforeEach(
        async(() => {
            TestBed.configureTestingModule({
                imports: [RouterTestingModule],
                declarations: [DotEditPageNavComponent],
                providers: [{ provide: MessageService, useValue: messageServiceMock }]
            }).compileComponents();
        }),
    );

    beforeEach(() => {
        fixture = TestBed.createComponent(DotEditPageNavComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should have menu list', () => {
        const menuList = fixture.debugElement.query(By.css('.edit-page-nav'));
        expect(menuList).not.toBeNull();
    });

    it('should have menu items', () => {
        const menuListItems = fixture.debugElement.queryAll(By.css('.edit-page-nav__item'));
        expect(menuListItems.length).toEqual(2);

        const labels = ['Content', 'Layout'];
        menuListItems.forEach((item, index) => {
            expect(item.nativeElement.textContent).toContain(labels[index]);
        });
    });
});
