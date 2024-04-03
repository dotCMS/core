import { byTestId, createHostFactory, SpectatorHost } from '@ngneat/spectator/jest';

import { DotMessageService } from '@dotcms/data-access';
import { DotStringTemplateOutletDirective } from '@dotcms/ui';
import { MockDotMessageService } from '@dotcms/utils-testing';

import { DotExperimentsDetailsTableComponent } from './dot-experiments-details-table.component';

const MOCK_DATA = [
    {
        key: 'value',
        key2: 'value2',
        key3: 'value3'
    }
];
const MOCK_DATA_WITH_TEMPLATE = [
    {
        id: 'value1',
        value: 'value2',
        label: 'value3'
    }
];

const messageServiceMock = new MockDotMessageService({
    'experiments.reports.summary.empty.title': 'title',
    'experiments.reports.summary.empty.description': 'description'
});

describe('DotExperimentsDetailsTableComponent', () => {
    let spectator: SpectatorHost<DotExperimentsDetailsTableComponent>;
    const createHost = createHostFactory({
        component: DotExperimentsDetailsTableComponent,
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ],
        imports: [DotStringTemplateOutletDirective]
    });

    it('should render the default templates with only data', () => {
        spectator = createHost(`<dot-experiments-details-table [data]="data" />`, {
            hostProps: {
                data: MOCK_DATA
            }
        });

        const headers = spectator.queryAll(byTestId('default-header-columns'));

        expect(headers.length).toEqual(3);
        expect(headers[0]).toContainText('key');
        expect(headers[1]).toContainText('key2');
        expect(headers[2]).toContainText('key3');

        const rows = spectator.queryAll(byTestId('default-row'));
        expect(spectator.queryAll(byTestId('default-row')).length).toEqual(3);
        expect(rows[0]).toContainText('value');
        expect(rows[1]).toContainText('value2');
        expect(rows[2]).toContainText('value3');
    });

    it('should render the header and footer sent in content projection', () => {
        const titles = ['TITLE 1', 'TITLE 2', 'TITLE 3'];
        spectator = createHost(
            `
            <dot-experiments-details-table [data]="data">
                <ng-template #headers>
                  <div data-testId="template-title">${titles[0]}</div>
                  <div data-testId="template-title">${titles[1]}</div>
                  <div data-testId="template-title">${titles[2]}</div>
                </ng-template>
                
                <ng-template #rows let-row>
                  <div data-testId="template-row">{{row.id}}</div>
                  <div data-testId="template-row">{{row.value}}</div>
                  <div data-testId="template-row">{{row.label}}</div>
                </ng-template>
            </dot-experiments-details-table>`,
            {
                hostProps: {
                    data: MOCK_DATA_WITH_TEMPLATE
                }
            }
        );
        const headers = spectator.queryAll(byTestId('template-title'));
        expect(headers.length).toEqual(3);
        expect(headers[0]).toContainText(titles[0]);
        expect(headers[1]).toContainText(titles[1]);
        expect(headers[2]).toContainText(titles[2]);

        const rows = spectator.queryAll(byTestId('template-row'));
        expect(rows.length).toEqual(3);
        expect(rows[0]).toContainText(MOCK_DATA_WITH_TEMPLATE[0].id);
        expect(rows[1]).toContainText(MOCK_DATA_WITH_TEMPLATE[0].value);
        expect(rows[2]).toContainText(MOCK_DATA_WITH_TEMPLATE[0].label);
    });

    it('should render the title as string', () => {
        const expectedString = 'This is a string';
        spectator = createHost(`<dot-experiments-details-table [data]="data" [title]="title" />`, {
            hostProps: {
                data: MOCK_DATA,
                title: expectedString
            }
        });

        expect(spectator.query(byTestId('header-title'))).not.toBeNull();
        expect(spectator.query(byTestId('header-title'))).toContainText(expectedString);
    });

    it('should render the title as TemplateRef', () => {
        const expectedString = 'This is a string inside a templateRef';
        spectator = createHost(
            `
            <dot-experiments-details-table [data]="data" [title]="titleTpl"></dot-experiments-details-table>
            <ng-template #titleTpl>${expectedString}</ng-template>`,
            {
                hostProps: {
                    data: MOCK_DATA
                }
            }
        );

        expect(spectator.query(byTestId('header-title'))).not.toBeNull();
        expect(spectator.query(byTestId('header-title'))).toContainText(expectedString);
    });

    it('should render loading state', () => {
        spectator = createHost(
            `<dot-experiments-details-table [data]="data" [isLoading]="loading" />`,
            {
                hostProps: {
                    data: MOCK_DATA,
                    loading: true
                }
            }
        );
        expect(spectator.query(byTestId('loading-skeleton'))).not.toBeNull();
    });

    it('should render empty state', () => {
        spectator = createHost(
            `<dot-experiments-details-table [isEmpty]="empty" [data]="data" [isLoading]="loading" />`,
            {
                hostProps: {
                    data: MOCK_DATA,
                    loading: false,
                    empty: true
                }
            }
        );

        expect(spectator.query(byTestId('empty-template'))).not.toBeNull();
        expect(spectator.query(byTestId('empty-title'))).toContainText('title');
        expect(spectator.query(byTestId('empty-description'))).toContainText('description');
    });
});
