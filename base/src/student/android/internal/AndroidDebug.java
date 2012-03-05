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

import java.lang.reflect.Method;
import android.view.View;

// -------------------------------------------------------------------------
/**
 * Various utility methods that can be useful when debugging Android
 * applications.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class AndroidDebug
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Prevent instantiation.
     */
    private AndroidDebug()
    {
        // Do nothing.
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Tries to get the text that belongs to a view. This method uses
     * reflection and tries to call a public {@code getText()} method on the
     * specified view; if it works, the text is returned, otherwise null is
     * returned.
     *
     * @param view the view
     * @return the text of the view, or null if it could not be obtained
     */
    public static String tryGetText(View view)
    {
        try
        {
            Method method = view.getClass().getMethod("getText");
            CharSequence cs = (CharSequence) method.invoke(view);
            return cs.toString();
        }
        catch (Exception e)
        {
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets a human-readable string that represents the visibility of a view.
     *
     * @param view the view
     * @return one of the strings "invisible", "visible", or "gone", which
     *     correspond to the same named constants in the {@link View} class
     */
    public static String getVisibilityString(View view)
    {
        int visibility = view.getVisibility();

        switch (visibility)
        {
            case View.INVISIBLE:
                return "invisible";

            case View.VISIBLE:
                return "visible";

            case View.GONE:
                return "gone";

            default:
                return "unknown visibility " + visibility;
        }
    }


    // ----------------------------------------------------------
    /**
     * Prints a human-readable description of the specified view to standard
     * output.
     *
     * @param view the view
     */
    public static void viewPrint(View view)
    {
        System.out.print(view);

        String text = tryGetText(view);
        if (text != null)
        {
            System.out.print(": " + text);
        }

        System.out.print(" (" + getVisibilityString(view) + ")");
    }


    // ----------------------------------------------------------
    /**
     * Prints a human-readable description of the specified view to standard
     * output, followed by a newline.
     *
     * @param view the view
     */
    public static void viewPrintln(View view)
    {
        viewPrint(view);
        System.out.println();
    }
}
