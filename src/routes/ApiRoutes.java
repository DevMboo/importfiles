package routes;

import controllers.ApiController;
import core.RouteLoader;
import core.Router;

public class ApiRoutes implements RouteLoader {
    @Override
    public void loadRoutes() {
        Router router = new Router();
        Router.addRoute("GET", "/api", ApiController::get);
        Router.addRoute("GET", "/api/bases", ApiController::bases);
        Router.addRoute("POST", "/api/uploads", ApiController::uploads);
    }

}
