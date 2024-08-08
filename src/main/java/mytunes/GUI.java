package mytunes;

/**
 *
 * @author Jerry
 */
import java.util.List;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.io.File;
import javafx.application.Platform;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.*;

public class GUI extends JFrame {
    JPanel mainPanel, mainButtonPanel, sidePanel;
    JButton play, stop, pause, unpause, next, previous;
    JMenuBar mainMenubar;
    JTable mainSongTable;
    JScrollPane mainSongTableScrollPane;
    DefaultTableModel mainTableModel;
    MusicPlayer musicPlayer;
    JPopupMenu popupMenu, playlistPopupMenu;
    DefaultMutableTreeNode playlistRoot;
    JTree playlistTree;
    String playlistName;

    // Initialize components
    public GUI() {
        mainPanel = new JPanel();
        mainMenubar = new JMenuBar();
        musicPlayer = new MusicPlayer();
        popupMenu = new JPopupMenu();
        playlistPopupMenu = new JPopupMenu();
        playlistName = "";

        // Initialize sidePanel
        sidePanel = new JPanel();
        // Set width of sidePanel to 150 (1/8 of 1200)
        sidePanel.setPreferredSize(new Dimension(150, 600));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialize table
        String[] columnNames = {"Title", "Artist", "Album", "Year", "Genre", "Comment"};
        mainTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing only for the Comment column
                return column == 5;
            }
        };
        mainSongTable = new JTable(mainTableModel);
        mainSongTable.setFillsViewportHeight(true);
        mainSongTableScrollPane = new JScrollPane(mainSongTable);

        // Set sidePanel to the left of songTable
        this.setLayout(new BorderLayout());
        this.add(sidePanel, BorderLayout.WEST);
        this.add(mainSongTableScrollPane, BorderLayout.CENTER);

        mainButtonPanel = new JPanel();
        play = new JButton("Play");
        stop = new JButton("Stop");
        pause = new JButton("Pause");
        unpause = new JButton("Unpause");
        next = new JButton("Next");
        previous = new JButton("Previous");
    }

    public void go() {
        // Initialize JavaFX runtime
        Platform.startup(() -> {
        });

        this.setTitle("MyTunes");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(1200, 700);
        this.add(mainPanel);

        // Add Menu Component for main window
        buildMenu(mainMenubar, this, mainTableModel, mainSongTable, musicPlayer, playlistName);

        // Add SongTable Component for main window
        buildSongLibrary(mainSongTable, mainSongTableScrollPane, this, mainTableModel, musicPlayer, playlistName);

        // Add Button Panel for main window
        buildButtonPanel(mainSongTable, mainButtonPanel, this, play, stop, pause, unpause, next, previous, musicPlayer);

        // Add Popup Component for main window
        buildPopup(mainSongTable, mainTableModel, musicPlayer, "");

        // Add SidePanel Component for main window
        buildSidePanel();

        this.setVisible(true);
    }

    private void buildMenu(JMenuBar menubar, JFrame frame, DefaultTableModel tableModel, JTable songTable, MusicPlayer musicPlayer, String playlistName) {
        JMenu fileMenu = new JMenu("File");
        JMenuItem createPlaylist = new JMenuItem("Create Playlist");
        JMenuItem openAndPlay = new JMenuItem("Open Song");
        JMenuItem addSong = new JMenuItem("Add Song");
        JMenuItem deleteSong = new JMenuItem("Delete Song");
        JMenuItem exit = new JMenuItem("Exit");

        // Event Handlers
        createPlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String playlistName = JOptionPane.showInputDialog(null, "Enter Playlist Name:", "Create Playlist", JOptionPane.PLAIN_MESSAGE);
                if (playlistName != null && !playlistName.trim().isEmpty()) {
                    boolean success = musicPlayer.createPlaylist(playlistName);
                    if (success) {
                        // Add the new playlist to the tree
                        PlaylistNode newPlaylist = new PlaylistNode(playlistName);
                        playlistRoot.add(newPlaylist);

                        // Refresh the tree
                        ((DefaultTreeModel) playlistTree.getModel()).reload();

                        // Select the newly added playlist node
                        TreePath newPath = new TreePath(newPlaylist.getPath());
                        playlistTree.setSelectionPath(newPath);
                        playlistTree.scrollPathToVisible(newPath);

                        // Fetch and display the songs of new playlist in songTable
                        setSongs(tableModel, musicPlayer, playlistName);
                    } else {
                        JOptionPane.showMessageDialog(null, "Failed to create playlist.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        openAndPlay.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleOpenAndPlaySong(musicPlayer);
            }
        });

        addSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAddSong(tableModel, musicPlayer, playlistName);
            }
        });

        deleteSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDeleteSong(songTable, tableModel, musicPlayer);
            }
        });

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        fileMenu.add(createPlaylist);
        fileMenu.add(openAndPlay);
        fileMenu.add(addSong);
        fileMenu.add(deleteSong);
        fileMenu.addSeparator();
        fileMenu.add(exit);
        menubar.add(fileMenu);
        frame.setJMenuBar(menubar);
    }

    private void buildSongLibrary(JTable songTable, JScrollPane songTableScrollPane, JFrame frame, DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        // Adjust header render
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBackground(Color.LIGHT_GRAY);
        headerRenderer.setForeground(Color.BLACK);
        songTable.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        songTable.getTableHeader().setDefaultRenderer(headerRenderer);

        // Load song data from database
        setSongs(tableModel, musicPlayer, playlistName);

        // Event Handlers
        songTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        songTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = songTable.getSelectedRow();
                    if (selectedRow != -1) {
                        String title = (String) tableModel.getValueAt(selectedRow, 0);
                        Song selectedSong = musicPlayer.findSongByTitle(title);
                        if (selectedSong != null) {
                            musicPlayer.setSelectedSong(selectedSong, selectedRow);
                        }
                    }
                }
            }
        });

        // Setup DropTarget for songTable (drag and drop)
        setUpDropTarget(songTable, tableModel, musicPlayer, playlistName);
        
        songTable.setGridColor(Color.BLACK);
        frame.add(songTableScrollPane, BorderLayout.CENTER);
    }

    private void buildButtonPanel(JTable songTable, JPanel panel, JFrame frame, JButton play, JButton stop, JButton pause, JButton unpause, JButton next, JButton previous, MusicPlayer musicPlayer) {
        // Event handlers
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int currentRow = songTable.getSelectedRow();
                // Calculate the previous row index, wrap around if necessary
                int newRow = (currentRow - 1 + songTable.getRowCount()) % songTable.getRowCount();
                musicPlayer.previousSong();
                songTable.setRowSelectionInterval(newRow, newRow);
                songTable.scrollRectToVisible(songTable.getCellRect(newRow, 0, true));
            }
        });

        play.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (musicPlayer.getSelectedSong() != null) {
                    musicPlayer.playSong();
                } else {
                    JOptionPane.showMessageDialog(null, "No song selected.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                musicPlayer.stopPlaying();
            }
        });

        pause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                musicPlayer.pausePlaying();
            }
        });

        unpause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                musicPlayer.resumePlaying();
            }
        });

        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int currentRow = songTable.getSelectedRow();
                // Calculate the next row index, wrap around if necessary
                int newRow = (currentRow + 1) % songTable.getRowCount();
                musicPlayer.nextSong();
                songTable.setRowSelectionInterval(newRow, newRow);
                songTable.scrollRectToVisible(songTable.getCellRect(newRow, 0, true));
            }
        });

        panel.setLayout(new FlowLayout());
        panel.setBackground(Color.LIGHT_GRAY);
        panel.add(previous);
        panel.add(play);
        panel.add(stop);
        panel.add(pause);
        panel.add(unpause);
        panel.add(next);
        frame.add(panel, BorderLayout.SOUTH);
    }

    private void buildPopup(JTable songTable, DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        JMenuItem addSong = new JMenuItem("Add song to Library");
        JMenu addToPlaylistMenu = new JMenu("Add to Playlist");
        JMenuItem deleteSong = new JMenuItem("Delete currently selected song");

        List<String> playlists = musicPlayer.getPlaylists();

        // Populate the "Add to Playlist" submenu with playlist names
        for (String playlist : playlists) {
            JMenuItem playlistItem = new JMenuItem(playlist);
            playlistItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int selectedRowIndex = songTable.getSelectedRow();
                    if (selectedRowIndex >= 0) {
                        String songTitle = (String) songTable.getValueAt(selectedRowIndex, 0);

                        boolean status = musicPlayer.addToPlaylist(songTitle, playlist);
                        if (status) {
                            JOptionPane.showMessageDialog(null, "Song '" + songTitle + "' has been successfully added to the playlist " + playlist, "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(null, "Failed to add song '" + songTitle + "' to the playlist.", "Error", JOptionPane.ERROR_MESSAGE);
                        }

                    } else {
                        JOptionPane.showMessageDialog(null, "Please select a song to add to the playlist.", "Selection Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            addToPlaylistMenu.add(playlistItem);
        }

        // Event handlers
        addSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Add frame parameter
                handleAddSong(tableModel, musicPlayer, playlistName);
            }
        });

        deleteSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Add frame parameter
                handleDeleteSong(songTable, tableModel, musicPlayer);
            }
        });

        popupMenu.add(addSong);
        popupMenu.add(addToPlaylistMenu);
        popupMenu.addSeparator();
        popupMenu.add(deleteSong);
    }

    private void buildSidePanel() {
        sidePanel.setLayout(new FlowLayout(FlowLayout.LEFT));

        // Build library tree
        DefaultMutableTreeNode libraryRoot = new DefaultMutableTreeNode("Library");
        DefaultTreeModel libraryTreeModel = new DefaultTreeModel(libraryRoot);
        JTree libraryTree = new JTree(libraryTreeModel);
        libraryTree.setShowsRootHandles(false);
        libraryTree.setEditable(false);

        // Build playlist tree
        playlistRoot = new DefaultMutableTreeNode("Playlist");
        populatePlaylistTree(playlistRoot);
        DefaultTreeModel playlistTreeModel = new DefaultTreeModel(playlistRoot);
        playlistTree = new JTree(playlistTreeModel);
        playlistTree.setShowsRootHandles(false);
        playlistTree.setEditable(false);

        // Event listeners
        libraryTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) libraryTree.getLastSelectedPathComponent();

                playlistTree.clearSelection();
                if (node != null && node.getUserObject().equals("Library")) {
                    // Display all songs
                    playlistName = "";
                    // Reset the DropTarget for songTable (drag and drop) 
                    setUpDropTarget(mainSongTable, mainTableModel, musicPlayer, playlistName);
                    
                    setSongs(mainTableModel, musicPlayer, playlistName);
                }
            }
        });

        playlistTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) playlistTree.getLastSelectedPathComponent();

                libraryTree.clearSelection();
                if (node != null && !node.getUserObject().equals("Playlist")) { // Ensure we're not clicking on the root "Playlist" node itself
                    playlistName = node.getUserObject().toString();
                    
                    // Reset the DropTarget for songTable (drag and drop) 
                    setUpDropTarget(mainSongTable, mainTableModel, musicPlayer, playlistName);
                    
                    // Display playlist's songs
                    setSongs(mainTableModel, musicPlayer, playlistName);
                }
            }
        });

        playlistTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) playlistTree.getLastSelectedPathComponent();
                    if (node != null && !node.getUserObject().equals("Playlist")) {
                        String playlistName = node.getUserObject().toString();
                        displayPlaylistPopup(e, playlistName);
                    }

                }
            }
        });

        sidePanel.add(libraryTree, BorderLayout.WEST);
        sidePanel.add(playlistTree, BorderLayout.WEST);
        sidePanel.setBackground(Color.WHITE);
        this.add(sidePanel, BorderLayout.WEST);
    }

    private void setSongs(DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        // Clear out existing rows
        tableModel.setRowCount(0);

        // Load songs from database
        List<Song> songs;
        if (!playlistName.isEmpty()) {
            songs = musicPlayer.getPlaylistSongs(playlistName);
        } else {
            songs = musicPlayer.getAllSongs();
        }
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
    }

    private void handleOpenAndPlaySong(MusicPlayer musicPlayer) {
        JFileChooser fileSelection = new JFileChooser();
        fileSelection.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileSelection.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 files", "mp3"));
        int selection = fileSelection.showOpenDialog(null);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileSelection.getSelectedFile();
            if (selectedFile == null || !selectedFile.exists() || !selectedFile.getName().endsWith(".mp3")) {
                JOptionPane.showMessageDialog(null, "Selected file is not a valid MP3 file.", "Error", JOptionPane.ERROR_MESSAGE);
            }

            // Play song
            musicPlayer.playSongFromFile(selectedFile);
        } else {
            JOptionPane.showMessageDialog(null, "Failed to play the song from file.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleAddSong(DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        JFileChooser fileSelection = new JFileChooser();
        fileSelection.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileSelection.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("MP3 files", "mp3"));
        int selection = fileSelection.showOpenDialog(null);
        if (selection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileSelection.getSelectedFile();
            if (selectedFile == null || !selectedFile.exists() || !selectedFile.getName().endsWith(".mp3")) {
                JOptionPane.showMessageDialog(null, "Selected file is not a valid MP3 file.", "Error", JOptionPane.ERROR_MESSAGE);
            }
            boolean status = musicPlayer.addSong(selectedFile);

            // Check if song is successfully added to database
            if (status) {
                // Refresh JTable
                setSongs(tableModel, musicPlayer, playlistName);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to add the song to the database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteSong(JTable songTable, DefaultTableModel tableModel, MusicPlayer musicPlayer) {
        int selectedRow = songTable.getSelectedRow();
        if (selectedRow != -1) {
            String title = (String) tableModel.getValueAt(selectedRow, 0);
            boolean status = musicPlayer.deleteSong(title);

            // Check if song is successfully deleted
            if (status) {
                tableModel.removeRow(selectedRow);
            } else {
                JOptionPane.showMessageDialog(null, "Failed to delete the song in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(null, "Please select a song to delete");
        }
    }

    private void displayPlaylistPopup(MouseEvent e, String playlistName) {
        // Clear existing menu items to avoid duplicates
        playlistPopupMenu.removeAll();

        JMenuItem openInNewWindowItem = new JMenuItem("Open in New Window");

        // Event listener
        openInNewWindowItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (playlistName != null) {
                    openPlaylistInNewWindow(playlistName);
                } else {
                    System.err.println("Playlist not found.");
                }
            }
        });

        playlistPopupMenu.add(openInNewWindowItem);
        playlistPopupMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void openPlaylistInNewWindow(String playlistName) {
        JFrame playlistWindow = new JFrame(playlistName);
        playlistWindow.setSize(1200, 700);
        playlistWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Create a JPanel for the new window's content
        JPanel panelForPlaylist = new JPanel();
        playlistWindow.add(panelForPlaylist);

        // Initialize components for the new window
        JPanel buttonPanelForPlaylist = new JPanel();
        DefaultTableModel tableModelForPlaylist = new DefaultTableModel(new String[]{"Title", "Artist", "Album", "Year", "Genre", "Comment"}, 0);
        JTable songTableForPlaylist = new JTable(tableModelForPlaylist);
        songTableForPlaylist.setFillsViewportHeight(true);
        JScrollPane songTableScrollPaneForPlaylist = new JScrollPane(songTableForPlaylist);
        JMenuBar menuBarForPlaylist = new JMenuBar();
        JButton playForPlaylist = new JButton("Play");
        JButton stopForPlaylist = new JButton("Stop");
        JButton pauseForPlaylist = new JButton("Pause");
        JButton unpauseForPlaylist = new JButton("Unpause");
        JButton nextForPlaylist = new JButton("Next");
        JButton previousForPlaylist = new JButton("Previous");
        MusicPlayer playlistPlayer = new MusicPlayer();

        // Add components to the panel for the playlist window
        panelForPlaylist.setLayout(new BorderLayout());
        buildMenu(menuBarForPlaylist, playlistWindow, tableModelForPlaylist, songTableForPlaylist, playlistPlayer, playlistName);
        buildSongLibrary(songTableForPlaylist, songTableScrollPaneForPlaylist, playlistWindow, tableModelForPlaylist, playlistPlayer, playlistName);
        buildButtonPanel(songTableForPlaylist, buttonPanelForPlaylist, playlistWindow, playForPlaylist, stopForPlaylist, pauseForPlaylist, unpauseForPlaylist, nextForPlaylist, previousForPlaylist, playlistPlayer);

        playlistWindow.setVisible(true);
        
        // Setup DropTarget for songTable (drag and drop)
        setUpDropTarget(songTableForPlaylist, tableModelForPlaylist, playlistPlayer, playlistName);

        // Refresh main library songs
        setSongs(mainTableModel, musicPlayer, "");
    }

    private void populatePlaylistTree(DefaultMutableTreeNode playlistRoot) {
        // Retrieve playlist names from database
        List<String> playlists = musicPlayer.getPlaylists();

        for (String playlist : playlists) {
            PlaylistNode playlistNode = new PlaylistNode(playlist);
            playlistRoot.add(playlistNode);
        }
    }

    private void setUpDropTarget(JComponent component, DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        new DropTarget(component, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = e.getTransferable();
                    List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                    List<Song> songs = musicPlayer.getAllSongs();
                    
                    // Add mp3 files to database and refresh table
                    for (File file : droppedFiles) {
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                            Song newSong = Song.extractMetaData(file);
                            
                            // Check if song exists in library
                            boolean songExists = songs.stream().anyMatch(song -> newSong.getTitle().equals(song.getTitle()));

                            // Add song to library if it doesn't exist
                            if (!songExists) {
                                boolean addedToLibrary = musicPlayer.addSong(file);
                                if (addedToLibrary) {
                                    setSongs(mainTableModel, musicPlayer, "");  // Refresh main library table
                                } else {
                                    JOptionPane.showMessageDialog(null, "Failed to add the song '" + file.getName() + "' to the library.", "Error", JOptionPane.ERROR_MESSAGE);
                                    continue;
                                }
                            }

                            // If a playlist name is provided, add the song to the playlist
                            if (!playlistName.isEmpty()) {
                                boolean addedToPlaylist = musicPlayer.addToPlaylist(newSong.getTitle(), playlistName);
                                if (!addedToPlaylist) {
                                    JOptionPane.showMessageDialog(null, "Failed to add the song '" + file.getName() + "' to the playlist.", "Error", JOptionPane.ERROR_MESSAGE);
                                } else {
                                    setSongs(tableModel, musicPlayer, playlistName);  // Refresh playlist table
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    class PlaylistNode extends DefaultMutableTreeNode {

        public PlaylistNode(Object userObject) {
            super(userObject);
        }
    }
}
