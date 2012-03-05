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

package student.android;

import android.content.Context;
import android.widget.ArrayAdapter;

// -------------------------------------------------------------------------
/**
 * A simple adapter that can easily be used to easily display an array of items
 * in a Spinner widget, with the standard type of view for the drop-down part
 * of the widget and the items in the pop-up list.
 *
 * @param <T> The type of object stored in the adapter.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class SimpleSpinnerAdapter<T> extends ArrayAdapter<T>
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new SimpleSpinnerAdapter with the specified context and
     * items.
     *
     * @param context the context
     * @param items the items
     */
    public SimpleSpinnerAdapter(Context context, T... items)
    {
        super(context, android.R.layout.simple_spinner_item, items);

        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }
}
