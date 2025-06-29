package repositories;

import database.Database;

import models.TbRefBases;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class TbRefBasesRepository {

    public void insert(TbRefBases base) throws SQLException {
        String sql = "INSERT INTO TB_REF_BASES (nome_tabela_raw, descricao, cod_documento, exemplo_base, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, base.getNomeTabelaRaw());
            stmt.setString(2, base.getDescricao());
            stmt.setString(3, base.getCodDocumento());
            stmt.setString(4, base.getExemploBase());
            stmt.setString(5, base.getStatus());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    base.setIdBases(rs.getLong(1));
                }
            }
        }
    }

    public List<TbRefBases> findAll() throws SQLException {
        List<TbRefBases> list = new ArrayList<>();
        String sql = "SELECT * FROM TB_REF_BASES";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TbRefBases base = new TbRefBases();
                base.setIdBases(rs.getLong("id_bases"));
                base.setNomeTabelaRaw(rs.getString("nome_tabela_raw"));
                base.setDescricao(rs.getString("descricao"));
                base.setCodDocumento(rs.getString("cod_documento"));
                base.setExemploBase(rs.getString("exemplo_base"));
                base.setStatus(rs.getString("status"));
                list.add(base);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Erro ao buscar bases: " + e.getMessage());
        }

        return list;
    }

    public String buscarNomeTabelaRawPorIdBase(Long idBase) throws SQLException {
        String sql = "SELECT nome_tabela_raw FROM TB_REF_BASES WHERE id_bases = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, idBase);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("nome_tabela_raw");
                }
            }
        }
        return null;
    }
}