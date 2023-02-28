import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotSelectEditContentletComponent } from './dot-select-edit-contentlet.component';

describe('DotSelectEditContentletComponent', () => {
    let component: DotSelectEditContentletComponent;
    let fixture: ComponentFixture<DotSelectEditContentletComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotSelectEditContentletComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotSelectEditContentletComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
