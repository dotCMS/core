import AboutUs from "./components/aboutUs";
import Blogs from "./components/blogs";
import Destinations from "./components/destinations";

// Footer component
function Footer({ blogs, destinations }) {
    return (
        <footer className="p-4 bg-slate-600 text-white py-24">
            <div className="grid md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3 gap-7 mx-24">
                <AboutUs />
                <Blogs blogs={blogs} />
                <Destinations destinations={destinations} />
            </div>
        </footer>
    );
}

export default Footer;
