import { createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotAvatarDirective, DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';
import { LoginServiceMock, MockDotMessageService, mockDotPersona } from '@dotcms/utils-testing';

import { DotPersonaSelectedItemComponent } from './dot-persona-selected-item.component';

const messageServiceMock = new MockDotMessageService({
    'modes.persona.selector.title.preview': 'Previewing As',
    'modes.persona.selector.title.edit': 'Personalize As',
    'modes.persona.no.persona': 'Default Visitor',
    'editpage.personalization.content.add.message': 'Add content...'
});

describe('DotPersonaSelectedItemComponent', () => {
    let spectator: Spectator<DotPersonaSelectedItemComponent>;

    const createComponent = createComponentFactory({
        component: DotPersonaSelectedItemComponent,
        imports: [
            NoopAnimationsModule,
            DotIconComponent,
            DotAvatarDirective,
            AvatarModule,
            BadgeModule,
            TooltipModule,
            DotSafeHtmlPipe,
            DotMessagePipe
        ],
        providers: [
            { provide: LoginService, useClass: LoginServiceMock },
            { provide: DotMessageService, useValue: messageServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ props: { persona: mockDotPersona } });
    });

    it('should have p-avatar with right properties', () => {
        const avatar = spectator.debugElement.query(By.css('p-avatar'));
        const avatarInstance = avatar.componentInstance;

        expect(avatarInstance.image).toBe(mockDotPersona.photo);

        const personaName = spectator.debugElement.query(By.css('.dot-persona-selector__name'));
        expect(personaName.nativeElement.textContent.trim()).toBe(mockDotPersona.name);

        const badge = avatar.query(By.css('.p-badge'));
        if (mockDotPersona.personalized) {
            expect(badge).toBeTruthy();
        }
    });

    it('should render persona name and label', () => {
        const name = spectator.query('.dot-persona-selector__name');
        expect(name?.textContent?.trim()).toBe('Global Investor');
    });

    describe('tooltip properties', () => {
        it('should set properties to null when enable', () => {
            const container = spectator.debugElement.query(
                By.css('.dot-persona-selector__container')
            );
            const tooltipDirective = container.injector.get(Tooltip);
            expect(tooltipDirective.content).toBeNull();
            expect(tooltipDirective.tooltipPosition).toBeNull();
        });

        it('should set properties correctly when disable', () => {
            spectator = createComponent({
                props: { persona: mockDotPersona, disabled: true }
            });

            const container = spectator.debugElement.query(
                By.css('.dot-persona-selector__container')
            );
            const tooltipDirective = container.injector.get(Tooltip);
            expect(tooltipDirective.tooltipPosition).toBe('bottom');
            expect(tooltipDirective.content).toBe('Add content...');
        });
    });
});
