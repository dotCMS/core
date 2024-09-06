import type { FC, ReactNode } from "react";

const Header: FC<{ children: ReactNode }> = ({ children }) => {
  return (
    <header className="flex items-center justify-between p-4 bg-blue-500">
      <div className="flex items-center">
        <h2 className="text-3xl font-bold text-white">
          <a href="/">TravelLux in Astro</a>
        </h2>
      </div>
      {children}
    </header>
  );
};

export default Header;
