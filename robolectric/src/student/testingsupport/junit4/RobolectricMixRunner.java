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

package student.testingsupport.junit4;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.bytecode.ClassHandler;
import com.xtremelabs.robolectric.internal.RobolectricTestRunnerInterface;
import com.xtremelabs.robolectric.util.DatabaseConfig;
import com.xtremelabs.robolectric.util.H2Map;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.internal.runners.model.MultipleFailureException;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import student.testingsupport.shadows.ShadowActivity;
import student.testingsupport.shadows.ShadowContentResolver;
import student.testingsupport.shadows.ShadowExifInterface;
import student.testingsupport.shadows.ShadowMapActivity;
import student.testingsupport.shadows.ShadowMapView;
import student.testingsupport.shadows.ShadowOverlay;
import student.testingsupport.shadows.ShadowView;
import student.testingsupport.shadows.ShadowViewGroup;

// -------------------------------------------------------------------------
/**
 * A custom JUnit runner which uses reflection to run both JUnit3 as well as
 * JUnit4 tests, ensuring that the Robolectric framework's injection of Android
 * shadow classes occurs. The usefulness of this is that it can be used with a
 * {@code @RunWith} annotation in a parent class, and the resulting subclasses
 * can be written as if they are JUnit3 tests, but advanced users can use
 * annotations as well, and any functionality dictated by the superclass, for
 * instance {@code @Rule} annotations, will be applied to the children as
 * well.
 *
 * It also looks for JUnit3 setUp() and tearDown() methods and performs them
 * as if they are JUnit4 {@code @Before}s and {@code @After}s.
 *
 * @author Tony Allevato
 * @author Last changed by $Author$
 * @version $Revision$, $Date$
 */
public class RobolectricMixRunner extends RobolectricTestRunner
{
    //~ Instance/static variables .............................................

    private List<FrameworkMethod> befores = null;
    private boolean junit3methodsAdded = false;
    private boolean junit3aftersAdded = false;

    private static SQLiteDatabase database;


    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a JUnitMixRunner to run {@code klass}
     *
     * @param  klass The test class to run
     * @throws InitializationError if the test class is malformed.
     */
    public RobolectricMixRunner(Class<?> klass)
        throws InitializationError
    {
        super(klass);
    }


    //~ Methods ...............................................................

    // ----------------------------------------------------------
    /**
     * Gets the single instance of the SQLiteDatabase to be used during testing,
     * creating it if necessary.
     *
     * @return the SQLiteDatabase to be used during testing
     */
    public static SQLiteDatabase getDatabase()
    {
        if (database == null)
        {
            database = SQLiteDatabase.openDatabase("content", null, 0);
        }

        return database;
    }


    // ----------------------------------------------------------
    /**
     * Register any additional shadow classes that the student library uses.
     *
     * @param method the test method about to be run
     */
    @Override
    public void beforeTest(Method method)
    {
        super.beforeTest(method);
    }


    // ----------------------------------------------------------
    /**
     * Build the databases used to maintain things like the media library.
     *
     * TODO Make this more general. Perhaps allow the instructor test cases
     * themselves to set up mock databases for testing.
     */
    protected void buildDatabases()
    {
        DatabaseConfig.setDatabaseMap(new H2Map());

        if (database != null)
        {
            database.close();
            database = null;
        }

        SQLiteDatabase db = getDatabase();

        db.execSQL("create table media_images (_id varchar(255), "
            + "mime_type varchar(255), _data varchar(255))");

        File[] files = new File(".").listFiles();
        int id = 1;

        for (File file : files)
        {
            String name = file.getName();
            String mimeType = null;

            // FIXME generalize
            if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".jpe"))
            {
                mimeType = "image/jpeg";
            }
            else if (name.endsWith(".gif"))
            {
                mimeType = "image/gif";
            }
            else if (name.endsWith(".png"))
            {
                mimeType = "image/png";
            }

