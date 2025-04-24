package photos05.android.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an album in the photo album application.
 * Each album has a name, a list of photos, and methods to manage those photos.
 * 
 * @author Kaileb Cole
 * @author Maxime Deperrois
 */
public class Album implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private List<Photo> photos;
    private User user;

    /**
     * Creates a new album with the given name.
     * 
     * @param name the name of the album
     */
    public Album(String name) {
        this.name = name;
        this.photos = new ArrayList<>();
    }

    /**
     * Returns the name of the album.
     * 
     * @return the name of the album
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the album.
     * 
     * @param name the new name of the album
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the list of photos in the album.
     * 
     * @return the list of photos
     */
    public List<Photo> getPhotos() {
        return photos;
    }

    /**
     * Adds a photo to the album.
     * 
     * @param photo the photo to add
     */
    public void addPhoto(Photo photo) {
        if(!photos.contains(photo)) {
            photos.add(photo);
        }
    }

    /**
     * Removes a photo from the album.
     * 
     * @param photo the photo to remove
     */
    public void removePhoto(Photo photo) {
        photos.remove(photo);
    }

    /**
     * Returns the user associated with the album.
     * 
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user associated with the album.
     * 
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }
}