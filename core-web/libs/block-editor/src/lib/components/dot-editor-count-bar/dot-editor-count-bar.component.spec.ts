import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotEditorCountBarComponent } from './dot-editor-count-bar.component';

describe('EditorCountBarComponent', () => {
    let component: DotEditorCountBarComponent;
    let fixture: ComponentFixture<DotEditorCountBarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotEditorCountBarComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotEditorCountBarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
