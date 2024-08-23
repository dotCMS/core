import type { FC } from "react";
import type { DotCMSContentlet } from "../../types";

export type WebPageContentProps = DotCMSContentlet;

export const WebPageContent: FC<WebPageContentProps> = ({ title, body }) => {
  return (
    <>
      <h1 className="text-xl font-bold">{title}</h1>
      <div dangerouslySetInnerHTML={{ __html: body as string }} />
    </>
  );
};
