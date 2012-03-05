package student.android;

import com.google.android.maps.GeoPoint;
import java.io.IOException;

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


    //~ Methods ...............................................................

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


    // ----------------------------------------------------------
    /**
     * Stores the latitude and longitude value in a float array. The first
     * element is the latitude, and the second element is the longitude.
     * Returns false if the Exif tags are not available.
     *
     * @param output a float array large enough to hold two elements; when the
     *     method returns, {@code output[0]} will contain the latitude and
     *     {@code output[1]} will contain the longitude
     * @return true if the file contained EXIF information, or false if it did
     *     not
     */
    public boolean getLatLong(float[] output)
    {
        String latValue = getAttribute(ExifInterface.TAG_GPS_LATITUDE);
        String latRef = getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        String lngValue = getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
        String lngRef = getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

        if (latValue != null && latRef != null && lngValue != null
            && lngRef != null)
        {
            output[0] = convertRationalLatLonToFloat(latValue, latRef);
            output[1] = convertRationalLatLonToFloat(lngValue, lngRef);

            return true;
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    private static float convertRationalLatLonToFloat(String rationalString,
        String ref)
    {
        try
        {
            String[] parts = rationalString.split(",");

            String[] pair;
            pair = parts[0].split("/");
            double degrees = (Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim()));

            pair = parts[1].split("/");
            double minutes = (Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim()));

            pair = parts[2].split("/");
            double seconds = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

            double result = degrees + (minutes / 60.0) + (seconds / 3600.0);

            if (ref.equals("S") || ref.equals("W"))
            {
                return (float) -result;
            }
            else
            {
                return (float) result;
            }
        }
        catch (RuntimeException e)
        {
            // if for whatever reason we can't parse the lat long then return
            // null
            return 0f;
        }
    }
}
