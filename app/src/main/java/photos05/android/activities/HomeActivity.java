package photos05.android.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import photos05.android.R;
import photos05.android.model.Album;
import photos05.android.util.DataManager;
import photos05.android.model.User;
import photos05.android.util.AlbumDialogHelper;

import java.io.File;
import java.util.ArrayList;

public class HomeActivity extends AppCompatActivity{
    private ListView albumListView;
    private ArrayList<Album> albums = new ArrayList<>();

    private ArrayAdapter<String> adapter;

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set Up Screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Get Most Recent Album
        albumListView = findViewById(R.id.albumListView);
        loadUserData();
        displayAlbums();

        albumListView.setOnItemClickListener((parent, view, position, id) -> openAlbum(position));

        albumListView.setOnItemLongClickListener((parent, view, position, id) -> {
            showAlbumOptionsDialog(position);
            return true;
        });

        findViewById(R.id.addAlbumButton).setOnClickListener(v -> createNewAlbum());
    }

    // Load user data from DataManager
    private void loadUserData()  {
        user = DataManager.loadUser(this);

        // Test for Null User, initialize new user if null
        if (user == null) {
            user = new User("default");
        }

        albums = new ArrayList<>(user.getAlbums());
    }

    // Save User Data to storage
    private void saveUserData() {
        DataManager.saveUser(user, this);
    }

    // Displays the albums in the ListView
    private void displayAlbums() {
        ArrayList<String> albumNames = new ArrayList<>();

        // Add each album to the list of names
        for (Album album : albums) {
            albumNames.add(album.getName());
        }

        // Load and Set the Adapter to display albums
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, albumNames);
        albumListView.setAdapter(adapter);
    }

    // Creates a new album
    private void createNewAlbum() {
        AlbumDialogHelper.promptForAlbumName(this, "Create New Album", name -> {
            if (user.getAlbumByName(name) != null) {
                Toast.makeText(this, "Album already exists", Toast.LENGTH_SHORT).show();
                return;
            }
            Album newAlbum = new Album(name);
            user.addAlbum(newAlbum);
            saveUserData();
            albums.add(newAlbum);
            displayAlbums();
        });
    }

    // Opens an album by user selection
    private void openAlbum(int index) {
        Album selectedAlbum = albums.get(index);
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.putExtra("albumName", selectedAlbum.getName());
        startActivity(intent);
    }

    // Provide the user with options for the album
    private void showAlbumOptionsDialog(int index) {
        String[] options = { "Rename", "Delete" };

        // Create a dialog to show the options
        new AlertDialog.Builder(this)
                .setTitle(albums.get(index).getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: renameAlbum(index); break;
                        case 1: deleteAlbum(index); break;
                    }
                })
                .show();
    }

    // Rename the selected album
    private void renameAlbum(int index) {
        Album album = albums.get(index);
        AlbumDialogHelper.promptForAlbumName(this, "Rename Album", new AlbumDialogHelper.OnNameEnteredListener() {
            @Override
            public void onNameEntered(String newName) {
                if (user.getAlbumByName(newName) != null) {
                    Toast.makeText(HomeActivity.this, "Album name already exists", Toast.LENGTH_SHORT).show();
                    return;
                }
                album.setName(newName);
                saveUserData();
                displayAlbums();
            }
        });
    }

    // Delete the selected album
    private void deleteAlbum(int index) {
        Album album = albums.get(index);
        new AlertDialog.Builder(this)
                .setTitle("Delete Album")
                .setMessage("Are you sure you want to delete " + album.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    user.removeAlbum(album);
                    albums.remove(album);
                    saveUserData();
                    displayAlbums();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}
