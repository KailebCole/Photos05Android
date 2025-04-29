package photos05.android.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import photos05.android.R;
import photos05.android.model.Album;
import photos05.android.model.Photo;
import photos05.android.model.User;
import photos05.android.util.DataManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity{
    private GridView gridView;
    private ArrayAdapter<String> adapter;
    private List<String> photoPaths = new ArrayList<>();
    private Album currentAlbum;
    private User user;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        // Set up photo album grid
        gridView = findViewById(R.id.photoGridView);

        // Set up add photo button
        Button addPhotoButton = findViewById(R.id.addPhotoButton);
        addPhotoButton.setOnClickListener(v -> openImagePicker());

        // Get User
        user = DataManager.loadUser(this);

        // Get the album information from the previous intent
        String albumName = getIntent().getStringExtra("albumName");
        currentAlbum = user.getAlbumByName(albumName);

        // Get the user information from the file
        user = DataManager.loadUser(this);

        if(currentAlbum != null){
            for(Photo photo : currentAlbum.getPhotos()){
                photoPaths.add(photo.getFilePath());
            }
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, photoPaths);
        gridView.setAdapter(adapter);

        // Set up item click listener to open photo
        gridView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            String path = photoPaths.get(position);
            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putExtra("photoPath", path);
            intent.putExtra("albumName", currentAlbum.getName());
            startActivity(intent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            addPhotoToAlbum(imageUri.toString());
        }
    }

    private void addPhotoToAlbum(String path) {
        // Prevent duplicate photos
        for (Photo p : currentAlbum.getPhotos()) {
            if (p.getFilePath().equals(path)) {
                Toast.makeText(this, "Photo already exists in album", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            Photo newPhoto = new Photo(path);
            currentAlbum.addPhoto(newPhoto);

            photoPaths.add(path);
            adapter.notifyDataSetChanged();
            DataManager.saveUser(user, this);

            Toast.makeText(this, "Photo added successfully!", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to add photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

}
