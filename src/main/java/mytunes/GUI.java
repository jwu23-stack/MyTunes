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
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class GUI extends JFrame {
    JPanel panel, buttonPanel, sidePanel;
    JButton play, stop, pause, unpause, next, previous;
    JMenuBar menubar;
    JTable songTable;
    JScrollPane songTableScrollPane;
    DefaultTableModel tableModel;
    MusicPlayer musicPlayer;
    JPopupMenu popupMenu;

    // Initialize components
    public GUI() {
        panel = new JPanel();
        menubar = new JMenuBar();
        musicPlayer = new MusicPlayer();
        popupMenu = new JPopupMenu();
        
        // Initialize sidePanel
        sidePanel = new JPanel();
        // Set width of sidePanel to 150 (1/8 of 1200)
        sidePanel.setPreferredSize(new Dimension(150, 600));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Initialize table
        String[] columnNames = {"Title", "Artist", "Album", "Year", "Genre", "Comment"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // Allow editing only for the Comment column
                return column == 5;
            }
        };
        songTable = new JTable(tableModel);
        songTable.setFillsViewportHeight(true);
        songTableScrollPane = new JScrollPane(songTable);
        
        // Set sidePanel to the left of songTable
        this.setLayout(new BorderLayout());
        this.add(sidePanel, BorderLayout.WEST);
        this.add(songTableScrollPane, BorderLayout.CENTER);

        buttonPanel = new JPanel();
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
        this.add(panel);

        // Add Menu Component
        buildMenu();

        // Add SongTable Component
        buildSongLibrary();

        // Add Button Panel
        buildButtonPanel();

        // Add Popup Component
        buildPopup();
        
        // Add SidePanel Component
        buildSidePanel();

        this.setVisible(true);
    }

    private void buildMenu() {
        JMenu fileMenu = new JMenu("File");
        JMenuItem openAndPlay = new JMenuItem("Open Song");
        JMenuItem addSong = new JMenuItem("Add Song");
        JMenuItem deleteSong = new JMenuItem("Delete Song");
        JMenuItem exit = new JMenuItem("Exit");

        // Event Handlers
        openAndPlay.addActionListener(new ActionListener() {
           @Override
           public void actionPerformed(ActionEvent e) {
               handleOpenAndPlaySong();
           }
        });
        
        addSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAddSong();
            }
        });

        deleteSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDeleteSong();
            }
        });

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

    private void buildSongLibrary() {
        // Adjust header render
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBackground(Color.LIGHT_GRAY);
        headerRenderer.setForeground(Color.BLACK);
        songTable.getTableHeader().setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        songTable.getTableHeader().setDefaultRenderer(headerRenderer);

        // Load song data from database
        setSongs();

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
        DropTarget dropTarget = new DropTarget(songTable, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = e.getTransferable();
                    List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    // Add mp3 files to database and refresh table
                    for (File file : droppedFiles) {
                        if (file.isFile() && file.getName().toLowerCase().endsWith(".mp3")) {
                            boolean status = musicPlayer.addSong(file);
                            if (status) {
                                // Refresh JTable
                                setSongs();
                            } else {
                                JOptionPane.showMessageDialog(null, "Failed to add the song '" + file.getName() + "' to the database.", "Error", JOptionPane.ERROR_MESSAGE);
                                
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        songTable.setGridColor(Color.BLACK);
        this.add(songTableScrollPane, BorderLayout.CENTER);
    }

    private void buildButtonPanel() {
        // Event handlers
        previous.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                musicPlayer.previousSong();
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
                if (musicPlayer.getSelectedSong() != null) {
                    musicPlayer.pausePlaying();
                } else {
                    JOptionPane.showMessageDialog(null, "No song selected.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        unpause.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (musicPlayer.getSelectedSong() != null) {
                    musicPlayer.resumePlaying();
                } else {
                    JOptionPane.showMessageDialog(null, "No song selected.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        next.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                musicPlayer.nextSong();
            }
        });

        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.setBackground(Color.LIGHT_GRAY);
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(stop);
        buttonPanel.add(pause);
        buttonPanel.add(unpause);
        buttonPanel.add(next);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void buildPopup() {
        JMenuItem addSong = new JMenuItem("Add song to Library");
        JMenuItem deleteSong = new JMenuItem("Delete currently selected song");

        // Event handlers
        addSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleAddSong();
            }
        });

        deleteSong.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleDeleteSong();
            }
        });

        popupMenu.add(addSong);
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
        DefaultMutableTreeNode playlistRoot = new DefaultMutableTreeNode("Playlist");
        populatePlaylistTree(playlistRoot);
        DefaultTreeModel playlistTreeModel = new DefaultTreeModel(playlistRoot);
        JTree playlistTree = new JTree(playlistTreeModel);
        playlistTree.setShowsRootHandles(false);
        playlistTree.setEditable(false);
        
        sidePanel.add(libraryTree, BorderLayout.WEST);
        sidePanel.add(playlistTree, BorderLayout.WEST);
        sidePanel.setBackground(Color.WHITE);
        this.add(sidePanel, BorderLayout.WEST);
    }

    private void setSongs() {
        // Clear out existing rows
        tableModel.setRowCount(0);

        // Load songs from database
        List<Song> songs = musicPlayer.getAllSongs();
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
    
    private void handleOpenAndPlaySong() {
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

    private void handleAddSong() {
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
                setSongs();
            } else {
                JOptionPane.showMessageDialog(null, "Failed to add the song to the database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteSong() {
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
    
    private void populatePlaylistTree(DefaultMutableTreeNode playlistRoot) {
        // Sample playlists (change later)
        String[] playlists = {"Rock", "Classical", "Jazz"};
        
        for (String playlist: playlists) {
            PlaylistNode playlistNode = new PlaylistNode(playlist);
            playlistRoot.add(playlistNode);
        }
    }
    
    class PlaylistNode extends DefaultMutableTreeNode {
        public PlaylistNode(Object userObject) {
            super(userObject);
        }
    }
}
