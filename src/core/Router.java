package core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Router {

    private static class RouteEntry {
        BiConsumer<HttpRequest, HttpResponse> handler;
        List<Filter> filters;

        RouteEntry(BiConsumer<HttpRequest, HttpResponse> handler, List<Filter> filters) {
            this.handler = handler;
            this.filters = filters;
        }
    }

    private static final Map<String, RouteEntry> routes = new HashMap<>();

    private static final List<BiFunction<HttpRequest, HttpResponse, Boolean>> globalFilters = new ArrayList<>();

    private static final Map<String, List<BiFunction<HttpRequest, HttpResponse, Boolean>>> routeFilters = new HashMap<>();

    public static void addRoute(String method, String path, BiConsumer<HttpRequest, HttpResponse> handler) {
        routes.put(method.toUpperCase() + " " + path, new RouteEntry(handler, Collections.emptyList()));
    }

    public static void addRoute(String method, String path, BiConsumer<HttpRequest, HttpResponse> handler, Filter... filters) {
        routes.put(method.toUpperCase() + " " + path, new RouteEntry(handler, Arrays.asList(filters)));
    }

    public static void addGlobalFilter(BiFunction<HttpRequest, HttpResponse, Boolean> filter) {
        globalFilters.add(filter);
    }

    public static void addRouteFilter(String method, String path, BiFunction<HttpRequest, HttpResponse, Boolean> filter) {
        String key = method.toUpperCase() + " " + path;
        routeFilters.computeIfAbsent(key, k -> new ArrayList<>()).add(filter);
    }

    public static void handle(HttpRequest req, HttpResponse res) {
        String method = req.getMethod();
        String path = req.getPath();

        if (method == null || path == null) {
            res.setStatus(400);
            res.send("400 - Requisição mal formada");
            return;
        }

        if (path.startsWith("/static/")) {
            serveStaticFile(path, res);
            return;
        }

        String key = method.toUpperCase() + " " + path;

        // executa filtros globais (mantido igual)
        for (BiFunction<HttpRequest, HttpResponse, Boolean> filter : globalFilters) {
            boolean ok = filter.apply(req, res);
            if (!ok) {
                return;
            }
        }

        // executa filtros de rota globais (mantido igual)
        List<BiFunction<HttpRequest, HttpResponse, Boolean>> filters = routeFilters.get(key);
        if (filters != null) {
            for (BiFunction<HttpRequest, HttpResponse, Boolean> filter : filters) {
                boolean ok = filter.apply(req, res);
                if (!ok) {
                    return;
                }
            }
        }

        // aqui pega RouteEntry, não BiConsumer
        RouteEntry routeEntry = routes.get(key);

        if (routeEntry != null) {
            try {
                for (Filter filter : routeEntry.filters) {
                    if (!filter.apply(req, res)) {
                        return;
                    }
                }
            } catch (IOException e) {
                res.setStatus(500);
                res.send("Erro ao executar filtro da rota: " + e.getMessage());
                return;
            }

            routeEntry.handler.accept(req, res);
        } else {
            res.setStatus(404);
            res.send("404 - Rota não encontrada");
        }
    }

    private static void serveStaticFile(String path, HttpResponse res) {
        String relativePath = path.substring("/static/".length());
        Path filePath = Paths.get("static", relativePath);

        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            res.setStatus(404);
            res.send("Arquivo estático não encontrado");
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            String contentType = getContentType(relativePath);
            res.setContentType(contentType);

            // Se for tipo texto, pode enviar como string
            if (contentType.startsWith("text/") || contentType.equals("application/javascript")) {
                String content = new String(fileBytes, StandardCharsets.UTF_8);
                res.send(content);
            } else {
                // Envia bytes direto para imagens e outros binários
                res.sendBytes(fileBytes);
            }

        } catch (IOException e) {
            res.setStatus(500);
            res.send("Erro ao ler arquivo estático: " + e.getMessage());
        }
    }


    private static String getContentType(String filename) {
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".js")) return "application/javascript";
        if (filename.endsWith(".html")) return "text/html";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".gif")) return "image/gif";
        return "application/octet-stream";
    }
}
