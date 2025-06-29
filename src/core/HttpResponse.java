package core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class HttpResponse {
    private OutputStream outputStream;
    private BufferedWriter writer;
    private String contentType = "text/plain";
    private int status = 200;

    // Recebe OutputStream, cria BufferedWriter para escrita de texto
    public HttpResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    public void setContentType(String type) {
        this.contentType = type;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    // Envia conteúdo texto
    public String send(String body) {
        try {
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            writer.write("HTTP/1.1 " + status + " OK\r\n");
            writer.write("Content-Type: " + contentType + "; charset=UTF-8\r\n");
            writer.write("Content-Length: " + bodyBytes.length + "\r\n");
            writer.write("\r\n");
            writer.flush(); // Limpa cabeçalho antes de enviar corpo
            outputStream.write(bodyBytes); // Escreve corpo em bytes
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return body;
    }

    // Envia bytes brutos (imagens, binários)
    public void sendBytes(byte[] bytes) {
        try {
            writer.write("HTTP/1.1 " + status + " OK\r\n");
            writer.write("Content-Type: " + contentType + "\r\n");
            writer.write("Content-Length: " + bytes.length + "\r\n");
            writer.write("\r\n");
            writer.flush(); // cabeçalho enviado
            outputStream.write(bytes); // corpo binário
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendUnauthorized(HttpResponse res) {
        res.setStatus(401);
        res.send("401 - Não autorizado");
    }

    public void redirect(String location) {
        try {
            writer.write("HTTP/1.1 302 Found\r\n");
            writer.write("Location: " + location + "\r\n");
            writer.write("Content-Length: 0\r\n");
            writer.write("\r\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
