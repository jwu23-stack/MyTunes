package mytunes;

/**
 *
 * @author Jerry
 */
public class MyTunes {

    public static void main(String[] args) {
        MusicPlayer musicPlayer = new MusicPlayer();
        GUI gui = new GUI(musicPlayer);
        musicPlayer.setGUI(gui);
        gui.go();
    }
}
