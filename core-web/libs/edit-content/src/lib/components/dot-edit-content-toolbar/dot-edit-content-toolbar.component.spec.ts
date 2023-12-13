import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentToolbarComponent } from './dot-edit-content-toolbar.component';

describe('DotEditContentToolbarComponent', () => {
    let component: DotEditContentToolbarComponent;
    let fixture: ComponentFixture<DotEditContentToolbarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentToolbarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentToolbarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
