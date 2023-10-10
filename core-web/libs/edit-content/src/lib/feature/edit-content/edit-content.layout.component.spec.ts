import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { EditContentLayoutComponent } from './edit-content.layout.component';

describe('FormComponent', () => {
    let component: EditContentLayoutComponent;
    let fixture: ComponentFixture<EditContentLayoutComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [EditContentLayoutComponent, RouterTestingModule]
        }).compileComponents();

        fixture = TestBed.createComponent(EditContentLayoutComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
