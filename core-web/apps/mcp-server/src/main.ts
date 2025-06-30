import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { z } from 'zod';

import { registerContentTypeTools } from './tools/contenttypes';
import { registerContextTools } from './tools/context';
import { registerWorkflowTools } from './tools/workflow';

const server = new McpServer({
    name: 'DotCMS',
    version: '1.0.0'
});

const DOTCMS_URL = process.env.DOTCMS_URL;
const AUTH_TOKEN = process.env.AUTH_TOKEN;

const urlSchema = z.string().url();
const tokenSchema = z.string().min(1, 'AUTH_TOKEN cannot be empty');

try {
    urlSchema.parse(DOTCMS_URL);
    tokenSchema.parse(AUTH_TOKEN);
} catch (e) {
    // eslint-disable-next-line no-console
    console.error('Invalid environment variables:', e);
    process.exit(1);
}

// Register content type tools
registerContentTypeTools(server);

// Register workflow tools
registerWorkflowTools(server);

// Register context tools
registerContextTools(server);

const transport = new StdioServerTransport();
(async () => {
    await server.connect(transport);
})();

// import { WorkflowService } from './services/workflow';

// const workflowService = new WorkflowService();

// const ts = new Date().getTime();

// workflowService.saveContent({
//     body: 'Experience the ultimate adrenaline rush with our unique Surfing with Crocodiles adventure. This carefully designed activity combines professional surfing instruction with wildlife education in a controlled, safe environment.',
//     tags: 'extreme-sports,water-activities,wildlife,adventure',
//     title: `Surfing with Crocodiles ${ts}`,
//     urlTitle: `surfing-with-crocodiles-${ts}`,
//     languageId: '1',
//     contentHost: '48190c8c-42c4-46af-8d1a-0cd5db894797',
//     contentType: 'Activity',
//     description:
//         'An adrenaline-pumping water sport that combines the thrill of surfing with the excitement of navigating crocodile-inhabited waters.'
// }).then((response) => {
//     console.log(response);
// }).catch((error) => {
//     console.error(error);
// });


// const contentTypeService = new ContentTypeService();

// contentTypeService
//     .create({
//         name: 'Promo 2',
//         clazz: 'com.dotcms.contenttype.model.type.ImmutableSimpleContentType',
//         fields: [
//             {
//                 name: 'Host',
//                 clazz: 'com.dotcms.contenttype.model.field.ImmutableHostFolderField',
//                 required: true,
//                 variable: 'contentHost',
//                 fieldType: 'Host-Folder',
//                 sortOrder: 1,
//                 searchable: true
//             },
//             {
//                 name: 'Title',
//                 clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
//                 listed: true,
//                 required: true,
//                 variable: 'title',
//                 fieldType: 'Text',
//                 sortOrder: 2,
//                 searchable: true
//             },
//             {
//                 clazz: 'com.dotcms.contenttype.model.field.ImmutableTextAreaField',
//                 required: false,
//                 name: 'Description',
//                 fieldType: 'Textarea',
//                 hint: '',
//                 searchable: false,
//                 indexed: false,
//                 id: null
//             },
//             {
//                 hint: 'Visual asset for the promotion',
//                 name: 'Image',
//                 clazz: 'com.dotcms.contenttype.model.field.ImmutableImageField',
//                 required: false,
//                 variable: 'image',
//                 fieldType: 'Image',
//                 sortOrder: 4
//             },
//             {
//                 hint: "Button text or action phrase (e.g., 'Learn More', 'Get Started')",
//                 name: 'Call to Action',
//                 clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
//                 required: false,
//                 variable: 'cta',
//                 fieldType: 'Text',
//                 sortOrder: 5
//             },
//             {
//                 hint: 'URL where the CTA should redirect',
//                 name: 'Link',
//                 clazz: 'com.dotcms.contenttype.model.field.ImmutableTextField',
//                 required: false,
//                 variable: 'link',
//                 fieldType: 'Text',
//                 sortOrder: 6
//             }
//         ],
//         description: 'Promotional content for campaigns, announcements, and special offers'
//     })
//     .then((response) => {
//         console.log(response);
//     })
//     .catch((error) => {
//         console.error(error);
//     });
