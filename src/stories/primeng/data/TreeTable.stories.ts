// also exported from '@storybook/angular' if you can deal with breaking changes in 6.1
import { Meta } from '@storybook/angular/types-6-0';
import { TreeTableModule } from 'primeng/treetable';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { MultiSelectModule } from 'primeng/multiselect';
import { InputTextModule } from 'primeng/inputtext';
import { ToastModule } from 'primeng/toast';
import { ContextMenuModule } from 'primeng/contextmenu';

export default {
  title: 'PrimeNG/Data/TreeTable',
  parameters: {
    docs: {
      description: {
        component:
          'A listing data table: https://primefaces.org/primeng/showcase/#/table',
      },
    },
  },
} as Meta;

const cols = [
  { field: 'name', header: 'Name' },
  { field: 'size', header: 'Size' },
  { field: 'type', header: 'Type' },
];

const files = [
  {
    data: {
      name: 'Applications',
      size: '200mb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'Angular',
          size: '25mb',
          type: 'Folder',
        },
        children: [
          {
            data: {
              name: 'angular.app',
              size: '10mb',
              type: 'Application',
            },
          },
          {
            data: {
              name: 'cli.app',
              size: '10mb',
              type: 'Application',
            },
          },
          {
            data: {
              name: 'mobile.app',
              size: '5mb',
              type: 'Application',
            },
          },
        ],
      },
      {
        data: {
          name: 'editor.app',
          size: '25mb',
          type: 'Application',
        },
      },
      {
        data: {
          name: 'settings.app',
          size: '50mb',
          type: 'Application',
        },
      },
    ],
  },
  {
    data: {
      name: 'Cloud',
      size: '20mb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'backup-1.zip',
          size: '10mb',
          type: 'Zip',
        },
      },
      {
        data: {
          name: 'backup-2.zip',
          size: '10mb',
          type: 'Zip',
        },
      },
    ],
  },
  {
    data: {
      name: 'Desktop',
      size: '150kb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'note-meeting.txt',
          size: '50kb',
          type: 'Text',
        },
      },
      {
        data: {
          name: 'note-todo.txt',
          size: '100kb',
          type: 'Text',
        },
      },
    ],
  },
  {
    data: {
      name: 'Documents',
      size: '75kb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'Work',
          size: '55kb',
          type: 'Folder',
        },
        children: [
          {
            data: {
              name: 'Expenses.doc',
              size: '30kb',
              type: 'Document',
            },
          },
          {
            data: {
              name: 'Resume.doc',
              size: '25kb',
              type: 'Resume',
            },
          },
        ],
      },
      {
        data: {
          name: 'Home',
          size: '20kb',
          type: 'Folder',
        },
        children: [
          {
            data: {
              name: 'Invoices',
              size: '20kb',
              type: 'Text',
            },
          },
        ],
      },
    ],
  },
  {
    data: {
      name: 'Downloads',
      size: '25mb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'Spanish',
          size: '10mb',
          type: 'Folder',
        },
        children: [
          {
            data: {
              name: 'tutorial-a1.txt',
              size: '5mb',
              type: 'Text',
            },
          },
          {
            data: {
              name: 'tutorial-a2.txt',
              size: '5mb',
              type: 'Text',
            },
          },
        ],
      },
      {
        data: {
          name: 'Travel',
          size: '15mb',
          type: 'Text',
        },
        children: [
          {
            data: {
              name: 'Hotel.pdf',
              size: '10mb',
              type: 'PDF',
            },
          },
          {
            data: {
              name: 'Flight.pdf',
              size: '5mb',
              type: 'PDF',
            },
          },
        ],
      },
    ],
  },
  {
    data: {
      name: 'Main',
      size: '50mb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'bin',
          size: '50kb',
          type: 'Link',
        },
      },
      {
        data: {
          name: 'etc',
          size: '100kb',
          type: 'Link',
        },
      },
      {
        data: {
          name: 'var',
          size: '100kb',
          type: 'Link',
        },
      },
    ],
  },
  {
    data: {
      name: 'Other',
      size: '5mb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'todo.txt',
          size: '3mb',
          type: 'Text',
        },
      },
      {
        data: {
          name: 'logo.png',
          size: '2mb',
          type: 'Picture',
        },
      },
    ],
  },
  {
    data: {
      name: 'Pictures',
      size: '150kb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'barcelona.jpg',
          size: '90kb',
          type: 'Picture',
        },
      },
      {
        data: {
          name: 'primeng.png',
          size: '30kb',
          type: 'Picture',
        },
      },
      {
        data: {
          name: 'prime.jpg',
          size: '30kb',
          type: 'Picture',
        },
      },
    ],
  },
  {
    data: {
      name: 'Videos',
      size: '1500mb',
      type: 'Folder',
    },
    children: [
      {
        data: {
          name: 'primefaces.mkv',
          size: '1000mb',
          type: 'Video',
        },
      },
      {
        data: {
          name: 'intro.avi',
          size: '500mb',
          type: 'Video',
        },
      },
    ],
  },
];

const BasicTemplate = `
  <p-treeTable [value]="files" [columns]="cols">
      <ng-template pTemplate="caption">
          FileSystem
      </ng-template>
      <ng-template pTemplate="header" let-columns>
          <tr>
              <th *ngFor="let col of columns">
                  {{col.header}}
              </th>
              <th style="width: 8rem">
                  <p-button icon="pi pi-cog"></p-button>
              </th>
          </tr>
      </ng-template>
      <ng-template pTemplate="body" let-rowNode let-rowData="rowData" let-columns="columns">
          <tr>
              <td *ngFor="let col of columns; let i = index">
                  <p-treeTableToggler [rowNode]="rowNode" *ngIf="i == 0"></p-treeTableToggler>
                  {{rowData[col.field]}}
              </td>
              <td>
                  <p-button icon="pi pi-search" styleClass="p-button-success" [style]="{'margin-right': '.5em'}"></p-button>
                  <p-button icon="pi pi-pencil" styleClass="p-button-warning"></p-button>
              </td>
          </tr>
      </ng-template>
      <ng-template pTemplate="summary">
              <div style="text-align:left">
                  <p-button icon="pi pi-refresh"></p-button>
              </div>
      </ng-template>
  </p-treeTable>
`;
export const Basic = (_args: any) => {
  return {
    props: {
      files,
      cols
    },
    moduleMetadata: {
      imports: [
        TreeTableModule,
        ButtonModule,
        DialogModule,
        MultiSelectModule,
        InputTextModule,
        ToastModule,
        ContextMenuModule,
      ],
    },
    template: BasicTemplate,
  };
};

Basic.parameters = {
  docs: {
    source: {
      code: BasicTemplate,
    },
    iframeHeight: 500,
  },
};
