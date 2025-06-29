package models;

public class User {
    private int id;
    private String nome;
    private String email;
    private String senha;

    public User(int id, String nome, String email ) {
        this.id = id;
        this.nome = nome;
        this.email = email;
    }

    public User(String nome, String email, String senha) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getSenha() { return senha; }
}