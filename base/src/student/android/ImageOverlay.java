package student.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

// -------------------------------------------------------------------------
/**
 * A Google Maps overlay that represents a single marker on the map. While
 * Android provides the {@code ItemizedOverlay} class for placing multiple
 * markers in a single overlay, this class has an interface that is much easier
 * for students to use.
 *
 * @author  Tony Allevato
 * @version 2011.09.17
 */
public class ImageOverlay extends Overlay
{
    //~ Instance/static variables .............................................

    // The path to the file that the overlay's marker was created from.
    private String file;

    // The geographic location of the marker on the map.
    private GeoPoint location;

    // An arbitrary object associated with the overlay.
    private Object data;

    // The Drawable object that will represent the marker.
    private Drawable marker;

    // A listener that will be notified when the marker is clicked, if any.
    private OnClickListener onClickListener;

    // The maximum size of the marker on the screen.
    private static final int MAX_SIZE = 40;


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new {@code ImageOverlay} at the specified location on the map.
     *
     * @param file the path to the image file that will be displayed as the
     *     marker on the map
     * @param location a {@code GeoPoint} that determines where the marker will
     *     be placed on the map
     */
    public ImageOverlay(String file, GeoPoint location)
    {
        this.file = file;
        this.marker = createMarkerImage(file);
        this.location = location;
    }


    // ----------------------------------------------------------
    /**
     * Creates a new {@code ImageOverlay} at the specified location on the map.
     *
     * @param drawable a drawable that will be used to draw the marker
     * @param location a {@code GeoPoint} that determines where the marker will
     *     be placed on the map
     */
    public ImageOverlay(Drawable drawable, GeoPoint location)
    {
        this.file = null;
        this.marker = drawable.mutate();

        Rect rect = marker.getBounds();
        marker.setBounds(
            -rect.width() / 2,
            -rect.height() + 1,
            rect.width() / 2,
            1);

        this.location = location;
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Gets the path to the image file that the overlay represents.
     *
     * @return the path to the image file that the overlay represents
     */
    public String getFilePath()
    {
        return file;
    }


    // ----------------------------------------------------------
    /**
     * Gets the location of the overlay on the map.
     *
     * @return a {@code GeoPoint} that contains the location of the overlay
     */
    public GeoPoint getLocation()
    {
        return location;
    }


    // ----------------------------------------------------------
    /**
     * Gets the arbitrary data object associated with the overlay.
     *
     * @return an arbitrary {@code Object} that is associated with the overlay
     */
    public Object getData()
    {
        return data;
    }


    // ----------------------------------------------------------
    /**
     * Sets the arbitrary data object associated with the overlay. This can be
     * useful when you want to have easy access to an object that the overlay
     * represents, and you can retrieve it by calling the {@link #getData()}
     * method, for example, when the overlay is clicked.
     *
     * @param newData an arbitrary {@code Object} that will be associated with
     *     the overlay
     */
    public void setData(Object newData)
    {
        data = newData;
    }


    // ----------------------------------------------------------
    /**
     * Gets the listener that will be called when the marker is clicked.
     *
     * @return the {@link ImageOverlay.OnClickListener} that will be
     *     called when the marker is clicked, or null if there isn't one
     */
    public OnClickListener getOnClickListener()
    {
        return this.onClickListener;
    }


    // ----------------------------------------------------------
    /**
     * Sets the listener that will be called when the marker is clicked.
     *
     * @param listener the {@link ImageOverlay.OnClickListener} that will be
     *     called when the marker is clicked
     */
    public void setOnClickListener(OnClickListener listener)
    {
        this.onClickListener = listener;
    }


    // ----------------------------------------------------------
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow)
    {
        Point point = mapView.getProjection().toPixels(location, null);
        drawAt(canvas, marker, point.x, point.y, shadow);
    }


    // ----------------------------------------------------------
    @Override
    public boolean onTap(GeoPoint point, MapView mapView)
    {
        if (onClickListener != null)
        {
            if (hitTest(point, mapView))
            {
                onClickListener.onClick(this, mapView);
                return true;
            }
        }

        return false;
    }


    // ----------------------------------------------------------
    /**
     * Determines if the specified geopoint falls anywhere within the bounds of
     * the marker image.
     *
     * @param point the geopoint to test
     * @param mapView the MapView on which this overlay is added
     * @return true if the geopoint is within the bounds of the marker image,
     *     otherwise false
     */
    private boolean hitTest(GeoPoint point, MapView mapView)
    {
        Point bottomCenter = mapView.getProjection().toPixels(location, null);
        Point tapped = mapView.getProjection().toPixels(point, null);

        Rect bounds = marker.copyBounds();
        bounds.offset(bottomCenter.x, bottomCenter.y);

        return bounds.contains(tapped.x, tapped.y);
    }


    // ----------------------------------------------------------
    /**
     * Creates the marker image from the specified image file, scaling it down
     * if necessary and setting the bounds of the drawable.
     *
     * @param file the file
     * @return a Drawable that is scaled down and whose bounds will position it
     *     on top of the point where it is added
     */
    private static Drawable createMarkerImage(String file)
    {
        Bitmap bitmap = BitmapFactory.decodeFile(file);

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        double ratio = (double) width / height;

        int newWidth, newHeight;

        if (width > height)
        {
            newWidth = MAX_SIZE;
            newHeight = (int) (MAX_SIZE / ratio);
        }
        else
        {
            newHeight = MAX_SIZE;
            newWidth = (int) (MAX_SIZE * ratio);
        }

        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        BitmapDrawable drawable = new BitmapDrawable(bitmap);
        drawable.setBounds(-newWidth / 2, -newHeight + 1, newWidth / 2, 1);
        return drawable;
    }


    //~ Inner classes .........................................................

    // ----------------------------------------------------------
    /**
     * A listener that is notified when an {@link ImageOverlay} is clicked on
     * its {@code MapView}.
     */
    public interface OnClickListener
    {
        // ----------------------------------------------------------
        /**
         * This method is called when an {@link ImageOverlay} is clicked on its
         * {@code MapView}.
         *
         * @param overlay the {@link ImageOverlay} that was clicked
         * @param mapView the {@code MapView} that contained the overlay
         */
        public void onClick(ImageOverlay overlay, MapView mapView);
    }
}
