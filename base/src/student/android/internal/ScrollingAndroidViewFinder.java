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

package student.android.internal;

import android.app.Instrumentation;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

// -------------------------------------------------------------------------
/**
 * <p>
 * A class that recursively finds the first view that matches a given criteria,
 * scrolling if necessary whenever a scrollable view (such as a
 * {@link android.widget.ListView}) is encountered.
 * </p><p>
 * Unlike {@link AndroidViewFinder}, which finds <b>all</b> views that satisfy
 * a particular criteria, this class only permits finding the first view that
 * does so. This is due to the way that Android can reuse views that are used
 * scrolling adapter-based containers.
 * </p>
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class ScrollingAndroidViewFinder
{
    //~ Static/instance variables .............................................

    private Instrumentation instrumentation;
    private List<View> roots;


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new {@code ScrollingAndroidViewFinder} that starts searching
     * from the specified root view.
     *
     * @param instrumentation the instrumentation
     * @param root the root view
     */
    public ScrollingAndroidViewFinder(
        Instrumentation instrumentation, View root)
    {
        this.instrumentation = instrumentation;
        this.roots = new ArrayList<View>();
        roots.add(root);
    }


    // ----------------------------------------------------------
    /**
     * Creates a new {@code ScrollingAndroidViewFinder} that starts searching
     * from the specified root views.
     *
     * @param instrumentation the instrumentation
     * @param roots the root views
     */
    public ScrollingAndroidViewFinder(
        Instrumentation instrumentation, List<View> roots)
    {
        this.instrumentation = instrumentation;
        this.roots = new ArrayList<View>(roots);
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Gets the first view that matches the specified filter.
     *
     * @param filter the filter to match
     * @return the list of Views that match the filter
     */
    public View findFirst(AndroidViewFilter filter)
    {
        View viewFound = null;

        for (View root : roots)
        {
            viewFound = findFirstRecursively(filter, root);

            if (viewFound != null)
            {
                break;
            }
        }

        return viewFound;
    }


    // ----------------------------------------------------------
    /**
     * A recursive helper method used to implement
     * {@link #findFirst(AndroidViewFilter)}.
     *
     * @param filter the filter to match
     * @param view the view being tested
     */
    private View findFirstRecursively(final AndroidViewFilter filter,
                                      View view)
    {
        if (filter.test(view))
        {
            return view;
        }

        final View[] viewHolder = new View[1];

        if (view instanceof ViewGroup)
        {
            final ViewGroup vg = (ViewGroup) view;

            AndroidScroller scroller = AndroidScroller.createScroller(
                view, instrumentation);

            scroller.doWhileScrolling(new Callable<Boolean>() {
                public Boolean call() throws Exception
                {
                    for (int i = 0; i < vg.getChildCount(); i++)
                    {
                        View child = vg.getChildAt(i);

                        viewHolder[0] =
                            findFirstRecursively(filter, child);

                        if (viewHolder[0] != null)
                        {
                            return true;
                        }
                    }

                    return false;
                }
            });
        }

        return viewHolder[0];
    }
}
