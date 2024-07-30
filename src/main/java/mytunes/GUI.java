package mytunes;

/**
 *
 * @author Jerry
 */
import java.util.List;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

public class GUI extends JFrame {

    JPanel panel, buttonPanel;
    JButton play, stop, pause, unpause, next, previous;
    JMenuBar menubar;
    JTable songTable;
    JScrollPane songTableScrollPane;
    DefaultTableModel tableModel;

    // Initialize components
    public GUI() {
        panel = new JPanel();
        menubar = new JMenuBar();

        // Initialize table
        String[] columnNames = {"Title", "Artist", "Album", "Year", "Genre", "Comment"};
        tableModel = new DefaultTableModel(columnNames, 0);
        songTable = new JTable(tableModel);
        songTable.setFillsViewportHeight(true);
        songTableScrollPane = new JScrollPane(songTable);

        buttonPanel = new JPanel();
        play = new JButton("Play");
        stop = new JButton("Stop");
        pause = new JButton("Pause");
        unpause = new JButton("Unpause");
        next = new JButton("Next");
        previous = new JButton("Previous");
    }

    public void go() {
        this.setTitle("MyTunes");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 700);
        this.add(panel);

        // Add Menu Component
        buildMenu();

        // Add SongTable Component
        buildSongLibrary();

        // Add Button Panel
        buildButtonPanel();

        this.setVisible(true);
    }

    public void buildMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem openAndPlay = new JMenuItem("Open Song");
        JMenuItem addSong = new JMenuItem("Add Song");
        JMenuItem deleteSong = new JMenuItem("Delete Song");
        JMenuItem exit = new JMenuItem("Exit");

        // Implement logic for other buttons later
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        fileMenu.add(openAndPlay);
        fileMenu.add(addSong);
        fileMenu.add(deleteSong);
        fileMenu.addSeparator();
        fileMenu.add(exit);
        menubar.add(fileMenu);
        this.setJMenuBar(menubar);
    }

    public void buildSongLibrary() {
        // Adjust header render
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBackground(Color.LIGHT_GRAY);
        headerRenderer.setForeground(Color.BLACK);
        songTable.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        songTable.getTableHeader().setDefaultRenderer(headerRenderer);

        // Load data from database
        DatabaseManager dbManager = new DatabaseManager();
        List<Song> songs = dbManager.getAllSongs();

        for (Song song : songs) {
            tableModel.addRow(new Object[]{
                song.getTitle(),
                song.getArtist(),
                song.getAlbum(),
                song.getYear(),
                song.getGenre(),
                song.getComment()
            });
        }

        songTable.setGridColor(Color.BLACK);
        this.add(songTableScrollPane, BorderLayout.CENTER);
    }

    public void buildButtonPanel() {
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(stop);
        buttonPanel.add(pause);
        buttonPanel.add(unpause);
        buttonPanel.add(next);

        this.add(buttonPanel, BorderLayout.SOUTH);
    }

}
