package controllers;

import core.HttpRequest;
import core.HttpResponse;
import utils.TemplateEngine;

public class HomeController {
    public static void index(HttpRequest req, HttpResponse res) {
        String html = TemplateEngine.render("public/templates/index.html");

        res.setContentType("text/html");
        res.send(html);
    }
}
