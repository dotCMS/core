import { useEffect, useState } from "react";
import type { DotCMSBasicContentlet } from "@dotcms/types";

import { Contentlets } from "@react/shared/contentlets";
import { dotCMSClient } from "@utils/client";

export const Destinations = () => {
  const [destinations, setDestinations] = useState<DotCMSBasicContentlet[]>([]);

  useEffect(() => {
    dotCMSClient.content
      .getCollection("Destination")
      .sortBy([
        {
          field: "modDate",
          order: "desc",
        },
      ])
      .limit(3)
      .then((response) => {
        setDestinations(response.contentlets);
      })
      .catch((error) => {
        console.error(`Error fetching Destinations`, error);
      });
  }, []);

  return (
    <div className="flex flex-col">
      <h2 className="text-2xl font-bold mb-7 text-black">
        Popular Destinations
      </h2>
      {!!destinations.length && <Contentlets contentlets={destinations} />}
    </div>
  );
};
