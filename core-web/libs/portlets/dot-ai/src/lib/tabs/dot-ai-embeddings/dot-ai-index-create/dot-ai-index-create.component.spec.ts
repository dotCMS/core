import { byTestId, createComponentFactory, mockProvider, Spectator } from '@openng/spectator/jest';

import { DynamicDialogConfig, DynamicDialogRef } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotAiIndexCreateComponent } from './dot-ai-index-create.component';

describe('DotAiIndexCreateComponent', () => {
    let spectator: Spectator<DotAiIndexCreateComponent>;
    let dialogRef: DynamicDialogRef;

    const createComponent = createComponentFactory({
        component: DotAiIndexCreateComponent,
        providers: [
            mockProvider(DynamicDialogRef),
            mockProvider(DotMessageService),
            { provide: DynamicDialogConfig, useValue: { data: { indexes: ['default'] } } }
        ],
        shallow: true
    });

    beforeEach(() => {
        spectator = createComponent();
        dialogRef = spectator.inject(DynamicDialogRef);
    });

    const fill = (testId: string, value: string) =>
        spectator.typeInElement(value, spectator.query(byTestId(testId)) as HTMLElement);

    /** PrimeNG puts its click handler on the inner <button>, not the p-button host. */
    const clickButton = (testId: string) =>
        spectator.click(
            spectator.query(byTestId(testId))?.querySelector('button') as HTMLButtonElement
        );

    it('should keep submit disabled until both name and query are given', () => {
        const submit = () =>
            spectator.query(byTestId('dotai-index-create-submit'))?.querySelector('button');

        expect(submit()?.disabled).toBe(true);

        fill('dotai-index-create-name', 'blogs');
        spectator.detectChanges();
        expect(submit()?.disabled).toBe(true);

        fill('dotai-index-create-query', '+contentType:Blog');
        spectator.detectChanges();
        expect(submit()?.disabled).toBe(false);
    });

    it('should close with an add-mode payload including the optional shaping fields', () => {
        fill('dotai-index-create-name', 'blogs');
        fill('dotai-index-create-query', '+contentType:Blog');
        fill('dotai-index-create-fields', 'title,body');
        spectator.detectChanges();

        clickButton('dotai-index-create-submit');

        expect(dialogRef.close).toHaveBeenCalledWith({
            mode: 'add',
            indexName: 'blogs',
            query: '+contentType:Blog',
            fields: 'title,body'
        });
    });

    it('should trim whitespace off the name and query', () => {
        fill('dotai-index-create-name', '  blogs  ');
        fill('dotai-index-create-query', '  +contentType:Blog  ');
        spectator.detectChanges();

        clickButton('dotai-index-create-submit');

        expect(dialogRef.close).toHaveBeenCalledWith(
            expect.objectContaining({ indexName: 'blogs', query: '+contentType:Blog' })
        );
    });

    it('should close with nothing on cancel, so the caller does no work', () => {
        clickButton('dotai-index-create-cancel');

        expect(dialogRef.close).toHaveBeenCalledWith();
    });
});
