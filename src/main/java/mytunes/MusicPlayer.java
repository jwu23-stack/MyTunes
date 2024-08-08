package mytunes;

/**
 *
 * @author Jerry
 */
import java.util.List;
import java.io.File;
import javafx.application.Platform;
import javafx.scene.media.*;

public class MusicPlayer {
    private DatabaseManager dbManager;
    private MediaPlayer mediaPlayer; // Controls audio playback
    private Media media; // Holds media resource
    private Song selectedSong;
    private List<Song> songs;
    private int selectedIndex = 0; // Index of current song

    public MusicPlayer() {
        dbManager = new DatabaseManager();
    }

    public List<Song> getAllSongs() {
        songs = dbManager.getAllSongs();
        return songs;
    }

    public Song getSelectedSong() {
        return selectedSong;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedSong(Song selectedSong, int selectedIndex) {
        this.selectedSong = selectedSong;
        this.selectedIndex = selectedIndex;
    }

    public Song findSongByTitle(String title) {
        for (Song song : songs) {
            if (song.getTitle().equalsIgnoreCase(title)) {
                return song;
            }
        }
        return null;
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
        Song songToRemove = findSongByTitle(title);
        songs.remove(songToRemove);
        return dbManager.deleteSong(title);
    }

    public void playSong() {
        if (selectedSong != null && selectedIndex >= 0) {
            Platform.runLater(() -> {
                try {
                    // Pause current media player if it's already playing
                    if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                        mediaPlayer.pause();
                    }
                    media = new Media(selectedSong.getFileURL(selectedSong.getTitle()));
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("No song selected or invalid index");
        }
    }

    public void playSongFromFile(File mp3File) {
        Platform.runLater(() -> {
            try {
                // Pause current media player if it's already playing
                if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.pause();
                }
                media = new Media(mp3File.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING;
    }

    public void pausePlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    public void resumePlaying() {
        if (mediaPlayer != null && (!mediaPlayer.getStatus().equals(MediaPlayer.Status.PLAYING))) {
            mediaPlayer.play();
        }
    }

    public void nextSong() {
        if (isPlaying()) { // Stop playing current song if needed
            stopPlaying();
        }

        selectedIndex++;
        if (selectedIndex < songs.size()) { // Fetch next song in library
            selectedSong = songs.get(selectedIndex);
            setSelectedSong(selectedSong, selectedIndex);
            playSong();
        } else { // Loop back to first song if end of list
            selectedIndex = 0;
            selectedSong = songs.get(selectedIndex);
            setSelectedSong(selectedSong, selectedIndex);
            playSong();
        }
    }

    public void previousSong() {
        if (isPlaying()) { // Stop playing current song if needed
            stopPlaying();
        }

        selectedIndex--;
        if (selectedIndex >= 0) { // Fetch previous song in library
            selectedSong = songs.get(selectedIndex);
            setSelectedSong(selectedSong, selectedIndex);
            playSong();
        } else { // Loop back to last song if beginning of list
            selectedIndex = songs.size() - 1;
            selectedSong = songs.get(selectedIndex);
            setSelectedSong(selectedSong, selectedIndex);
            playSong();
        }
    }
    
    public boolean createPlaylist(String name) {
        return dbManager.addPlaylist(name);
    }
    
    public List<Song> getPlaylistSongs(String playlistName) {
        songs = dbManager.getPlaylistSongs(playlistName);
        return songs;
    }
    
    public List<String> getPlaylists() {
        return dbManager.getPlaylists();
    }
    
    public boolean addToPlaylist(String songTitle, String playlistName) {
        return dbManager.addToPlaylist(songTitle, playlistName);
    }
    
    public boolean deletePlaylist(int playlistId) {
        return dbManager.deletePlaylistById(playlistId);
    }
    
    public int findPlaylist(String playlistName) {
        return dbManager.findPlaylistIdByName(playlistName);
    }
}
