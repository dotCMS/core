import { DotTheme } from '@models/dot-edit-layout-designer';

export const mockDotThemes: DotTheme[] = [
    {
        name: 'Test Theme 1',
        title: 'Theme tittle',
        inode: '1234g',
        themeThumbnail: null,
        identifier: 'test',
        host: {
            hostName: 'Test',
            inode: '1',
            identifier: '345'
        }
    },
    {
        name: 'Test Theme 2',
        title: 'Theme tittle',
        inode: '13r3fd234g',
        themeThumbnail: null,
        identifier: 'test',
        host: {
            hostName: 'Test',
            inode: '2',
            identifier: '345'
        }
    },
    {
        name: 'Test Theme 3',
        title: 'Theme tittle',
        inode: '123dedw4g',
        themeThumbnail: 'test',
        identifier: 'test',
        host: {
            hostName: 'Test',
            inode: '3',
            identifier: '345'
        }
    }
];
