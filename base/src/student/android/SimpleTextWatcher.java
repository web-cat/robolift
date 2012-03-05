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

import android.text.Editable;
import android.text.TextWatcher;

// -------------------------------------------------------------------------
/**
 * This class simplifies the usaage of {@link TextWatcher}s in Android. The
 * {@code TextWatcher} interface has three methods that must be implemented,
 * when most users only need one,
 * {@link TextWatcher#onTextChanged(CharSequence, int, int, int)}. This class
 * implements the interface and provides empty implementations for the other
 * two methods so that you only have to implement {@code onTextChanged}.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public abstract class SimpleTextWatcher implements TextWatcher
{
    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * This method is called to notify you that, somewhere within {@code s},
     * the text has been changed.
     */
    public void afterTextChanged(Editable s)
    {
        // Do nothing.
    }


    // ----------------------------------------------------------
    /**
     * This method is called to notify you that, within {@code s}, the
     * {@code count} characters beginning at {@code start} are about to be
     * replaced by new text with length {@code after}.
     *
     * It is an error to attempt to make changes to {@code s} from this
     * callback.
     */
    public void beforeTextChanged(CharSequence s, int start, int count,
        int after)
    {
        // Do nothing.
    }


    // ----------------------------------------------------------
    /**
     * This method is called to notify you that, within {@code s}, the
     * {@code count} characters beginning at {@code start} have just replaced
     * old text that had length {@code before}.
     *
     * It is an error to attempt to make changes to {@code s} from this
     * callback.
     */
    public abstract void onTextChanged(CharSequence s, int start, int before,
                                       int count);
}
