package photos05.android.model;

import androidx.annotation.NonNull;

import java.io.Serializable;

/**
 * Represents a tag in the photo album application.
 * Each tag has a name and a value.
 * 
 * @author Kaileb Cole
 * @author Maxime Deperrois
 */
public class Tag implements Serializable {
    private String name;
    private String value;

    /**
     * Creates a new tag with the given name and value.
     * 
     * @param name the name of the tag
     * @param value the value of the tag
     */
    public Tag(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the name of the tag.
     * 
     * @return the name of the tag
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value of the tag.
     * 
     * @return the value of the tag
     */
    public String getValue() {
        return value;
    }

    /**
     * Checks if this tag is equal to another object.
     * Two tags are considered equal if both their name and value are the same.
     * 
     * @param obj the object to compare
     * @return true if the tags are equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return name.equals(tag.name) && value.equals(tag.value);
    }

    /**
     * Returns the hash code of the tag.
     * 
     * @return the hash code of the tag
     */
    @Override
    public int hashCode() {
        return name.hashCode() + value.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return name + " :" + value;
    }
}
