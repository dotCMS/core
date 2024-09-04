import type { FC } from "react";

import Contentlet from "./contentlet";
import { Entry } from "./entry";
import type { DotCMSContentlet } from "@dotcms/types";

export type ContentletsProps = {
  contentlets: DotCMSContentlet[];
};

export const Contentlets: FC<ContentletsProps> = ({ contentlets }) => {
  return (
    <ul className="flex flex-col gap-7">
      {contentlets.map((contentlet) => (
        <Contentlet contentlet={contentlet} key={contentlet.identifier}>
          <li className="flex gap-7 min-h-16">
            <Entry contentlet={contentlet} />
          </li>
        </Contentlet>
      ))}
    </ul>
  );
};
