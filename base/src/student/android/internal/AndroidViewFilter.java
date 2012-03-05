/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2007-2010 Virginia Tech
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
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;

//-------------------------------------------------------------------------
/**
 *  This class Represents a filter or query that can be used to describe
 *  a {@link View} when searching.  Note that the methods and fields
 *  in this class are designed specifically to support a natural, readable,
 *  boolean expression "mini-language" for use in describing a single
 *  {@link View} (or group of {@link View}s) by its (or their)
 *  properties.  As a result, it does violate some conventions regarding
 *  the use of public fields (although note that all here are immutable)
 *  and occasionally even the naming conventions for constants (e.g.,
 *  <code>where</code>).  However, breaking these conventions is necessary
 *  in this class in order to support the more natural syntax for filter
 *  expressions, and so was deemed a better design choice.
 *  <p>
 *  Client classes that wish to use these filters should add the
 *  following static import directive:
 *  </p>
 *  <pre>
 *  import static student.testingsupport.AndroidViewFilter.ClientImports.*;
 *  </pre>
 *  <p>
 *  Note that the {@link student.AndroidTestCase} class already re-exports the
 *  items defined in the {@link ClientImports} nested class, so GUI test
 *  cases should <em>not</em> include the static import.
 *  </p>
 *  <p>
 *  The expressions that you can create with this class are designed to
 *  represent "filters" or boolean predicates that can be applied to a
 *  View, returning true if the view "matches" the filter or false
 *  if the view does not match.
 *  </p>
 *  <p>
 *  Often, a filter object is created solely for the purpose of passing
 *  the filter into some other operation, such as a search operation.  For
 *  example, the {@link student.AndroidTestCase} class provides a
 *  {@link student.AndroidTestCase#getView(Class,AndroidViewFilter) getView()}
 *  method that takes a filter as a parameter.  For the examples below, we
 *  will use <code>getView()</code> as the context, specifying each
 *  filter as an argument value in a call to that method.
 *  </p>
 *  <p>
 *  The basic principles for using this class are as follows:
 *  </p>
 *  <ul>
 *  <li><p>Never try to create a AndroidViewFilter object directly.  Instead,
 *         always write something that looks like a boolean expression, and
 *         that starts with the operator <code>where</code>:</p>
 *  <pre>
 *  Button button = getView(Button.class, where.idIs(R.id.okButton));
 *  </pre></li>
 *  <li><p>The basic properties you can check with filters include:
 *         <code>idIs()</code>, <code>textIs()</code>,
 *         <code>enabledIs()</code>, <code>hasFocusIs()</code>,
 *         <code>hasFocusIs()</code>, and <code>typeIs()</code>.  They are
 *         all used the same way:</p>
 *  <pre>
 *  Button done = getView(Button.class, where.textIs("Done"));
 *  TextView name = getView(TextView.class, where.textIs("Name:"));
 *  </pre></li>
 *  <li><p>You can combine filters using logical "and" as necessary:</p>
 *  <pre>
 *  Button done = getView(Button.class,
 *      where.idIs(R.id.done).and.enabledIs(true).and.hasFocusIs(true));
 *  </pre></li>
 *  <li><p>You can also use "or":</p>
 *  <pre>
 *  Button done = getView(Button.class,
 *      where.idIs(R.id.done).or.enabledIs(true).or.hasFocusIs(true));
 *  </pre></li>
 *  <li><p>Operators like "and" and "or" are interpreted strictly left to
 *  right.  There is <b>no precedence</b>, because of the way Java interprets
 *  dot notation.</p>
 *  <pre>
 *  Button done = getView(Button.class,
 *      where.idIs(R.id.done).or.enabledIs(true).and.hasFocusIs(true));
 *      // means ((id = R.id.done or enabled = true) and focus = true)
 *      // note that the left operator is always evaluated first!
 *  </pre>
 *  <p>If you want to force a different order of evaluation
 *  than strictly left-to-right, then use parentheses by writing the
 *  appropriate operator as <code>and()</code> or <code>or()</code>.  Just
 *  be sure to start the new expression inside the parentheses with
 *  <code>where</code>:</p>
 *  <pre>
 *  Button done = getView(Button.class,
 *      where.idIs(R.id.done).or(where.enabledIs(true).and.hasFocusIs(true)));
 *      // now means (id = R.id.done or (enabled = true and focus = true))
 *      // because of the extra parentheses used
 *  </pre></li>
 *  <li><p>Finally, you can even use "not" (logical negation), but it is
 *  called like a method, so parentheses (and thus a leading <code>where)
 *  are <em>always required</em> to make the intended extent of the negation
 *  clear:</p>
 *  <pre>
 *  Button done = getView(Button.class,
 *      where.idIs(R.id.done).and.not(where.enabledIs(true).or.hasFocusIs(true)));
 *  </pre></li>
 *  </ul>
 *
 *  @author  Tony Allevato, Stephen Edwards
 *  @author  Last changed by $Author: stedwar2 $
 *  @version $Revision: 1.5 $, $Date: 2010/07/26 13:59:37 $
 */
