import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotAvatarDirective, DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService, mockDotPersona } from '@dotcms/utils-testing';

import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';

const messageServiceMock = new MockDotMessageService({
    'modes.persona.selector.title.preview': 'Previewing As',
    'modes.persona.selector.title.edit': 'Personalize As',
    'modes.persona.no.persona': 'Default Visitor',
    'editpage.personalization.content.add.message': 'Add content...'
});

@Component({
    template: `
        <dot-persona-selected-item [persona]="persona"></dot-persona-selected-item>
    `,
    standalone: false
})
class TestHostComponent {
    persona = mockDotPersona;
}

describe('DotPersonaSelectedItemComponent', () => {
    let component: DotPersonaSelectedItemComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotPersonaSelectedItemComponent, TestHostComponent],
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
                DotAvatarDirective,
                AvatarModule,
                BadgeModule,
                TooltipModule,
                DotSafeHtmlPipe,
                DotMessagePipe
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.debugElement.query(
            By.css('dot-persona-selected-item')
        ).componentInstance;
        de = fixture.debugElement;
        fixture.detectChanges();
    });

    it('should have p-avatar with right properties', () => {
        const avatar = fixture.debugElement.query(By.css('p-avatar'));

        const { image } = avatar.componentInstance;

        expect(image).toBe(mockDotPersona.photo);
        expect(avatar.query(By.css('.p-badge'))).toBeTruthy();
        expect(avatar.attributes['ng-reflect-text']).toBe(mockDotPersona.name);
    });

    it('should render persona name and label', () => {
        const name = de.query(By.css('.dot-persona-selector__name')).nativeElement;
        expect(name.innerText.trim()).toBe('Global Investor');
    });

    describe('tooltip properties', () => {
        let container: HTMLDivElement;

        it('should set properties to null when enable', () => {
            container = de.query(By.css('.dot-persona-selector__container')).nativeElement;
            expect(container.getAttribute('ng-reflect-tooltip-position')).toEqual(null);
            expect(container.getAttribute('ng-reflect-content')).toEqual(null);
        });

        it('should set properties correctly when disable', () => {
            component.disabled = true;
            fixture.detectChanges();
            container = de.query(By.css('.dot-persona-selector__container')).nativeElement;
            expect(container.getAttribute('ng-reflect-tooltip-position')).toEqual('bottom');
            expect(container.getAttribute('ng-reflect-content')).toEqual('Add content...');
        });
    });
});
