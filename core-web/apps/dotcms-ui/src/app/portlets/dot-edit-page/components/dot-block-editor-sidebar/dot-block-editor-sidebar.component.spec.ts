import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotBlockEditorSidebarComponent } from '@portlets/dot-edit-page/components/dot-block-editor-sidebar/dot-block-editor-sidebar.component';

describe('DotBlockEditorSidebarComponent', () => {
    let component: DotBlockEditorSidebarComponent;
    let fixture: ComponentFixture<DotBlockEditorSidebarComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotBlockEditorSidebarComponent]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(DotBlockEditorSidebarComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
