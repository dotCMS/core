import { Contentlets } from "@react/shared/contentlets";
import type { DotCMSContentlet } from "@dotcms/types";
import { client } from "@utils/client";
import { useEffect, useState } from "react";

export const Destinations = () => {
  const [destinations, setDestinations] = useState<DotCMSContentlet[]>([]);

  useEffect(() => {
    client.content
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
