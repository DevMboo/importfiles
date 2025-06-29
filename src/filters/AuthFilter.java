package filters;

import core.Filter;
import core.HttpRequest;
import core.HttpResponse;

import java.io.IOException;

public class AuthFilter implements Filter {
    @Override
    public boolean apply(HttpRequest req, HttpResponse res) throws IOException {
        String auth = req.getQueryParam("auth");
        if ("secret".equals(auth)) {
            return true; // autorizado
        } else {
            res.sendUnauthorized(res);
            return false;
        }
    }
}
