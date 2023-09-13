import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotDropZoneMessageComponent } from './dot-drop-zone-message.component';

describe('DotDropZoneMessageComponent', () => {
    let component: DotDropZoneMessageComponent;
    let fixture: ComponentFixture<DotDropZoneMessageComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotDropZoneMessageComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotDropZoneMessageComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
