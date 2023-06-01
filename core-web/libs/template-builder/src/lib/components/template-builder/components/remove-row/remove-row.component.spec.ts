import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RemoveRowComponent } from './remove-row.component';

describe('RemoveRowComponent', () => {
    let component: RemoveRowComponent;
    let fixture: ComponentFixture<RemoveRowComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [RemoveRowComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(RemoveRowComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
