/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2011 Virginia Tech
 |
 |  This file is part of the Student-Library.
 |
 |  The Student-Library is free software; you can redistribute it and/or
 |  modify it under the terms of the GNU Lesser General Public License as
 |  published by the Free Software Foundation; either version 3 of the
 |  License, or (at your option) any later version.
 |
 |  The Student-Library is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU Lesser General Public License for more details.
 |
 |  You should have received a copy of the GNU Lesser General Public License
 |  along with the Student-Library; if not, see <http://www.gnu.org/licenses/>.
\*==========================================================================*/

package student.testingsupport.shadows;

import android.content.Context;
import android.util.AttributeSet;
import java.lang.reflect.Method;
import android.graphics.Canvas;
import com.xtremelabs.robolectric.internal.Implementation;
import android.view.View;
import com.xtremelabs.robolectric.internal.Implements;

// -------------------------------------------------------------------------
/**
 * An extension of Robolectric's built-in ShadowView that calls the real view's
 * onDraw method when invalidate() is called, for code coverage purposes.
 *
 * @author Tony Allevato
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
@Implements(View.class)
public class ShadowView extends com.xtremelabs.robolectric.shadows.ShadowView
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    public void __constructor__(Context context, AttributeSet attributeSet, int defStyle)
    {
        super.__constructor__(context, attributeSet, defStyle);

        // Ugh.
        this.layout(0, 0, 400, 400);
    }


    // ----------------------------------------------------------
    /**
     * Overridden to explicitly call {@link View#onDraw(Canvas)} through
     * reflection, for the purpose of achieving full code coverage.
     */
    @Implementation
    public void invalidate()
    {
        super.invalidate();

        // TODO Should a similar shadow for ViewGroup be provided that does
        // this for its children?

        try
        {
            // This goes here because ShadowView has layout() as final, which
            // is where I would rather put it.
            Method onMeasure = realView.getClass().getDeclaredMethod(
                "onMeasure", int.class, int.class);
            onMeasure.setAccessible(true);
            onMeasure.invoke(realView, getWidth(), getHeight());
        }
        catch (Exception e)
        {
            System.err.println("View.invalidate() couldn't invoke onMeasure "
                + "because of the following error:");
            e.printStackTrace(System.err);
        }

        try
        {
            Method onDraw = realView.getClass().getDeclaredMethod(
                "onDraw", Canvas.class);
            onDraw.setAccessible(true);
            onDraw.invoke(realView, new Canvas());
        }
        catch (Exception e)
        {
            System.err.println("View.invalidate() couldn't invoke onDraw "
                + "because of the following error:");
            e.printStackTrace(System.err);
        }
    }


    // ----------------------------------------------------------
    /**
     * Overridden to explicitly call {@link View#onDraw(Canvas)} through
     * reflection, for the purpose of achieving full code coverage.
     */
    @Implementation
    public void postInvalidate()
    {
        postInvalidateDelayed(0);
    }
}
