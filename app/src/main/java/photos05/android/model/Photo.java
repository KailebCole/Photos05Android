package photos05.android.model;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Represents a photo in the photo album application.
 * Each photo has a caption, a date, and a list of tags.
 * 
 * @author Kaileb Cole
 * @author Maxime Deperrois
 */
public class Photo implements Serializable{
    private static final long serialVersionUID = 1L;

    private String caption;
    private String filePath;
    private LocalDateTime lastDateModified;
    private List<Tag> tags;

    /**
     * Creates a new photo at the given file path.
     * 
     * @param filePath the file path of the photo
     * @throws IOException if the file path is invalid
     */
    public Photo(String filePath) throws IOException {
        this.filePath = filePath;
        this.tags = new ArrayList<>();
        this.lastDateModified = getLastModifiedDate(filePath);
    }

    /**
     * Returns the last modified date of the photo.
     * 
     * @param filePath the file path of the photo
     * @return the last modified date of the photo
     * @throws IOException if the file path is invalid
     */
    private LocalDateTime getLastModifiedDate(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        FileTime fileTime = Files.getLastModifiedTime(path);
        return fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Returns the last modified date of the photo.
     * 
     * @return the last modified date of the photo
     */
    public LocalDateTime getLastModifiedDate() {
        return lastDateModified;
    }

    /**
     * Returns the caption of the photo.
     * 
     * @return the caption of the photo
     */
    public String getCaption() {
        return caption;
    }

    /**
     * Sets the caption of the photo.
     * 
     * @param caption the new caption of the photo
     */
    public void setCaption(String caption) {
        this.caption = caption;
    }

    /**
     * Returns the file path of the photo.
     * 
     * @return the file path of the photo
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Returns the list of tags of the photo.
     * 
     * @return the list of tags of the photo
     */
    public List<Tag> getTags() {
        return tags;
    }

    /**
     * Adds a tag to the list of tags of the photo.
     * 
     * @param tag the tag to add
     */
    public void addTag(Tag tag) {
        tags.add(tag);
    }

    /**
     * Removes a tag from the list of tags of the photo.
     * 
     * @param tag the tag to remove
     */
    public void removeTag(Tag tag) {
        tags.remove(tag);
    }
}
