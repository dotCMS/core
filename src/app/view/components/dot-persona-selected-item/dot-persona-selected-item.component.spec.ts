import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';
import { DebugElement } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { By } from '@angular/platform-browser';
import { mockDotPersona } from '@tests/dot-persona.mock';
import { TooltipModule } from 'primeng/primeng';
import { LoginService } from 'dotcms-js';
import { LoginServiceMock } from '@tests/login-service.mock';
import { MockDotMessageService } from '@tests/dot-message-service.mock';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

const messageServiceMock = new MockDotMessageService({
    'modes.persona.selector.title.preview': 'Previewing As',
    'modes.persona.selector.title.edit': 'Personalize As',
    'modes.persona.no.persona': 'Default Visitor',
    'editpage.personalization.content.add.message': 'Add content...'
});

describe('DotPersonaSelectedItemComponent', () => {
    let component: DotPersonaSelectedItemComponent;
    let fixture: ComponentFixture<DotPersonaSelectedItemComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotPersonaSelectedItemComponent],
            providers: [
                {
                    provide: LoginService,
                    useClass: LoginServiceMock
                },
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ],
            imports: [BrowserAnimationsModule, DotIconModule, DotAvatarModule, TooltipModule, DotPipesModule]
        }).compileComponents();

        fixture = TestBed.createComponent(DotPersonaSelectedItemComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.persona = mockDotPersona;
        fixture.detectChanges();
    });

    it('should have dot-avatar with right properties', () => {
        const avatar: DebugElement = de.query(By.css('dot-avatar'));
        expect(avatar.componentInstance.label).toBe(mockDotPersona.name);
        expect(avatar.componentInstance.showDot).toBe(mockDotPersona.personalized);
        expect(avatar.componentInstance.url).toBe(mockDotPersona.photo);
        expect(avatar.componentInstance.size).toBe(32);
    });

    it('should render persona name and label', () => {
        const label = de.query(By.css('.dot-persona-selector__label')).nativeElement;
        const name = de.query(By.css('.dot-persona-selector__name')).nativeElement;
        expect(label.innerText.trim()).toBe('Previewing As');
        expect(name.innerText).toBe('Global Investor');
    });

    describe('tooltip properties', () => {
        let container: HTMLDivElement;

        it('should set properties to null when enable', () => {
            container = de.query(By.css('.dot-persona-selector__container')).nativeElement;
            expect(container.getAttribute('ng-reflect-tooltip-position')).toEqual(null);
            expect(container.getAttribute('ng-reflect-text')).toEqual(null);
        });

        it('should set properties correctly when disable', () => {
            component.disabled = true;
            fixture.detectChanges();
            container = de.query(By.css('.dot-persona-selector__container')).nativeElement;
            expect(container.getAttribute('ng-reflect-tooltip-position')).toEqual('bottom');
            expect(container.getAttribute('ng-reflect-text')).toEqual('Add content...');
        });
    });
});
