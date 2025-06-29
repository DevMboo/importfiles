import core.HttpServer;
import routes.WebRoutes;
import routes.ApiRoutes;
import services.UploadService;

import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        HttpServer server = new HttpServer(8080);

        server.loadRoutes(List.of(
                new WebRoutes(),
                new ApiRoutes()
        ));

        server.start();
    }
}
