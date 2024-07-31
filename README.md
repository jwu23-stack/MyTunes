## MyTunes Application
### Overview
MyTunes is a comprehensive music management application designed to organize, play, and manage your music collection. It supports various functionalites such as playing songs, managing playlists, and interacting with an AWS SQL database for storage. This document outlines the basic features and requirements of the MyTunes application.

### Features
#### GUI Components 
- Buttons: Located at the bottom of the GUI, these buttons allow users to control playback, including play, stop, pause/unpause, skip to the next/previous song.
- Song Library Panel: Displays a list of all songs in the library, showing details such as title, artist, album, year, and genre. Users can edit comments for each song
- Drag and Drop Support: Users can drag and drop songs from their files onto the table to add them to the library.
- Popup Menu: Accessible by right-clicking anywhere in the Library area, this menu allows users to add or delete songs.
- File Menu Bar: Includes options for opening and playing a song not in the library, exiting the application, adding a song, and deleting a song.

#### Database
MyTunes utilizes an SQL database that is hosted on AWS RDS service. All songs added through the GUI **must exist** in the user's **Downloads folder**.

### Requirements
- JDK 22 or higher
- Java IDE (e.g. Netbeans, Eclipse, etc.)
