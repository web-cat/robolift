package student.testingsupport.shadows;

import com.google.android.maps.GeoPoint;
import android.view.MotionEvent;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

@Implements(Overlay.class)
public class ShadowOverlay
{
    public Overlay realOverlay;


    public ShadowOverlay(Overlay realOverlay)
    {
        this.realOverlay = realOverlay;
    }


    @Implementation
    public boolean onTouchEvent(MotionEvent e, MapView mapView)
    {
        GeoPoint point = mapView.getProjection().fromPixels(
            (int) e.getX(), (int) e.getY());

        return realOverlay.onTap(point, mapView);
    }
}
