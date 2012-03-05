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
import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.app.Instrumentation.ActivityResult;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.SystemClock;
import android.test.ActivityInstrumentationTestCase2;
import android.test.TouchUtils;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.EditText;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;
import student.android.MediaUtils;
import student.android.internal.AndroidViewFilter;
import student.android.internal.AndroidViewFinder;
import student.android.internal.ScrollingAndroidViewFinder;
import student.testingsupport.StringNormalizer;

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
    extends ActivityInstrumentationTestCase2<ActivityType>
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

    // Features copied from student.TestCase, which we unfortunately can't
    // extend since we must extend Android's ActivityInstrumentationTestCase2.
    private StringNormalizer       sn = new StringNormalizer(true);

    // Used for communicating with assertTrue() and assertFalse().  Ideally,
    // they should be instance vars, but assertTrue() and assertFalse()
    // have to be static so these messages must be too.
    private static String predicateReturnsTrueReason;
    private static String predicateReturnsFalseReason;

    private static Boolean trimStackTraces;

    // Initialized in a static initializer at the bottom of this class.
    private static Class<?> windowManager;

    private Class<?> cachedIdClass;

    private HashMap<ActivityMonitor, String> currentTestMonitors;

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
        super((Class<ActivityType>) activityClass);
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
        super(ActivityTestCase.<ActivityType> activityClassForName(
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
        predicateReturnsTrueReason = null;
        predicateReturnsFalseReason = null;
        lastTouchView = null;
        currentTestMonitors = new HashMap<ActivityMonitor, String>();

        setActivityInitialTouchMode(true);

        try
        {
            super.runBare();
        }
        finally
        {
            // Make sure any activity monitors are cleaned up even if the test
            // case throws an exception or otherwise fails.

            resetActivityMonitors();
        }

        // Check to see if any prepare* calls were unsatisfied. This will cause
        // a test case failure if true.
        checkForUnhitMonitors();
    }


    // ----------------------------------------------------------
    /**
     * Clear out old monitors from the instrumentation state -- it doesn't
     * appear that this happens automatically between test runs.
     */
    private void resetActivityMonitors()
    {
        for (ActivityMonitor monitor : currentTestMonitors.keySet())
        {
            getInstrumentation().removeMonitor(monitor);
        }

        currentTestMonitors.clear();
    }


    // ----------------------------------------------------------
    /**
     * Check to make sure that all monitors installed by prepare* calls
     * were hit. If not, then the student either has extraneous prepare*
     * calls or their solution code did not correctly start the activity
     * that the test was prepared for. In either case, a test case failure
     * should result.
     */
    private void checkForUnhitMonitors()
    {
        String firstFailure = null;

        for (ActivityMonitor monitor : currentTestMonitors.keySet())
        {
            String failureReason = currentTestMonitors.get(monitor);

            boolean hit = getInstrumentation().checkMonitorHit(monitor, 0);
            if (!hit && firstFailure == null)
            {
                firstFailure = failureReason;
            }
        }

        if (firstFailure != null)
        {
            fail(firstFailure);
        }
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
     * Access the string normalizer that this test case uses in
     * fuzzy string comparisons.  You can set your preferences for
     * fuzzy string comparisons using this object's methods.  These settings
     * are persistent from test case method to test case method, so it is
     * sufficient to set them in your test class constructor if you want
     * to use the same settings for all of your test case methods.
     * @return the string normalizer
     * @see #assertFuzzyEquals(String, String)
     * @see StringNormalizer
     * @see student.testingsupport.StringNormalizer#addStandardRules()
     */
    protected StringNormalizer stringNormalizer()
    {
        return sn;
    }


    // ----------------------------------------------------------
    /**
     * Asserts that two Strings are equal, respecting preferences for what
     * differences matter.  This method mirrors the static
     * {@link junit.framework.TestCase#assertEquals(String,String)}
     * method, augmenting its behavior with the ability to make "fuzzy"
     * string comparisons that ignore things like differences in spacing,
     * punctuation, or capitalization.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.
     * @param expected The expected value
     * @param actual   The value to test
     */
    public void assertFuzzyEquals(String expected, String actual)
    {
        assertFuzzyEquals(null, expected, actual);
    }


    // ----------------------------------------------------------
    /**
     * Asserts that two Strings are equal, respecting preferences for what
     * differences matter.  This method mirrors the static
     * {@link junit.framework.TestCase#assertEquals(String,String)}
     * method, augmenting its behavior with the ability to make "fuzzy"
     * string comparisons that ignore things like differences in spacing,
     * punctuation, or capitalization.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.
     * @param message  The message to use for a failed assertion
     * @param expected The expected value
     * @param actual   The value to test
     */
    public void assertFuzzyEquals(
        String message, String expected, String actual)
    {
        if (message != null)
        {
            message += " (after normalizing strings)";
        }
        try
        {
            assertEquals(
                message, stringNormalizer().normalize(expected),
                stringNormalizer().normalize(actual));
        }
        catch (AssertionFailedError e)
        {
            trimStack(e);
            throw e;
        }
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is true. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertTrue(String,boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param message   The message to use for a failed assertion
     * @param condition The condition to check
     */
    public static void assertTrue(String message, boolean condition)
    {
        String falseReason = predicateReturnsFalseReason;
        predicateReturnsFalseReason = null;
        predicateReturnsTrueReason = null;
        if (falseReason != null)
        {
            if (message == null)
            {
                message = falseReason;
            }
            else
            {
                message += " " + falseReason;
            }
        }
        try
        {
            junit.framework.TestCase.assertTrue(message, condition);
        }
        catch (AssertionFailedError e)
        {
            trimStack(e);
            throw e;
        }
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is true. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertTrue(boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param condition The condition to check
     */
    public static void assertTrue(boolean condition)
    {
        assertTrue(null, condition);
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is false. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertFalse(String,boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param message   The message to use for a failed assertion
     * @param condition The condition to check
     */
    public static void assertFalse(String message, boolean condition)
    {
        String trueReason = predicateReturnsTrueReason;
        predicateReturnsFalseReason = null;
        predicateReturnsTrueReason = null;
        if (trueReason != null)
        {
            if (message == null)
            {
                message = trueReason;
            }
            else
            {
                message += " " + trueReason;
            }
        }
        try
        {
            junit.framework.TestCase.assertFalse(message, condition);
        }
        catch (AssertionFailedError e)
        {
            trimStack(e);
            throw e;
        }
    }


    // ----------------------------------------------------------
    /**
     * Asserts that a condition is false. If it isn't, it throws an
     * AssertionFailedError with the given message.  This is a
     * special version of
     * {@link junit.framework.TestCase#assertFalse(boolean)}
     * that issues special diagnostics when the assertion fails, if
     * the given condition supports it.
     * @param condition The condition to check
     */
    public static void assertFalse(boolean condition)
    {
        assertFalse(null, condition);
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare ints with doubles, but autoboxing
     * will allow you to compare them as objects, which is never desired,
     * so this overloaded method flags the problem as a test case failure
     * rather than letting it go undiagnosed.
     * @param expected The expected value
     * @param actual The actual value
     */
    public static void assertEquals(int expected, double actual)
    {
        fail("Your test case calls assertEquals() with an int (" + expected
            + ") and a double (" + actual + "), but comparing them directly "
            + "may give incorrect results.  Instead, use a type cast to call "
            + "either assertEquals(int, int) or assertEquals(double, double, "
            + "double).  Don't forget that comparing doubles takes a third "
            + "argument indicating how close they have to be to be considered "
            + "equal.");
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare ints with doubles, but autoboxing
     * will allow you to compare them as objects, which is never desired,
     * so this overloaded method flags the problem as a test case failure
     * rather than letting it go undiagnosed.
     * @param message  The message to use if the assertion fails
     * @param expected The expected value
     * @param actual   The actual value
     */
    public static void assertEquals(String message, int expected, double actual)
    {
        fail("Your test case calls assertEquals() with an int (" + expected
            + ") and a double (" + actual + "), but comparing them directly "
            + "may give incorrect results.  Instead, use a type cast to call "
            + "either assertEquals(String, int, int) or assertEquals(String, "
            + "double, double, "
            + "double).  Don't forget that comparing doubles takes a third "
            + "argument indicating how close they have to be to be considered "
            + "equal.");
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare ints with doubles, but autoboxing
     * will allow you to compare them as objects, which is never desired,
     * so this overloaded method flags the problem as a test case failure
     * rather than letting it go undiagnosed.
     * @param expected The expected value
     * @param actual The actual value
     */
    public static void assertEquals(double expected, int actual)
    {
        fail("Your test case calls assertEquals() with a double (" + expected
            + ") and an int (" + actual + "), but comparing them directly "
            + "may give incorrect results.  Instead, use a type cast to call "
            + "either assertEquals(int, int) or assertEquals(double, double, "
            + "double).  Don't forget that comparing doubles takes a third "
            + "argument indicating how close they have to be to be considered "
            + "equal.");
    }


    // ----------------------------------------------------------
    /**
     * There is no assertion to compare ints with doubles, but autoboxing
     * will allow you to compare them as objects, which is never desired,
     * so this overloaded method flags the problem as a test case failure
     * rather than letting it go undiagnosed.
     * @param message  The message to use if the assertion fails
     * @param expected The expected value
     * @param actual   The actual value
     */
    public static void assertEquals(String message, double expected, int actual)
    {
        fail("Your test case calls assertEquals() with a double (" + expected
            + ") and an int (" + actual + "), but comparing them directly "
            + "may give incorrect results.  Instead, use a type cast to call "
            + "either assertEquals(String, int, int) or assertEquals(String, "
            + "double, double, "
            + "double).  Don't forget that comparing doubles takes a third "
            + "argument indicating how close they have to be to be considered "
            + "equal.");
    }


    // ----------------------------------------------------------
    /**
     * The assertion to compare two doubles requires three arguments, but
     * autoboxing will instead the wrong assertion if you only provide two
     * arguments.  This overloaded method flags the problem as a test case
     * failure rather than letting it go undiagnosed.
     * @param expected The expected value
     * @param actual   The actual value
     */
    public static void assertEquals(double expected, double actual)
    {
        fail("Your test case calls assertEquals() with two doubles (" + expected
            + " and " + actual + "), but comparing them directly "
            + "may give incorrect results.  Instead, use assertEquals("
            + "double, double, "
            + "double).  Don't forget that comparing doubles takes a third "
            + "argument indicating how close they have to be to be considered "
            + "equal.");
    }


    // ----------------------------------------------------------
    /**
     * The assertion to compare two doubles requires three arguments, but
     * autoboxing will instead the wrong assertion if you only provide two
     * arguments.  This overloaded method flags the problem as a test case
     * failure rather than letting it go undiagnosed.
     * @param message  The message to use if the assertion fails
     * @param expected The expected value
     * @param actual   The actual value
     */
    public static void assertEquals(
        String message, double expected, double actual)
    {
        fail("Your test case calls assertEquals() with two doubles (" + expected
            + " and " + actual + "), but comparing them directly "
            + "may give incorrect results.  Instead, use assertEquals(String, "
            + "double, double, "
            + "double).  Don't forget that comparing doubles takes a third "
            + "argument indicating how close they have to be to be considered "
            + "equal.");
    }


    // ----------------------------------------------------------
    /**
     * Takes a string and, if it is too long, shortens it by replacing the
     * middle with an ellipsis.  For example, calling <code>compact("hello
     * there", 6, 3)</code> will return "hel...ere".
     * @param content The string to shorten
     * @param threshold Strings longer than this will be compacted, while
     *        strings less than or equal to this limit will be returned
     *        unchanged
     * @param prefixLen How many characters at the front and back of the
     *        string to keep.  This number must be less than or equal to half
     *        the threshold
     * @return The shortened version of the string
     */
    public static String compact(String content, int threshold, int prefixLen)
    {
        if (content != null && content.length() > threshold)
        {
            assert prefixLen < (threshold + 1) / 2;
            return content.substring(0, prefixLen) + "..."
                + content.substring(content.length() - prefixLen);
        }
        else
        {
            return content;
        }
    }


    // ----------------------------------------------------------
    /**
     * Takes a string and, if it is too long, shortens it by replacing the
     * middle with an ellipsis.
     * @param content The string to shorten
     * @return The shortened version of the string
     */
    public static String compact(String content)
    {
        return compact(content, 15, 5);
    }


    // ----------------------------------------------------------
    /**
     * Determines whether two Strings are equal.  This method is identical
     * to {@link String#equals(Object)}, but is provided for symmetry with
     * the other comparison predicates provided in this class.  For
     * assertion writing, remember that
     * {@link junit.framework.TestCase#assertEquals(String,String)} will
     * produce more useful information on failure, however.
     * @param left  The first string to compare
     * @param right The second string to compare
     * @return True if the strings are equal
     */
    public boolean equals(String left, String right)
    {
        boolean result = left == right;
        if (left != null && right != null)
        {
            result = left.equals(right);
        }
        if (result)
        {
            predicateReturnsTrueReason =
                "<" + compact(left) + "> was the same as:<"
                + compact(right) + ">";
        }
        else
        {
            String msg =
                (new junit.framework.ComparisonFailure(null, left, right))
                    .getMessage();
            if (msg.startsWith("null "))
            {
                msg = msg.substring("null ".length());
            }
            predicateReturnsFalseReason = msg;
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Determines whether two Strings are equal, respecting preferences for
     * what differences matter.  This method mirrors
     * {@link #equals(String,String)}, augmenting its behavior with the
     * ability to make "fuzzy" string comparisons that ignore things like
     * differences in spacing, punctuation, or capitalization.  It is also
     * identical to {@link #assertFuzzyEquals(String,String)}, except that it
     * returns the boolean result of the comparison instead of making a
     * test case assertion.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.  For assertion writing, remember that
     * {@link #assertFuzzyEquals(String,String)} will
     * produce more useful information on failure, however.
     * @param left  The first string to compare
     * @param right The second string to compare
     * @return True if the strings are equal
     */
    public boolean fuzzyEquals(String left, String right)
    {
        return equals(stringNormalizer().normalize(left),
            stringNormalizer().normalize(right));
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression.  A null for the actual value is treated the same as an
     * empty string for the purposes of matching.  The regular expression
     * must match the full string (all characters taken together).  To
     * match a substring, use {@link #containsRegex(String,String...)}
     * instead.
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected pattern is the <b>second</b>.
     * </p>
     * @param actual   The value to test
     * @param expected The expected value (interpreted as a regular
     *                 expression {@link Pattern})
     * @return True if the actual matches the expected pattern
     */
    public boolean equalsRegex(String actual, String expected)
    {
        return equalsRegex(actual, Pattern.compile(expected));
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression.  A null for the actual value is treated the same as an
     * empty string for the purposes of matching.  The regular expression
     * must match the full string (all characters taken together).  To
     * match a substring, use {@link #containsRegex(String,Pattern...)}
     * instead.
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected pattern is the <b>second</b>.
     * </p>
     * @param actual   The value to test
     * @param expected The expected value
     * @return True if the actual matches the expected pattern
     */
    public boolean equalsRegex(String actual, Pattern expected)
    {
        if (actual == null)
        {
            actual = "";
        }
        boolean result = expected.matcher(actual).matches();
        if (result)
        {
            predicateReturnsTrueReason =
                "<" + compact(actual) + "> matches regex:<"
                + compact(expected.toString(), 25, 10) + ">";
        }
        else
        {
            predicateReturnsFalseReason =
                "<" + compact(actual) + "> does not match regex:<"
                + compact(expected.toString(), 25, 10) + ">";
        }
        return result;
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression, respecting preferences for what differences matter.
     * A null for the actual value is treated the same as an empty string
     * for the purposes of matching.  The regular expression must match
     * the full string (all characters taken together).  To match a substring,
     * use {@link #fuzzyContainsRegex(String,String...)} instead.
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected pattern is the <b>second</b>.
     * </p>
     * <p>Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.</p>
     * @param actual   The value to test
     * @param expected The expected value (interpreted as a regular
     *                 expression {@link Pattern})
     * @return True if the actual matches the expected pattern
     */
    public boolean fuzzyEqualsRegex(String actual, String expected)
    {
        return fuzzyEqualsRegex(actual, Pattern.compile(expected));
    }


    // ----------------------------------------------------------
    /**
     * Determines whether a String exactly matches an expected regular
     * expression, respecting preferences for what differences matter.
     * A null for the actual value is treated the same as an empty string
     * for the purposes of matching.  The regular expression must match
     * the full string (all characters taken together).  To match a substring,
     * use {@link #fuzzyContainsRegex(String,String...)} instead.
     * <p>Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.</p>
     * @param actual   The value to test
     * @param expected The expected value
     * @return True if the actual matches the expected pattern
     */
    public boolean fuzzyEqualsRegex(String actual, Pattern expected)
    {
        return equalsRegex(stringNormalizer().normalize(actual), expected);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order.  In addition to the string to search, you can provide an
     * arbitrary number of additional parameters to search for.  If you only
     * provide one substring, this method behaves the same as
     * {@link String#contains(CharSequence)}.  If you provide more than
     * one substring, it looks for each such element in turn
     * in the larger string, making sure they are all found in the proper order
     * (each substring must strictly follow the previous one, although there
     * can be any amount of intervening characters between any two substrings
     * in the array).  If the larger string is null, this method returns
     * false (since it can contain nothing).
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected substrings are the <b>second</b>.
     * </p>
     * @param largerString The target to look in
     * @param substrings   One or more substrings to look for (in order)
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean contains(String largerString, String ... substrings)
    {
        int pos = (largerString == null) ? -1 : 0;
        for (int i = 0; i < substrings.length  &&  pos >= 0; i++)
        {
            pos = largerString.indexOf(substrings[i], pos);
            if (pos >= 0)
            {
                pos += substrings[i].length();
            }
            else
            {
                predicateReturnsFalseReason =
                    "<" + compact(largerString) + "> does not contain:<"
                    + compact(substrings[i], 25, 10) + ">";
                if (substrings.length > 1)
                {
                    predicateReturnsFalseReason += "(substring " + i + ")";
                }
                break;
            }
        }
        if (pos >= 0)
        {
            predicateReturnsTrueReason =
                "<" + compact(largerString) + "> contains:";
            for (int i = 0; i < substrings.length; i++)
            {
                if (i > 0)
                {
                    predicateReturnsTrueReason += ", ";
                }
                predicateReturnsTrueReason +=
                    "<" + compact(substrings[i], 25, 10) + ">";
            }
            return true;
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, respecting preferences for what differences matter.  It
     * looks for each of the specified substrings in turn
     * in the larger string, making sure they are all found in the proper order
     * (each substring must strictly follow the previous one, although there
     * can be any amount of intervening characters between any two substrings
     * in the array).  If the larger string is null, this method returns
     * false (since it can contain nothing).
     * <p>This method makes "fuzzy" string comparisons that ignore things
     * like differences in spacing, punctuation, or capitalization.  Use
     * {@link #stringNormalizer()} to access and modify the
     * {@link StringNormalizer} object's preferences for comparing
     * strings.
     * </p>
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected substrings are the <b>second</b>.
     * </p>
     * @param largerString The target to look in
     * @param substrings   The substrings to look for (in order)
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean fuzzyContains(String largerString, String ... substrings)
    {
        // Normalized the array of expected substrings
        String[] normalizedSubstrings = new String[substrings.length];
        for (int i = 0; i < substrings.length; i++)
        {
            normalizedSubstrings[i] =
                stringNormalizer().normalize(substrings[i]);
        }

        // Now call the regular version on the normalized args
        return contains(
            stringNormalizer().normalize(largerString), normalizedSubstrings);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as a regular
     * expressions.  It looks for each of the specified regular expressions
     * in turn in the larger string, making sure they are all found in the
     * proper order (each substring must strictly follow the previous one,
     * although there can be any amount of intervening characters between
     * any two substrings in the array).  If the larger string is null, this
     * method returns false (since it can contain nothing).
     * <p>
     * Note that this predicate uses the opposite parameter ordering
     * from JUnit assertions: The value to test is the <b>first</b>
     * parameter, and the expected substrings are the <b>second</b>.
     * </p>
     * @param largerString The target to look in
     * @param substrings   A sequence of expected substrings (interpreted as
     *                     regular expression {@link Pattern}s), which must
     *                     occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * regular expressions in order.
     */
    public boolean containsRegex(String largerString, String ... substrings)
    {
        Pattern[] patterns = new Pattern[substrings.length];
        for (int i = 0; i < substrings.length; i++)
        {
            patterns[i] = Pattern.compile(substrings[i]);
        }
        return containsRegex(largerString, patterns);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as a regular
     * expressions.  It looks for each of the specified regular expressions
     * in turn in the larger string, making sure they are all found in the
     * proper order (each substring must strictly follow the previous one,
     * although there can be any amount of intervening characters between
     * any two substrings in the array).  If the larger string is null, this
     * method returns false (since it can contain nothing).
     * @param largerString The target to look in
     * @param substrings   A sequence of expected regular expressions, which
     *                     must occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * regular expressions in order.
     */
    public boolean containsRegex(String largerString, Pattern ... substrings)
    {
        boolean result = true;
        int pos = 0;
        for (int i = 0; i < substrings.length; i++)
        {
            Matcher matcher = substrings[i].matcher(largerString);
            result = matcher.find(pos);
            if (!result)
            {
                predicateReturnsFalseReason =
                    "<" + compact(largerString) + "> does not contain regex:<"
                    + compact(substrings[i].toString(), 25, 10) + ">";
                if (substrings.length > 1)
                {
                    predicateReturnsFalseReason += "(pattern " + i + ")";
                }
                break;
            }
            pos = matcher.end();
        }
        if (result)
        {
            predicateReturnsTrueReason =
                "<" + compact(largerString) + "> contains regexes:";
            for (int i = 0; i < substrings.length; i++)
            {
                if (i > 0)
                {
                    predicateReturnsTrueReason += ", ";
                }
                predicateReturnsTrueReason +=
                    "<" + compact(substrings[i].toString(), 25, 10) + ">";
            }
            return true;
        }
        else
        {
            return false;
        }
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as regular
     * expressions, and respecting preferences for what differences matter.
     * This method behaves just like {@link #fuzzyContains(String,String...)},
     * except that the second argument is interpreted as an array of regular
     * expressions.  String normalization rules are only appled to the
     * larger string, not to the regular expressions.
     * @param largerString The target to look in
     * @param substrings   An array of expected substrings (interpreted as
     *                     regular expression {@link Pattern}s), which must
     *                     occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean fuzzyContainsRegex(
        String largerString, String ... substrings)
    {
        Pattern[] patterns = new Pattern[substrings.length];
        for (int i = 0; i < substrings.length; i++)
        {
            patterns[i] = Pattern.compile(substrings[i]);
        }
        return fuzzyContainsRegex(largerString, patterns);
    }


    // ----------------------------------------------------------
    /**
     * Determine whether one String contains a sequence of other substrings
     * in order, where the expected substrings are specified as regular
     * expressions, and respecting preferences for what differences matter.
     * This method behaves just like {@link #fuzzyContains(String,String...)},
     * except that the second argument is interpreted as an array of regular
     * expressions.  String normalization rules are only appled to the
     * larger string, not to the regular expressions.
     * @param largerString The target to look in
     * @param substrings   An array of expected substrings, which must
     *                     occur in the same order in the larger string
     * @return True if the largerString contains all of the specified
     * substrings in order.
     */
    public boolean fuzzyContainsRegex(
        String largerString, Pattern ... substrings)
    {
        return containsRegex(
            stringNormalizer().normalize(largerString), substrings);
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
        TouchUtils.clickView(this, view);

        getInstrumentation().waitForIdleSync();
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

        getInstrumentation().waitForIdleSync();
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
        int[] screenOffset = new int[2];
        view.getLocationOnScreen(screenOffset);

        TouchUtils.drag(this, xStart + screenOffset[0], xEnd + screenOffset[0],
                yStart + screenOffset[1], yEnd + screenOffset[1], steps);

        getInstrumentation().waitForIdleSync();
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
        // Request focus on the view and select its entire text content.

        getActivity().runOnUiThread(new Runnable() {
            public void run()
            {
                view.requestFocus();
                view.setSelection(0, view.getText().length());
            }
        });

        // Clear the existing text in the view and then enter the new text.

        sendKeys(KeyEvent.KEYCODE_CLEAR);

        if (text != null && text.length() > 0)
        {
            getInstrumentation().sendStringSync(text);
        }

        getInstrumentation().waitForIdleSync();
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

        getInstrumentation().waitForIdleSync();
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
    private void purgeHitMonitors()
    {
        for (ActivityMonitor monitor : currentTestMonitors.keySet())
        {
            getInstrumentation().checkMonitorHit(monitor, 1);
        }
    }


    // ----------------------------------------------------------
    private void prepareUpcomingActivityResult(IntentFilter intentFilter,
        int resultCode, Intent data, String reason)
    {
        purgeHitMonitors();

        ActivityResult result = new ActivityResult(resultCode, data);
        ActivityMonitor monitor = new ActivityMonitor(
            intentFilter, result, true);
        getInstrumentation().addMonitor(monitor);

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

        currentTestMonitors.put(monitor, failureReason);
    }


    // ----------------------------------------------------------
    public void prepareForUpcomingActivity(String action)
    {
        purgeHitMonitors();

        IntentFilter intentFilter = new IntentFilter(action);
        try
        {
            intentFilter.addDataType("*/*");
        }
        catch (MalformedMimeTypeException e)
        {
            // Do nothing.
        }

        ActivityMonitor monitor = new ActivityMonitor(
            intentFilter, null, true);
        getInstrumentation().addMonitor(monitor);

        String failureReason;

        failureReason = "Expected an activity with intent action "
            + action + " to start, but one never did.";

        currentTestMonitors.put(monitor, failureReason);
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
        purgeHitMonitors();

        ActivityResult result = new ActivityResult(resultCode, data);
        ActivityMonitor monitor = new ActivityMonitor(
            activityClassName, result, true);
        getInstrumentation().addMonitor(monitor);

        currentTestMonitors.put(monitor, "Expected the activity "
            + activityClassName + " to start, but it never did.");
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
     * @param filter the search critera
     */
    public void selectItemInList(AbsListView listView, AndroidViewFilter filter)
    {
        ScrollingAndroidViewFinder finder =
            new ScrollingAndroidViewFinder(getInstrumentation(), listView);

        View toSelect = finder.findFirst(filter);

        assertNotNull("Cannot find item in ListView matching: " + filter,
            toSelect);

        click(toSelect);
    }


    // ----------------------------------------------------------
    /**
     * Simulates selecting the menu item with the specified ID from the options
     * menu. A test case failure will result if the menu item is disabled.
     *
     * @param id the id of the menu item to select
     */
    /*public void selectOptionsMenuItem(int id)
    {
        boolean success =
            getInstrumentation().invokeMenuActionSync(getActivity(), id, 0);

        assertTrue("The menu item with id \"" + getFieldNameForId(id)
            + "\" is disabled.", success);

        getInstrumentation().waitForIdleSync();
    }*/


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
    /*public void selectOptionsMenuItem(String id)
    {
        selectOptionsMenuItem(getIdByName(id));
    }*/


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

        MotionEvent event = obtainMotionEvent(
                view, MotionEvent.ACTION_DOWN, x, y);

        getInstrumentation().sendPointerSync(event);
        getInstrumentation().waitForIdleSync();
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

        MotionEvent event = obtainMotionEvent(
                lastTouchView, MotionEvent.ACTION_MOVE, x, y);

        getInstrumentation().sendPointerSync(event);
        getInstrumentation().waitForIdleSync();
    }


    // ----------------------------------------------------------
    /**
     * Simulates lifting the finger up from the screen at the end of a touch
     * operation.
     */
    public void touchUp()
    {
        assertNotNull("touchDown should only be called after touchUp.",
            lastTouchView);

        MotionEvent event = obtainMotionEvent(
                lastTouchView, MotionEvent.ACTION_UP, lastTouchX, lastTouchY);

        getInstrumentation().sendPointerSync(event);
        getInstrumentation().waitForIdleSync();

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
     * @return the MotionEvent that was obtained
     */
    private MotionEvent obtainMotionEvent(View view, int action,
                                          float x, float y)
    {
        long uptime = SystemClock.uptimeMillis();

        int[] screenOffset = new int[2];
        view.getLocationOnScreen(screenOffset);

        return MotionEvent.obtain(uptime, uptime, action,
                x + screenOffset[0], y + screenOffset[1], 0);
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
        Field viewsField;
        Field instanceField;

        try
        {
            viewsField = windowManager.getDeclaredField("mViews");
            instanceField = windowManager.getDeclaredField("mWindowManager");

            viewsField.setAccessible(true);
            instanceField.setAccessible(true);
            Object instance = instanceField.get(null);
            View[] views = (View[]) viewsField.get(instance);

            return Arrays.asList(views);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }


    // ----------------------------------------------------------
    private static void trimStack(Throwable t)
    {
        if (trimStackTraces == null)
        {
            try
            {
                String setting =
                    System.getProperty("student.TestCase.trimStackTraces");
                if (setting == null)
                {
                    trimStackTraces = true;
                }
                else
                {
                    setting = setting.toLowerCase().trim();
                    trimStackTraces = "yes".equals(setting)
                        || "true".equals(setting)
                        || "on".equals(setting)
                        || "1".equals(setting);
                }
            }
            catch (Exception e)
            {
                trimStackTraces = true;
            }
        }

        if (!trimStackTraces)
        {
            return;
        }

        StackTraceElement[] oldTrace = t.getStackTrace();
        int pos1 = 0;
        while (pos1 < oldTrace.length
            && (oldTrace[pos1].getClassName().equals("junit.framework.Assert")))
        {
            ++pos1;
        }
        int pos2 = pos1;
        while (pos2 < oldTrace.length
            && (oldTrace[pos2].getClassName().equals("student.TestCase")))
        {
            ++pos2;
        }

        // It would be good to strip out a top-level stack trace element for
        // student.TestCase.runBare(), which will come soon

        if (pos2 > pos1 && pos2 < oldTrace.length - 1)
        {
            StackTraceElement[] newTrace =
                new StackTraceElement[oldTrace.length - (pos2 - pos1)];
            if (pos1 > 0)
            {
                System.arraycopy(oldTrace, 0, newTrace, 0, pos1);
            }
            System.arraycopy(
                oldTrace, pos2, newTrace, pos1, newTrace.length - pos1);
            t.setStackTrace(newTrace);
        }
    }


    // ----------------------------------------------------------
    static
    {
        try
        {
            windowManager = Class.forName("android.view.WindowManagerImpl");
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (SecurityException e)
        {
            e.printStackTrace();
        }
    }
}
