package photos05.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.widget.EditText;

public class AlbumDialogHelper {
    public interface OnNameEnteredListener {
        void onNameEntered(String name);
    }

    // Default Dialog Box
    public static void promptForAlbumName(Context context, String title, OnNameEnteredListener listener) {
        EditText input = new EditText(context);
        input.setHint("Album name");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(input)
                .setPositiveButton("OK", (dialog, which) -> {
                    String name = input.getText().toString();
                    if (!name.isEmpty()) {
                        listener.onNameEntered(name);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
