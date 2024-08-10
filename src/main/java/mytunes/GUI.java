package mytunes;

/**
 *
 * @author Jerry
 */
import java.util.*;
import java.util.List;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.table.*;
import java.io.File;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.tree.*;

public class GUI extends JFrame {
    JPanel mainPanel, mainTimerPanel, mainButtonPanel, sidePanel;
    JButton play, stop, pause, unpause, next, previous;
    JSlider volumeSlider;
    JMenuBar mainMenubar;
    JTable mainSongTable;
    JScrollPane mainSongTableScrollPane;
    DefaultTableModel mainTableModel;
    JLabel mainVolumeLabel, mainElapsedTimeLabel, mainRemainingTimeLabel;
    MusicPlayer musicPlayer;
    JPopupMenu popupMenu, playlistPopupMenu;
    JCheckBoxMenuItem artistItem, albumItem, yearItem, genreItem, commentItem;
    JProgressBar mainProgressBar;
    DefaultMutableTreeNode playlistRoot;
    DefaultTreeModel libraryTreeModel;
    JTree playlistTree;
    String playlistName;
    Map<Integer, TableColumn> hiddenColumns;
    Map<String, DefaultTableModel> playlistTableModels;

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
        volumeSlider = new JSlider(0, 100, 20);
        mainVolumeLabel = new JLabel("Volume");

        artistItem = new JCheckBoxMenuItem("Artist", true);
        albumItem = new JCheckBoxMenuItem("Album", true);
        yearItem = new JCheckBoxMenuItem("Year", true);
        genreItem = new JCheckBoxMenuItem("Genre", true);
        commentItem = new JCheckBoxMenuItem("Comment", true);
        hiddenColumns = new HashMap<>();
        
        mainTimerPanel = new JPanel(new FlowLayout());
        mainProgressBar = new JProgressBar(0, 100);
        mainElapsedTimeLabel = new JLabel("0:00:00");
        mainRemainingTimeLabel = new JLabel("0:00:00");

        playlistTableModels = new HashMap<>();
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

        // Enable drag and drop between main window and playlist window
        mainSongTable.setDragEnabled(true);
        mainSongTable.setTransferHandler(new SongTransferHandler());

        // Add Button Panel for main window
        buildButtonPanel(mainSongTable, mainButtonPanel, mainTimerPanel, this, play, stop, pause, unpause, next, previous, musicPlayer, mainVolumeLabel, volumeSlider, mainProgressBar, mainElapsedTimeLabel, mainRemainingTimeLabel);

        // Add Popup Component for main window
        buildPopup(mainSongTable, mainTableModel, musicPlayer, "");

        // Add SidePanel Component for main window
        buildSidePanel();

        // Add Column Header Popup
        JCheckBoxMenuItem[] menuItems = {artistItem, albumItem, yearItem, genreItem, commentItem};
        buildTableHeaderPopup(mainSongTable, mainTableModel, menuItems);

        // Load Column configuration
        loadColumnConfiguration(menuItems, mainSongTable, mainTableModel);

        // Shutdown hook to save column configuration
        Runtime.getRuntime().addShutdownHook(new Thread(() -> saveColumnConfiguration(menuItems)));

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
                handleDeleteSong(songTable, tableModel, musicPlayer, playlistName);
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
        
        // Enable column sorting on all columns
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        songTable.setRowSorter(sorter);

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
        
        songTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int column = songTable.columnAtPoint(e.getPoint());
                    if (column != -1) {
                        List<? extends RowSorter.SortKey> sortKeys = sorter.getSortKeys();
                        
                        if (!sortKeys.isEmpty() && sortKeys.get(0).getColumn() == column) {
                            sorter.toggleSortOrder(column);
                        } else {
                            // Sort this column in ascending order
                            sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(column, SortOrder.ASCENDING)));
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

