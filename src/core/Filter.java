package core;

import java.io.IOException;

public interface Filter {
    /**
     * Aplica o filtro.
     * @param req A requisição.
     * @param res A resposta.
     * @return true para continuar a cadeia, false para interromper (ex: autenticação falhou).
     */
    boolean apply(HttpRequest req, HttpResponse res) throws IOException;
}