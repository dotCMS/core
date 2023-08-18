import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotDropZoneComponent } from './dot-drop-zone.component';

describe('DotDropZoneComponent', () => {
    let component: DotDropZoneComponent;
    let fixture: ComponentFixture<DotDropZoneComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotDropZoneComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotDropZoneComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
