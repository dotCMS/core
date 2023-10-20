import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';

import { AIContentActionsComponent } from './ai-content-actions.component';

import { AiContentService } from '../../shared';

describe('AIContentActionsComponent', () => {
    let component: AIContentActionsComponent;
    let fixture: ComponentFixture<AIContentActionsComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [ReactiveFormsModule, HttpClientTestingModule],
            declarations: [AIContentActionsComponent],
            providers: [AiContentService]
        }).compileComponents();

        fixture = TestBed.createComponent(AIContentActionsComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
