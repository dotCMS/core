import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DotAiClippyContentGeneratorComponent } from './dot-ai-clippy-content-generator.component';

describe('DotAiClippyContentGeneratorComponent', () => {
    let component: DotAiClippyContentGeneratorComponent;
    let fixture: ComponentFixture<DotAiClippyContentGeneratorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [DotAiClippyContentGeneratorComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(DotAiClippyContentGeneratorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
