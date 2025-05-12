import Blogs from "./components/blogs";
import Destinations from "./components/destinations";

function Footer({ blogs, destinations }) {
    return (
        <footer className="p-4 bg-slate-600 text-white py-24">
            <div className="grid md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3 gap-7 mx-24">
                <div className="flex flex-col gap-7">
                    <h2 className="text-2xl font-bold text-white">About us</h2>
                    <p className="text-sm text-white">
                        We are TravelLux, a community of dedicated travel
                        experts, journalists, and bloggers. Our aim is to offer
                        you the best insight on where to go for your travel as
                        well as to give you amazing opportunities with free
                        benefits and bonuses for registered clients.
                    </p>
                </div>

                <Blogs blogs={blogs} />
                <Destinations destinations={destinations} />
            </div>
        </footer>
    );
}

export default Footer;
