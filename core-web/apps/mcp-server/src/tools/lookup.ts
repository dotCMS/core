import { lookupTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(lookupTool);

export { schema, metadata };
export default handler;
