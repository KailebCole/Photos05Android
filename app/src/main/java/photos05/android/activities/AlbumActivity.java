package photos05.android.activities;
import photos05.android.model.Photo;
import photos05.android.model.Tag;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;

import photos05.android.R;
import photos05.android.model.Album;
import photos05.android.model.User;
import photos05.android.util.DataManager;

import java.io.IOException;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity{
    private static final String TAG = "AlbumActivity";
    private GridView gridView;
    private ArrayAdapter<String> adapter;
    private List<String> photoPaths = new ArrayList<>();
    private ActivityResultLauncher<Intent> selectPhotoLauncher;
    private Album currentAlbum;
    private User user;

    private int screenWidth;
    private int squareImageSideLength;
    private int padding = 8;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        // Image Sizes based on Screen Size
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        squareImageSideLength = ((screenWidth - padding) / 3);

        // Add photo button
        Button addPhotoButton = findViewById(R.id.addPhotoButton);
        if (addPhotoButton == null) {
            Log.e(TAG, "onCreate: Button not found!");
            return;
        }
        // Set a click listener on the button to open the image picker
        addPhotoButton.setOnClickListener(v -> addPhoto());

        // Get User
        user = DataManager.loadUser(this);

        Log.d(TAG, "onCreate: Add Photo Button created");

        // Set up grid view
        gridView = findViewById(R.id.photoGridView);
        if (gridView == null) {
            Log.e(TAG, "onCreate: GridView not found!");
            return;
        }
        adapter = new ArrayAdapter<String>(this, 0, photoPaths) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ImageView imageView;
                if (convertView == null) {
                    imageView = new ImageView(getContext());
                    imageView.setLayoutParams(new GridView.LayoutParams(squareImageSideLength, squareImageSideLength));
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setPadding(padding, padding, padding, padding);
                } else {
                    imageView = (ImageView) convertView;
                }

                Uri uri = Uri.parse(getItem(position));
                try (InputStream stream = getContentResolver().openInputStream(uri)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(stream);
                    imageView.setImageBitmap(bitmap);
                } catch (IOException | SecurityException e) {
                    Log.e(TAG, "Failed to load image for URI: " + uri, e);
                    imageView.setImageResource(android.R.drawable.ic_menu_report_image); // fallback image
                }

                return imageView;
            }
        };

        gridView.setAdapter(adapter);

        // Get the album name from intent and load album
        String albumName = getIntent().getStringExtra("albumName");
        if (albumName != null) {
            currentAlbum = user.getAlbumByName(albumName);
            if (currentAlbum != null) {
                for (Photo p : currentAlbum.getPhotos()) {
                    photoPaths.add(p.getFilePath());
                }
            }
        }

        // Set listener on each image to allow the user to open an image
        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putExtra("albumName", currentAlbum.getName());
            intent.putExtra("photoIndex", position);
            startActivity(intent);
        });

        // Long click image
        gridView.setOnItemLongClickListener((parent, view, position, id) -> {
            showPhotoOptionsDialog(position);
            return true;
        });

        // Implement Save/Load Capability
        selectPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Uri uri = data.getData();
                        if (uri != null) {
                            // Persist access
                            final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                            try {
                                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            }
                            catch (SecurityException e) {
                                Log.e(TAG, "URI permission error", e);
                            }

                            String path = uri.toString();

                            // Prevent duplicates
                            if (currentAlbum.getPhotos().stream().anyMatch(p -> p.getFilePath().equals(path))) {
                                Toast.makeText(this, "This photo already exists in the album!", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            try {
                                Photo photo = new Photo(path);
                                currentAlbum.addPhoto(photo);
                                photoPaths.add(path);
                                adapter.notifyDataSetChanged();

                                // Persist user data
                                DataManager.saveUser(user, this);
                                Toast.makeText(this, "Photo added and saved!", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                Log.e(TAG, "Photo creation failed for path: " + path, e);
                                Toast.makeText(this, "Failed to add photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }
        );
    }

    private void addPhoto() {
        Log.d(TAG, "addPhoto: Prompting user to select a photo");
        // Create an intent to open the SAF file picker
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        selectPhotoLauncher.launch(intent);
    }


    // Function to provide user options on long hold of what to do to a photo
    private void showPhotoOptionsDialog(int index) {
        String[] options = { "Add a Tag", "Move", "Copy", "Delete" };

        new AlertDialog.Builder(this)
                .setTitle(photoPaths.get(index))
                .setItems(options, (dialog, which) -> {
                    Photo selectedPhoto = null;
                    for (Photo photo : currentAlbum.getPhotos()) {
                        if (photo.getFilePath().equals(photoPaths.get(index))) {
                            selectedPhoto = photo;
                            break;
                        }
                    }

                    if (selectedPhoto == null) return;

                    switch (which) {
                        case 0: // Add a Tag
                            AddTagToPhoto(selectedPhoto);
                            break;

                        case 1: // Move
                            movePhotoToAlbum(selectedPhoto);
                            break;

                        case 2: // Copy
                            copyPhotoToAlbum(selectedPhoto);
                            break;

                        case 3: // Delete
                            currentAlbum.removePhoto(selectedPhoto);
                            photoPaths.remove(index);
                            adapter.notifyDataSetChanged();
                            DataManager.saveUser(user, this);
                            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    // Function to tag a photo of specific tag types
    public void AddTagToPhoto(Photo photo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Tag");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Dropdown for tag type
        final Spinner tagTypeSpinner = new Spinner(this);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Person", "Location"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner.setAdapter(spinnerAdapter);
        layout.addView(tagTypeSpinner);

        // Input for tag value
        final EditText tagValueInput = new EditText(this);
        tagValueInput.setHint("Tag value");
        layout.addView(tagValueInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String tagName = tagTypeSpinner.getSelectedItem().toString();
            String tagValue = tagValueInput.getText().toString().trim();

            if (!tagValue.isEmpty()) {
                Tag myTag = new Tag(tagName, tagValue);
                boolean tagExists = photo.getTags().stream().anyMatch(t -> t.equals(myTag));

                if (tagExists) {
                    Toast.makeText(this, "Tag already exists: " + myTag, Toast.LENGTH_SHORT).show();
                } else {
                    photo.addTag(myTag);
                    Toast.makeText(this, "Tag added: " + myTag, Toast.LENGTH_SHORT).show();
                    DataManager.saveUser(user, this);
                }
            } else {
                Toast.makeText(this, "Tag value is required", Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Function to move a photo from one album to another
    private void movePhotoToAlbum(Photo photo) {
        List<String> albumNames = new ArrayList<>();
        for (Album album : user.getAlbums()) {
            if (!album.getName().equals(currentAlbum.getName())) {
                albumNames.add(album.getName());
            }
        }

        String[] namesArray = albumNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Move to which album?")
                .setItems(namesArray, (dialog, which) -> {
                    String selectedAlbumName = namesArray[which];
                    Album targetAlbum = user.getAlbumByName(selectedAlbumName);
                    if (targetAlbum == null) return;

                    try {
                        Photo movedPhoto = new Photo(photo.getFilePath());
                        targetAlbum.addPhoto(movedPhoto);
                        currentAlbum.removePhoto(photo);
                        photoPaths.remove(photo.getFilePath());
                        adapter.notifyDataSetChanged();
                        DataManager.saveUser(user, this);
                        Toast.makeText(this, "Photo moved successfully", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to move photo", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Function to copy a photo from one album to another
    private void copyPhotoToAlbum(Photo photo) {
        List<String> albumNames = new ArrayList<>();
        for (Album album : user.getAlbums()) {
            if (!album.getName().equals(currentAlbum.getName())) {
                albumNames.add(album.getName());
            }
        }

        String[] namesArray = albumNames.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Copy to which album?")
                .setItems(namesArray, (dialog, which) -> {
                    String selectedAlbumName = namesArray[which];
                    Album targetAlbum = user.getAlbumByName(selectedAlbumName);
                    if (targetAlbum == null) return;

                    try {
                        Photo copiedPhoto = new Photo(photo.getFilePath());
                        targetAlbum.addPhoto(copiedPhoto);
                        DataManager.saveUser(user, this);
                        Toast.makeText(this, "Photo copied successfully", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to copy photo", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    protected void onResume() {
        super.onResume();

        user = DataManager.loadUser(this);
        String albumName = getIntent().getStringExtra("albumName");
        currentAlbum = user.getAlbumByName(albumName);

        photoPaths.clear();
        if (currentAlbum != null) {
            for (Photo p : currentAlbum.getPhotos()) {
                photoPaths.add(p.getFilePath());
            }
        }

        adapter.notifyDataSetChanged();
    }
}
