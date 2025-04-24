package photos05.android.util;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import photos05.android.model.User;

public class DataManager {
    public static User loadUser(Context context) {
        try {
            File file = new File(context.getFilesDir(), "user_data.dat");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            return (User) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return new User("default");
        }
    }

    public static void saveUser(User user, Context context) {
        try {
            File file = new File(context.getFilesDir(), "user_data.dat");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(user);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
