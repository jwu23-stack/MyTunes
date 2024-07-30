package mytunes;

/**
 *
 * @author Jerry
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // AWS Credentials to AWS SQL Database

    private static final String URL = "jdbc:mysql://admin.cx2c440uu0lx.us-east-2.rds.amazonaws.com:3306/mytunes";
    private static final String USER = "admin";
    private static final String PASSWORD = "12345678";

    public Connection connect() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        String query = "SELECT title, artist, album, year, genre, comment FROM songs";

        try (Connection connection = connect(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String title = resultSet.getString("title");
                String artist = resultSet.getString("artist");
                String album = resultSet.getString("album");
                int year = resultSet.getInt("year");
                String genre = resultSet.getString("genre");
                String comment = resultSet.getString("comment");

                songs.add(new Song(title, artist, album, year, genre, comment));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songs;
    }

}
