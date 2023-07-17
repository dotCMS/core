import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { AvatarModule } from 'primeng/avatar';
import { BadgeModule } from 'primeng/badge';
import { ButtonModule } from 'primeng/button';

import { DotAvatarDirective } from '@directives/dot-avatar/dot-avatar.directive';
import { DotMessageService } from '@dotcms/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import { MockDotMessageService, mockDotPersona } from '@dotcms/utils-testing';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPersonaSelectorOptionComponent } from './dot-persona-selector-option.component';

@Component({
    template: ` <dot-persona-selector-option [persona]="persona"></dot-persona-selector-option>`
})
class TestHostComponent {
    persona = mockDotPersona;
}

describe('DotPersonaSelectorOptionComponent', () => {
    let component: DotPersonaSelectorOptionComponent;
    let fixture: ComponentFixture<TestHostComponent>;
    let de: DebugElement;

    const messageServiceMock = new MockDotMessageService({
        'modes.persona.personalized': 'Personalized'
    });

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [DotPersonaSelectorOptionComponent, TestHostComponent],
            imports: [
                BrowserAnimationsModule,
                DotAvatarDirective,
                BadgeModule,
                AvatarModule,
                DotPipesModule,
                DotMessagePipe,
                ButtonModule
            ],
            providers: [
                {
                    provide: DotMessageService,
                    useValue: messageServiceMock
                }
            ]
        }).compileComponents();

        fixture = TestBed.createComponent(TestHostComponent);
        component = fixture.debugElement.query(
            By.css('dot-persona-selector-option')
        ).componentInstance;
        de = fixture.debugElement.query(By.css('dot-persona-selector-option'));
    });

    describe('elements', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should have p-avatar with right properties', () => {
            const avatar = fixture.debugElement.query(By.css('p-avatar'));

            const { image } = avatar.componentInstance;

            expect(image).toBe(mockDotPersona.photo);
            expect(avatar.query(By.css('.p-badge'))).toBeTruthy();
            expect(avatar.attributes['ng-reflect-text']).toBe(mockDotPersona.name);
        });

        it('should have personalized button with right properties', () => {
            const btnElement: DebugElement = de.query(By.css('button'));
            expect(btnElement.nativeElement.innerText).toBe('Personalized');
            expect(btnElement.attributes.icon).toBe('pi pi-times');
            expect(btnElement.attributes.iconPos).toBe('right');
        });

        it('should label set personalized class', () => {
            const lblElement: DebugElement = de.query(
                By.css('.dot-persona-selector-option__label')
            );
            expect(lblElement.nativeElement.innerText).toBe(mockDotPersona.name);
            expect(lblElement.nativeElement.classList).toContain(
                'dot-persona-selector-option__personalized'
            );
        });
    });

    describe('not personalize', () => {
        it('should label not set personalized class', () => {
            const hostComp = fixture.componentInstance;
            hostComp.persona = {
                ...mockDotPersona,
                personalized: false
            };
            fixture.detectChanges();

            const lblElement: DebugElement = de.query(
                By.css('.dot-persona-selector-option__label')
            );
            expect(lblElement.nativeElement.innerText).toBe(mockDotPersona.name);
            expect(lblElement.nativeElement.classList).not.toContain(
                'dot-persona-selector-option__personalized'
            );
        });

        it('should not display personalized button when personalized is false', () => {
            const hostComp = fixture.componentInstance;
            hostComp.persona = {
                ...mockDotPersona,
                personalized: false
            };
            fixture.detectChanges();

            const btnElement: DebugElement = de.query(By.css('button'));
            expect(btnElement).toBeNull();
        });

        it('should not display personalized button when canDespersonalize is false', () => {
            component.canDespersonalize = false;
            fixture.detectChanges();

            const btnElement: DebugElement = de.query(By.css('button'));
            expect(btnElement).toBeNull();
        });
    });

    describe('events', () => {
        beforeEach(() => {
            spyOn(component.switch, 'emit');
            spyOn(component.delete, 'emit');
            fixture.detectChanges();
        });

        it('should emit persona when field clicked', () => {
            de.triggerEventHandler('click', {
                stopPropagation: () => {
                    //
                }
            });
            expect(component.switch.emit).toHaveBeenCalledWith(mockDotPersona);
        });

        it('should emit persona when delete clicked', () => {
            const btnElement: DebugElement = de.query(By.css('button'));
            btnElement.triggerEventHandler('click', {
                stopPropagation: () => {
                    //
                }
            });
            expect(component.delete.emit).toHaveBeenCalledWith(mockDotPersona);
        });
    });
});