    private void buildButtonPanel(JTable songTable, JPanel panel, JPanel timerPanel, JFrame frame, JButton play, JButton stop, JButton pause, JButton unpause, JButton next, JButton previous, MusicPlayer musicPlayer, JLabel volumeLabel, JSlider volumeSlider, JProgressBar progressBar, JLabel elapsedTime, JLabel remainingTime) {
        // Hide volume slider and progress bar before song selection
        volumeLabel.setVisible(false);
        volumeSlider.setVisible(false);
        timerPanel.setVisible(false);

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
                    updateProgress(musicPlayer, elapsedTime, remainingTime, progressBar);
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

        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int volume = volumeSlider.getValue();
                musicPlayer.setVolume(volume);
            }
        });
        
        songTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = songTable.getSelectedRow();
                    if (selectedRow != -1) {
                        volumeLabel.setVisible(true);
                        volumeSlider.setVisible(true);
                        timerPanel.setVisible(true);
                    } else {
                        volumeLabel.setVisible(false);
                        volumeSlider.setVisible(false);
                        timerPanel.setVisible(false);
                    }
                }
            }
        });

        panel.setLayout(new BorderLayout());
        panel.setBackground(Color.LIGHT_GRAY);
        
        // Add song progress bar and timers into panel
        progressBar.setPreferredSize(new Dimension(600, 20));
        timerPanel.add(elapsedTime);
        timerPanel.add(progressBar);
        timerPanel.add(remainingTime);
        panel.add(timerPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        volumeSlider.setMajorTickSpacing(10);
        volumeSlider.setMinorTickSpacing(1);
        buttonPanel.add(previous);
        buttonPanel.add(play);
        buttonPanel.add(stop);
        buttonPanel.add(pause);
        buttonPanel.add(unpause);
        buttonPanel.add(next);
        buttonPanel.add(volumeLabel);
        buttonPanel.add(volumeSlider);
        panel.add(buttonPanel, BorderLayout.CENTER);
        
        frame.add(panel, BorderLayout.SOUTH);
    }

    private void buildPopup(JTable songTable, DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        // Clear existing items
        popupMenu.removeAll();
        
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
                            refreshPlaylistTable(playlist);
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
                handleDeleteSong(songTable, tableModel, musicPlayer, playlistName);
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
        libraryTreeModel = new DefaultTreeModel(libraryRoot);
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

    private void buildTableHeaderPopup(JTable songTable, DefaultTableModel tableModel, JCheckBoxMenuItem[] menuItems) {
        JPopupMenu headerPopup = new JPopupMenu();

        // Add event handlers to each menuItem
        for (JCheckBoxMenuItem menuItem : menuItems) {
            // Add items to popup menu
            headerPopup.add(menuItem);

            // Add Event handler
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toggleColumnVisibility(songTable, tableModel, menuItem, menuItem.getText());
                }
            });

        }

        songTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    headerPopup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void toggleColumnVisibility(JTable table, DefaultTableModel tableModel, JCheckBoxMenuItem menuItem, String columnName) {
        TableColumnModel columnModel = table.getColumnModel();
        int modelIndex = findColumnIndex(tableModel, columnName);

        if (modelIndex > 0) { // Exclude "Title" column (index 0) from being toggled
            if (menuItem.isSelected()) {
                int viewIndex = table.convertColumnIndexToView(modelIndex);

                if (viewIndex == -1) {
                    // Column is hidden, so we need to make it visible again
                    TableColumn column = getHiddenColumn(modelIndex);
                    if (column != null) {
                        int newPosition = findOriginalColumnPosition(columnModel, modelIndex);
                        addColumnAtPosition(columnModel, column, newPosition);
                    }
                }
            } else {
                // Hide the column
                int viewIndex = table.convertColumnIndexToView(modelIndex);
                if (viewIndex != -1) {
                    TableColumn column = columnModel.getColumn(viewIndex);
                    columnModel.removeColumn(column);
                    addHiddenColumn(column);
                }
            }
        }
    }

    private void addColumnAtPosition(TableColumnModel columnModel, TableColumn newColumn, int position) {
        if (position < 0 || position > columnModel.getColumnCount()) {
            throw new IllegalArgumentException("Position out of range");
        }

        // Temporary list to hold columns
        List<TableColumn> columns = new ArrayList<>();

        // Remove columns and store them in the list
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columns.add(columnModel.getColumn(i));
        }

        // Clear the existing columns from the model
        for (TableColumn column : columns) {
            columnModel.removeColumn(column);
        }

        // Add columns back in the desired order
        for (int i = 0; i < columns.size(); i++) {
            if (i == position) {
                columnModel.addColumn(newColumn);
            }
            columnModel.addColumn(columns.get(i));
        }

        // If the position is at the end, add the new column
        if (position >= columns.size()) {
            columnModel.addColumn(newColumn);
        }
    }

    private void addHiddenColumn(TableColumn column) {
        hiddenColumns.put(column.getModelIndex(), column);
    }

    private TableColumn getHiddenColumn(int modelIndex) {
        return hiddenColumns.remove(modelIndex);
    }

    private int findColumnIndex(DefaultTableModel tableModel, String columnName) {
        for (int i = 0; i < tableModel.getColumnCount(); i++) {
            if (tableModel.getColumnName(i).equals(columnName)) {
                return i;
            }
        }
        return -1; // Column not found
    }

    private int findOriginalColumnPosition(TableColumnModel columnModel, int modelIndex) {
        for (int i = 1; i < columnModel.getColumnCount(); i++) { // Start from 1 to skip "Title"
            TableColumn column = columnModel.getColumn(i);
            if (column.getModelIndex() > modelIndex) {
                return i;
            }
        }

        // If no column has a higher model index, place it at the end
        return columnModel.getColumnCount();
    }

    private void saveColumnConfiguration(JCheckBoxMenuItem[] menuItems) {
        Preferences prefs = Preferences.userNodeForPackage(GUI.class);

        for (JCheckBoxMenuItem item : menuItems) {
            prefs.putBoolean(item.getText(), item.isSelected());
        }
    }

    private void loadColumnConfiguration(JCheckBoxMenuItem[] menuItems, JTable table, DefaultTableModel tableModel) {
        Preferences prefs = Preferences.userNodeForPackage(GUI.class);

        for (JCheckBoxMenuItem item : menuItems) {
            boolean isSelected = prefs.getBoolean(item.getText(), true);
            item.setSelected(isSelected);
            toggleColumnVisibility(table, tableModel, item, item.getText());
        }
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

    private void handleDeleteSong(JTable songTable, DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        int selectedRow = songTable.getSelectedRow();
        if (selectedRow != -1) {
            String title = (String) tableModel.getValueAt(selectedRow, 0);
            boolean status = musicPlayer.deleteSong(title);

            // Check if song is successfully deleted
            if (status) {
                tableModel.removeRow(selectedRow);

                // Refresh table
                setSongs(tableModel, musicPlayer, playlistName);
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
        JMenuItem deletePlaylist = new JMenuItem("Delete Playlist");

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

        deletePlaylist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int playlistId = musicPlayer.findPlaylist(playlistName);

                if (playlistId > 0) {
                    int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this playlist?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        try {
                            boolean status = musicPlayer.deletePlaylist(playlistId);
                            if (status) {
                                // Find the node in the JTree that corresponds to the deleted playlist
                                DefaultTreeModel libraryTreeModel = (DefaultTreeModel) playlistTree.getModel();
                                DefaultMutableTreeNode root = (DefaultMutableTreeNode) libraryTreeModel.getRoot();

                                // Traverse the tree to find the node to delete
                                DefaultMutableTreeNode playlistNodeToRemove = findAndSetNode(root, playlistName);

                                // If the node was found, remove it from the tree
                                if (playlistNodeToRemove != null) {
                                    libraryTreeModel.removeNodeFromParent(playlistNodeToRemove);
                                    libraryTreeModel.reload();
                                }

                                // Refresh main library table
                                setSongs(mainTableModel, musicPlayer, "");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a playlist to delete");
                }
            }
        });

        playlistPopupMenu.add(openInNewWindowItem);
        playlistPopupMenu.add(deletePlaylist);
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
        playlistTableModels.put(playlistName, tableModelForPlaylist);
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
        JSlider playlistSlider = new JSlider(0, 100, 20);
        JLabel playlistVolumeLabel = new JLabel("Volume");
        JCheckBoxMenuItem playlistArtistItem = new JCheckBoxMenuItem("Artist", true);
        JCheckBoxMenuItem playlistAlbumItem = new JCheckBoxMenuItem("Album", true);
        JCheckBoxMenuItem playlistYearItem = new JCheckBoxMenuItem("Year", true);
        JCheckBoxMenuItem playlistGenreItem = new JCheckBoxMenuItem("Genre", true);
        JCheckBoxMenuItem playlistCommentItem = new JCheckBoxMenuItem("Comment", true);
        JPanel playlistTimerPanel = new JPanel(new FlowLayout());
        JProgressBar playlistProgressBar = new JProgressBar(0, 100);
        JLabel playlistElapsedTimeLabel = new JLabel("0:00:00");
        JLabel playlistRemainingTimeLabel = new JLabel("0:00:00");
        

        MusicPlayer playlistPlayer = new MusicPlayer(); 

        // Add components to the panel for the playlist window
        panelForPlaylist.setLayout(new BorderLayout());
        buildMenu(menuBarForPlaylist, playlistWindow, tableModelForPlaylist, songTableForPlaylist, playlistPlayer, playlistName);
        buildSongLibrary(songTableForPlaylist, songTableScrollPaneForPlaylist, playlistWindow, tableModelForPlaylist, playlistPlayer, playlistName);
        buildButtonPanel(songTableForPlaylist, buttonPanelForPlaylist, playlistTimerPanel, playlistWindow, playForPlaylist, stopForPlaylist, pauseForPlaylist, unpauseForPlaylist, nextForPlaylist, previousForPlaylist, playlistPlayer, playlistVolumeLabel, playlistSlider, playlistProgressBar, playlistElapsedTimeLabel, playlistRemainingTimeLabel);

        // Add Column Header Popup
        JCheckBoxMenuItem[] menuItems = {playlistArtistItem, playlistAlbumItem, playlistYearItem, playlistGenreItem, playlistCommentItem};
        buildTableHeaderPopup(songTableForPlaylist, tableModelForPlaylist, menuItems);
        loadColumnConfiguration(menuItems, songTableForPlaylist, tableModelForPlaylist);

        playlistWindow.setVisible(true);

        // Setup DropTarget for songTable (drag and drop)
        if (playlistWindow.isVisible()) {
            setUpDropTarget(songTableForPlaylist, tableModelForPlaylist, playlistPlayer, playlistName);
        }
        
        // Event Handler
        playlistWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                playlistPlayer.stopPlaying();
            }
        });

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

    private DefaultMutableTreeNode findAndSetNode(DefaultMutableTreeNode node, String playlistName) {
        if (node.getUserObject().equals(playlistName)) {
            return node;
        } else {
            Enumeration children = node.children();
            while (children.hasMoreElements()) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) children.nextElement();
                DefaultMutableTreeNode resultNode = findAndSetNode(childNode, playlistName);
                if (resultNode != null) {
                    return resultNode;
                }
            }
        }
        return null;
    }

    private void refreshPlaylistTable(String playlistName) {
        DefaultTableModel model = playlistTableModels.get(playlistName);
        if (model != null) {
            model.setRowCount(0);
            List<Song> songs = musicPlayer.getPlaylistSongs(playlistName);
            for (Song song : songs) {
                model.addRow(new Object[]{
                    song.getTitle(),
                    song.getArtist(),
                    song.getAlbum(),
                    song.getYear(),
                    song.getGenre(),
                    song.getComment()
                });
            }
        }
    }

    private void setUpDropTarget(JComponent component, DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        new DropTarget(component, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = e.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) { // Handle file drop
                        List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        handleFileDrop(droppedFiles, tableModel, musicPlayer, playlistName);
                    } else { // Handle row drop from mainSongTable to playlist window
                        String data = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                        String[] songTitles = data.split("\n");

                        for (String songTitle : songTitles) {
                            boolean addedToPlaylist = musicPlayer.addToPlaylist(songTitle, playlistName);
                            if (!addedToPlaylist) {
                                JOptionPane.showMessageDialog(null, "Failed to add the song " + songTitle + " to the playlist.", "Error", JOptionPane.ERROR_MESSAGE);
                            }
                           
                        }
                        setSongs(tableModel, musicPlayer, playlistName);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void handleFileDrop(List<File> droppedFiles, DefaultTableModel tableModel, MusicPlayer musicPlayer, String playlistName) {
        List<Song> songs = musicPlayer.getAllSongs();

        boolean songsAdded = false; // Flag to check if any songs were added

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
                        songsAdded = true;
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
                // If any songs were added to the library, refresh the main library table
                if (songsAdded) {
                    setSongs(mainTableModel, musicPlayer, "");  // Refresh main library table
                }
            }
        }
    }
    
    private void updateProgress(MusicPlayer musicPlayer, JLabel elapsedTimeLabel, JLabel remaingTimeLabel, JProgressBar progressBar) {
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get current elapsed time
                int elapsedTime = musicPlayer.getElapsedTime();
                int songLength = musicPlayer.getSongLength();
                
                // Update elapsed time label
                elapsedTimeLabel.setText(formatTime(elapsedTime));
                
                // Update remaining time label
                remaingTimeLabel.setText(formatTime(songLength - elapsedTime));
                
                // Update progress bar
                int progress = (int) ((double) elapsedTime / songLength * 100);
                progressBar.setValue(progress);
                
                if (elapsedTime >= songLength) {
                    ((Timer) e.getSource()).stop();
                    progressBar.setValue(100);
                    progressBar.setValue(0);
                    elapsedTimeLabel.setText("0:00:00");
                }
            }
        });
        timer.start();
    }
    
    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }

    class PlaylistNode extends DefaultMutableTreeNode {

        public PlaylistNode(Object userObject) {
            super(userObject);
        }
    }

    // Override TransferHandler for JTable
    class SongTransferHandler extends TransferHandler {

        @Override
        protected Transferable createTransferable(JComponent c) {
            JTable table = (JTable) c;
            int[] selectedRows = table.getSelectedRows();
            List<String> songTitles = new ArrayList<>();
            for (int row : selectedRows) {
                String songTitle = (String) table.getValueAt(row, 0);
                songTitles.add(songTitle);
            }
            return new StringSelection(String.join("\n", songTitles));
        }

        @Override
        public int getSourceActions(JComponent c) {
            return COPY;
        }
    }
}
