package photos05.android.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user in the photo album application.
 * Each user has a username, a list of albums, and methods to manage those albums.
 * 
 * @author Kaileb Cole
 * @author Maxime Deperrois
 */
/**
 * Represents a user with a username and a list of albums.
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private List<Album> albums;

    /**
     * Constructs a new User with the specified username.
     *
     * @param username the username of the user
     */
    public User(String username) {
        this.username = username;
        this.albums = new ArrayList<>();
    }

    /**
     * Returns the username of the user.
     *
     * @return the username of the user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user.
     *
     * @param username the new username of the user
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the list of albums of the user.
     *
     * @return the list of albums of the user
     */
    public List<Album> getAlbums() {
        return albums;
    }

    /**
     * Adds an album to the user's list of albums.
     *
     * @param album the album to be added
     */
    public void addAlbum(Album album) {
        album.setUser(this);
        albums.add(album);
    }

    /**
     * Removes an album from the user's list of albums.
     *
     * @param album the album to be removed
     */
    public void removeAlbum(Album album) {
        albums.remove(album);
    }

    public Album getAlbumByName(String name) {
        for (Album album : albums) {
            if (album.getName().equalsIgnoreCase(name)) {
                return album;
            }
        }
        return null;
    }
}