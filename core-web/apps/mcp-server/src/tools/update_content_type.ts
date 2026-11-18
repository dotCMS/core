import { updateContentTypeTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(updateContentTypeTool);

export { schema, metadata };
export default handler;
