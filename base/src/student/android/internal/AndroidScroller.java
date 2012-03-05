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

import java.util.concurrent.Callable;
import android.app.Instrumentation;
import android.widget.AbsListView;
import android.view.View;

// -------------------------------------------------------------------------
/**
 * <p>
 * Automates scrolling of various types of views in Android, for testing
 * purposes. Currently, only {@link android.widget.ListView} is supported; all
 * other types of views return a "null" scroller that provides the same
 * interface but does not actually do any scrolling. In the future, more types
 * of views will be supported.
 * </p><p>
 * In most cases, students won't need to use this class directly. It is used
 * by methods like {@link student.AndroidTestCase#selectItemInList(String)} to
 * perform the scrolling necessary to find an item.
 * </p>
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public abstract class AndroidScroller
{
    //~ Constants .............................................................

    // ----------------------------------------------------------
    /**
     * The direction to scroll.
     */
    public enum Direction
    {
        /** Scroll up (not currently supported). */
        UP,

        /** Scroll down. */
        DOWN
    }


    //~ Static/instance variables .............................................

    /** The view being scrolled. */
    protected View view;

    /** The instrumentation. */
    protected Instrumentation instrumentation;


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Creates a scroller for the specified view. Currently, only
     * {@link android.widget.ListView} is supported.
     *
     * @param view the view to obtain a scroller for
     * @param instrumentation the instrumentation
     * @return a scroller for the specified view
     */
    public static AndroidScroller createScroller(View view,
        Instrumentation instrumentation)
    {
        AndroidScroller scroller = null;

        if (view instanceof AbsListView)
        {
            scroller = new AbsListViewScroller();
        }
        else
        {
            scroller = new NullScroller();
        }

        scroller.view = view;
        scroller.instrumentation = instrumentation;
        return scroller;
    }


    // ----------------------------------------------------------
    /**
     * Scrolls the view in the specified direction. The amount of scrolling is
     * determined by the scroller itself, but in most cases this will be
     * roughly a page.
     *
     * @param direction the direction to scroll
     * @return true if more scrolling is possible after this operation is
     *     complete, or false if the end of the scrollable area has been
     *     reached
     */
    public abstract boolean scroll(Direction direction);


    // ----------------------------------------------------------
    /**
     * Scrolls to the end of the view, repeatedly calling the specified task
     * once for each page scroll that was performed, until the end of the
     * scrollable area is reached.
     *
     * @param task a {@code Callable<Boolean>} object that will be executed
     *     during scrolling. The task should return true to stop the task
     *     before scrolling is complete, or false to continue.
     */
    public void doWhileScrolling(Callable<Boolean> task)
    {
        boolean taskDone = false;

        try
        {
            taskDone = task.call();
        }
        catch (Exception e)
        {
            taskDone = true;
        }

        while (!taskDone && scroll(Direction.DOWN))
        {
            try
            {
                taskDone = task.call();
            }
            catch (Exception e)
            {
                taskDone = true;
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * A helper method to pause for 1/4 sec between scroll operations, wrapping
     * the call to {@link Thread#sleep(long)} in an empty exception handler.
     */
    protected void pause()
    {
        try
        {
            Thread.sleep(250);
        }
        catch (InterruptedException e)
        {
            // Do nothing.
        }
    }


    //~ Inner classes .........................................................

    // ----------------------------------------------------------
    /**
     * A "null" scroller that does not actually do any scrolling.
     */
    private static class NullScroller extends AndroidScroller
    {
        // ----------------------------------------------------------
        @Override
        public boolean scroll(Direction direction)
        {
            // Do nothing and immediately return that no more scrolling is
            // possible.
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * A scroller for ListViews.
     */
    private static class AbsListViewScroller extends AndroidScroller
    {
        // ----------------------------------------------------------
        @Override
        public boolean scroll(Direction direction)
        {
            AbsListView lv = (AbsListView) view;

            if (direction == Direction.DOWN)
            {
                if (lv.getLastVisiblePosition() >= lv.getCount() - 1)
                {
                    scrollToLine(lv.getLastVisiblePosition());
                    return false;
                }

                if (lv.getFirstVisiblePosition() != lv.getLastVisiblePosition())
                {
                    scrollToLine(lv.getLastVisiblePosition());
                }
                else
                {
                    scrollToLine(lv.getFirstVisiblePosition() + 1);
                }
            }

            pause();
            return true;
        }


        // ----------------------------------------------------------
        private void scrollToLine(final int line)
        {
            instrumentation.runOnMainSync(new Runnable() {
                public void run()
                {
                    ((AbsListView) view).setSelection(line);
                }
            });
        }
    }
}
