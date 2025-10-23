import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotHackathonTestComponent } from './dot-hackathon-test.component';

describe('DotHackathonTestComponent', () => {
    let component: DotHackathonTestComponent;
    let fixture: ComponentFixture<DotHackathonTestComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotHackathonTestComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotHackathonTestComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
