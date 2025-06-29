package core;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private Map<String, String> queryParams = new HashMap<>();
    private Map<String, String> postParams = new HashMap<>();
    private Map<String, byte[]> fileUploads = new HashMap<>();
    private String rawBody;

    public HttpRequest(InputStream input) throws IOException {
        // Lê headers manualmente, linha a linha, sem BufferedReader
        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
        int prev = 0, curr;
        while ((curr = input.read()) != -1) {
            headerBuf.write(curr);
            if (prev == '\r' && curr == '\n') {
                // Verifica se terminou os headers (\r\n\r\n)
                byte[] arr = headerBuf.toByteArray();
                int len = arr.length;
                if (len >= 4 && arr[len-4] == '\r' && arr[len-3] == '\n' && arr[len-2] == '\r' && arr[len-1] == '\n') {
                    break;
                }
            }
            prev = curr;
        }
        String headersStr = headerBuf.toString(StandardCharsets.UTF_8);
        String[] headerLines = headersStr.split("\r\n");
        String requestLine = headerLines[0];
        String[] parts = requestLine.split(" ");
        if (parts.length >= 2) {
            method = parts[0];
            String rawPath = parts[1];
            if (rawPath.contains("?")) {
                String[] split = rawPath.split("\\?");
                path = split[0];
                parseQueryString(split[1], queryParams);
            } else {
                path = rawPath;
            }
        }

        int contentLength = 0;
        String contentType = null;
        for (int i = 1; i < headerLines.length; i++) {
            String header = headerLines[i];
            if (header.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(header.split(":", 2)[1].trim());
            }
            if (header.toLowerCase().startsWith("content-type:")) {
                contentType = header.split(":", 2)[1].trim();
            }
        }

        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            if (contentType != null && contentType.startsWith("multipart/form-data")) {
                String boundary = contentType.split("boundary=")[1];
                byte[] bodyBytes = input.readNBytes(contentLength);
                parseMultipart(bodyBytes, boundary);
            } else if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                byte[] bodyBytes = input.readNBytes(contentLength);
                rawBody = new String(bodyBytes, StandardCharsets.UTF_8);
                parseQueryString(rawBody, postParams);
            } else if (contentLength > 0) {
                byte[] bodyBytes = input.readNBytes(contentLength);
                rawBody = new String(bodyBytes, StandardCharsets.UTF_8);
            }
        }
    }

    private void parseQueryString(String query, Map<String, String> targetMap) throws UnsupportedEncodingException {
        if (query == null || query.isEmpty()) return;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], "UTF-8");
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
            targetMap.put(key, value);
        }
    }

    private void parseMultipart(byte[] body, String boundary) throws IOException {
        String delimiter = "--" + boundary;
        int pos = 0;
        int len = body.length;

        while (pos < len) {
            int delimStart = indexOf(body, delimiter.getBytes(StandardCharsets.UTF_8), pos);
            if (delimStart < 0) break;
            pos = delimStart + delimiter.length();

            if (pos + 2 <= len && new String(body, pos, 2, StandardCharsets.UTF_8).equals("--")) break;
            if (pos < len && (body[pos] == '\r' || body[pos] == '\n')) {
                if (body[pos] == '\r' && pos + 1 < len && body[pos + 1] == '\n') pos += 2;
                else pos++;
            }

            int headersEnd = indexOf(body, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), pos);
            if (headersEnd < 0) break;
            String headers = new String(body, pos, headersEnd - pos, StandardCharsets.UTF_8);
            pos = headersEnd + 4;

            String name = null, filename = null;
            for (String h : headers.split("\r\n")) {
                if (h.toLowerCase().startsWith("content-disposition:")) {
                    for (String attr : h.split(";")) {
                        attr = attr.trim();
                        if (attr.startsWith("name=")) {
                            name = attr.split("=", 2)[1].replace("\"", "");
                        } else if (attr.startsWith("filename=")) {
                            filename = attr.split("=", 2)[1].replace("\"", "");
                        }
                    }
                }
            }

            int partEnd = indexOf(body, ("\r\n" + delimiter).getBytes(StandardCharsets.UTF_8), pos);
            if (partEnd < 0) partEnd = len;

            if (filename != null && !filename.isEmpty()) {
                byte[] fileData = new byte[partEnd - pos];
                System.arraycopy(body, pos, fileData, 0, fileData.length);
                fileUploads.put(filename, fileData);
            } else if (name != null) {
                String value = new String(body, pos, partEnd - pos, StandardCharsets.UTF_8).trim();
                postParams.put(name, value);
            }
            pos = partEnd;
        }
    }

    private int indexOf(byte[] data, byte[] pattern, int start) {
        outer: for (int i = start; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    public Map<String, byte[]> getFileUploads() {
        return fileUploads;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryParam(String name) {
        return queryParams.get(name);
    }

    public String getPostParam(String name) {
        return postParams.get(name);
    }

    public Map<String, String> getAllPostParams() {
        return postParams;
    }

    public String getRawBody() {
        return rawBody;
    }
}