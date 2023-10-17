import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditContentTextAreaComponent } from './dot-edit-content-text-area.component';

describe('DotEditContentTextAreaComponent', () => {
    let component: DotEditContentTextAreaComponent;
    let fixture: ComponentFixture<DotEditContentTextAreaComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEditContentTextAreaComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditContentTextAreaComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
