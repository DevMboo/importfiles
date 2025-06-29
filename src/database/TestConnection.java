package database;

public class TestConnection {
    public static void main(String[] args) {
        try {
            var conn = Database.getConnection();
            System.out.println("Conectado com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
