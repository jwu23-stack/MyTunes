package mytunes;

/**
 *
 * @author Jerry
 */
import java.util.List;
import java.io.File;

public class MusicPlayer {
    private DatabaseManager dbManager;
    
    public MusicPlayer() {
        dbManager = new DatabaseManager();
    }
    
    public List<Song> getAllSongs() {
        return dbManager.getAllSongs();
    }
    
    public boolean addSong(File mp3File) {
        Song song = Song.extractMetaData(mp3File);
        if (song != null) {
            return dbManager.addSong(song);
        } else {
            System.out.println("Failed to extract metadata from: " + mp3File.getName());
            return false;
        }
    }
    
    public boolean deleteSong(String title) {
        return dbManager.deleteSong(title);
    }
}
