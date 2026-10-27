import { pageCreateTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(pageCreateTool);

export { schema, metadata };
export default handler;
