package student.testingsupport.shadows;

import android.view.ViewGroup;
import com.xtremelabs.robolectric.internal.Implementation;
import com.xtremelabs.robolectric.internal.Implements;

//-------------------------------------------------------------------------
/**
 * An extension of Robolectric's built-in ShadowViewGroup that invalidates a
 * view group's children when its own invalidate() method is called.
 *
 * @author Tony Allevato
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
@Implements(ViewGroup.class)
public class ShadowViewGroup
    extends com.xtremelabs.robolectric.shadows.ShadowViewGroup
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Overridden to invalidate all of the view group's children.
     */
    @Implementation
    public void invalidate()
    {
        super.invalidate();

        // Force the children to be invalidated as well.

        for (int i = 0; i < getChildCount(); i++)
        {
            getChildAt(i).invalidate();
        }
    }
}
