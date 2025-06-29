package routes;

import controllers.HomeController;
import core.RouteLoader;
import core.Router;

public class WebRoutes implements RouteLoader {
    @Override
    public void loadRoutes() {
        // Users Routes
        Router.addRoute("GET", "/", HomeController::index);
    }
}
