package models;

public class TbRefBases {
    private Long idBases;
    private String nomeTabelaRaw;
    private String descricao;
    private String codDocumento;
    private String exemploBase;
    private String status;

    // Getters e Setters
    public Long getIdBases() { return idBases; }
    public void setIdBases(Long idBases) { this.idBases = idBases; }

    public String getNomeTabelaRaw() { return nomeTabelaRaw; }
    public void setNomeTabelaRaw(String nomeTabelaRaw) { this.nomeTabelaRaw = nomeTabelaRaw; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getCodDocumento() { return codDocumento; }
    public void setCodDocumento(String codDocumento) { this.codDocumento = codDocumento; }

    public String getExemploBase() { return exemploBase; }
    public void setExemploBase(String exemploBase) { this.exemploBase = exemploBase; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}