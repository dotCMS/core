import { StructureTypeView } from '@models/contentlet';
import { StructureType } from '@models/contentlet/structure-type.model';

export const mockDotContentlet: StructureTypeView[] = [
    {
        name: 'CONTENT',
        label: 'Content',
        types: [
            {
                type: StructureType.CONTENT,
                name: 'Banner',
                inode: '4c441ada-944a-43af-a653-9bb4f3f0cb2b',
                action: '1',
                variable: 'Banner'
            },
            {
                type: StructureType.CONTENT,
                name: 'Blog',
                inode: '799f176a-d32e-4844-a07c-1b5fcd107578',
                action: '2',
                variable: 'Blog'
            }
        ]
    },
    {
        name: 'WIDGET',
        label: 'Widget',
        types: [
            {
                type: StructureType.WIDGET,
                name: 'Document Listing',
                inode: '4316185e-a95c-4464-8884-3b6523f694e9',
                action: '3',
                variable: 'DocumentListing'
            }
        ]
    },
    {
        name: 'RECENT_CONTENT',
        label: 'Recent Content',
        types: [
            {
                type: StructureType.CONTENT,
                name: 'Content (Generic)',
                inode: '2a3e91e4-fbbf-4876-8c5b-2233c1739b05',
                action: '4',
                variable: 'webPageContent'
            }
        ]
    }
];
