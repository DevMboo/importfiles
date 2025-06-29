package services;

import models.User;
import repositories.UserRepository;

import java.util.List;

public class UserService {
    private final UserRepository repository = new UserRepository();

    public void salvarUsuario(String nome, String email, String senha) {
        User user = new User(nome, email, senha);
        repository.save(user);
    }

    public List<User> listarUsuarios() {
        return repository.findAll();
    }
}
