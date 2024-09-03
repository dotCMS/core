import type { DotcmsNavigationItem } from "@dotcms/types";
import type { FC } from "react";

export type NavigationProps = {
  items?: DotcmsNavigationItem[];
  className?: string;
};

export const Navigation: FC<NavigationProps> = ({ items, className }) => {
  const currentQueryParams = window.location.search;

  return (
    <nav className={className}>
      <ul className="flex space-x-4 text-white">
        <li>
          <a href={`/${currentQueryParams}`}> Home</a>
        </li>
        {items?.map((item) => (
          <li key={item.folder}>
            <a href={item.href + currentQueryParams} target={item.target}>
              {item.title}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
};
