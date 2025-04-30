package photos05.android.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
    private TextView tagListText;
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
        tagListText = findViewById(R.id.tagListTextView);
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
            new AlertDialog.Builder(this)
                    .setTitle("Delete Photo")
                    .setMessage("Are you sure you want to delete this photo?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (!photos.isEmpty()) {
                            Photo toDelete = photos.get(currentIndex);
                            photos.remove(currentIndex);
                            currentAlbum.getPhotos().remove(toDelete);
                            DataManager.saveUser(user, this);
                            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();

                            if (photos.isEmpty()) {
                                finish();
                            } else {
                                currentIndex = Math.min(currentIndex, photos.size() - 1);
                                updatePhotoView();
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void updatePhotoView() {
        Photo currentPhoto = photos.get(currentIndex);
        Uri photoUri = Uri.parse(currentPhoto.getFilePath());

        // Display the image
        imageView.setImageURI(photoUri);

        // Extract file name
        String fileName = photoUri.getLastPathSegment();
        captionText.setText(fileName != null ? fileName : "Unknown");

        // Display tags
        List<Tag> tags = currentPhoto.getTags();
        if (tags.isEmpty()) {
            tagListText.setText("Tags: None");
        } else {
            StringBuilder tagDisplay = new StringBuilder("Tags:");
            for (Tag tag : tags) {
                tagDisplay.append("\n• ").append(tag.getName()).append(": ").append(tag.getValue());
            }
            tagListText.setText(tagDisplay.toString());
        }
    }

    private void showNextPhoto() {
        if (currentIndex < photos.size() - 1) {
            currentIndex++;
            updatePhotoView();
        }
    }

    private void showPreviousPhoto() {
        if (currentIndex > 0) {
            currentIndex--;
            updatePhotoView();
        }
    }
}
