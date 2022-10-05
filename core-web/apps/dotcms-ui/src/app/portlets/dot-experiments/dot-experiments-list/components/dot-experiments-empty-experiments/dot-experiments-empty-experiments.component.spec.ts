import { DotExperimentsEmptyExperimentsComponent } from './dot-experiments-empty-experiments.component';
import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';
import { CUSTOM_ELEMENTS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { Button, ButtonModule } from 'primeng/button';
import { DotIconComponent } from '@dotcms/ui';

@Pipe({ name: 'dm' })
class MockPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

describe('DotExperimentsEmptyExperimentsComponent', () => {
    let spectator: Spectator<DotExperimentsEmptyExperimentsComponent>;
    let pButton: Button | null;

    const createComponent = createComponentFactory({
        imports: [ButtonModule],
        component: DotExperimentsEmptyExperimentsComponent,
        componentMocks: [DotIconComponent],
        declarations: [MockPipe],
        schemas: [CUSTOM_ELEMENTS_SCHEMA]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should show icon, title and button', () => {
        const description = 'description message';
        spectator.setInput({
            description,
            showButton: true
        });

        pButton = spectator.query(Button);

        expect(spectator.query(byTestId('description'))).toHaveText(description);
        expect(pButton.label).toBe('experimentspage.add.new.experiment');
    });

    it('should show icon and title, not button', () => {
        const description = 'description message';
        spectator.setInput({
            description,
            showButton: false
        });

        pButton = spectator.query(Button);

        expect(spectator.query(byTestId('description'))).toHaveText(description);
        expect(pButton).not.toExist();
    });
});
