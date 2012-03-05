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

package student;

import android.content.IntentFilter.MalformedMimeTypeException;
import org.junit.After;
import student.testingsupport.shadows.ShadowActivity;
import android.app.Instrumentation.ActivityMonitor;
import android.app.Instrumentation.ActivityResult;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import java.util.Iterator;
import student.android.MediaUtils;
import android.app.Activity;
import android.os.SystemClock;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowDialog;
import com.xtremelabs.robolectric.tester.android.view.TestMenu;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;

import student.android.internal.AndroidViewFilter;
import student.android.internal.AndroidViewFinder;

//-------------------------------------------------------------------------
/**
 * <p>
 * This class provides a more student-friendly interface for testing Android
 * activities than the standard Android class that it extends,
 * {@link android.test.ActivityInstrumentationTestCase2}. It contains methods
 * that simplify looking up views in your activity and  simulating interactions
 * such as clicks, touches, and key input.
 * </p><p>
 * This class is a generic class; it takes a type parameter indicating the
 * class name of the activity to be tested. This allows the inherited
 * {@link #getActivity()} method to return the correct type of activity,
 * without requiring you to cast it, if you need to call methods that belong
 * to your activity class.
 * </p><p>
 * To use this class, you must also provide a no-argument constructor that
 * passes your activity class to the superclass constructor. For example, if
 * the activity that you want to test is named {@code MyActivity}, you could
 * define your test class as follows:
 * </p>
 * <pre>
 * public class MyActivityTest extends student.AndroidTestCase&lt;MyActivity&gt;
 * {
 *     public MyActivityTest()
 *     {
 *         super(MyActivity.class);
 *     }
 *
 *     // Define your setUp() and test*() methods...
 * }
 * </pre>
 * <p>
 * Notice that the test class used as a generic type parameter in the
 * {@code extends} clause is the same as the class passed to {@code super()}.
 * </p><p>
 * This class also duplicates all of the customized behavior and added
 * assertions that are provided by the {@link student.TestCase} class, with the
 * exception of manipulating standard input/output.
 * </p>
 *
 * @param <ActivityType> The name of the activity class that this test case
 *     will be testing.
 *
 * @author  Tony Allevato
 * @author  Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class ActivityTestCase<ActivityType extends Activity>
    extends TestCase
{
    //~ Constants .............................................................

    /**
     * This field re-exports the <code>where</code> operator from
     * {@link student.android.internal.AndroidViewFilter} so that it is available
     * in test methods without requiring a static import.
     */
    public final AndroidViewFilter.Operator where =
        AndroidViewFilter.ClientImports.where;


    //~ Static/instance variables .............................................

    // Initialized in a static initializer at the bottom of this class.
    //private static Class<?> windowManager;

    private Class<?> cachedIdClass;

    private Class<ActivityType> activityClass;
    private ActivityType activity;

    // Used to remember state for the touchUp method.
    private View lastTouchView;
    private float lastTouchX;
    private float lastTouchY;


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new {@code AndroidTestCase} object.
     *
     * @param activityClass the class of the activity being tested, which
     *     should match the generic parameter given when you extended
     *     {@code AndroidTestCase}
     */
    @SuppressWarnings("unchecked")
    public ActivityTestCase(Class<? extends ActivityType> activityClass)
    {
        this.activityClass = (Class<ActivityType>) activityClass;
    }


    // ----------------------------------------------------------
    /**
     * This constructor is only intended to be used by instructors writing
     * reference tests. Students should not use it.
     *
     * @param activityClassName the fully-qualified class name of the activity
     *     being tested
     */
    public ActivityTestCase(String activityClassName)
    {
        this(ActivityTestCase.<ActivityType> activityClassForName(
                activityClassName));
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Override the default {@code TestCase.runBare()} to ensure proper test
     * harness setup and tear down that won't likely be accidentally overridden
     * by a derived class. Specifically, we ensure that the activity under test
     * always starts in touch mode.
     *
     * @throws Throwable if an exception occurred
     */
    public void runBare() throws Throwable
    {
        super.runBare();
    }


    // ----------------------------------------------------------
    /**
     * Resets the cached information about the last touch that was simulated.
     */
    @Before
    public void resetLastTouch()
    {
        lastTouchView = null;
    }


    // ----------------------------------------------------------
    /**
     * TODO document
     */
    @After
    public void checkForUnhitActivities()
    {
        ShadowActivity shadow =
            (ShadowActivity) Robolectric.shadowOf(getActivity());

        String failureReason = shadow.failureReasonIfNotAllMonitorsHit();
        if (failureReason != null)
        {
            fail(failureReason);
        }
    }


    // ----------------------------------------------------------
    /**
     * Creates the activity under test and invokes its onCreate method.
     *
     * @throws Throwable if an error occurred
     */
    @Before
    public void createActivity() throws Throwable
    {
        activity = activityClass.newInstance();

        Method onCreate = activityClass.getDeclaredMethod(
            "onCreate", android.os.Bundle.class);
        onCreate.setAccessible(true);

        onCreate.invoke(activity, new Object[] { null });
    }


    // ----------------------------------------------------------
    /**
     * Sets up the fixture, for example, open a network connection. This
     * method is called before each test in this class is executed.
     */
    @Override
    protected void setUp()
        throws Exception
    {
        // Included only for Javadoc purposes--implementation adds nothing.
        super.setUp();
    }


    // ----------------------------------------------------------
    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after each test in this class is executed.
     */
    @Override
    protected void tearDown()
        throws Exception
    {
        // Included only for Javadoc purposes--implementation adds nothing.
        super.tearDown();
    }


    // ----------------------------------------------------------
    /**
     * Gets the activity under test.
     *
     * @return the activity under test
     */
    public ActivityType getActivity()
    {
        return activity;
    }


    // ----------------------------------------------------------
    /**
     * The "not" operator for negating an existing filter, when the not
     * operation is at the very beginning of the expression, re-exported
     * from {@link student.android.internal.AndroidViewFilter} so that it is
     * available in test methods without requiring a static import.  This
     * method is designed to be used in expressions like
     * <code>not(where.enabledIs(true).or.hasFocusIs(true))</code>.
     *
     * @param otherFilter The filter to negate
     * @return A new filter that represents a combination of the left
     *         filter with "NOT otherFilter".
     */
    public static AndroidViewFilter not(AndroidViewFilter otherFilter)
    {
        return AndroidViewFilter.ClientImports.not(otherFilter);
    }


    // ----------------------------------------------------------
    /**
     * Look up all views in the activity being tested by specifying their
     * class. All matching objects are returned in a list.
     *
     * @param <T>  This method is a generic method, and the type T used as
     *             the <code>List</code> element type in the return value is
     *             implicitly deduced from the provided argument
     *             <code>type</codE>.
     * @param type The type (class) of the views you wish to retrieve, and
     *             also the way you specify the type of the elements in the
     *             list returned by this method.
     * @return A list of all views of the desired type that were found. This
     *         will be an empty list (not null) if no matching views are found.
     * @see #getView(Class)
     * @see #getFirstViewMatching(Class)
     */
    public <T extends View> List<T> getAllViewsMatching(Class<T> type)
    {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) getAllViewsMatching(where.typeIs(type));
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Look up all views in the activity being tested by specifying their
     * class and an {@link AndroidViewFilter}.  All matching objects are
     * returned in a list.
     *
     * @param <T>  This method is a generic method, and the type T used as
     *             the <code>List</code> element type in
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the views you wish to retrieve, and
     *             also the way you specify the type of elements in the
     *             list returned by this method.
     * @param filter The search criteria.
     * @return A list of all views found matching the criteria specified. This
     *         will be an empty list (not null) if no matching views are found.
     *
     * @see #getView(Class,AndroidViewFilter)
     * @see #getAllViewsMatching(Class,AndroidViewFilter)
     */
    public <T extends View> List<T> getAllViewsMatching(
        Class<T> type, AndroidViewFilter filter)
    {
        @SuppressWarnings("unchecked")
        List<T> result = (List<T>) getAllViewsMatching(
                where.typeIs(type).and(filter));
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Look up all views in the activity being tested by specifying
     * an {@link AndroidViewFilter}.
     * All matching objects are returned in a list.
     * This method is more general than
     * {@link #getAllViewsMatching(Class,AndroidViewFilter)}, since no
     * class needs to be specified, but that also means the return type
     * is less specific (it is always <code>List&lt;View&gt;</code>).
     * @param filter The search criteria.
     * @return A list of all views found matching the criteria specified.
     *         This will be an empty list (not null) if no matching views
     *         are found.
     * @see #getView(AndroidViewFilter)
     * @see #getAllViewsMatching(AndroidViewFilter)
     */
    public List<View> getAllViewsMatching(AndroidViewFilter filter)
    {
        return getViewFinder().find(filter);
    }


    // ----------------------------------------------------------
    /**
     * Look up a view in the activity being tested by specifying its class.
     * This method expects the given class to identify at least one such
     * view. If no matching view exists, the test case will fail with an
     * appropriate message. If more than one matching view exists, the first
     * one found will be returned (although client code should not expect a
     * specific search order).
     *
     * @param <T>  This method is a generic method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the view you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @return The first view of the desired type that was found
     *         (a test case failure results if there are none).
     * @see #getView(Class)
     * @see #getAllViewsMatching(Class)
     */
    public <T extends View> T getFirstViewMatching(Class<T> type)
    {
        @SuppressWarnings("unchecked")
        T result = (T) getFirstViewMatching(where.typeIs(type));
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Look up a view in the activity being tested by specifying its class
     * and an {@link AndroidViewFilter}.
     * This method expects the given criteria to identify at least one such
     * view. If no matching view exists, the test case will fail with an
     * appropriate message. If more than one matching view exists, the first
     * one found will be returned (although client code should not expect a
     * specific search order).
     *
     * @param <T>  This method is a generic method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the view you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @param filter The search criteria.
     * @return The first view that was found matching the criteria
     *         specified (a test case failure results if there are none).
     * @see #getView(Class,AndroidViewFilter)
     * @see #getAllViewsMatching(Class,AndroidViewFilter)
     */
    public <T extends View> T getFirstViewMatching(
        Class<T> type, AndroidViewFilter filter)
    {
        @SuppressWarnings("unchecked")
        T result = (T) getFirstViewMatching(where.typeIs(type).and(filter));
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Look up a view in the activity being tested by specifying
     * an {@link AndroidViewFilter}.  This method is more general
     * than {@link #getFirstViewMatching(Class,AndroidViewFilter)}, since no
     * class needs to be specified, but that also means the return type
     * is less specific (it is always <code>View</code>).
     * This method expects the given criteria to identify at least one such
     * view. If no matching view exists, the test case will fail with an
     * appropriate message.  If more than one matching view exists, the first
     * one found will be returned (although client code should not expect a
     * specific search order).
     *
     * @param filter The search criteria.
     * @return The first view that was found matching the criteria
     *         specified (a test case failure results if there are none).
     * @see #getView(AndroidViewFilter)
     * @see #getAllViewsMatching(AndroidViewFilter)
     */
    public View getFirstViewMatching(AndroidViewFilter filter)
    {
        View result = null;

        List<View> views = getViewFinder().find(filter);

        if (views.size() == 0)
        {
            fail("Cannot find component matching: " + filter);
        }
        else
        {
            result = views.get(0);
        }

        return result;
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Look up a view in the activity being tested by specifying its class.
     * This method expects the given class to identify a unique view,
     * meaning that there should only be one instance of the given class
     * in the entire layout of the activity.  This can be useful if, for
     * example, you are trying to retrieve a custom view object created from a
     * class you only instantiate once.
     * </p><p>
     * If no matching view exists, the test case will fail with an
     * appropriate message.  If more than one matching view exists,
     * the test case will fail with an appropriate message.
     * </p>
     *
     * @param <T>  This method is a generic method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the view you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @return The single view of the desired type that was found
     *         (otherwise, a test case failure results).
     * @see #getFirstViewMatching(Class)
     * @see #getAllViewsMatching(Class)
     */
    public <T extends View> T getView(Class<T> type)
    {
        @SuppressWarnings("unchecked")
        T result = (T) getView(where.typeIs(type));
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Look up a view in the GUI of the activity being tested by specifying its
     * class and its ID.
     *
     * @param <T> This method is a generic method, and the type T used for the
     *     return value is deduced from the provided argument {@code type}.
     * @param type The type (class) of the view you wish to receive, and also
     *     the way you specify the return type of this method.
     * @param id The ID of the desired component.
     * @return The single view matching the criteria specified (otherwise, a
     *     test case failure results).
     */
    public <T extends View> T getView(Class<T> type, int id)
    {
        // Unlike the other overloads of this method, this one is implemented
        // as a thin wrapper around Activity.findViewById, for consistency with
        // standard behavior.

        @SuppressWarnings("unchecked")
        T view = (T) getActivity().findViewById(id);

        if (view == null)
        {
            fail("Cannot find view with id \"" + getFieldNameForId(id) + "\"");
        }

        return view;
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Look up a view in the GUI of the activity being tested by specifying its
     * class and the name of the {@code R.id} field that represents its ID.
     * This method is mainly intended for instructors so that they can specify
     * the names of view IDs for reference testing without requiring that the
     * integer values themselves match up for every student.
     * </p><p>
     * The following two statements are equivalent, assuming that the
     * {@code R.id} being referenced below is the one in the application's
     * package:
     * <pre>
     *     getView(SomeView.class, R.id.foo)
     *     getView(SomeView.class, "foo")
     * </pre>
     * </p><p>
     * If you wish to combine this lookup with other filters, you must call
     * {@link #getIdByName(String)} directly and pass that into
     * {@link AndroidViewFilter.Operator#idIs(int)}.
     * </p>
     *
     * @param <T> This method is a generic method, and the type T used for the
     *     return value is deduced from the provided argument {@code type}.
     * @param type The type (class) of the view you wish to receive, and also
     *     the way you specify the return type of this method.
     * @param id The field name representing the ID of the desired component.
     * @return The single view matching the criteria specified (otherwise, a
     *     test case failure results).
     */
    public <T extends View> T getView(Class<T> type, String id)
    {
        return getView(type, getIdByName(id));
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Look up a view in the activity being tested by specifying its class
     * and an {@link AndroidViewFilter}.
     * </p><p>
     * This method expects exactly one view to match your criteria.
     * If no matching view exists, the test case will fail with an
     * appropriate message.  If more than one matching view exists,
     * the test case will fail with an appropriate message.
     * </p>
     *
     * @param <T>  This method is a generic method, and the type T used for
     *             the return value is implicitly deduced from the provided
     *             argument <code>type</code>.
     * @param type The type (class) of the view you wish to retrieve, and
     *             also the way you specify the return type of this method.
     * @param filter The search criteria.
     * @return The single view matching the criteria specified
     *         (otherwise, a test case failure results).
     * @see #getFirstViewMatching(Class,AndroidViewFilter)
     * @see #getAllViewsMatching(Class,AndroidViewFilter)
     */
    public <T extends View> T getView(Class<T> type, AndroidViewFilter filter)
    {
        @SuppressWarnings("unchecked")
        T result = (T) getView(where.typeIs(type).and(filter));
        return result;
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Look up a view in the activity being tested, using a filter to specify
     * which view you want.  This method is more general than
     * {@link #getView(Class,AndroidViewFilter)}, since no class needs to be
     * specified, but that also means the return type is less specific (it is
     * always <code>View</code>).
     * </p><p>
     * This method expects the given filter to identify a unique component. If
     * no matching component exists, the test case will fail with an
     * appropriate message.  If more than one matching component exists, the
     * test case will fail with an appropriate message.
     * </p>
     *
     * @param filter The search criteria.
     * @return The single view matching the provided filter (otherwise, a
     *         test case failure results).
     * @see #getFirstViewMatching(AndroidViewFilter)
     * @see #getAllViewsMatching(AndroidViewFilter)
     */
    public View getView(AndroidViewFilter filter)
    {
        List<View> views = getViewFinder().find(filter);
        View result = null;

        if (views.size() == 0)
        {
            fail("Cannot find view matching: " + filter);
        }
        else if (views.size() > 1)
        {
            fail("Found " + views.size() + " views matching: " + filter);
        }
        else
        {
            result = views.get(0);
        }

        return result;
    }


    //~ Basic Component Action Methods ........................................

    // ----------------------------------------------------------
    /**
     * Simulates clicking the specified view as if the user had tapped on it
     * with his or her finger. This method can be used when you don't care
     * which particular location on the view is clicked (for example, if you
     * are clicking a button).
     *
     * @param view The view to click.
     */
    public void click(View view)
    {
        view.performClick();
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Simulates clicking the specified view as if the user had tapped on it
     * with his or her finger in the specified location. This method is
     * identical to calling {@link #touchDown(View,float,float)} immediately
     * followed by {@link #touchUp()}.
     * </p><p>
     * This method is intended for clicking on views where the position of the
     * click matters (for example, a canvas where you are drawing shapes based
     * on touch position). If you want to click a widget like a button, then
     * just use {@link #click(View)} instead.
     * </p>
     *
     * @param view The view to click
     * @param x the x-coordinate to click, relative to the view
     * @param y the y-coordinate to click, relative to the view
     */
    public void click(View view, float x, float y)
    {
        touchDown(view, x, y);
        touchUp();
    }


    // ----------------------------------------------------------
    /**
     * Simulates a one-finger drag from one point in the view to another. The
     * drag only generates motion events for the start and the end of the drag,
     * not for any positions in between.
     *
     * @param view The view to drag in.
     * @param xStart The x-coordinate of the start of the drag.
     * @param yStart The y-coordinate of the start of the drag.
     * @param xEnd The x-coordinate of the end of the drag.
     * @param yEnd The y-coordinate of the end of the drag.
     */
    public void drag(View view, float xStart, float yStart,
                     float xEnd, float yEnd)
    {
        drag(view, xStart, yStart, xEnd, yEnd, 1);
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Simulates a one-finger drag from one point in the view to another. The
     * drag will also generate a number of intermediate motion events between
     * the start and end points, as specified by the {@code steps} parameter.
     * </p><p>
     * If you need more control over the drag operation (for example, dragging
     * a path that is not a straight line), then you should use a combination
     * of {@link #touchDown(View,float,float)},
     * {@link #touchMove(float, float)}, and {@link #touchUp()} instead.
     * </p>
     *
     * @param view The view to drag in.
     * @param xStart The x-coordinate of the start of the drag.
     * @param yStart The y-coordinate of the start of the drag.
     * @param xEnd The x-coordinate of the end of the drag.
     * @param yEnd The y-coordinate of the end of the drag.
     * @param steps The number of intermediate motion events to generate,
     *     including the final one that represents lifting the finger.
     */
    public void drag(View view, float xStart, float yStart,
                     float xEnd, float yEnd, int steps)
    {
        // TODO Handle steps by interpolating moves.

        touchDown(view, xStart, yStart);
        touchMove(xEnd, yEnd);
        touchUp();
    }


    // ----------------------------------------------------------
    /**
     * Type the given text into the given component, replacing any existing
     * text already there. If the empty string or {@code null} is given, then
     * this method simply removes all existing text.
     *
     * @param view The {@code EditText} view to enter text on.
     * @param text The text to enter on the view.
     */
    public void enterText(final EditText view, String text)
    {
        // TODO This implementation may not be ideal from a testing perspective
        // since it sets the text of the field directly (so it may not fire the
        // appropriate TextWatcher or onKey* notifications). Investigate
        // sending raw KeyEvents instead, as the Android instrumentation does.

        if (text != null && text.length() > 0)
        {
            view.setText(text);
        }
        else
        {
            view.setText("");
        }
    }


    // ----------------------------------------------------------
    /**
     * Give the focus to a specific view.
     *
     * @param view The view to receive focus.
     */
    public void focus(final View view)
    {
        getActivity().runOnUiThread(new Runnable() {
            public void run()
            {
                view.requestFocus();
            }
        });
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Prepares to select a media file with the specified filename in the next
     * media chooser activity that is started. When the chooser is invoked, it
     * will be intercepted and a result will be automatically sent to the
     * invoking activity that contains the {@code Uri} of the specified file.
     * </p><p>
     * This method must be called a some point <em>before</em> the action that
     * starts the media chooser. For example, if you have a button with ID
     * {@code fooButton} that starts the media chooser when clicked, you must
     * order the statements as follows:
     * <pre>
     *     prepareToSelectMediaInChooser("my_image.jpg");
     *     click(getView(Button.class, R.id.fooButton));
     * </pre>
     * </p><p>
     * A test case failure will result if no media could be found with the
     * specified filename, if the chooser activity never started before the
     * end of the test, or if some other error occurred when trying to start
     * the chooser.
     * </p>
     *
     * @param filename the filename of the media to be selected
     */
    public void prepareToSelectMediaInChooser(String filename)
    {
        ContentResolver resolver = getActivity().getContentResolver();
        Uri uri = MediaUtils.uriForMediaWithFilename(resolver, filename);

        assertNotNull("Could not find an item named \"" + filename + "\""
            + " in the media library.", uri);

        prepareToSelectInChooser(uri);
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Prepares to select an item with the specified {@code Uri} in the next
     * chooser activity that is started. When the chooser is invoked, it will
     * be intercepted and a result will be automatically sent to the invoking
     * activity that contains the specified {@code Uri}.
     * </p><p>
     * This method must be called a some point <em>before</em> the action that
     * starts the chooser. For example, if you have a button with ID
     * {@code fooButton} that starts the chooser when clicked, you must order
     * the statements as follows:
     * <pre>
     *     prepareToSelectInChooser(someUri);
     *     click(getView(Button.class, R.id.fooButton));
     * </pre>
     * </p><p>
     * A test case failure will result if the chooser activity never started
     * before the end of the test, or if some other error occurred when trying
     * to start the chooser.
     * </p>
     *
     * @param uri the {@code Uri} of the data to be selected
     */
    public void prepareToSelectInChooser(Uri uri)
    {
        Intent intent = new Intent();
        intent.setData(uri);

        IntentFilter filter = new IntentFilter(Intent.ACTION_GET_CONTENT);

        try
        {
            filter.addDataType("*/*");
        }
        catch (MalformedMimeTypeException e)
        {
            // Do nothing.
        }

        prepareUpcomingActivityResult(filter, Activity.RESULT_OK, intent,
            "Expected the media chooser to start, but it never did.");
    }


    // ----------------------------------------------------------
    public void prepareForUpcomingActivity(String action)
    {
        IntentFilter intentFilter = new IntentFilter(action);

        ShadowActivity shadowActivity =
            (ShadowActivity) Robolectric.shadowOf(getActivity());
        shadowActivity.addPendingResult(intentFilter, 0, null,
            "Expected an activity with the intent action "
            + action + " to start, but one never did.");
    }


    // ----------------------------------------------------------
    private void prepareUpcomingActivityResult(IntentFilter intentFilter,
        int resultCode, Intent data, String reason)
    {
        ShadowActivity shadowActivity =
            (ShadowActivity) Robolectric.shadowOf(getActivity());
        shadowActivity.addPendingResult(intentFilter, resultCode, data, reason);

        String failureReason;

        if (reason == null)
        {
            StringBuffer buffer = new StringBuffer("{ ");
            Iterator<String> it = intentFilter.actionsIterator();

            while (it.hasNext())
            {
                buffer.append(it.next());

                if (it.hasNext())
                {
                    buffer.append(", ");
                }
            }

            buffer.append(" }");

            failureReason = "Expected an activity with intent actions "
                + buffer.toString() + " to start, but one never did.";
        }
        else
        {
            failureReason = reason;
        }
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Prepares for an activity to start as a result of an upcoming action in
     * the test. When the activity is invoked, it will be intercepted and a
     * result will be automatically sent to the invoking activity that contains
     * the specified result code and intent.
     * </p><p>
     * This method must be called a some point <em>before</em> the action that
     * starts the activity. For example, if you have a button with ID
     * {@code fooButton} that starts the activity when clicked, you must order
     * the statements as follows:
     * <pre>
     *     prepareUpcomingActivityResult(ExampleActivity.class,
     *         Activity.RESULT_OK, someIntent);
     *     click(getView(Button.class, R.id.fooButton));
     * </pre>
     * </p><p>
     * A test case failure will result if the activity never started before the
     * end of the test, or if some other error occurred when trying to start
     * the activity.
     * </p>
     *
     * @param activityClass the activity class to prepare for and intercept
     * @param resultCode the result code that the activity should send (usually
     *     {@code Activity.RESULT_OK} or {@code Activity.RESULT_CANCELLED}
     * @param data an {@code Intent} that represents data that the activity
     *     should return as part of its result
     */
    public void prepareUpcomingActivityResult(
        Class<? extends Activity> activityClass, int resultCode, Intent data)
    {
        prepareUpcomingActivityResult(activityClass.getCanonicalName(),
            resultCode, data);
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Prepares for an activity to start as a result of an upcoming action in
     * the test. When the activity is invoked, it will be intercepted and a
     * result will be automatically sent to the invoking activity that contains
     * the specified result code and intent.
     * </p><p>
     * This method must be called a some point <em>before</em> the action that
     * starts the activity. For example, if you have a button with ID
     * {@code fooButton} that starts the activity when clicked, you must order
     * the statements as follows:
     * <pre>
     *     prepareUpcomingActivityResult("cs2114.example.ExampleActivity",
     *         Activity.RESULT_OK, someIntent);
     *     click(getView(Button.class, R.id.fooButton));
     * </pre>
     * </p><p>
     * A test case failure will result if the activity never started before the
     * end of the test, or if some other error occurred when trying to start
     * the activity.
     * </p>
     *
     * @param activityClassName the fully-qualified name of the activity class
     *     to prepare for and intercept
     * @param resultCode the result code that the activity should send (usually
     *     {@code Activity.RESULT_OK} or {@code Activity.RESULT_CANCELLED}
     * @param data an {@code Intent} that represents data that the activity
     *     should return as part of its result
     */
    public void prepareUpcomingActivityResult(String activityClassName,
        int resultCode, Intent data)
    {
        ShadowActivity shadowActivity =
            (ShadowActivity) Robolectric.shadowOf(getActivity());
        shadowActivity.addPendingResult(activityClassName, resultCode, data);
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Looks for an item with exactly the specified text in a {@code ListView}
     * that is currently open in a Dialog or a Spinner, scrolling if necessary
     * to find the item.
     * </p><p>
     * This method does not look for lists that are part of the main activity
     * window, only those that are popped up by a Dialog or a Spinner; if such
     * a ListView cannot be found, a test case failure will result. To select
     * an item in a specific ListView, use
     * {@link #selectItemInList(AbsListView, String)} instead.
     * </p>
     *
     * @param text the exact text of the item to select
     */
    public void selectItemInList(String text)
    {
        selectItemInList(where.textIs(text));
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Looks for an item matching the specified filter in a {@code ListView}
     * that is currently open in a Dialog or a Spinner, scrolling if necessary
     * to find the item.
     * </p><p>
     * This method does not look for lists that are part of the main activity
     * window, only those that are popped up by a Dialog or a Spinner; if such
     * a ListView cannot be found, a test case failure will result. To select
     * an item in a specific ListView, use
     * {@link #selectItemInList(AbsListView, String)} instead.
     * </p>
     *
     * @param filter the search criteria
     */
    public void selectItemInList(AndroidViewFilter filter)
    {
        AbsListView listView = null;

        List<View> decorViews = getAllDecorViews();
        for (int i = decorViews.size() - 1; i >= 1; i++)
        {
            View decorView = decorViews.get(i);

            AndroidViewFinder finder = new AndroidViewFinder(decorView);

            List<View> listViews = finder.find(
                where.typeIs(AbsListView.class).and.visibilityIs(true));

            if (listViews.size() > 0)
            {
                listView = (AbsListView) listViews.get(0);
                break;
            }
        }

        assertNotNull("Cannot find a ListView that is in a currently opened "
            + "Dialog or Spinner.", listView);

        selectItemInList(listView, filter);
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Looks for an item with exactly the specified text in a particular
     * ListView, scrolling if necessary to find the item.
     * </p><p>
     * This method is most useful when you already know the ListView that has
     * the item that you wish to select. If you want to select an item in the
     * currently opened Dialog or Spinner, use {@link #selectItemInList(String)}
     * instead.
     * </p>
     *
     * @param listView the ListView containing the item you want to select
     * @param text the exact text of the item to select
     */
    public void selectItemInList(AbsListView listView, String text)
    {
        selectItemInList(listView, where.textIs(text));
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Looks for an item with exactly the specified text in a particular
     * ListView, scrolling if necessary to find the item.
     * </p><p>
     * This method is most useful when you already know the ListView that has
     * the item that you wish to select. If you want to select an item in the
     * currently opened Dialog or Spinner, use
     * {@link #selectItemInList(AndroidViewFilter)} instead.
     * </p>
     *
     * @param listView the ListView containing the item you want to select
     * @param filter the search criteria
     */
    public void selectItemInList(AbsListView listView, AndroidViewFilter filter)
    {
        /*ScrollingAndroidViewFinder finder =
            new ScrollingAndroidViewFinder(getInstrumentation(), listView);

        View toSelect = finder.findFirst(filter);

        assertNotNull("Cannot find item in ListView matching: " + filter,
            toSelect);

        click(toSelect);*/
    }


    // ----------------------------------------------------------
    /**
     * Simulates selecting the menu item with the specified ID from the options
     * menu. A test case failure will result if the menu item is disabled.
     *
     * @param id the id of the menu item to select
     */
    public void selectOptionsMenuItem(int id)
    {
        TestMenu menu = new TestMenu(activity);
        boolean success = activity.onCreateOptionsMenu(menu);

        assertTrue("The activity's options menu was not created.", success);

        MenuItem item = null;

        for (int i = 0; i < menu.size(); i++)
        {
            MenuItem currentItem = menu.getItem(i);

            if (currentItem.getItemId() == id)
            {
                item = currentItem;
                break;
            }
        }

        assertNotNull("Could not find a menu item with id \"" +
            getFieldNameForId(id) + "\"", item);

        activity.onOptionsItemSelected(item);
    }


    // ----------------------------------------------------------
    /**
     * <p>
     * Simulates selecting the menu item with the specified ID from the options
     * menu. A test case failure will result if the menu item is disabled. The
     * ID is given as the name of the {@code R.id} field. This method is mainly
     * intended for instructors so that they can specify the names of menu item
     * IDs for reference testing without requiring that the integer values
     * themselves match up for every student.
     * </p><p>
     * The following two statements are equivalent, assuming that the
     * {@code R.id} being referenced below is the one in the application's
     * package:
     * <pre>
     *     selectOptionsMenuItem(R.id.foo)
     *     selectOptionsMenuItem("foo")
     * </pre>
     * </p>
     *
     * @param id the ID of the menu item to select
     */
    public void selectOptionsMenuItem(String id)
    {
        selectOptionsMenuItem(getIdByName(id));
    }


    // ----------------------------------------------------------
    /**
     * Simulates pressing a finger down on the screen in the specified view,
     * at the coordinates relative to the origin of that view.
     *
     * @param view the view to touch
     * @param x the x-coordinate of the touch, relative to the origin of the
     *     view
     * @param y the y-coordinate of the touch, relative to the origin of the
     *     view
     */
    public void touchDown(View view, float x, float y)
    {
        assertNull("touchDown should only be called if a previous touch was "
            + "ended by calling touchUp.", lastTouchView);

        lastTouchX = x;
        lastTouchY = y;
        lastTouchView = view;

        sendMotionEvent(view, MotionEvent.ACTION_DOWN, x, y);
    }


    // ----------------------------------------------------------
    /**
     * Simulates moving the finger to the specified coordinates, relative to
     * the origin of the last view that was touched by
     * {@link #touchDown(View,float,float)}.
     *
     * @param x the x-coordinate of the touch, relative to the origin of the
     *     view
     * @param y the y-coordinate of the touch, relative to the origin of the
     *     view
     */
    public void touchMove(float x, float y)
    {
        assertNotNull("touchMove should only be called between touchDown and "
            + "touchUp.", lastTouchView);

        lastTouchX = x;
        lastTouchY = y;

        sendMotionEvent(lastTouchView, MotionEvent.ACTION_MOVE, x, y);
    }


    // ----------------------------------------------------------
    /**
     * Simulates lifting the finger up from the screen at the end of a touch
     * operation.
     */
    public void touchUp()
    {
        assertNotNull("touchUp should only be called after touchDown.",
            lastTouchView);

        sendMotionEvent(
                lastTouchView, MotionEvent.ACTION_UP, lastTouchX, lastTouchY);

        lastTouchView = null;
    }


    //~ Protected Methods .....................................................

    // ----------------------------------------------------------
    /**
     * Gets the {@link AndroidViewFinder} that can be used to traverse the view
     * hierarchy of the activity and find subviews that meet certain criteria.
     *
     * @return the {@link AndroidViewFinder} to use for the test methods
     */
    protected AndroidViewFinder getViewFinder()
    {
        return new AndroidViewFinder(getAllDecorViews());
    }


    //~ Private Methods .......................................................

    // ----------------------------------------------------------
    /**
     * Gets the activity class with the specified fully-qualified name. Causes
     * test initialization to fail with an appropriate hint message if the
     * class is not found.
     *
     * @param className the fully-qualified class name of the activity
     * @return the Java class with the specified className
     */
    private static <T> Class<T> activityClassForName(String className)
    {
        try
        {
            @SuppressWarnings("unchecked")
            Class<T> klass = (Class<T>) Class.forName(className);

            assertTrue("The class " + className + " is not a subclass of "
                    + "android.app.Activity.",
                    android.app.Activity.class.isAssignableFrom(klass));

            return klass;
        }
        catch (Exception e)
        {
            fail("Could not find the activity class " + className + ".");
            return null;
        }
    }


    // ----------------------------------------------------------
    /**
     * A helper method to retrieve the {@code R.id} class in the application's
     * package, so that the names of numeric IDs can be looked up. If such a
     * class cannot be found, a test case failure will occur.
     *
     * @return the {@code Class} object corresponding to the R.id class in the
     *     application's package.
     */
    private Class<?> idClass()
    {
        // TODO Replace this with new reflection introspection code instead.

        if (cachedIdClass == null)
        {
            String packageName = getActivity().getPackageName();

            try
            {
                cachedIdClass = Class.forName(packageName + ".R$id");
            }
            catch (ClassNotFoundException e)
            {
                fail("Cannot find the R.id class in the application package "
                        + packageName);
            }
        }

        return cachedIdClass;
    }


    // ----------------------------------------------------------
    /**
     * A helper method to retrieve the integer value of the field in the
     * {@code R.id} class with the specified name. If there is no field with
     * the specified name, the test case fails.
     *
     * @param fieldName the name of the ID field
     * @return the numeric value of the ID
     */
    public int getIdByName(String fieldName)
    {
        // TODO Replace this with new reflection introspection code instead.

        try
        {
            Field idField = idClass().getField(fieldName);
            return idField.getInt(null);
        }
        catch (Exception e)
        {
            fail("Cannot find a resource ID with name \"" + fieldName + "\"");
            return 0;
        }
    }


    // ----------------------------------------------------------
    /**
     * A helper method to retrieve the field name in the {@code R.id} class
     * of the ID that has the specified value. This is used to provide
     * human-readable messages when searches fail. If no field matches the ID,
     * then the test case fails.
     *
     * @param id The numeric ID of a view, defined in the {@code R.id} class.
     * @return The field name of the specified ID.
     */
    private String getFieldNameForId(int id)
    {
        // TODO Replace this with new reflection introspection code instead.

        Field[] idFields = idClass().getFields();

        for (Field idField : idFields)
        {
            int modifiers = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;

            if (idField.getType().equals(int.class)
                    && (idField.getModifiers() & modifiers) == modifiers)
            {
                try
                {
                    int value = idField.getInt(null);

                    if (value == id)
                    {
                        return idField.getName();
                    }
                }
                catch (Exception e)
                {
                    // Do nothing.
                }
            }
        }

        fail("Cannot find an id in the R.id class with value 0x"
                + Integer.toString(id, 16));

        return null;
    }


    // ----------------------------------------------------------
    /**
     * A convenience method for obtaining motion events, which computes
     * coordinates relative to a view instead of using absolute screen
     * coordinates.
     *
     * @param view the view associated with the touch event
     * @param action the action (one of the MotionEvent.ACTION_* constants)
     * @param x the x-coordinate of the motion event
     * @param y the y-coordinate of the motion event
     */
    private void sendMotionEvent(View view, int action, float x, float y)
    {
        long uptime = SystemClock.uptimeMillis();

        int[] screenOffset = new int[2];
        view.getLocationOnScreen(screenOffset);

        MotionEvent event = MotionEvent.obtain(uptime, uptime, action,
                x + screenOffset[0], y + screenOffset[1], 0);

        view.onTouchEvent(event);
        view.dispatchTouchEvent(event);
    }


    // ----------------------------------------------------------
    /**
     * Gets a list of all the decor views currently visible on the screen. This
     * list has the activity's decor view at position 0, and following that are
     * other views that might be visible on top of it, such as a Spinner's
     * drop-down list, a Dialog, or a Toast view.
     *
     * @return a {@code List} of all decor views currently visible
     */
    private List<View> getAllDecorViews()
    {
        ArrayList<View> views = new ArrayList<View>();

        // Add the current dialog's view, if any.
        try
        {
            ShadowDialog dialog =
                Robolectric.shadowOf(ShadowDialog.getLatestDialog());

            if (dialog != null)
            {
                dialog.findViewById(-1);

                Field field =
                    ShadowDialog.class.getDeclaredField("inflatedView");
                field.setAccessible(true);
                View view = (View) field.get(dialog);

                if (view != null)
                {
                    views.add(view);
                }
            }
        }
        catch (Exception e)
        {
            // Do nothing.
        }

        // Add the activity's content view itself.
        views.add(Robolectric.shadowOf(getActivity()).getContentView());

        return views;
    }
}
