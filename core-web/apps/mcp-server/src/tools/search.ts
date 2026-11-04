import { searchTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(searchTool);

export { schema, metadata };
export default handler;
