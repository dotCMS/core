import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AddStyleClassesDialogComponent } from './add-style-classes-dialog.component';

describe('AddStyleClassesDialogComponent', () => {
    let component: AddStyleClassesDialogComponent;
    let fixture: ComponentFixture<AddStyleClassesDialogComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [AddStyleClassesDialogComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(AddStyleClassesDialogComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
