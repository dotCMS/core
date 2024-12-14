import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentSidebarUntranslatedLocaleComponent } from './dot-edit-content-sidebar-untranslated-locale.component';

describe('DotEditContentSidebarUntranslatedLocaleComponent', () => {
    let component: DotEditContentSidebarUntranslatedLocaleComponent;
    let fixture: ComponentFixture<DotEditContentSidebarUntranslatedLocaleComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentSidebarUntranslatedLocaleComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentSidebarUntranslatedLocaleComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
