import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotUveToolbarComponent } from './dot-uve-toolbar.component';

describe('DotUveToolbarComponent', () => {
    let component: DotUveToolbarComponent;
    let fixture: ComponentFixture<DotUveToolbarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotUveToolbarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotUveToolbarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
