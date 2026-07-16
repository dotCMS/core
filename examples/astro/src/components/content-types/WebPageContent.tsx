import type { DotCMSBasicContentlet } from "@dotcms/types";

interface WebPageContentProps extends DotCMSBasicContentlet {
  body: string;
}

function WebPageContent({ title, body }: WebPageContentProps) {
  return (
    <>
      <h1 className="text-xl font-bold">{title}</h1>
      <div dangerouslySetInnerHTML={{ __html: body }} />
    </>
  );
}

export default WebPageContent;
