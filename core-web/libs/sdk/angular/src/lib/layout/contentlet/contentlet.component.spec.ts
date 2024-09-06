import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ContentletComponent } from './contentlet.component';

describe('ContentletComponent', () => {
    let component: ContentletComponent;
    let fixture: ComponentFixture<ContentletComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ContentletComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(ContentletComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
