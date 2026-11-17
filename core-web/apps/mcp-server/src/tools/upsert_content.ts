import { upsertContentTool } from '@dotcms/ai/tools';

import { xmcpTool } from '../lib/tools';

const { schema, metadata, handler } = xmcpTool(upsertContentTool);

export { schema, metadata };
export default handler;
