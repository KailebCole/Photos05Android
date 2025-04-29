package photos05.android.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import photos05.android.R;
import photos05.android.model.Album;
import photos05.android.model.Photo;
import photos05.android.model.User;
import photos05.android.util.DataManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import photos05.android.R;

public class AlbumActivity extends AppCompatActivity{
    private static final String TAG = "AlbumActivity";
    private GridView gridView;
    private ArrayAdapter<String> adapter;
    private List<String> photoPaths = new ArrayList<>();
    private ActivityResultLauncher<Intent> selectPhotoLauncher;
    private Album currentAlbum;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        //add photo button
        Button addPhotoButton = findViewById(R.id.addPhotoButton);
        if (addPhotoButton == null) {
            Log.e(TAG, "onCreate: Button not found!");
            return;
        }
        // Set a click listener on the button to open the image picker
        addPhotoButton.setOnClickListener(v -> addPhoto());

        Log.d(TAG, "onCreate: Add Photo Button created");

        //set up grid view
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
                    imageView.setLayoutParams(new GridView.LayoutParams(200, 200)); // Adjust size as needed
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imageView.setPadding(8, 8, 8, 8); // Add padding
                } else {
                    imageView = (ImageView) convertView;
                }

                // Load the image into the ImageView
                imageView.setImageURI(Uri.parse(getItem(position)));

                return imageView;
            }
        };

        gridView.setAdapter(adapter);
        //set up SAF launcher
        selectPhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Get the URI of the selected file
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            Log.d(TAG, "onActivityResult: File URI: " + uri);
                            // Add the path of the image to the photoPaths
                            if (!photoPaths.contains(uri.toString())) {
                                photoPaths.add(uri.toString());
                            } else {
                                Toast.makeText(this, "This Photo Already Exists in this album!", Toast.LENGTH_SHORT).show();
                            }
                            //Notify that the dataset has changed
                            adapter.notifyDataSetChanged();
                            Toast.makeText(this, "Photo Path added", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d(TAG, "onActivityResult: File URI does not exist");
                            Toast.makeText(this, "Error selecting file", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void addPhoto() {
        Log.d(TAG, "addPhoto: Prompting user to select a photo");
        // Create an intent to open the SAF file picker
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*"); // Filter for image types only
        selectPhotoLauncher.launch(intent);
    }
}
