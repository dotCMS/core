import AboutUs from "./components/aboutUs";
import Blogs from "./components/blogs";
import Destinations from "./components/destinations";

// Footer component
function Footer() {
    return (
        <footer className="p-4 py-12 text-white bg-purple-100">
            <div className="grid gap-7 mx-24 md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3">
                <AboutUs />
                <Blogs />
                <Destinations />
            </div>
        </footer>
    );
}

export default Footer;