public abstract class AndroidViewFilter
{
    //~ Instance/static variables .............................................

    private String description;


    //~ Constructor ...........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new filter object.  This constructor is not public, since
     * all filters are expected to be created using operators rather than
     * by calling new.
     * @param description A string description of this filter, used in
     *                    {@link #toString()}.
     */
    protected AndroidViewFilter(String description)
    {
        this.description = description;
    }


    //~ Public Fields .........................................................

    // These fields are public to afford a more natural syntax, although they
    // can never be manipulated since they are final and have no mutators.
    // They are instance fields instead of static fields, because that is
    // necessary for their semantics.

    // ----------------------------------------------------------
    /**
     * The "and" operator for combining filters, designed to be used in
     * expressions like <code>where.idIs(...).and.enabledIs(true)</code>.
     * This operator is implemented as a public field so that the simple
     * <code>.and.</code> notation can be used as a connective between
     * filters.  If you want to use parentheses for grouping to define
     * the right argument, see {@link #and(AndroidViewFilter)} instead.
     */
    public final BinaryOperator and = new BinaryOperator() {
        // ----------------------------------------------------------
        @Override
        protected boolean combine(boolean leftResult, boolean rightResult)
        {
            return leftResult && rightResult;
        }

        // ----------------------------------------------------------
        @Override
        protected String description(
            String leftDescription, String rightDescription)
        {
            return "(" + leftDescription + " AND " + rightDescription + ")";
        }

    };


    // ----------------------------------------------------------
    /**
     * The "or" operator for combining filters, designed to be used in
     * expressions like <code>where.idIs(R.id.abc).or.idIs(R.id.def)</code>.
     * This operator is implemented as a public field so that the simple
     * <code>.or.</code> notation can be used as a connective between
     * filters.  If you want to use parentheses for grouping to define
     * the right argument, see {@link #or(AndroidViewFilter)} instead.
     */
    public final BinaryOperator or = new BinaryOperator() {
        // ----------------------------------------------------------
        @Override
        protected boolean combine(boolean leftResult, boolean rightResult)
        {
            return leftResult || rightResult;
        }

        // ----------------------------------------------------------
        @Override
        protected String description(
            String leftDescription, String rightDescription)
        {
            return "(" + leftDescription + " OR " + rightDescription + ")";
        }

    };


    //~ Public Methods ........................................................

    // ----------------------------------------------------------
    /**
     *  This base class represents an operator used to create a query.
     *  As the base class for all operators, it defines the primitive
     *  query operations supported for all {@link View} objects,
     *  each of which can be combined using any operator.
     */
    public static abstract class Operator
    {
        // ----------------------------------------------------------
        /**
         * Create a filter that compares the ID of a view against a
         * given value.
         * @param id The id to look for
         * @return A new filter that succeeds only on views with the
         *         given ID.
         */
        public AndroidViewFilter idIs(final int id)
        {
            return applySelfTo(AndroidViewFilter.idIs(id));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks the text of a view by calling the
         * view's <code>getText()</code> method.
         * @param text The text to look for
         * @return A new filter that succeeds only on views where
         *         <code>getText()</code> returns the specified text.
         */
        public AndroidViewFilter textIs(final String text)
        {
            return applySelfTo(AndroidViewFilter.textIs(text));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks the text of a view by calling the
         * view's <code>getText()</code> method. Unlike {@link #textIs(String)},
         * which is an exact match, this method only checks for containment.
         * @param text The text to look for
         * @return A new filter that succeeds only on views where the string
         *         returned by <code>getText()</code> contains the specified
         *         text.
         */
        public AndroidViewFilter textContains(final String text)
        {
            return applySelfTo(AndroidViewFilter.textContains(text));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that succeeds if a view has focus.
         * @param value True when searching for a view with focus, or
         *              false when searching for one without.
         * @return A new filter that succeeds only on views that currently
         *         have focus
         */
        public AndroidViewFilter hasFocusIs(final boolean value)
        {
            return applySelfTo(AndroidViewFilter.hasFocusIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that succeeds if a view is enabled.
         * @param value True when searching for an enabled view, or false
         *              when searching for a disabled view.
         * @return A new filter that succeeds only on views that currently
         *         are enabled
         */
        public AndroidViewFilter enabledIs(final boolean value)
        {
            return applySelfTo(AndroidViewFilter.enabledIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that succeeds if a view has a certain visibility.
         * @param value One of the constants View.VISIBILE, View.INVISIBLE,
         *        or View.GONE
         * @return A new filter that succeeds only on views that have the
         *         specified visibility
         */
        public AndroidViewFilter visibilityIs(final int value)
        {
            return applySelfTo(AndroidViewFilter.visibilityIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that succeeds if a view has a certain visibility.
         * @param value True when searching for a visible view, or false when
         *        searching for an invisible view
         * @return A new filter that succeeds only on views that have the
         *         specified visibility
         */
        public AndroidViewFilter visibilityIs(final boolean value)
        {
            return applySelfTo(AndroidViewFilter.visibilityIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks the class of a view.
         * @param aClass The required class to check for (any subclass will
         *               also match).
         * @return A new filter that only succeeds on instances of the
         *         given class.
         */
        public AndroidViewFilter typeIs(final Class<? extends View> aClass)
        {
            return applySelfTo(AndroidViewFilter.typeIs(aClass));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's width.
         * @param value The width to look for.
         * @return A new filter that succeeds only on views that have
         *         the given width.
         */
        public AndroidViewFilter widthIs(final int value)
        {
            return applySelfTo(AndroidViewFilter.widthIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's height.
         * @param value The height to look for.
         * @return A new filter that succeeds only on views that have
         *         the given height.
         */
        public AndroidViewFilter heightIs(final int value)
        {
            return applySelfTo(AndroidViewFilter.heightIs(value));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's size.
         * @param width The required width.
         * @param height The required height.
         * @return A new filter that succeeds only on views that have
         *         the given size.
         */
        public AndroidViewFilter sizeIs(final int width, final int height)
        {
            return applySelfTo(AndroidViewFilter.sizeIs(width, height));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's size.
         * @param maxWidth The required width.
         * @param maxHeight The required height.
         * @return A new filter that succeeds only on views that have
         *         the given size.
         */
        public AndroidViewFilter sizeIsWithin(
            final int maxWidth, final int maxHeight)
        {
            return applySelfTo(AndroidViewFilter.sizeIsWithin(
                maxWidth, maxHeight));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's x-coordinate.
         * @param x The required x-coordinate, relative to the
         *          view's parent.
         * @return A new filter that succeeds only on views that have
         *         the given x-coordinate.
         */
        public AndroidViewFilter xLocationIs(final int x)
        {
            return applySelfTo(AndroidViewFilter.xLocationIs(x));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's y-coordinate.
         * @param y The required y-coordinate, relative to the
         *          view's parent.
         * @return A new filter that succeeds only on views that have
         *         the given y-coordinate.
         */
        public AndroidViewFilter yLocationIs(final int y)
        {
            return applySelfTo(AndroidViewFilter.yLocationIs(y));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's location relative to
         * its parent.
         * @param x The required x-coordinate, relative to the
         *          view's parent.
         * @param y The required y-coordinate, relative to the
         *          view's parent.
         * @return A new filter that succeeds only on views that have
         *         the given location.
         */
        public AndroidViewFilter locationIs(final int x, final int y)
        {
            return applySelfTo(AndroidViewFilter.locationIs(x, y));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks whether a view's location (its
         * top left corner) lies within a specific rectangle.
         * @param region A rectangle defining a region in the view's
         *               parent.
         * @return A new filter that succeeds only on views that have
         *         a location within the given region.
         */
        public AndroidViewFilter isLocatedWithin(final Rect region)
        {
            return applySelfTo(AndroidViewFilter.isLocatedWithin(region));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks whether a view's bounding box
         * lies within a specific rectangle--that is, whether the entire view's
         * area, rather than just its top left corner, lies within the
         * specified region.
         * @param region A rectangle defining a region in the view's
         *               parent.
         * @return A new filter that succeeds only on views that
         *         lie entirely within the given region, as determined by
         *         {@link Rect#contains(Rect)}.
         */
        public AndroidViewFilter isContainedWithin(final Rect region)
        {
            return applySelfTo(AndroidViewFilter.isContainedWithin(region));
        }


        // ----------------------------------------------------------
        /**
         * Create a filter that checks a view's parent
         * @param parent the parent view of the view being checked
         * @return A new filter that succeeds only on views that
         *         are children of parent
         */
        public AndroidViewFilter parentIs(final View parent)
        {
            return applySelfTo(AndroidViewFilter.parentIs(parent));
        }


        // ----------------------------------------------------------
        /**
          * Create a filter that checks a view's ancestor
          * @param ancestor one of the ancestors of the view being checked
          * @return A new filter that succeeds only on views that
          *         are descendants of ancestor
          */
        public AndroidViewFilter ancestorIs(final ViewParent ancestor)
        {
            return applySelfTo(AndroidViewFilter.ancestorIs(ancestor));
        }


        // ----------------------------------------------------------
        /**
         * Concrete subclasses must override this to implement an
         * operation on the filter being passed in to transform it into
         * another filter.
         * @param otherFilter The argument to transform (second argument,
         *               for binary operators)
         * @return A new compound filter that includes the given argument
         *         as one subfilter, after applying this operator to it.
         */
        protected abstract AndroidViewFilter applySelfTo(
            final AndroidViewFilter otherFilter);
    }


    // ----------------------------------------------------------
    /**
     * A non-static subclass for binary operators that implicitly
     * captures the outer filter to which it belongs, using it as
     * the first/left argument to the operator.
     */
    public abstract class BinaryOperator
        extends Operator
    {
        // ----------------------------------------------------------
        /**
         * The "not" operator for negating an existing filter, when you
         * want to use parentheses to group its righthand argument.  This
         * method is designed to be used in expressions like
         * <code>where.idIs(R.id.abc).and.not(enabledIs(true).or.hasFocusIs(true))</code>.
         * If you wish to use the <code>.not.</code> notation instead, leaving
         * off the parentheses, see {@link BinaryOperator#not}.
         *
         * @param otherFilter The filter to negate
         * @return A new filter that represents a combination of the left
         *         filter with "NOT otherFilter".
         */
        public AndroidViewFilter not(final AndroidViewFilter otherFilter)
        {
            return applySelfTo(primitiveNot(otherFilter));
        }


        // ----------------------------------------------------------
        /**
         * Implements a composite filter based on a binary operation,
         * where the "left"/"first" filter is the parent from which this
         * class was created, and the "right"/"second" filter is the
         * argument supplied to this operation.
         * @param otherFilter The argument to transform (second argument,
         *               for binary operators)
         * @return A new compound filter that represents a combination
         *         of the first and second filters.
         */
        @Override
        protected AndroidViewFilter applySelfTo(final AndroidViewFilter otherFilter)
        {
            return new AndroidViewFilter(description(
                AndroidViewFilter.this.toString(), otherFilter.toString()))
            {
                public boolean test(View view)
                {
                    return combine(AndroidViewFilter.this.test(view),
                        otherFilter.test(view));
                }
            };
        }


        // ----------------------------------------------------------
        /**
         * Concrete subclasses must override this to implement the
         * appropriate logic for combining the results of the two filters
         * being combined.
         * @param leftResult The boolean result of the left filter
         * @param rightResult The boolean result of the right filter
         * @return The result of this combined filter.
         */
        protected abstract boolean combine(
            boolean leftResult, boolean rightResult);


        // ----------------------------------------------------------
        /**
         * Concrete subclasses must override this to implement the
         * appropriate logic for building a description of this filter
         * based on the descriptions of the two filters
         * being combined.
         * @param leftDescription The description of the left filter
         * @param rightDescription The description of the right filter
         * @return The description of this combined filter.
         */
        protected abstract String description(
            String leftDescription, String rightDescription);
    }


    // ----------------------------------------------------------
    /**
     * Get a string representation of this filter.
     * @return A string representation of this filter.
     */
    public String toString()
    {
        return description;
    }


    // ----------------------------------------------------------
    /**
     * The "and" operator for combining filters, when you want to use
     * parentheses to group its righthand argument.  This method is designed
     * to be used in expressions like
     * <code>where.idIs(R.id.abc).and(enabledIs(true).or.hasFocusIs(true))</code>.
     * If you wish to use the <code>.and.</code> notation instead, leaving
     * off the parentheses, see {@link BinaryOperator#and(AndroidViewFilter)}.
     *
     * @param otherFilter  The second argument to "and".
     * @return A new filter object that represents "this AND otherFilter".
     */
    public final AndroidViewFilter and(final AndroidViewFilter otherFilter)
    {
        final AndroidViewFilter self = this;
        AndroidViewFilter gf =  new AndroidViewFilter("(" + this + " AND " + otherFilter + ")")
        {
            public boolean test(View view)
            {
               return self.test(view) && otherFilter.test(view);
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    /**
     * The "or" operator for combining filters, when you want to use
     * parentheses to group its righthand argument.  This method is designed
     * to be used in expressions like
     * <code>where.idIs(R.id.abc).or(enabledIs(true).and.hasFocusIs(true))</code>.
     * If you wish to use the <code>.or.</code> notation instead, leaving
     * off the parentheses, see {@link BinaryOperator#or(AndroidViewFilter)}.
     *
     * @param otherFilter  The second argument to "or".
     * @return A new filter object that represents "this OR otherFilter".
     */
    public final AndroidViewFilter or(final AndroidViewFilter otherFilter)
    {
        final AndroidViewFilter self = this;
        AndroidViewFilter gf = new AndroidViewFilter("(" + this + " OR " + otherFilter + ")")
        {
            public boolean test(View view)
            {
                return self.test(view) || otherFilter.test(view);
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    /**
     * Evaluate whether a view matches this filter.  This operation is
     * intended to be overridden by each subclass to implement the actual
     * check that a specific kind of filter performs.
     * @param view The view to check
     * @return true if the view matches this filter
     */
    public abstract boolean test(View view);


    // ----------------------------------------------------------
    /**
     * This class represents the "where" operator that is used to begin
     * a filter expression.  Client classes that wish to support filter
     * syntax should declare a final field (static or instance) like
     * this:
     * <pre>
     * public static final AndroidViewFilter.WhereOperator where =
     *     new AndroidViewFilter.WhereOperator();
     * </pre>
     */
    public static class ClientImports
    {
        // ----------------------------------------------------------
        /**
         * This object represents the "where" operator that is used to begin
         * a filter expression.
         */
        public static final Operator where = new Operator() {
            // ----------------------------------------------------------
            @Override
            protected AndroidViewFilter applySelfTo(AndroidViewFilter filter)
            {
                return filter;
            }
        };


        // ----------------------------------------------------------
        /**
         * The "not" operator for negating an existing filter, when the not
         * operation is at the very beginning of the expression.  This
         * method is designed to be used in expressions like
         * <code>not(where.enabledIs(true).or.hasFocusIs(true))</code>.
         *
         * @param otherFilter The filter to negate
         * @return A new filter that represents a combination of the left
         *         filter with "NOT otherFilter".
         */
        public static AndroidViewFilter not(final AndroidViewFilter otherFilter)
        {
            return primitiveNot(otherFilter);
        }
    }


    //~ Private Methods/Declarations ..........................................

    // ----------------------------------------------------------
    private static AndroidViewFilter idIs(final int id)
    {
        AndroidViewFilter gf = new AndroidViewFilter("id = \"" + id + "\"")
        {
            public boolean test(View view)
            {
                return view.getId() == id;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static AndroidViewFilter textIs(final String text)
    {
        AndroidViewFilter gf = new AndroidViewFilter("text = \"" + text + "\"")
        {
            public boolean test(View view)
            {
                Method m = null;
                try
                {
                    m = view.getClass().getMethod("getText");
                    return ((CharSequence) m.invoke(view)).equals(text);
                }
                catch (Exception e)
                {
                    return false;
                }
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static AndroidViewFilter textContains(final String text)
    {
        AndroidViewFilter gf = new AndroidViewFilter("text = \"" + text + "\"")
        {
            public boolean test(View view)
            {
                Method m = null;
                try
                {
                    m = view.getClass().getMethod("getText");
                    return ((CharSequence) m.invoke(view)).toString().contains(
                        text);
                }
                catch (Exception e)
                {
                    return false;
                }
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static AndroidViewFilter hasFocusIs(final boolean value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("hasFocus = " + value)
        {
            public boolean test(View view)
            {
                return view.hasFocus() == value;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter enabledIs(final boolean value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("enabled = " + value)
        {
            public boolean test(View view)
            {
                return view.isEnabled() == value;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter visibilityIs(final boolean value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("visibility = " + value)
        {
            public boolean test(View view)
            {
                return value == (view.getVisibility() == View.VISIBLE);
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter visibilityIs(final int value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("visibility = " + value)
        {
            public boolean test(View view)
            {
                return view.getVisibility() == value;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter widthIs(final int value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("width = " + value)
        {
            public boolean test(View view)
            {
                return view.getWidth() == value;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter heightIs(final int value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("height = " + value)
        {
            public boolean test(View view)
            {
                return view.getHeight() == value;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter sizeIs(final int width, final int height)
    {
        AndroidViewFilter gf = new AndroidViewFilter("size = (" + width + ", " + height + ")")
        {
            public boolean test(View view)
            {
                return view.getWidth() == width
                    && view.getHeight() == height;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter sizeIsWithin(
        final int maxWidth, final int maxHeight)
    {
        AndroidViewFilter gf = new AndroidViewFilter(
            "sizeIsWithin(" + maxWidth + ", " + maxHeight + ")")
        {
            public boolean test(View view)
            {
                return view.getWidth() <= maxWidth
                    && view.getHeight() <= maxHeight;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter xLocationIs(final int value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("xLocation = " + value)
        {
            public boolean test(View view)
            {
                return view.getLeft() == value;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter yLocationIs(final int value)
    {
        AndroidViewFilter gf = new AndroidViewFilter("yLocation = " + value)
        {
            public boolean test(View view)
            {
                return view.getTop() == value;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter locationIs(final int x, final int y)
    {
        AndroidViewFilter gf = new AndroidViewFilter("location = (" + x + ", " + y + ")")
        {
            public boolean test(View view)
            {
                return view.getLeft() == x && view.getTop() == y;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter isLocatedWithin(final Rect region)
    {
        AndroidViewFilter gf = new AndroidViewFilter("isLocatedWithin(" + region + ")")
        {
            public boolean test(View view)
            {
                return region.contains(view.getLeft(), view.getTop());
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter isContainedWithin(final Rect region)
    {
        AndroidViewFilter gf = new AndroidViewFilter("isContainedWithin(" + region + ")")
        {
            public boolean test(View view)
            {
                Rect r = new Rect(view.getLeft(), view.getTop(),
                        view.getWidth(), view.getHeight());
                return region.contains(r);
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static AndroidViewFilter typeIs(final Class<? extends View> aClass)
    {
        AndroidViewFilter gf = new AndroidViewFilter("type = " + aClass.getSimpleName())
        {
            public boolean test(View view)
            {
                return aClass.isAssignableFrom(view.getClass());
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static AndroidViewFilter parentIs(final View parent)
    {
        AndroidViewFilter gf = new AndroidViewFilter("parent is " + parent)
        {
            public boolean test(View view)
            {
                return view.getParent() == parent;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static AndroidViewFilter ancestorIs(final ViewParent ancestor)
    {
        AndroidViewFilter gf = new AndroidViewFilter("ancestor is " + ancestor)
        {
            public boolean test(View view)
            {
                ViewParent vp = view.getParent();
                while (vp != null && vp != ancestor)
                {
                    vp = vp.getParent();
                }
                return vp != null;
            }
        };
        return gf;
    }


    // ----------------------------------------------------------
    private static final AndroidViewFilter primitiveNot(
        final AndroidViewFilter otherFilter)
    {
        return new AndroidViewFilter("(NOT " + otherFilter + ")")
        {
            public boolean test(View view)
            {
                return !otherFilter.test(view);
            }
        };
    }

}