            if (mimeType != null)
            {
                // FIXME other columns
                ContentValues values = new ContentValues();
                values.put("_id", id++);
                values.put("mime_type", mimeType);
                values.put("_data", name);

                db.insert("media_images", null, values);
            }
        }
    }


    // ----------------------------------------------------------
    @Override
    protected void bindShadowClasses()
    {
        Robolectric.bindShadowClass(ShadowActivity.class);
        Robolectric.bindShadowClass(ShadowMapActivity.class);
        Robolectric.bindShadowClass(ShadowView.class);
        Robolectric.bindShadowClass(ShadowViewGroup.class);
        Robolectric.bindShadowClass(ShadowContentResolver.class);
        Robolectric.bindShadowClass(ShadowExifInterface.class);
        Robolectric.bindShadowClass(ShadowMapView.class);
        Robolectric.bindShadowClass(ShadowOverlay.class);

        buildDatabases();
    }


    // ----------------------------------------------------------
    @Override
    protected void resetStaticState()
    {
        super.resetStaticState();

        setStaticValue(MediaStore.Images.Media.class, "EXTERNAL_CONTENT_URI",
            Uri.parse("content://media/external/images/media"));
        setStaticValue(MediaStore.Images.Media.class, "INTERNAL_CONTENT_URI",
            Uri.parse("content://media/internal/images/media"));

        setStaticValue(ExifInterface.class, "TAG_ORIENTATION", "Orientation");
        setStaticValue(ExifInterface.class, "TAG_DATETIME", "DateTime");
        setStaticValue(ExifInterface.class, "TAG_MAKE", "Make");
        setStaticValue(ExifInterface.class, "TAG_MODEL", "Model");
        setStaticValue(ExifInterface.class, "TAG_FLASH", "Flash");
        setStaticValue(ExifInterface.class, "TAG_IMAGE_WIDTH", "ImageWidth");
        setStaticValue(ExifInterface.class, "TAG_IMAGE_LENGTH", "ImageLength");
        setStaticValue(ExifInterface.class, "TAG_GPS_LATITUDE", "GPSLatitude");
        setStaticValue(ExifInterface.class, "TAG_GPS_LONGITUDE", "GPSLongitude");
        setStaticValue(ExifInterface.class, "TAG_GPS_LATITUDE_REF", "GPSLatitudeRef");
        setStaticValue(ExifInterface.class, "TAG_GPS_LONGITUDE_REF", "GPSLongitudeRef");
        setStaticValue(ExifInterface.class, "TAG_GPS_ALTITUDE", "GPSAltitude");
        setStaticValue(ExifInterface.class, "TAG_GPS_ALTITUDE_REF", "GPSAltitudeRef");
        setStaticValue(ExifInterface.class, "TAG_GPS_TIMESTAMP", "GPSTimeStamp");
        setStaticValue(ExifInterface.class, "TAG_GPS_DATESTAMP", "GPSDateStamp");
        setStaticValue(ExifInterface.class, "TAG_WHITE_BALANCE", "WhiteBalance");
        setStaticValue(ExifInterface.class, "TAG_FOCAL_LENGTH", "FocalLength");
        setStaticValue(ExifInterface.class, "TAG_GPS_PROCESSING_METHOD", "GPSProcessingMethod");
    }


    // ----------------------------------------------------------
    /**
     * Returns a {@link Statement}: run all non-overridden {@code @Before}
     * methods on this class and superclasses, as well as any JUnit3 setUp
     * methods, before running {@code next}; if any throws an Exception, stop
     * execution and pass the exception on.
     *
     * Note that in BlockJUnit4ClassRunner this method is deprecated.
     */
    @Override
    protected Statement withBefores(
        FrameworkMethod method, Object target, Statement statement)
    {
        List<FrameworkMethod> annotatedBefores =
            getTestClass().getAnnotatedMethods(Before.class);

        if (befores != annotatedBefores)
        {
            befores = annotatedBefores;
            // FIXME: This code only finds setUp() if it is public,
            // when the inherited method is protected.
            Method[] methods = getTestClass().getJavaClass().getMethods();
            for (Method m : methods)
            {
                // Need to check for correct signature
                // Need to ensure it isn't annotated as @Before already
                if (m.getName().equals("setUp"))
                {
                    FrameworkMethod fm = new FrameworkMethod(m);
                    // add at the end, so it will be executed last, after
                    // all other @Before methods
                    befores.add(fm);
                }
            }
        }

        return befores.isEmpty()
            ? statement
            : new RunBefores(statement, befores, target);
    }


    // ----------------------------------------------------------
    /**
     * Returns a {@link Statement}: run all non-overridden {@code @After}
     * methods, as well as any JUnit3 tearDown methods, on this class and
     * superclasses before running {@code next}; all After methods are always
     * executed: exceptions thrown by previous steps are combined, if
     * necessary, with exceptions from After methods into a
     * {@link MultipleFailureException}.
     *
     * Note that in BlockJUnit4ClassRunner this method is deprecated.
     */
    @Override
    protected Statement withAfters(
        FrameworkMethod method, Object target, Statement statement)
    {
        List<FrameworkMethod> afters =
            getTestClass().getAnnotatedMethods(After.class);

        if (!junit3aftersAdded)
        {
            Method[] methods = getTestClass().getJavaClass().getMethods();
            for (Method m : methods)
            {
                // Need to check for correct signature
                // Need to ensure it isn't annotated as @Before already
                if (m.getName().equals("tearDown"))
                {
                    FrameworkMethod fm = new FrameworkMethod(m);
                    // Add at position zero, so it will be executed first,
                    // before all other @After methods
                    afters.add(0, fm);
                }
            }
            junit3aftersAdded = true;
        }

        return afters.isEmpty()
            ? statement
            : new RunAfters(statement, afters, target);
    }


    // ----------------------------------------------------------
    /**
     * Gathers all JUnit4 and JUnit3 test methods from this class and its
     * superclasses.
     *
     * @return the list of test methods to run.
     */
    @Override
    protected List<FrameworkMethod> getChildren()
    {
        List<FrameworkMethod> children = super.computeTestMethods();

        if (!junit3methodsAdded)
        {
            Method[] methods = getTestClass().getJavaClass().getMethods();
            for (Method method : methods)
            {
                FrameworkMethod fm = new FrameworkMethod(method);
                if (method.getName().startsWith("test")
                    && !children.contains(fm))
                {
                    children.add(fm);
                }
            }
            junit3methodsAdded = true;
        }

        return children;
    }


    // ----------------------------------------------------------
    /**
     * Adds to {@code errors} a throwable for each problem noted with the
     * test class (available from {@link #getTestClass()}).  Default
     * implementation adds an error for each method annotated with
     * {@code @BeforeClass} or {@code @AfterClass} that is not
     * {@code public static void} with no arguments.
     */
    protected void collectInitializationErrors(List<Throwable> errors)
    {
        super.collectInitializationErrors(errors);
        for (int i = 0; i < errors.size(); i++)
        {
            if (errors.get(i).getMessage().equals("No runnable methods"))
            {
                errors.remove(i);
                break;
            }
        }
    }


    // ----------------------------------------------------------
    /**
     * Adds Robolectric before/after processing to a statement.
     *
     * @param method The test method being decorated
     * @param statement The statement representing the test method
     * @return A new statement with decorations attached.
     */
    protected Statement withRobolectricSupport(
        final FrameworkMethod method, final Statement statement)
    {
        try
        {
            Field classHandlerField =
                RobolectricTestRunner.class.getDeclaredField("classHandler");
            classHandlerField.setAccessible(true);
            final ClassHandler classHandler =
                (ClassHandler) classHandlerField.get(this);

            Field delegateField =
                RobolectricTestRunner.class.getDeclaredField("delegate");
            delegateField.setAccessible(true);
            final RobolectricTestRunnerInterface delegate =
                (RobolectricTestRunnerInterface) delegateField.get(this);

            return new Statement()
            {
                @Override public void evaluate() throws Throwable
                {
                    try
                    {
                        if (classHandler != null)
                        {
                            classHandler.beforeTest();
                        }
                        delegate.internalBeforeTest(method.getMethod());

                        statement.evaluate();
                    }
                    finally
                    {
                        delegate.internalAfterTest(method.getMethod());
                        if (classHandler != null)
                        {
                            classHandler.afterTest();
                        }
                    }
                }
            };
        }
        catch (Exception e)
        {
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }
            else
            {
                throw new RuntimeException(e);
            }
        }
    }


    // ----------------------------------------------------------
    @SuppressWarnings("deprecation")
    protected Statement methodBlock(FrameworkMethod method)
    {
        Object test;
        try
        {
            test = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable
                {
                    return createTest();
                }
            }.run();
        }
        catch (Throwable e)
        {
            return new Fail(e);
        }

        Statement statement = methodInvoker(method, test);
        statement = possiblyExpectingExceptions(method, test, statement);
        statement = withPotentialTimeout(method, test, statement);
        statement = withBefores(method, test, statement);
        statement = withAfters(method, test, statement);
        statement = withRules(method, test, statement);
        statement = withRobolectricSupport(method, statement);
        statement = new RunTestMethodWrapper(statement, test);
        return statement;
    }


    // ----------------------------------------------------------
//    @Override
//    protected Statement methodInvoker(FrameworkMethod method, Object test)
//    {
//        // Replace JUnit4's InvokeMethod with our custom version here:
//        return new InvokeMethod(method, test);
//    }


    // ----------------------------------------------------------
    // Should've been protected in parent, but wasn't!
    protected Statement withRules(
        FrameworkMethod method, Object target, Statement statement)
    {
        Statement result = statement;
        for (MethodRule each : getTestClass()
            .getAnnotatedFieldValues(target, Rule.class, MethodRule.class))
        {
            result = each.apply(result, method, target);
        }
        return result;
    }
}
