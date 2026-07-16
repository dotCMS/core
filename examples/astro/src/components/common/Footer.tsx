import { RecommendedCard } from "@/components/ui";
import type { DotCMSBasicContentlet } from "@dotcms/types";

interface Blog extends DotCMSBasicContentlet {
  urlMap: string;
}

interface Destination extends DotCMSBasicContentlet {
  urlMap: string;
}

interface FooterProps {
  blogs: Blog[];
  destinations: Destination[];
}

function Footer({ blogs, destinations }: FooterProps) {
  return (
    <footer className="p-4 bg-slate-600 text-white py-24">
      <div className="grid md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3 gap-7 mx-24">
        <div className="flex flex-col gap-7">
          <h2 className="text-2xl font-bold text-white">About us</h2>
          <p className="text-sm text-white">
            We are TravelLux, a community of dedicated travel experts,
            journalists, and bloggers. Our aim is to offer you the best insight
            on where to go for your travel as well as to give you amazing
            opportunities with free benefits and bonuses for registered clients.
          </p>
        </div>

        <Blogs blogs={blogs} />
        <Destinations destinations={destinations} />
      </div>
    </footer>
  );
}

function Blogs({ blogs }: { blogs: Blog[] }) {
  if (!blogs?.length) return null;

  return (
    <div className="flex flex-col">
      <h2 className="text-2xl font-bold mb-7 text-white">Latest Blog Posts</h2>
      <div className="flex flex-col gap-5">
        {blogs.map((blog: Blog) => (
          <RecommendedCard key={blog.identifier} contentlet={blog} />
        ))}
      </div>
    </div>
  );
}

function Destinations({ destinations }: { destinations: Destination[] }) {
  if (!destinations?.length) return null;

  return (
    <div className="flex flex-col">
      <h2 className="text-2xl font-bold mb-7 text-white">
        Popular Destinations
      </h2>
      <div className="flex flex-col gap-5">
        {destinations.map((destination: Destination) => (
          <RecommendedCard
            key={destination.identifier}
            contentlet={destination}
          />
        ))}
      </div>
    </div>
  );
}

export default Footer;
