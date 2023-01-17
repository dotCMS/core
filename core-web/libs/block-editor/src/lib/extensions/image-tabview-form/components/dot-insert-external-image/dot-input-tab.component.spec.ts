import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotInsertExternalImageComponent } from './dot-insert-external-image.component';

describe('DotInputTabComponent', () => {
    let component: DotInsertExternalImageComponent;
    let fixture: ComponentFixture<DotInsertExternalImageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotInsertExternalImageComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotInsertExternalImageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
