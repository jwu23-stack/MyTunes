package mytunes;

/**
 *
 * @author Jerry
 */
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import io.github.cdimascio.dotenv.*;

public class DatabaseManager {

    public Connection connect() throws SQLException {
        Dotenv dotenv = Dotenv.load();
        String dbUrl = dotenv.get("DB_URL");
        String dbUser = dotenv.get("DB_USER");
        String dbPassword = dotenv.get("DB_PASSWORD");
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    // Return the list of songs from SQL Database
    public List<Song> getAllSongs() {
        List<Song> songs = new ArrayList<>();
        String query = "SELECT title, artist, album, year, genre, comment FROM songs";

        try (Connection connection = connect(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String title = resultSet.getString("title");
                String artist = resultSet.getString("artist");
                String album = resultSet.getString("album");
                String year = resultSet.getString("year");
                String genre = resultSet.getString("genre");
                String comment = resultSet.getString("comment");

                songs.add(new Song(title, artist, album, year, genre, comment));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return songs;
    }

    public boolean addSong(Song song) {
        // Prepare query to prevent SQL Injection
        String query = "INSERT INTO songs (title, artist, album, year, genre, comment) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, song.getTitle());
            statement.setString(2, song.getArtist());
            statement.setString(3, song.getAlbum());
            statement.setString(4, song.getYear());
            statement.setString(5, song.getGenre());
            statement.setString(6, song.getComment());

            // Execute query
            int rowsAffected = statement.executeUpdate();
            return (rowsAffected > 0);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSong(String title) {
        // Prepare query to prevent SQL Injection
        String query = "DELETE FROM songs WHERE title = ?";

        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, title);

            // Execute query
            int rowsAffected = statement.executeUpdate();
            return (rowsAffected > 0);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
