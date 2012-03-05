package student.testingsupport.shadows;

import student.testingsupport.junit4.RobolectricMixRunner;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteCursor;
import android.provider.MediaStore;
import android.database.Cursor;
import android.net.Uri;
import com.xtremelabs.robolectric.internal.Implementation;
import android.content.ContentResolver;
import com.xtremelabs.robolectric.internal.Implements;

@Implements(ContentResolver.class)
public class ShadowContentResolver
{
    private Cursor cursor;


    @Implementation
    public Cursor query(Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder)
    {
        // TODO For the love of god, make this more general.
        // Seriously, FIXME I don't like this at all.

        String part = uri.getSchemeSpecificPart();
        String externalPart = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            .getSchemeSpecificPart();

        if (part.startsWith(externalPart))
        {
            SQLiteDatabase db = RobolectricMixRunner.getDatabase();
            String actualSelection;

            if (part.length() > externalPart.length() + 1)
            {
                String rest = part.substring(externalPart.length() + 1);

                if (selection == null)
                {
                    actualSelection = "_id = " + rest;
                }
                else
                {
                    actualSelection = "_id = " + rest + " and " + selection;
                }
            }
            else
            {
                actualSelection = selection;
            }

            return db.query("media_images", projection, actualSelection,
                selectionArgs, null, null, sortOrder);
        }
        else
        {
            return cursor;
        }
    }


    public void setCursor(Cursor cursor)
    {
        this.cursor = cursor;
    }
}
