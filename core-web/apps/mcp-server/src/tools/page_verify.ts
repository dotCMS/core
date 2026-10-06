import { pageVerifyTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(pageVerifyTool);

export { schema, metadata };
export default handler;
