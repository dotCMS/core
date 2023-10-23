import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { AIImagePromptComponent } from './ai-image-prompt.component';

import { AiContentService } from '../../shared';

describe('AIImagePromptComponent', () => {
    let component: AIImagePromptComponent;
    let fixture: ComponentFixture<AIImagePromptComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ReactiveFormsModule, HttpClientTestingModule],
            declarations: [AIImagePromptComponent],
            providers: [AiContentService]
        }).compileComponents();

        fixture = TestBed.createComponent(AIImagePromptComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
