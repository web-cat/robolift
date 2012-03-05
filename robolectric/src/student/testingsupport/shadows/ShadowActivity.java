package student.testingsupport.shadows;

import java.util.ArrayList;
import java.util.Iterator;
import android.content.IntentFilter;
import java.util.List;
import android.content.Intent;
import android.graphics.Canvas;
import com.xtremelabs.robolectric.internal.Implementation;
import android.app.Activity;
import android.view.View;
import com.xtremelabs.robolectric.internal.Implements;

//-------------------------------------------------------------------------
/**
 * An extension of Robolectric's built-in ShadowActivity that invalidates an
 * inflated layout's views when {@link Activity#setContentView(int)} is called.
 *
 * @author Tony Allevato
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
@Implements(Activity.class)
public class ShadowActivity
    extends com.xtremelabs.robolectric.shadows.ShadowActivity
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Overridden to explicitly call {@link View#onDraw(Canvas)} through
     * reflection, for the purpose of achieving full code coverage.
     */
    @Implementation
    public void setContentView(int layoutResID)
    {
        super.setContentView(layoutResID);
        getContentView().invalidate();
    }


    // ----------------------------------------------------------
    @Implementation
    public void startActivity(Intent intent)
    {
        super.startActivity(intent);

        PendingResult pending = findMatchingPendingResult(intent);
        if (pending != null)
        {
            pendingResults.remove(pending);
        }
    }


    // ----------------------------------------------------------
    @Implementation
    public void startActivityForResult(Intent intent, int requestCode)
    {
        super.startActivityForResult(intent, requestCode);

        PendingResult pending = findMatchingPendingResult(intent);
        if (pending != null)
        {
            receiveResult(intent, pending.resultCode, pending.resultIntent);
            pendingResults.remove(pending);
        }
    }


    // ----------------------------------------------------------
    public String failureReasonIfNotAllMonitorsHit()
    {
        if (pendingResults.isEmpty())
        {
            return null;
        }
        else
        {
            return pendingResults.get(0).failureReason;
        }
    }


    // ----------------------------------------------------------
    public void addPendingResult(IntentFilter intentFilter, int resultCode,
        Intent resultIntent, String failureReason)
    {
        PendingResult pending = new PendingResult();
        pending.intentFilter = intentFilter;
        pending.resultCode = resultCode;
        pending.resultIntent = resultIntent;
        pending.failureReason = failureReason;

        pendingResults.add(pending);
    }


    // ----------------------------------------------------------
    public void addPendingResult(String activityClassName, int resultCode,
        Intent resultIntent)
    {
        PendingResult pending = new PendingResult();
        pending.activityClassName = activityClassName;
        pending.resultCode = resultCode;
        pending.resultIntent = resultIntent;

        pendingResults.add(pending);
    }


    // ----------------------------------------------------------
    private PendingResult findMatchingPendingResult(Intent intent)
    {
        for (PendingResult pending : pendingResults)
        {
            if (pending.intentFilter != null)
            {
                Iterator<String> it = pending.intentFilter.actionsIterator();

                while (it.hasNext())
                {
                    if (it.next().equals(intent.getAction()))
                    {
                        return pending;
                    }
                }
            }
            else
            {
                // FIXME???
                if (intent.getComponent().getClassName().equals(
                    pending.activityClassName))
                {
                    return pending;
                }
            }
        }

        return null;
    }


    private List<PendingResult> pendingResults = new ArrayList<PendingResult>();

    public class PendingResult
    {
        public IntentFilter intentFilter;
        public String activityClassName;
        public int resultCode;
        public Intent resultIntent;
        public String failureReason;
    }
}
