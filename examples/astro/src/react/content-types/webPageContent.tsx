import type { DotCMSContentlet } from "@dotcms/types";
import type { FC } from "react";

export type WebPageContentProps = DotCMSContentlet;

export const WebPageContent: FC<WebPageContentProps> = ({ title, body }) => {
  return (
    <>
      <h1 className="text-xl font-bold">{title}</h1>
      <div dangerouslySetInnerHTML={{ __html: body as string }} />
    </>
  );
};
