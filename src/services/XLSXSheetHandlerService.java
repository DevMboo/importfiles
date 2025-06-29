package services;

import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import repositories.UploadRepository;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class XLSXSheetHandlerService extends DefaultHandler {
    private final List<String> colunas;
    private final int totalColunasEsperadas;
    private final UploadRepository uploadRepository;
    private final String nomeTabela;
    private final ReadOnlySharedStringsTable sharedStrings;
    private final List<String[]> batch = new ArrayList<>(5000);
    private int count = 0, linhasInvalidas = 0;
    private List<String> currentRow = new ArrayList<>();
    private StringBuilder currentValue = new StringBuilder();
    private boolean isCellValue = false;
    private String cellType = null;
    private int lastColumnIndex = -1;


    public XLSXSheetHandlerService(List<String> colunas, int totalColunasEsperadas, UploadRepository uploadRepository, String nomeTabela, ReadOnlySharedStringsTable sharedStrings) {
        this.colunas = colunas;
        this.totalColunasEsperadas = totalColunasEsperadas;
        this.uploadRepository = uploadRepository;
        this.nomeTabela = nomeTabela;
        this.sharedStrings = sharedStrings;
    }

    public int getCount() { return count; }
    public int getLinhasInvalidas() { return linhasInvalidas; }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("row".equals(qName)) {
            currentRow = new ArrayList<>();
            lastColumnIndex = -1;
        } else if ("c".equals(qName)) {
            cellType = attributes.getValue("t");
            String cellRef = attributes.getValue("r");
            int colIndex = getColumnIndex(cellRef);
            // Preenche células vazias entre a última e a atual
            for (int i = lastColumnIndex + 1; i < colIndex; i++) {
                currentRow.add("");
            }
            lastColumnIndex = colIndex;
        } else if ("v".equals(qName) || "t".equals(qName)) {
            isCellValue = true;
            currentValue.setLength(0);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (("v".equals(qName) || "t".equals(qName)) && isCellValue) {
            String value = currentValue.toString();
            if ("s".equals(cellType) && sharedStrings != null) {
                try {
                    int idx = Integer.parseInt(value);
                    value = sharedStrings.getItemAt(idx).getString();
                } catch (Exception e) {
                    System.err.println("Erro ao processar string compartilhada: " + e.getMessage());
                }
            }
            currentRow.add(value);
            isCellValue = false;
            cellType = null;
        } else if ("row".equals(qName)) {
            // Preenche células vazias até o final da linha
            for (int i = currentRow.size(); i < totalColunasEsperadas; i++) {
                currentRow.add("");
            }
            if (!currentRow.isEmpty()) {
                if (count == 0) {
                    // Pula o header (primeira linha)
                    count++;
                } else {
                    if (currentRow.size() != totalColunasEsperadas) {
                        linhasInvalidas++;
                        if (linhasInvalidas <= 10) {
                            System.err.println("[Upload] Linha inválida XLSX - esperado: " +
                                    totalColunasEsperadas + ", obtido: " + currentRow.size());
                        }
                    } else {
                        batch.add(currentRow.toArray(new String[0]));
                        count++;

                        if (batch.size() >= 5000) {
                            try {
                                uploadRepository.inserirBatchNoBanco(nomeTabela, colunas, batch);
                                batch.clear();
                            } catch (SQLException e) {
                                throw new RuntimeException("Erro ao inserir batch no banco: " + e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        }
    }

    // Função utilitária para converter referência de célula (ex: "C5") para índice de coluna (0-based)
    private int getColumnIndex(String cellRef) {
        if (cellRef == null) return lastColumnIndex + 1;
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char ch = cellRef.charAt(i);
            if (Character.isLetter(ch)) {
                col = col * 26 + (ch - 'A' + 1);
            } else {
                break;
            }
        }
        return col - 1;
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (isCellValue) {
            currentValue.append(ch, start, length);
        }
    }

    @Override
    public void endDocument() {
        if (!batch.isEmpty()) {
            try {
                uploadRepository.inserirBatchNoBanco(nomeTabela, colunas, batch);
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao inserir batch no banco: " + e.getMessage(), e);
            }
        }
    }
}