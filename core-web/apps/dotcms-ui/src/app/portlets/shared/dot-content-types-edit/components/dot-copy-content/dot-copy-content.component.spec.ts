import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotCopyContentComponent } from './dot-copy-content.component';

describe('DotCopyContentComponent', () => {
    let component: DotCopyContentComponent;
    let fixture: ComponentFixture<DotCopyContentComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotCopyContentComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotCopyContentComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
