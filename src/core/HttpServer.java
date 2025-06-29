package core;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class HttpServer {
    private int port;

    public HttpServer(int port) {
        this.port = port;
    }

    public void loadRoutes(List<RouteLoader> routeLoaders) {
        for (RouteLoader loader : routeLoaders) {
            loader.loadRoutes();
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server start in port -> " + port);
            System.out.println("Let's go code");

            while (true) {
                Socket clientSocket = serverSocket.accept();

                new Thread(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) throws IOException {
        InputStream input = socket.getInputStream();
        // Passe diretamente o OutputStream para HttpResponse
        HttpResponse response = new HttpResponse(socket.getOutputStream());

        HttpRequest request = new HttpRequest(input);

        Router.handle(request, response);

        socket.close();
    }

}
