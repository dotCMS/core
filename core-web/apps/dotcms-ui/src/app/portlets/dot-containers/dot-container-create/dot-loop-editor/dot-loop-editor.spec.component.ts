import { DotLoopEditorComponent } from './dot-loop-editor.component';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

describe('DotLoopEditorComponent', () => {
    let component: DotLoopEditorComponent;
    let fixture: ComponentFixture<DotLoopEditorComponent>;

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            declarations: [DotLoopEditorComponent],
            imports: [DotMessagePipeModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotLoopEditorComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });
});
