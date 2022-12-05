import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';
import { DebugElement } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { DotIconModule } from '@dotcms/ui';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { By } from '@angular/platform-browser';
import { mockDotPersona } from '@dotcms/utils-testing';
import { TooltipModule } from 'primeng/tooltip';
import { LoginService } from '@dotcms/dotcms-js';
import { LoginServiceMock } from '@dotcms/utils-testing';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessageService } from '@dotcms/data-access';
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
            imports: [
                BrowserAnimationsModule,
                DotIconModule,
                DotAvatarModule,
                TooltipModule,
                DotPipesModule
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(DotPersonaSelectedItemComponent);
        component = fixture.componentInstance;
        de = fixture.debugElement;
        component.persona = mockDotPersona;
        fixture.detectChanges();
    });

    it('should have dot-avatar with right properties', () => {
        const avatar: DebugElement = de.query(By.css('dot-avatar'));
        expect(avatar.componentInstance.showDot).toBe(mockDotPersona.personalized);
        expect(avatar.componentInstance.url).toBe(mockDotPersona.photo);
        expect(avatar.componentInstance.size).toBe(24);
    });

    it('should render persona name and label', () => {
        const name = de.query(By.css('.dot-persona-selector__name')).nativeElement;
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
