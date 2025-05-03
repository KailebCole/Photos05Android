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
import android.widget.AutoCompleteTextView;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        // Get User
        user = DataManager.loadUser(this);

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

        Button searchButton = findViewById(R.id.searchButton);
        if (searchButton == null) {
            Log.e(TAG, "onCreate: Button not found!");
            return;
        }
        // Set a click listener on the button to open the image picker
        searchButton.setOnClickListener(v -> {
            showSearchDialog();
        });

        Spinner tagTypeSpinner = findViewById(R.id.tagTypeSpinner);
        AutoCompleteTextView tagValueInput = findViewById(R.id.searchTagValueInput);

        // Setup spinner values
        ArrayAdapter<String> tagTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"Person", "Location"});
        tagTypeSpinner.setAdapter(tagTypeAdapter);

        // Gather unique tag values from all albums
        Set<String> allTagValues = new HashSet<>();
        for (Album album : user.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    allTagValues.add(tag.getValue());
                }
            }
        }

        // Setup AutoComplete suggestions
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(allTagValues));
        tagValueInput.setAdapter(autoCompleteAdapter);

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

    private void showSearchDialog() {
        final String[] modes = { "Single Tag", "Tag OR Tag", "Tag AND Tag" };

        AlertDialog.Builder modeBuilder = new AlertDialog.Builder(this);
        modeBuilder.setTitle("Select Search Mode");
        modeBuilder.setItems(modes, (dialog, which) -> {
            String selectedMode = modes[which];
            promptForTags(selectedMode);
        });
        modeBuilder.show();
    }

    private void promptForTags(String mode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search by Tag");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Dropdown for tag types
        Spinner tagTypeSpinner1 = new Spinner(this);
        ArrayAdapter<String> tagTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Location", "Person"});
        tagTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagTypeSpinner1.setAdapter(tagTypeAdapter);
        layout.addView(tagTypeSpinner1);

        // Autocomplete for tag values
        final AutoCompleteTextView tagValueInput1 = new AutoCompleteTextView(this);
        tagValueInput1.setHint("Tag value");
        ArrayAdapter<String> autoCompleteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(getAllTagValues()));
        tagValueInput1.setAdapter(autoCompleteAdapter);
        tagValueInput1.setThreshold(1);
        layout.addView(tagValueInput1);

        final Spinner tagTypeSpinner2;
        final AutoCompleteTextView tagValueInput2;

        if (!mode.equals("Single Tag")) {
            tagTypeSpinner2 = new Spinner(this);
            tagTypeSpinner2.setAdapter(tagTypeAdapter);
            layout.addView(tagTypeSpinner2);

            tagValueInput2 = new AutoCompleteTextView(this);
            tagValueInput2.setHint("Second tag value");
            tagValueInput2.setAdapter(autoCompleteAdapter);
            tagValueInput2.setThreshold(1);
            layout.addView(tagValueInput2);
        } else {
            tagTypeSpinner2 = null;
            tagValueInput2 = null;
        }

        builder.setView(layout);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String tagType1 = tagTypeSpinner1.getSelectedItem().toString();
            String tagValue1 = tagValueInput1.getText().toString().trim();

            final String tagType2 = tagTypeSpinner2 != null ? tagTypeSpinner2.getSelectedItem().toString() : null;
            final String tagValue2 = tagValueInput2 != null ? tagValueInput2.getText().toString().trim() : null;

            List<String> matches = new ArrayList<>();

            for (Album album : user.getAlbums()) {
                for (Photo photo : album.getPhotos()) {
                    boolean match1 = photo.getTags().stream().anyMatch(
                            tag -> tag.getName().equalsIgnoreCase(tagType1) &&
                                    tag.getValue().toLowerCase().startsWith(tagValue1.toLowerCase())
                    );

                    boolean match2 = tagType2 != null && photo.getTags().stream().anyMatch(
                            tag -> tag.getName().equalsIgnoreCase(tagType2) &&
                                    tag.getValue().toLowerCase().startsWith(tagValue2.toLowerCase())
                    );

                    boolean shouldInclude;
                    switch (mode) {
                        case "Single Tag":
                            shouldInclude = match1;
                            break;
                        case "Tag OR Tag":
                            shouldInclude = match1 || match2;
                            break;
                        case "Tag AND Tag":
                            shouldInclude = match1 && match2;
                            break;
                        default:
                            shouldInclude = false;
                            break;
                    }

                    if (shouldInclude && !matches.contains(photo.getFilePath())) {
                        matches.add(photo.getFilePath());
                    }
                }
            }

            photoPaths.clear();
            photoPaths.addAll(matches);
            adapter.notifyDataSetChanged();

            if (matches.isEmpty()) {
                Toast.makeText(this, "No matches found.", Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(AlbumActivity.this, PhotoViewerActivity.class);
            intent.putExtra("albumName", currentAlbum.getName()); // or a dummy name
            intent.putExtra("photoIndex", 0);
            intent.putStringArrayListExtra("photoPaths", new ArrayList<>(matches));
            startActivity(intent);

        });


        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }


    private void runTagSearch(String mode, Tag tag1, Tag tag2) {
        photoPaths.clear();

        for (Photo photo : currentAlbum.getPhotos()) {
            List<Tag> tags = photo.getTags();

            boolean hasTag1 = containsTag(tags, tag1);
            boolean hasTag2 = tag2 != null && containsTag(tags, tag2);

            boolean shouldInclude = false;

            switch (mode) {
                case "Single Tag":
                    shouldInclude = hasTag1;
                    break;
                case "Tag OR Tag":
                    shouldInclude = hasTag1 || hasTag2;
                    break;
                case "Tag AND Tag":
                    shouldInclude = hasTag1 && hasTag2;
                    break;
            }

            if (shouldInclude) {
                photoPaths.add(photo.getFilePath());
            }
        }

        adapter.notifyDataSetChanged();
    }

    private Set<String> getAllTagValues() {
        Set<String> tagValues = new HashSet<>();
        for (Album album : user.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                for (Tag tag : photo.getTags()) {
                    tagValues.add(tag.getValue());
                }
            }
        }
        return tagValues;
    }


    private boolean containsTag(List<Tag> tags, Tag target) {
        for (Tag tag : tags) {
            if (tag.equals(target)) {
                return true;
            }
        }
        return false;
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
