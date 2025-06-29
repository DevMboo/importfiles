package services;

import models.TbRefBases;
import repositories.TbRefBasesRepository;

import java.sql.SQLException;
import java.util.List;

public class TbRefBasesService {
    private final TbRefBasesRepository repository = new TbRefBasesRepository();

    public void adicionarBase(TbRefBases base) throws SQLException {
        // Aqui você pode adicionar regras de negócio antes de inserir
        repository.insert(base);
    }

    public List<TbRefBases> listarBases() throws SQLException {
        return repository.findAll();
    }
}