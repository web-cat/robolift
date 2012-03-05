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

import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// -------------------------------------------------------------------------
/**
 * A class that recursively finds views that match a certain criteria.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class AndroidViewFinder
{
    //~ Static/instance variables .............................................

    // The root views that will be searched.
    private List<View> roots;


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new {@code AndroidViewFinder} that starts searching from the
     * specified root view.
     *
     * @param root the root view
     */
    public AndroidViewFinder(View root)
    {
        this.roots = new ArrayList<View>();
        roots.add(root);
    }


    // ----------------------------------------------------------
    /**
     * Creates a new {@code AndroidViewFinder} that starts searching from the
     * specified root views.
     *
     * @param roots the root views
     */
    public AndroidViewFinder(List<View> roots)
    {
        this.roots = new ArrayList<View>(roots);
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Gets a list of views on that match the specified filter.
     *
     * @param filter the filter to match
     * @return the list of Views that match the filter
     */
    public List<View> find(AndroidViewFilter filter)
    {
        ArrayList<View> viewsFound = new ArrayList<View>();

        ArrayList<View> reverseRoots = new ArrayList<View>(roots);
        Collections.reverse(reverseRoots);

        for (View root : reverseRoots)
        {
            findRecursively(filter, root, viewsFound);
        }

        return viewsFound;
    }


    // ----------------------------------------------------------
    /**
     * A recursive helper method used to implement
     * {@link #find(AndroidViewFilter)}.
     *
     * @param filter the filter to match
     * @param view the current view being tested
     * @param viewsFound a List that collects the views that match the filter
     */
    private void findRecursively(AndroidViewFilter filter, View view,
                                 List<View> viewsFound)
    {
        if (filter.test(view))
        {
            viewsFound.add(view);
        }

        if (view instanceof ViewGroup)
        {
            final ViewGroup vg = (ViewGroup) view;

            for (int i = 0; i < vg.getChildCount(); i++)
            {
                View child = vg.getChildAt(i);

                findRecursively(filter, child, viewsFound);
            }
        }
    }
}
