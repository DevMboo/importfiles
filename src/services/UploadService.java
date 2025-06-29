package services;

import repositories.TbRefBasesRepository;
import repositories.UploadRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;


public class UploadService {
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public void salvarArquivos(Map<String, byte[]> arquivos, String idBase) throws IOException {
        File pasta = new File("src/uploads/bases");
        if (!pasta.exists() && !pasta.mkdirs()) throw new IOException("Não foi possível criar a pasta de uploads");

        for (String nomeOriginal : arquivos.keySet()) {
            String extensao = "";
            int idx = nomeOriginal.lastIndexOf('.');
            if (idx > 0) extensao = nomeOriginal.substring(idx);
            String novoNome = nomeOriginal.replace(extensao, "") + "_" + idBase + extensao;
            File destino = new File(pasta, novoNome);

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                fos.write(arquivos.get(nomeOriginal));
            }
            executor.submit(() -> processarArquivo(destino, idBase));
        }
    }

    private void processarArquivo(File arquivo, String idBase) {
        try {
            if (arquivo.getName().endsWith(".csv")) {
                processarCSV(arquivo, idBase);
            } else if (arquivo.getName().endsWith(".xlsx")) {
                processarXLSX(arquivo, idBase);
            }
        } catch (Exception e) {
            System.err.println("[Upload] Erro ao processar arquivo: " + arquivo.getName());
            e.printStackTrace();
        }
    }

    private void processarCSV(File arquivo, String idBase) throws IOException {
        long inicio = System.currentTimeMillis();
        System.out.println("[Upload] Iniciando processamento do arquivo: " + arquivo.getName());

        TbRefBasesRepository refRepository = new TbRefBasesRepository();
        UploadRepository uploadRepository = new UploadRepository();

        String nomeTabela;
        List<String> colunas;
        int totalColunasEsperadas;
        try {
            nomeTabela = refRepository.buscarNomeTabelaRawPorIdBase(Long.parseLong(idBase));
            if (nomeTabela == null) throw new IllegalArgumentException("Base não encontrada para o id: " + idBase);
            colunas = uploadRepository.buscarColunasDaTabela(nomeTabela);
            totalColunasEsperadas = colunas.size();
        } catch (SQLException e) {
            throw new IOException("Erro ao acessar o banco de dados: " + e.getMessage(), e);
        }

        try (InputStreamReader isr = new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr, 64 * 1024)) {

            String header = br.readLine();
            if (header == null) throw new IOException("Arquivo CSV vazio");
            char separador = detectarSeparador(header);

            String linha;
            List<String[]> batch = new ArrayList<>(5000);
            int count = 0, linhasInvalidas = 0;

            while ((linha = br.readLine()) != null) {
                if (linha.trim().isEmpty()) continue;

                String[] valores = parseCSVLine(linha, separador);
                if (valores.length != totalColunasEsperadas) {
                    linhasInvalidas++;
                    if (linhasInvalidas <= 10)
                        System.err.println("[Upload] Linha inválida: " + linha);
                    continue;
                }

                batch.add(valores);
                count++;

                if (batch.size() == 5000) {
                    try {
                        uploadRepository.inserirBatchNoBanco(nomeTabela, colunas, batch);
                    } catch (SQLException e) {
                        throw new IOException("Erro ao inserir batch no banco: " + e.getMessage(), e);
                    }
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                try {
                    uploadRepository.inserirBatchNoBanco(nomeTabela, colunas, batch);
                } catch (SQLException e) {
                    throw new IOException("Erro ao inserir batch no banco: " + e.getMessage(), e);
                }
            }

            System.out.println("[Upload] Processamento concluído:");
            System.out.println(" - Linhas válidas inseridas: " + count);
            System.out.println(" - Linhas inválidas ignoradas: " + linhasInvalidas);
        } catch (IOException e) {
            throw new IOException("Erro ao ler/processar CSV: " + e.getMessage(), e);
        } finally {
            long fim = System.currentTimeMillis();
            System.out.println("[Upload] Tempo total de processamento: " + (fim - inicio) + " ms");
        }
    }

    private char detectarSeparador(String header) {
        int virgulas = header.length() - header.replace(",", "").length();
        int pontosVirgula = header.length() - header.replace(";", "").length();
        return pontosVirgula > virgulas ? ';' : ',';
    }

    private String[] parseCSVLine(String linha, char separador) {
        List<String> valores = new ArrayList<>();
        boolean entreAspas = false;
        StringBuilder sb = new StringBuilder();
        for (char c : linha.toCharArray()) {
            if (c == '"') {
                entreAspas = !entreAspas;
            } else if (c == separador && !entreAspas) {
                valores.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        valores.add(sb.toString().trim());
        return valores.toArray(new String[0]);
    }

    public void processarXLSX(File arquivo, String idBase) throws IOException {
        long inicio = System.currentTimeMillis();
        System.out.println("[Upload] Iniciando processamento do arquivo XLSX: " + arquivo.getName());

        TbRefBasesRepository refRepository = new TbRefBasesRepository();
        UploadRepository uploadRepository = new UploadRepository();

        String nomeTabela;
        List<String> colunas;
        int totalColunasEsperadas;
        try {
            nomeTabela = refRepository.buscarNomeTabelaRawPorIdBase(Long.parseLong(idBase));
            if (nomeTabela == null) throw new IllegalArgumentException("Base não encontrada para o id: " + idBase);
            colunas = uploadRepository.buscarColunasDaTabela(nomeTabela);
            totalColunasEsperadas = colunas.size();
        } catch (SQLException e) {
            throw new IOException("Erro ao acessar o banco de dados: " + e.getMessage(), e);
        }

        System.out.println("Tentando abrir arquivo: " + arquivo.getAbsolutePath());
        System.out.println("[DEBUG] Arquivo existe? " + arquivo.exists() + " | Tamanho: " + arquivo.length());

        try {
            System.out.println("[DEBUG] Antes de abrir OPCPackage");
            org.apache.poi.openxml4j.opc.OPCPackage pkg = org.apache.poi.openxml4j.opc.OPCPackage.open(arquivo);

            System.out.println("[DEBUG] Abriu OPCPackage com sucesso");
            org.apache.poi.xssf.eventusermodel.XSSFReader reader = new org.apache.poi.xssf.eventusermodel.XSSFReader(pkg);
            System.out.println("[DEBUG] Criou XSSFReader");
            org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable strings = new org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable(pkg);
            System.out.println("[DEBUG] Criou ReadOnlySharedStringsTable");

            org.xml.sax.XMLReader parser = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
            System.out.println("[Upload] Criando parser XML para XLSX, usando SAX [LINHA] 173");
            XLSXSheetHandlerService handler = new XLSXSheetHandlerService(
                    colunas,
                    totalColunasEsperadas,
                    uploadRepository,
                    nomeTabela,
                    strings
            );
            parser.setContentHandler(handler);

            Iterator<InputStream> sheets = reader.getSheetsData();
            int sheetIndex = 0;
            while (sheets.hasNext()) {
                System.out.println("[Upload] Processando sheet #" + (sheetIndex + 1));
                sheetIndex++;
                try (InputStream sheet = sheets.next()) {
                    parser.parse(new org.xml.sax.InputSource(sheet));
                } catch (Exception e) {
                    System.err.println("[Upload] Erro ao processar sheet #" + sheetIndex);
                    e.printStackTrace();
                }
            }
            System.out.println("[Upload] Processamento concluído:");
            System.out.println(" - Linhas válidas inseridas: " + handler.getCount());
            System.out.println(" - Linhas inválidas ignoradas: " + handler.getLinhasInvalidas());
        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
            System.err.println("[ERRO] Formato inválido de arquivo XLSX: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[ERRO] Falha ao abrir ou processar arquivo XLSX: " + e.getMessage());
            e.printStackTrace();
        }

        long fim = System.currentTimeMillis();
        System.out.println("[Upload] Tempo total de processamento: " + (fim - inicio) + " ms");
    }
}