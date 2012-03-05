package student.android;

import java.io.IOException;

import com.google.android.maps.GeoPoint;

// -------------------------------------------------------------------------
/**
 * A subclass of {@code android.media.ExifInterface} that fixes an arithmetic
 * bug that reduces the accuracy of the latitude/longitude extracted from
 * JPEG images.
 *
 * @author  Tony Allevato
 * @version 2011.09.18
 */
public class ExifInterface extends android.media.ExifInterface
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Initializes a new {@code ExifInterface} for the specified image.
     *
     * @param filename the filename of the image to extract EXIF information
     *     from
     *
     * @throws IOException if there was a problem opening the file
     */
    public ExifInterface(String filename) throws IOException
    {
        super(filename);
    }


    // ----------------------------------------------------------
    /**
     * Gets the latitude and longitude value and returns it as a
     * {@code GeoPoint} object that can be directly used on a {@code MapView}.
     * Returns null if the Exif tags are not available.
     *
     * @return a {@code GeoPoint} containing the coordinates of the image, or
     *     null if the file did not contain EXIF information
     */
    public GeoPoint getLatLong()
    {
        float[] output = new float[2];

        if (getLatLong(output))
        {
            return new GeoPoint(
                (int) (output[0] * 1e6), (int) (output[1] * 1e6));
        }
        else
        {
            return null;
        }
    }
}
