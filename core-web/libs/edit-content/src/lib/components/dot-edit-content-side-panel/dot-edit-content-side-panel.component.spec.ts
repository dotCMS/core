import { createComponentFactory, Spectator, byTestId } from '@openng/spectator/jest';
import { MockComponent } from 'ng-mocks';
import { Subject } from 'rxjs';

import { ButtonModule } from 'primeng/button';
import { DrawerModule } from 'primeng/drawer';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotEditContentSidePanelComponent } from './dot-edit-content-side-panel.component';

import { EditContentDialogData } from '../../models/dot-edit-content-dialog.interface';
import { OverlayEditContentHost } from '../../services/host/overlay-edit-content-host';
import { DotEditContentLayoutComponent } from '../dot-edit-content-layout/dot-edit-content.layout.component';

describe('DotEditContentSidePanelComponent', () => {
    let spectator: Spectator<DotEditContentSidePanelComponent>;
    let saved$: Subject<DotCMSContentlet>;
    let mockHost: Pick<OverlayEditContentHost, 'saved$'>;

    const EDIT_DATA: EditContentDialogData = {
        mode: 'edit',
        contentletInode: 'inode-1',
        identifier: 'id-1',
        title: 'My Content'
    };

    const createComponent = createComponentFactory({
        component: DotEditContentSidePanelComponent,
        // Swap the heavy editor for a stub; feed a mock host so we control `saved$`.
        overrideComponents: [
            [
                DotEditContentSidePanelComponent,
                {
                    set: {
                        imports: [
                            DrawerModule,
                            ButtonModule,
                            MockComponent(DotEditContentLayoutComponent)
                        ],
                        providers: [{ provide: OverlayEditContentHost, useValue: undefined }]
                    }
                }
            ]
        ]
    });

    beforeEach(() => {
        saved$ = new Subject<DotCMSContentlet>();
        mockHost = { saved$: saved$.asObservable() };

        spectator = createComponent({
            providers: [{ provide: OverlayEditContentHost, useValue: mockHost }],
            detectChanges: false
        });
    });

    it('should create', () => {
        spectator.detectChanges();
        expect(spectator.component).toBeTruthy();
    });

    it('should render the content title in the header', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        expect(spectator.query(byTestId('side-panel-title'))?.textContent?.trim()).toBe(
            'My Content'
        );
    });

    it('should render the editor only when data is set', () => {
        spectator.setInput('data', null);
        spectator.detectChanges();
        expect(spectator.query(DotEditContentLayoutComponent)).toBeNull();

        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();
        expect(spectator.query(DotEditContentLayoutComponent)).not.toBeNull();
    });

    /** PrimeNG `p-button` renders its clickable `<button>` inside the host. */
    const clickButton = (testId: string): void => {
        const button = spectator.query(byTestId(testId))?.querySelector('button');
        spectator.click(button as HTMLElement);
    };

    it('should toggle expanded state with the expand button', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        clickButton('side-panel-expand');
        expect(spectator.component['$expanded']()).toBe(true);

        clickButton('side-panel-expand');
        expect(spectator.component['$expanded']()).toBe(false);
    });

    it('should route close through the editor guard and emit `closed` when it proceeds', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        const confirmClose = jest
            .spyOn(layout, 'confirmClose')
            .mockImplementation((onProceed: () => void) => onProceed());

        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickButton('side-panel-close');

        expect(confirmClose).toHaveBeenCalledWith(expect.any(Function));
        expect(closedSpy).toHaveBeenCalledTimes(1);
    });

    it('should NOT emit `closed` when the editor guard cancels (unsaved changes kept)', () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        jest.spyOn(layout, 'confirmClose').mockImplementation(() => {
            /* user chose "Keep editing" → never calls onProceed */
        });

        const closedSpy = jest.fn();
        spectator.output('closed').subscribe(closedSpy);

        clickButton('side-panel-close');

        expect(closedSpy).not.toHaveBeenCalled();
    });

    it('should forward the host `saved$` stream to the `saved` output', async () => {
        spectator.setInput('data', EDIT_DATA);
        spectator.detectChanges();
        // `saved$` is subscribed in afterNextRender — wait for it to run.
        await spectator.fixture.whenStable();

        const savedSpy = jest.fn();
        spectator.output('saved').subscribe(savedSpy);

        const contentlet = { inode: 'inode-1' } as DotCMSContentlet;
        saved$.next(contentlet);

        expect(savedSpy).toHaveBeenCalledWith(contentlet);
    });

    it('should fire data.onContentSaved (last save) and data.onCancel on close', async () => {
        const onContentSaved = jest.fn();
        const onCancel = jest.fn();
        spectator.setInput('data', { ...EDIT_DATA, onContentSaved, onCancel });
        spectator.detectChanges();
        await spectator.fixture.whenStable();

        const contentlet = { inode: 'inode-2' } as DotCMSContentlet;
        saved$.next(contentlet);

        const layout = spectator.query(DotEditContentLayoutComponent);
        jest.spyOn(layout, 'confirmClose').mockImplementation((onProceed: () => void) =>
            onProceed()
        );

        clickButton('side-panel-close');

        expect(onContentSaved).toHaveBeenCalledWith(contentlet);
        expect(onCancel).toHaveBeenCalledTimes(1);
    });

    it('should not fire data.onContentSaved on close when nothing was saved', () => {
        const onContentSaved = jest.fn();
        const onCancel = jest.fn();
        spectator.setInput('data', { ...EDIT_DATA, onContentSaved, onCancel });
        spectator.detectChanges();

        const layout = spectator.query(DotEditContentLayoutComponent);
        jest.spyOn(layout, 'confirmClose').mockImplementation((onProceed: () => void) =>
            onProceed()
        );

        clickButton('side-panel-close');

        expect(onContentSaved).not.toHaveBeenCalled();
        expect(onCancel).toHaveBeenCalledTimes(1);
    });
});
