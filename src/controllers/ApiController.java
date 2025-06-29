package controllers;

import core.HttpRequest;
import core.HttpResponse;
import models.TbRefBases;
import services.TbRefBasesService;
import services.UploadService;

import java.util.List;

public class ApiController {

    private static final TbRefBasesService tbRefBasesService = new TbRefBasesService();
    private static final UploadService uploadService = new UploadService();

    public static void get(HttpRequest req, HttpResponse res) {
        String json = "{\"message\": \"Olá do Java Server!\"}";
        res.setContentType("application/json");
        res.send(json);
    }

    public static String bases(HttpRequest req, HttpResponse res) {
        try {
            List<TbRefBases> bases = tbRefBasesService.listarBases();
            StringBuilder json = new StringBuilder();
            json.append("[");
            for (int i = 0; i < bases.size(); i++) {
                TbRefBases base = bases.get(i);
                json.append("{")
                        .append("\"id_bases\":").append(base.getIdBases()).append(",")
                        .append("\"nome_tabela_raw\":\"").append(base.getNomeTabelaRaw()).append("\"")
                        .append("}");
                if (i < bases.size() - 1) json.append(",");
            }
            json.append("]");
            res.setContentType("application/json");
            return res.send(json.toString());
        } catch (Exception e) {
            res.setStatus(500);
            return "{\"error\": \"Erro ao buscar bases: " + e.getMessage() + "\"}";
        }
    }

    public static String uploads(HttpRequest req, HttpResponse res) {
        String idBase = req.getPostParam("base");
        try {
            uploadService.salvarArquivos(req.getFileUploads(), idBase);
            return res.send("Arquivos salvos com sucesso! Processamento iniciado em segundo plano.");
        } catch (Exception e) {
            res.setStatus(500);
            return res.send("Erro ao salvar arquivos: " + e.getMessage());
        }
    }
}
