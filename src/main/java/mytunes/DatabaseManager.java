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
    
    public List<String> getPlaylists() {
        List<String> playlists = new ArrayList<>();
        String query = "SELECT name FROM playlists";
        
        try (Connection connection = connect(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(query)) {

            while (resultSet.next()) {
                String playlistName = resultSet.getString("name");
                playlists.add(playlistName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return playlists;
    }
    
    public boolean addPlaylist(String name) {
        // Prepare query to prevent SQL Injection
        String query = "INSERT INTO playlists (name) VALUES (?)";
        
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, name);
            
            // Execute query
            int rowsAffected = statement.executeUpdate();
            return (rowsAffected > 0);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Song> getPlaylistSongs(String playlistName) {
        List<Song> songs = new ArrayList<>();
        // Prepare query to prevent SQL Injection
        String query = "SELECT s.* FROM songs AS s JOIN playlists_songs AS ps ON s.id = ps.song_id JOIN playlists AS p ON ps.playlist_id = p.id WHERE p.name = ?";
        
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playlistName);
            
            // Execute query
            ResultSet resultSet = statement.executeQuery();
            
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
    
    public boolean addToPlaylist(String songTitle, String playlistName) {
        // Find song and playlist ID
        int songId = findSongIdByName(songTitle);
        int playlistId = findPlaylistIdByName(playlistName);
        
        if (songId <= 0 || playlistId <= 0) {
            return false;
        }
        
        // Prepare query to prevent SQL Injection
        String query = "INSERT INTO playlists_songs (playlist_id, song_id) VALUES (?, ?)";
        
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, playlistId);
            statement.setInt(2, songId);
            
            // Execute query
            int rowsAffected = statement.executeUpdate();
            return (rowsAffected > 0);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
   
    private int findPlaylistIdByName(String playlistName) {
        // Prepare query to prevent SQL Injection
        String query = "SELECT id FROM playlists WHERE name = ?";
        
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playlistName);
            
            // Execute query
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // playlist not found
    }
    
    private int findSongIdByName(String songTitle) {
        // Prepare query to prevent SQL Injection
        String query = "SELECT id FROM songs WHERE title = ?";
        
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, songTitle);

            // Execute query
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // song not found
    }
}
