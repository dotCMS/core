import { byTestId, Spectator } from '@ngneat/spectator';
import { createComponentFactory } from '@ngneat/spectator/jest';

import { DotUveToolbarComponent } from './dot-uve-toolbar.component';

describe('DotUveToolbarComponent', () => {
    let spectator: Spectator<DotUveToolbarComponent>;
    const createComponent = createComponentFactory({
        component: DotUveToolbarComponent
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    it('should have preview button', () => {
        expect(spectator.query(byTestId('uve-toolbar-preview'))).toBeTruthy();
    });

    it('should have bookmark button', () => {
        expect(spectator.query(byTestId('uve-toolbar-bookmark'))).toBeTruthy();
    });

    it('should have copy url button', () => {
        expect(spectator.query(byTestId('uve-toolbar-copy-url'))).toBeTruthy();
    });

    it('should have api link button', () => {
        expect(spectator.query(byTestId('uve-toolbar-api-link'))).toBeTruthy();
    });

    it('should have experiments button', () => {
        expect(spectator.query(byTestId('uve-toolbar-running-experiment'))).toBeTruthy();
    });

    it('should have language selector', () => {
        expect(spectator.query(byTestId('uve-toolbar-language-selector'))).toBeTruthy();
    });

    it('should have persona selector', () => {
        expect(spectator.query(byTestId('uve-toolbar-persona-selector'))).toBeTruthy();
    });

    it('should have workflows button', () => {
        expect(spectator.query(byTestId('uve-toolbar-workflow-actions'))).toBeTruthy();
    });
});
