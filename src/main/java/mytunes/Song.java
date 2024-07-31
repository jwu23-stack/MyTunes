package mytunes;

/**
 *
 * @author Jerry
 */

import java.io.File;
import java.io.IOException;
import com.mpatric.mp3agic.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;

public class Song {
    private String title;
    private String artist;
    private String album;
    private String year;
    private String genre;
    private String comment;

    public Song(String title, String artist, String album, String year, String genre, String comment) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.year = year;
        this.genre = genre;
        this.comment = comment;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getYear() {
        return year;
    }

    public String getGenre() {
        return genre;
    }

    public String getComment() {
        return comment;
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public static Song extractMetaData(File mp3File) {
        try {
            Mp3File mp3 = new Mp3File(mp3File);
            if (mp3.hasId3v1Tag()) { // ID3V1 tag
                ID3v1 tag = mp3.getId3v1Tag();
                
                return new Song(
                        tag.getTitle(),
                        tag.getArtist(),
                        tag.getAlbum(),
                        tag.getYear(),
                        tag.getGenreDescription(),
                        tag.getComment()
                );
            } else if (mp3.hasId3v2Tag()) { // ID3V2 tag
                ID3v2 tag = mp3.getId3v2Tag();
                
                return new Song(
                        tag.getTitle(),
                        tag.getArtist(),
                        tag.getAlbum(),
                        tag.getYear(),
                        tag.getGenreDescription(),
                        tag.getComment()
                );
            } else { // no ID3 tag
                return new Song (mp3File.getName(), "", "", "", "", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public String getFileURL() throws IOException, URISyntaxException {
        // Construct path from Downloads directory and append MP3 file to path
        Path path = Paths.get(System.getProperty("user.home"), "Downloads");
        Path mp3FilePath = path.resolve(this.title + ".mp3");
        
        // Convert file path to URI
        URI uri = mp3FilePath.toUri();
        
        return uri.toString();
    }
}
