import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EditContentShellComponent } from './edit-content.shell.component';

describe('EditContentComponent', () => {
    let component: EditContentShellComponent;
    let fixture: ComponentFixture<EditContentShellComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditContentShellComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(EditContentShellComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
