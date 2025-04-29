package photos05.android.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import photos05.android.R;
import photos05.android.model.Album;
import photos05.android.model.Photo;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        imageView = findViewById(R.id.fullImageView);
        captionText = findViewById(R.id.captionTextView);
        tagListText = findViewById(R.id.tagListTextView);
        nextButton = findViewById(R.id.nextButton);
        prevButton = findViewById(R.id.prevButton);

        user = DataManager.loadUser(this);
        String albumName = getIntent().getStringExtra("albumName");
        String photoPath = getIntent().getStringExtra("photoPath");

        currentAlbum = user.getAlbumByName(albumName);
        photos = currentAlbum.getPhotos();

        // find current photo index
        for (int i = 0; i < photos.size(); i++) {
            if (photos.get(i).getFilePath().equals(photoPath)) {
                currentIndex = i;
                break;
            }
        }

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
    }

    private void updatePhotoView() {
        Photo currentPhoto = photos.get(currentIndex);
        imageView.setImageURI(Uri.parse(currentPhoto.getFilePath()));
        captionText.setText(currentPhoto.getCaption());
        //tagListText.setText(currentPhoto.getTagString());
    }
}
