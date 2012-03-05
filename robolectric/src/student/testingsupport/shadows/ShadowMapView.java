package student.testingsupport.shadows;

import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.widget.ZoomButtonsController;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

import java.util.ArrayList;
import java.util.List;

import static com.xtremelabs.robolectric.RobolectricForMaps.shadowOf;

/**
 * Shadow of {@code MapView} that simulates the internal state of a {@code MapView}. Supports {@code Projection}s,
 * {@code Overlay}s, and {@code TouchEvent}s
 */
@SuppressWarnings({"UnusedDeclaration"})
@Implements(MapView.class)
public class ShadowMapView extends com.xtremelabs.robolectric.shadows.ShadowMapView {
    private MapView realMapView;
    private Projection projection;

    public ShadowMapView(MapView mapView) {
        super(mapView);
        realMapView = mapView;

        this.layout(0, 0, 400, 400);
    }

    @Implementation
    public com.google.android.maps.Projection getProjection() {
        if (projection == null) {
            projection = new Projection() {
                public Point toPixels(GeoPoint geoPoint, Point point) {
                    if (point == null) {
                        point = new Point();
                    }

                    point.y = scaleDegree(geoPoint.getLatitudeE6(),
                        ShadowMapView.this.getBottom(),
                        ShadowMapView.this.getTop(),
                        ShadowMapView.this.getMapCenter().getLatitudeE6(),
                        ShadowMapView.this.getLatitudeSpan());
                    point.x = scaleDegree(geoPoint.getLongitudeE6(),
                        ShadowMapView.this.getLeft(),
                        ShadowMapView.this.getRight(),
                        ShadowMapView.this.getMapCenter().getLongitudeE6(),
                        ShadowMapView.this.getLongitudeSpan());
                    return point;
                }

                public GeoPoint fromPixels(int x, int y) {
                    int lat = scalePixel(y,
                        ShadowMapView.this.getBottom(),
                        -realMapView.getHeight(),
                        ShadowMapView.this.getMapCenter().getLatitudeE6(),
                        ShadowMapView.this.getLatitudeSpan());
                    int lng = scalePixel(x, ShadowMapView.this.getLeft(),
                        realMapView.getWidth(),
                        ShadowMapView.this.getMapCenter().getLongitudeE6(),
                        ShadowMapView.this.getLongitudeSpan());
                    return new GeoPoint(lat, lng);
                }

                public float metersToEquatorPixels(float v) {
                    return 0;
                }
            };
        }
        return projection;
    }

    private int scalePixel(int pixel, int minPixel, int maxPixel, int centerDegree, int spanDegrees) {
        int offsetPixels = pixel - minPixel;
        double ratio = offsetPixels / ((double) maxPixel);
        int minDegrees = centerDegree - spanDegrees / 2;
        return (int) (minDegrees + spanDegrees * ratio);
    }

    private int scaleDegree(int degree, int minPixel, int maxPixel, int centerDegree, int spanDegrees) {
        int minDegree = centerDegree - spanDegrees / 2;
        int offsetDegrees = degree - minDegree;
        double ratio = offsetDegrees / ((double) spanDegrees);
        int spanPixels = maxPixel - minPixel;
        return (int) (minPixel + spanPixels * ratio);
    }

/*    @Implementation
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        for (Overlay overlay : overlays) {
            if (overlay.onTouchEvent(event, realMapView)) {
                return true;
            }
        }

        GeoPoint mouseGeoPoint = getProjection().fromPixels((int) event.getX(), (int) event.getY());
        int diffX = 0;
        int diffY = 0;
        if (mouseDownOnMe) {
            diffX = (int) event.getX() - lastTouchEventPoint.x;
            diffY = (int) event.getY() - lastTouchEventPoint.y;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mouseDownOnMe = true;
                mouseDownCenter = getMapCenter();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mouseDownOnMe) {
                    moveByPixels(-diffX, -diffY);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mouseDownOnMe) {
                    moveByPixels(-diffX, -diffY);
                    mouseDownOnMe = false;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                getController().setCenter(mouseDownCenter);
                mouseDownOnMe = false;
                break;
        }

        lastTouchEventPoint = new Point((int) event.getX(), (int) event.getY());

        return super.dispatchTouchEvent(event);
    }*/

    private void moveByPixels(int x, int y) {
        Point center = getProjection().toPixels(getMapCenter(), null);
        center.offset(x, y);
        getController().setCenter(getProjection().fromPixels(center.x, center.y));
    }
}
