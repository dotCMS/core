import { executeTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(executeTool);

export { schema, metadata };
export default handler;
