package photos05.android.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import photos05.android.R;
import photos05.android.model.Album;
import photos05.android.model.Photo;
import photos05.android.model.Tag;
import photos05.android.model.User;
import photos05.android.util.DataManager;

public class PhotoViewerActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView captionText;
    private LinearLayout tagLayout;
    private Button nextButton, prevButton;

    private List<Photo> photos;
    private int currentIndex;
    private User user;
    private Album currentAlbum;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        imageView = findViewById(R.id.fullImageView);
        captionText = findViewById(R.id.captionTextView);
        tagLayout = findViewById(R.id.tagLayout);
        nextButton = findViewById(R.id.nextButton);
        prevButton = findViewById(R.id.prevButton);

        String albumName = getIntent().getStringExtra("albumName");
        int index = getIntent().getIntExtra("photoIndex", 0);

        user = DataManager.loadUser(this);
        currentAlbum = user.getAlbumByName(albumName);
        if (currentAlbum == null || currentAlbum.getPhotos().isEmpty()) {
            Toast.makeText(this, "No photos in this album", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        photos = currentAlbum.getPhotos();
        currentIndex = Math.max(0, Math.min(index, photos.size() - 1));

        updatePhotoView();

        nextButton.setOnClickListener(v -> {
            if (currentIndex < photos.size() - 1) {
                currentIndex++;
                updatePhotoView();
            }
        });

        prevButton.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updatePhotoView();
            }
        });

        Button deletePhotoButton = findViewById(R.id.deletePhotoButton);
        deletePhotoButton.setOnClickListener(v -> {
            if (!photos.isEmpty()) {
                Photo toDelete = photos.get(currentIndex);
                photos.remove(currentIndex);
                currentAlbum.getPhotos().remove(toDelete);
                DataManager.saveUser(user, this);
                Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();

                if (photos.isEmpty()) {
                    finish(); // Exit viewer if no more photos
                } else {
                    currentIndex = Math.min(currentIndex, photos.size() - 1);
                    updatePhotoView();
                }
            }
        });

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        showPreviousPhoto();
                    } else {
                        showNextPhoto();
                    }
                    return true;
                }
                return false;
            }
        });

        imageView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        imageView.setOnLongClickListener(v -> {
            showPhotoOptionsDialog(photos.get(currentIndex));
            return true;
        });

    }

    // Method to update the photo view with the current photo
    private void updatePhotoView() {
        Photo currentPhoto = photos.get(currentIndex);
        Uri photoUri = Uri.parse(currentPhoto.getFilePath());

        imageView.setImageURI(photoUri);

        String fileName = photoUri.getLastPathSegment();
        captionText.setText(fileName != null ? fileName : "Unknown");

        refreshTagDisplay(currentPhoto);
    }

    private void refreshTagDisplay(Photo photo) {
        LinearLayout tagLayout = findViewById(R.id.tagLayout);
        tagLayout.removeAllViews();

        for (Tag tag : photo.getTags()) {
            Button tagButton = new Button(this);
            tagButton.setText(tag.toString() + " ❌");
            tagButton.setTextSize(12);
            tagButton.setOnClickListener(v -> {
                photo.removeTag(tag);
                DataManager.saveUser(user, this);
                refreshTagDisplay(photo);
            });
            tagLayout.addView(tagButton);
        }
    }

    private void addTagDialog(Photo photo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Tag");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        Spinner tagTypeSpinner = new Spinner(this);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Person", "Location"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner.setAdapter(spinnerAdapter);
        layout.addView(tagTypeSpinner);

        EditText tagValueInput = new EditText(this);
        tagValueInput.setHint("Enter tag value");
        layout.addView(tagValueInput);

        builder.setView(layout);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String tagType = tagTypeSpinner.getSelectedItem().toString();
            String tagValue = tagValueInput.getText().toString().trim();

            if (!tagValue.isEmpty()) {
                Tag newTag = new Tag(tagType, tagValue);
                if (!photo.getTags().contains(newTag)) {
                    photo.addTag(newTag);
                    DataManager.saveUser(user, this);
                    refreshTagDisplay(photo);
                    Toast.makeText(this, "Tag added!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Duplicate tag", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }



    // Method to show options for the selected photo
    private void showPhotoOptionsDialog(Photo selectedPhoto) {
        String[] options = {"Add Tag", "Copy", "Move", "Delete"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: addTagDialog(selectedPhoto); break;
                        case 1: showAlbumPicker("Copy", selectedPhoto); break;
                        case 2: showAlbumPicker("Move", selectedPhoto); break;
                        case 3:
                            photos.remove(currentIndex);
                            currentAlbum.removePhoto(selectedPhoto);
                            DataManager.saveUser(user, this);
                            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();

                            if (photos.isEmpty()) {
                                finish();
                            } else {
                                currentIndex = Math.min(currentIndex, photos.size() - 1);
                                updatePhotoView();
                            }
                            break;
                    }
                }).show();
    }

    // Method to show album picker for copying or moving photos
    private void showAlbumPicker(String action, Photo photo) {
        List<String> albumNames = new ArrayList<>();
        for (Album album : user.getAlbums()) {
            if (!album.getName().equals(currentAlbum.getName())) {
                albumNames.add(album.getName());
            }
        }

        String[] namesArray = albumNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle(action + " to which album?")
                .setItems(namesArray, (dialog, which) -> {
                    String selectedAlbumName = namesArray[which];
                    Album targetAlbum = user.getAlbumByName(selectedAlbumName);
                    if (targetAlbum == null) return;

                    try {
                        Photo copy = new Photo(photo.getFilePath());
                        if (action.equals("Copy")) {
                            targetAlbum.addPhoto(copy);
                        } else if (action.equals("Move")) {
                            targetAlbum.addPhoto(copy);
                            photos.remove(currentIndex);
                            currentAlbum.removePhoto(photo);
                            if (photos.isEmpty()) {
                                DataManager.saveUser(user, this);
                                finish();
                                return;
                            } else {
                                currentIndex = Math.min(currentIndex, photos.size() - 1);
                                updatePhotoView();
                            }
                        }

                        DataManager.saveUser(user, this);
                        Toast.makeText(this, action + " successful", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to " + action + " photo", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Method to handle swipe gestures
    private void showNextPhoto() {
        if (currentIndex < photos.size() - 1) {
            currentIndex++;
            updatePhotoView();
        }
    }

    // Method to handle swipe gestures
    private void showPreviousPhoto() {
        if (currentIndex > 0) {
            currentIndex--;
            updatePhotoView();
        }
    }
}
