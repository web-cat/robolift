package student.testingsupport.shadows;

import java.util.Map;
import java.util.HashMap;
import android.media.ExifInterface;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;
import java.io.File;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.RationalNumber;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;

// -------------------------------------------------------------------------
/**
 * Currently only supports latitude and longitude. Add more.
 *
 * @author  Tony Allevato
 * @version 2011.09.16
 */
@Implements(ExifInterface.class)
public class ShadowExifInterface
{
    private JpegImageMetadata metadata;


    private static final Map<String, TagInfo> stringsToTags;


    static
    {
        stringsToTags = new HashMap<String, TagInfo>();

        stringsToTags.put(ExifInterface.TAG_ORIENTATION,
            TiffConstants.EXIF_TAG_ORIENTATION);
        stringsToTags.put(ExifInterface.TAG_DATETIME,
            TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        stringsToTags.put(ExifInterface.TAG_MAKE,
            TiffConstants.EXIF_TAG_MAKE);
        stringsToTags.put(ExifInterface.TAG_MODEL,
            TiffConstants.EXIF_TAG_MODEL);
        stringsToTags.put(ExifInterface.TAG_FLASH,
            TiffConstants.EXIF_TAG_FLASH);
        stringsToTags.put(ExifInterface.TAG_IMAGE_WIDTH,
            TiffConstants.EXIF_TAG_IMAGE_WIDTH);
        stringsToTags.put(ExifInterface.TAG_IMAGE_LENGTH,
            TiffConstants.EXIF_TAG_IMAGE_HEIGHT);

        stringsToTags.put(ExifInterface.TAG_GPS_LATITUDE,
            TiffConstants.GPS_TAG_GPS_LATITUDE);
        stringsToTags.put(ExifInterface.TAG_GPS_LONGITUDE,
            TiffConstants.GPS_TAG_GPS_LONGITUDE);

        stringsToTags.put(ExifInterface.TAG_GPS_LATITUDE_REF,
            TiffConstants.GPS_TAG_GPS_LATITUDE_REF);
        stringsToTags.put(ExifInterface.TAG_GPS_LONGITUDE_REF,
            TiffConstants.GPS_TAG_GPS_LONGITUDE_REF);

        stringsToTags.put("GPSAltitude",
            TiffConstants.GPS_TAG_GPS_ALTITUDE);
        stringsToTags.put("GPSAltitudeRef",
            TiffConstants.GPS_TAG_GPS_ALTITUDE);

        stringsToTags.put(ExifInterface.TAG_GPS_TIMESTAMP,
            TiffConstants.GPS_TAG_GPS_TIME_STAMP);
        stringsToTags.put(ExifInterface.TAG_GPS_DATESTAMP,
            TiffConstants.GPS_TAG_GPS_DATE_STAMP);

        stringsToTags.put(ExifInterface.TAG_WHITE_BALANCE,
            TiffConstants.EXIF_TAG_WHITE_BALANCE_1);
        stringsToTags.put(ExifInterface.TAG_FOCAL_LENGTH,
            TiffConstants.EXIF_TAG_FOCAL_LENGTH);

        stringsToTags.put(ExifInterface.TAG_GPS_PROCESSING_METHOD,
            TiffConstants.GPS_TAG_GPS_PROCESSING_METHOD);

    }


    public void __constructor__(String path)
    {
        try
        {
            // FIXME Dirty hack.
            String prefix = "/mnt/sdcard/download/";
            if (path.startsWith(prefix))
            {
                path = path.substring(prefix.length());
            }

            metadata = (JpegImageMetadata)Sanselan.getMetadata(new File(path));
        }
        catch (Exception e)
        {
            metadata = null;
        }
    }


    @Implementation
    public String getAttribute(String tag)
    {
        TiffField field = metadata.findEXIFValue(stringsToTags.get(tag));

        try
        {
            return field.getStringValue();
        }
        catch (Exception e)
        {
            return null;
        }
    }


    @Implementation
    public int getAttributeInt(String tag, int defaultValue)
    {
        try
        {
            return Integer.parseInt(getAttribute(tag));
        }
        catch (RuntimeException e)
        {
            return defaultValue;
        }
    }


    @Implementation
    public double getAttributeDouble(String tag, double defaultValue)
    {
        try
        {
            return getRationalValue(stringsToTags.get(tag)).doubleValue();
        }
        catch (RuntimeException e)
        {
            return defaultValue;
        }
    }


    @Implementation
    public double getAltitude(double defaultValue)
    {
        double altitude = getAttributeDouble("GPSAltitude", -1);
        int ref = getAttributeInt("GPSAltitudeRef", -1);

        if (altitude >= 0 && ref >= 0)
        {
            return (altitude * ((ref == 1) ? -1 : 1));
        }
        else
        {
            return defaultValue;
        }
    }


    @Implementation
    public boolean getLatLong(float[] output)
    {
        if (metadata != null)
        {
            RationalNumber[] latValue = getRationalsValue(TiffConstants.GPS_TAG_GPS_LATITUDE);
            String latRef = getStringValue(TiffConstants.GPS_TAG_GPS_LATITUDE_REF);
            RationalNumber[] lngValue = getRationalsValue(TiffConstants.GPS_TAG_GPS_LONGITUDE);
            String lngRef = getStringValue(TiffConstants.GPS_TAG_GPS_LONGITUDE_REF);

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
        else
        {
            return false;
        }
    }



    private RationalNumber getRationalValue(TagInfo tagInfo)
    {
        TiffField field = metadata.findEXIFValue(tagInfo);

        try
        {
            return (RationalNumber) field.getValue();
        }
        catch (Exception e)
        {
            return null;
        }
    }


    private RationalNumber[] getRationalsValue(TagInfo tagInfo)
    {
        TiffField field = metadata.findEXIFValue(tagInfo);

        try
        {
            return (RationalNumber[]) field.getValue();
        }
        catch (Exception e)
        {
            return null;
        }
    }


    private String getStringValue(TagInfo tagInfo)
    {
        TiffField field = metadata.findEXIFValue(tagInfo);

        try
        {
            return (String) field.getValue();
        }
        catch (Exception e)
        {
            return null;
        }
    }


    private static float convertRationalLatLonToFloat(
        RationalNumber[] rational, String ref)
    {
        try
        {
            double degrees = rational[0].doubleValue();
            double minutes = rational[1].doubleValue();
            double seconds = rational[2].doubleValue();

            double result = degrees + (minutes / 60.0) + (seconds / 3600.0);
            if ((ref.trim().equals("S") || ref.trim().equals("W")))
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
