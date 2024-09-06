import AboutUs from "./components/aboutUs";
import Blogs from "./components/blogs";
import Destinations from "./components/destinations";

// Footer component
function Footer() {
    return (
        <footer className="p-4 text-white bg-purple-100 py-24">
            <div className="grid md:grid-cols-3 sm:grid-cols-1 md:grid-rows-1 sm:grid-rows-3 gap-7 mx-24">
                <AboutUs />
                <Blogs />
                <Destinations />
            </div>
        </footer>
    );
}

export default Footer;
