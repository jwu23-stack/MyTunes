package mytunes;

/**
 *
 * @author Jerry
 */
import java.util.List;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import javafx.application.Platform;
import javafx.scene.media.*;
import javafx.util.Duration;

public class MusicPlayer {
    private DatabaseManager dbManager;
    private MediaPlayer mediaPlayer; // Controls audio playback
    private Media media; // Holds media resource
    private Song selectedSong;
    private List<Song> songs;
    private int selectedIndex = 0; // Index of current song
    private double currentVolume = 0.2; // Default volume
    private List<Song> recentSongs = new LinkedList<>();
    private boolean shuffle = false; 
    private boolean repeat = false;

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
    
    public void setRecentSongs(List<Song> recentSongs) {
        this.recentSongs = recentSongs;
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
        if (shuffle) { // Pick a random song
            selectedIndex = (int) (Math.random() * songs.size());
            selectedSong = songs.get(selectedIndex);
        }
        if (selectedSong != null && selectedIndex >= 0) {
            Platform.runLater(() -> {
                try {
                    // Stop current media player if it's already playing
                    if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                        mediaPlayer.stop();
                    }
                    media = new Media(selectedSong.getFileURL(selectedSong.getTitle()));
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setVolume(currentVolume);
                    mediaPlayer.play();
                    
                    if (!shuffle) { // Don't add shuffled song to recentSongs
                        addToRecentSongs(selectedSong);
                    }
                    
                    // Repeat current song if repeat field is true
                    mediaPlayer.setOnEndOfMedia(() -> {
                       if (repeat) {
                           playSong();
                       } 
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            System.out.println("No song selected or invalid index");
        }
    }
    
    public void addToRecentSongs(Song song) {
        if (recentSongs.contains(song)) {
            recentSongs.remove(song);
        }
        recentSongs.addFirst(song);
        
        // Only keep the most recent 10 songs
        if (recentSongs.size() > 10) {
            recentSongs.removeLast();
        }
    }
    
    public List<Song> getRecentSongs() {
        return new ArrayList<>(recentSongs);
    }

    public void playSongFromFile(File mp3File) {
        Platform.runLater(() -> {
            try {
                // Stop current media player if it's already playing
                if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    mediaPlayer.stop();
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
    
    public boolean isShuffle() {
        return shuffle;
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
        selectedIndex++;
        
        if (selectedIndex >= songs.size()) { // Loop back to first song if end of list
            selectedIndex = 0;
        }
        
        // Select next song
        selectedSong = songs.get(selectedIndex);
        setSelectedSong(selectedSong, selectedIndex);
        if (mediaPlayer != null) {
            if (isPlaying()) { // Stop playing current song if needed
                stopPlaying(); 
            }
        }
        
        playSong();
    }

    public void previousSong() {
        selectedIndex--;
        
        if (selectedIndex < 0) { // Loop back to last song if beginning of list
            selectedIndex = songs.size() - 1;
        }
        
        // Select previous song
        selectedSong = songs.get(selectedIndex);
        setSelectedSong(selectedSong, selectedIndex);
        
        if (mediaPlayer != null) {
            if (isPlaying()) {
                stopPlaying();
            }
        }
        
        playSong();
    }
    
    public int getVolume() {
        if (mediaPlayer != null) {
            return (int) (currentVolume * 100);
        }
        return 0; // Default value level if mediaPlayer is null
    }

    public void setVolume(int volume) {
        if (mediaPlayer != null) {
            currentVolume = volume / 100.0;
            mediaPlayer.setVolume(currentVolume);
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
    
    public int getElapsedTime() {
        if (mediaPlayer != null) {
            Duration currentTime = mediaPlayer.getCurrentTime();
            return (int) currentTime.toSeconds();
        }
        return 0;
    }
    
    public int getSongLength() {
        if (mediaPlayer != null) {
            Duration totalDuration = mediaPlayer.getTotalDuration();
            return (int) totalDuration.toSeconds();
        }
        return 0;
    }
    
    public void toggleShuffle() {
        shuffle = !shuffle;
    }
    
    public void toggleRepeat() {
        repeat = !repeat;
    }
}
