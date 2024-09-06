import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { DotAiService } from '@dotcms/data-access';

import { AIContentPromptComponent } from './ai-content-prompt.component';

describe('AIContentPromptComponent', () => {
    let component: AIContentPromptComponent;
    let fixture: ComponentFixture<AIContentPromptComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ReactiveFormsModule, HttpClientTestingModule],
            declarations: [AIContentPromptComponent],
            providers: [DotAiService]
        }).compileComponents();

        fixture = TestBed.createComponent(AIContentPromptComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
