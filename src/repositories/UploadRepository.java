package repositories;

import database.Database;
import java.sql.*;
import java.util.*;

public class UploadRepository {

    public List<String> buscarColunasDaTabela(String nomeTabela) throws SQLException {
        List<String> colunas = new ArrayList<>();
        String sql = "SELECT column_name FROM information_schema.columns WHERE table_name = ? ORDER BY ordinal_position";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomeTabela.toLowerCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                colunas.add(rs.getString("column_name"));
            }
        }
        return colunas;
    }

    public void inserirBatchNoBanco(String nomeTabela, List<String> colunas, List<String[]> batch) throws SQLException {
        if (colunas == null || colunas.isEmpty()) {
            throw new SQLException("Nenhuma coluna encontrada para a tabela: " + nomeTabela);
        }

        StringBuilder sql = new StringBuilder("INSERT INTO " + nomeTabela.toLowerCase() + " (");
        sql.append(String.join(",", colunas)).append(") VALUES (");
        sql.append("?,".repeat(colunas.size()));
        sql.setLength(sql.length() - 1);
        sql.append(")");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (String[] valores : batch) {
                for (int i = 0; i < colunas.size(); i++) {
                    ps.setString(i + 1, i < valores.length ? valores[i] : null);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}