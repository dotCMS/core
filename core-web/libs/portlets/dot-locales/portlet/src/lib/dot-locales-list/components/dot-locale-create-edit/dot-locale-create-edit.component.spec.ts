import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotLocaleCreateEditComponent } from './dot-locale-create-edit.component';

describe('DotLocaleCreateEditComponent', () => {
    let component: DotLocaleCreateEditComponent;
    let fixture: ComponentFixture<DotLocaleCreateEditComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotLocaleCreateEditComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLocaleCreateEditComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
