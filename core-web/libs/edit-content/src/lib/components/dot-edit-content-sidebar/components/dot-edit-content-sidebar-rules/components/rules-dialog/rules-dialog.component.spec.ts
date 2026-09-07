import { byTestId, createComponentFactory, Spectator } from '@openng/spectator/jest';

import { DynamicDialogConfig } from 'primeng/dynamicdialog';

import { DotMessageService } from '@dotcms/data-access';

import { DotRulesDialogComponent, RulesDialogData } from './rules-dialog.component';

describe('DotRulesDialogComponent', () => {
    let spectator: Spectator<DotRulesDialogComponent>;

    const defaultData: RulesDialogData = { identifier: 'page-123' };
    const configRef: { data: RulesDialogData | null | undefined } = { data: defaultData };

    const createComponent = createComponentFactory({
        component: DotRulesDialogComponent,
        providers: [
            { provide: DynamicDialogConfig, useValue: configRef },
            { provide: DotMessageService, useValue: { get: (key: string) => key } }
        ]
    });

    beforeEach(() => {
        configRef.data = defaultData;
        spectator = createComponent();
    });

    it('should create', () => {
        expect(spectator.component).toBeTruthy();
    });

    describe('with valid identifier', () => {
        it('should render the iframe', () => {
            expect(spectator.query(byTestId('rules-iframe'))).toBeTruthy();
        });

        it('should NOT render the empty state', () => {
            expect(spectator.query(byTestId('rules-empty'))).toBeFalsy();
        });

        it('should build the iframe url with the page identifier as realmId', () => {
            const iframe = spectator.query<HTMLIFrameElement>(byTestId('rules-iframe'));
            expect(iframe?.src).toContain('fromCore/rules');
            expect(iframe?.src).toContain('realmId=page-123');
        });

        it('should build the correct iframe url for a different identifier', () => {
            configRef.data = { identifier: 'abc-xyz-789' };
            spectator = createComponent();

            const iframe = spectator.query<HTMLIFrameElement>(byTestId('rules-iframe'));
            expect(iframe?.src).toContain('realmId=abc-xyz-789');
        });
    });

    describe('without valid identifier', () => {
        it('should render the empty state when data is undefined', () => {
            configRef.data = undefined;
            spectator = createComponent();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-iframe'))).toBeFalsy();
        });

        it('should render the empty state when data is null', () => {
            configRef.data = null;
            spectator = createComponent();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-iframe'))).toBeFalsy();
        });

        it('should render the empty state when identifier is empty string', () => {
            configRef.data = { identifier: '' };
            spectator = createComponent();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-iframe'))).toBeFalsy();
        });

        it('should render the empty state when data is empty object', () => {
            configRef.data = {} as RulesDialogData;
            spectator = createComponent();

            expect(spectator.query(byTestId('rules-empty'))).toBeTruthy();
            expect(spectator.query(byTestId('rules-iframe'))).toBeFalsy();
        });
    });
});
