package photos05.android.model;

import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a photo in the photo album application.
 * Each photo has a caption, a date, and a list of tags.
 * 
 * @author Kaileb Cole
 * @author Maxime Deperrois
 */
public class Photo implements Serializable{
    private static final long serialVersionUID = 1L;

    private String filePath;
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

        if (filePath.startsWith("file://")) {
            File file = new File(Uri.parse(filePath).getPath());
            if (!file.exists()) {
                throw new IOException("File does not exist: " + filePath);
            }
        }
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
