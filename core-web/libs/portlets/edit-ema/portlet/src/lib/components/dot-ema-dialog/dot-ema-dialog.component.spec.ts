import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEmaDialogComponent } from './dot-ema-dialog.component';

describe('DotEmaDialogComponent', () => {
    let component: DotEmaDialogComponent;
    let fixture: ComponentFixture<DotEmaDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotEmaDialogComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEmaDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
