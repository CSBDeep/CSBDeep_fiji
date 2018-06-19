/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mpicbg.csbd.normalize;

import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.exception.*;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.stat.ranking.NaNStrategy;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.KthSelector;
import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.MedianOf3PivotingStrategy;

import java.util.Arrays;

public class MyPercentile< T extends RealType< T >> {

    /** Maximum number of partitioning pivots cached (each level double the number of pivots). */
    private static final int MAX_CACHED_LEVELS = 10;

    /** Maximum number of cached pivots in the pivots cached array */
    private static final int PIVOTS_HEAP_LENGTH = 0x1 << MAX_CACHED_LEVELS - 1;

    /** Default KthSelector used with default pivoting strategy */
    private final MyKthSelector kthSelector;

    private final EstimationType estimationType;

    /** NaN Handling of the input as defined by {@link NaNStrategy} */
    private final NaNStrategy nanStrategy;

    /** Determines what percentile is computed when evaluate() is activated
     * with no quantile argument */
    private double quantile;

    /** Cached pivots. */
    private int[] cachedPivots;

    /** Stored data. */
    private RandomAccessibleInterval<T> storedData;

    /**
     * Constructs a MyPercentile with the following defaults.
     * <ul>
     *   <li>default quantile: 50.0, can be reset with {@link #setQuantile(double)}</li>
     *   <li>default NaN strategy: {@link NaNStrategy#REMOVED}</li>
     *   <li>a KthSelector that makes use of {@link MedianOf3PivotingStrategy}</li>
     * </ul>
     */
    public MyPercentile() {
        // No try-catch or advertised exception here - arg is valid
        this(50.0);
    }

    /**
     * Constructs a MyPercentile with the specific quantile value and the following
     * <ul>
     *   <li>default NaN strategy: {@link NaNStrategy#REMOVED}</li>
     *   <li>a Kth Selector : {@link KthSelector}</li>
     * </ul>
     * @param quantile the quantile
     * @throws MathIllegalArgumentException  if p is not greater than 0 and less
     * than or equal to 100
     */
    public MyPercentile(final double quantile) throws MathIllegalArgumentException {
        this(quantile, NaNStrategy.REMOVED,
             new MyKthSelector(new MyMedianOf3PivotingStrategy()));
    }

    /**
     * Copy constructor, creates a new {@code MyPercentile} identical
     * to the {@code original}
     *
     * @param original the {@code MyPercentile} instance to copy
     * @throws NullArgumentException if original is null
     */
    public MyPercentile(final MyPercentile original) throws NullArgumentException {

        MathUtils.checkNotNull(original);
        estimationType   = original.getEstimationType();
        nanStrategy      = original.getNaNStrategy();
        kthSelector      = original.getKthSelector();

        setData(original.getDataRef());
        if (original.cachedPivots != null) {
            System.arraycopy(original.cachedPivots, 0, cachedPivots, 0, original.cachedPivots.length);
        }
        setQuantile(original.quantile);

    }

    /**
     * Constructs a MyPercentile with the specific quantile value,
     * {@link EstimationType}, {@link NaNStrategy} and {@link KthSelector}.
     *
     * @param quantile the quantile to be computed
     * @param nanStrategy one of {@link NaNStrategy} to handle with NaNs
     * @param kthSelector a {@link KthSelector} to use for pivoting during search
     * @throws MathIllegalArgumentException if p is not within (0,100]
     * @throws NullArgumentException if type or NaNStrategy passed is null
     */
    protected MyPercentile(final double quantile,
                           final NaNStrategy nanStrategy,
                           final MyKthSelector kthSelector)
        throws MathIllegalArgumentException {
        setQuantile(quantile);
        cachedPivots = null;
        estimationType = new EstimationLegacy();
        MathUtils.checkNotNull(nanStrategy);
        MathUtils.checkNotNull(kthSelector);
        this.nanStrategy = nanStrategy;
        this.kthSelector = kthSelector;
    }

    /**
     * Get a reference to the stored data array.
     * @return reference to the stored data array (may be null)
     */
    protected RandomAccessibleInterval<T> getDataRef() {
        return storedData;
    }

    /** {@inheritDoc} */
    public void setData(final RandomAccessibleInterval<T> values) {
        if (values == null) {
            cachedPivots = null;
        } else {
            cachedPivots = new int[PIVOTS_HEAP_LENGTH];
            Arrays.fill(cachedPivots, -1);
        }
        storedData = (values == null) ? null : values;
    }

    /**
     * Returns the result of evaluating the statistic over the stored data.
     * <p>
     * The stored array is the one which was set by previous calls to
     * {@link #setData(RandomAccessibleInterval<T>)}
     * </p>
     * @param p the percentile value to compute
     * @return the value of the statistic applied to the stored data
     * @throws MathIllegalArgumentException if p is not a valid quantile value
     * (p must be greater than 0 and less than or equal to 100)
     */
    public T evaluate(final double p) throws MathIllegalArgumentException {
        return evaluate(getDataRef(), p);
    }

    /**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array.
     * <p>
     * Calls to this method do not modify the internal <code>quantile</code>
     * state of this statistic.</p>
     * <p>
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>values</code> has length
     * <code>0</code></li>
     * <li>Returns (for any value of <code>p</code>) <code>values[0]</code>
     *  if <code>values</code> has length <code>1</code></li>
     * <li>Throws <code>MathIllegalArgumentException</code> if <code>values</code>
     * is null or p is not a valid quantile value (p must be greater than 0
     * and less than or equal to 100) </li>
     * </ul></p>
     * <p>
     * See {@link MyPercentile} for a description of the percentile estimation
     * algorithm used.</p>
     *
     * @param values input array of values
     * @param p the percentile value to compute
     * @return the percentile value or Double.NaN if the array is empty
     * @throws MathIllegalArgumentException if <code>values</code> is null
     *     or p is invalid
     */
    public T evaluate(final RandomAccessibleInterval<T> values, final double p)
    throws MathIllegalArgumentException {
//        test(values, 0, 0);
        return evaluate(values, 0, values.dimension(0), p);
    }

    /**
     * Returns an estimate of the <code>quantile</code>th percentile of the
     * designated values in the <code>values</code> array.  The quantile
     * estimated is determined by the <code>quantile</code> property.
     * <p>
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>length = 0</code></li>
     * <li>Returns (for any value of <code>quantile</code>)
     * <code>values[begin]</code> if <code>length = 1 </code></li>
     * <li>Throws <code>MathIllegalArgumentException</code> if <code>values</code>
     * is null, or <code>start</code> or <code>length</code> is invalid</li>
     * </ul></p>
     * <p>
     * See {@link MyPercentile} for a description of the percentile estimation
     * algorithm used.</p>
     *
     * @param values the input array
     * @param start index of the first array element to include
     * @param length the number of elements to include
     * @return the percentile value
     * @throws MathIllegalArgumentException if the parameters are not valid
     *
     */
    public T evaluate(final RandomAccessibleInterval<T> values, final int start, final int length)
    throws MathIllegalArgumentException {
        return evaluate(values, start, length, quantile);
    }

     /**
     * Returns an estimate of the <code>p</code>th percentile of the values
     * in the <code>values</code> array, starting with the element in (0-based)
     * position <code>begin</code> in the array and including <code>length</code>
     * values.
     * <p>
     * Calls to this method do not modify the internal <code>quantile</code>
     * state of this statistic.</p>
     * <p>
     * <ul>
     * <li>Returns <code>Double.NaN</code> if <code>length = 0</code></li>
     * <li>Returns (for any value of <code>p</code>) <code>values[begin]</code>
     *  if <code>length = 1 </code></li>
     * <li>Throws <code>MathIllegalArgumentException</code> if <code>values</code>
     *  is null , <code>begin</code> or <code>length</code> is invalid, or
     * <code>p</code> is not a valid quantile value (p must be greater than 0
     * and less than or equal to 100)</li>
     * </ul></p>
     * <p>
     * See {@link MyPercentile} for a description of the percentile estimation
     * algorithm used.</p>
     *
     * @param values array of input values
     * @param p  the percentile to compute
     * @param begin  the first (0-based) element to include in the computation
     * @param length  the number of array elements to include
     * @return  the percentile value
     * @throws MathIllegalArgumentException if the parameters are not valid or the
     * input array is null
     */
    public T evaluate(final RandomAccessibleInterval<T> values, final int begin,
                           final long length, final double p)
        throws MathIllegalArgumentException {

//        test(values, begin, length);
        if (p > 100 || p <= 0) {
            throw new OutOfRangeException(
                    LocalizedFormats.OUT_OF_BOUNDS_QUANTILE_VALUE, p, 0, 100);
        }
        if (length == 0) {
            return null;
        }
        if (length == 1) {
            return ((IterableInterval<T>)values).firstElement(); // always return single value for n = 1
        }

        final RandomAccessibleInterval<T> work = getWorkArray(values, begin, (int) length);
        final int[] pivotsHeap = getPivots(values);
        return work.dimension(0) == 0 ? null :
                    estimationType.evaluate(work, pivotsHeap, p, kthSelector);
    }

    /**
     * Returns the value of the quantile field (determines what percentile is
     * computed when evaluate() is called with no quantile argument).
     *
     * @return quantile set while construction or {@link #setQuantile(double)}
     */
    public double getQuantile() {
        return quantile;
    }

    /**
     * Sets the value of the quantile field (determines what percentile is
     * computed when evaluate() is called with no quantile argument).
     *
     * @param p a value between 0 < p <= 100
     * @throws MathIllegalArgumentException  if p is not greater than 0 and less
     * than or equal to 100
     */
    public void setQuantile(final double p) throws MathIllegalArgumentException {
        if (p <= 0 || p > 100) {
            throw new OutOfRangeException(
                    LocalizedFormats.OUT_OF_BOUNDS_QUANTILE_VALUE, p, 0, 100);
        }
        quantile = p;
    }

    /**
     * {@inheritDoc}
     */
    public MyPercentile copy() {
        return new MyPercentile(this);
    }

    /**
     * Get the work array to operate. Makes use of prior {@code storedData} if
     * it exists or else do a check on NaNs and copy a subset of the array
     * defined by begin and length parameters. The set {@link #nanStrategy} will
     * be used to either retain/remove/replace any NaNs present before returning
     * the resultant array.
     *
     * @param values the array of numbers
     * @param begin index to start reading the array
     * @param length the length of array to be read from the begin index
     * @return work array sliced from values in the range [begin,begin+length)
     * @throws MathIllegalArgumentException if values or indices are invalid
     */
    protected RandomAccessibleInterval<T> getWorkArray(final RandomAccessibleInterval<T> values, final int begin, final int length) {
//            final RandomAccessibleInterval<T> work;
//            if (values == getDataRef()) {
//                work = getDataRef();
//            } else {
//                switch (nanStrategy) {
//                case MAXIMAL:// Replace NaNs with +INFs
//                    work = replaceAndSlice(values, begin, length, Double.NaN, Double.POSITIVE_INFINITY);
//                    break;
//                case MINIMAL:// Replace NaNs with -INFs
//                    work = replaceAndSlice(values, begin, length, Double.NaN, Double.NEGATIVE_INFINITY);
//                    break;
//                case REMOVED:// Drop NaNs from data
//                    work = removeAndSlice(values, begin, length, Double.NaN);
//                    break;
//                case FAILED:// just throw exception as NaN is un-acceptable
//                    work = copyOf(values, begin, length);
//                    MathArrays.checkNotNaN(work);
//                    break;
//                default: //FIXED
//                    work = copyOf(values,begin,length);
//                    break;
//                }
//            }
//            return work;
	    return getDataRef();
    }

//    /**
//     * Make a copy of the array for the slice defined by array part from
//     * [begin, begin+length)
//     * @param values the input array
//     * @param begin start index of the array to include
//     * @param length number of elements to include from begin
//     * @return copy of a slice of the original array
//     */
//    private double[] copyOf(final RandomAccessibleInterval<T> values, final int begin, final int length) {
//        MathArrays.verifyValues(values, begin, length);
//        return MathArrays.copyOfRange(values, begin, begin + length);
//    }

//    /**
//     * Replace every occurrence of a given value with a replacement value in a
//     * copied slice of array defined by array part from [begin, begin+length).
//     * @param values the input array
//     * @param begin start index of the array to include
//     * @param length number of elements to include from begin
//     * @param original the value to be replaced with
//     * @param replacement the value to be used for replacement
//     * @return the copy of sliced array with replaced values
//     */
//    private static double[] replaceAndSlice(final RandomAccessibleInterval<T> values,
//                                            final int begin, final int length,
//                                            final double original,
//                                            final double replacement) {
//        final RandomAccessibleInterval<T> temp = copyOf(values, begin, length);
//        for(int i = 0; i < length; i++) {
//            temp[i] = Precision.equalsIncludingNaN(original, temp[i]) ?
//                      replacement : temp[i];
//        }
//        return temp;
//    }
//
//    /**
//     * Remove the occurrence of a given value in a copied slice of array
//     * defined by the array part from [begin, begin+length).
//     * @param values the input array
//     * @param begin start index of the array to include
//     * @param length number of elements to include from begin
//     * @param removedValue the value to be removed from the sliced array
//     * @return the copy of the sliced array after removing the removedValue
//     */
//    private RandomAccessibleInterval<T> removeAndSlice(final RandomAccessibleInterval<T> values,
//                                           final int begin, final int length,
//                                           final T removedValue) {
//        verifyValues(values, begin, length, false);
//        final RandomAccessibleInterval<T> temp;
//        //BitSet(length) to indicate where the removedValue is located
//        final BitSet bits = new BitSet(length);
//	    Cursor<T> cursor = values.cursor();
//        for (int i = begin; i < begin+length; i++) {
//        	cursor.setPosition(i);
//            if (cursor.get().compareTo(removedValue) == 0) {
//                bits.set(i - begin);
//            }
//        }
//        //Check if empty then create a new copy
//        if (bits.isEmpty()) {
//            temp = copyOf(values, begin, length); // Nothing removed, just copy
//        } else if(bits.cardinality() == length){
//            temp = new double[0];                 // All removed, just empty
//        }else {                                   // Some removable, so new
//            temp = new double[length - bits.cardinality()];
//            int start = begin;  //start index from source array (i.e values)
//            int dest = 0;       //dest index in destination array(i.e temp)
//            int nextOne = -1;   //nextOne is the index of bit set of next one
//            int bitSetPtr = 0;  //bitSetPtr is start index pointer of bitset
//            while ((nextOne = bits.nextSetBit(bitSetPtr)) != -1) {
//                final int lengthToCopy = nextOne - bitSetPtr;
//                System.arraycopy(values, start, temp, dest, lengthToCopy);
//                dest += lengthToCopy;
//                start = begin + (bitSetPtr = bits.nextClearBit(nextOne));
//            }
//            //Copy any residue past start index till begin+length
//            if (start < begin + length) {
//                System.arraycopy(values,start,temp,dest,begin + length - start);
//            }
//        }
//        return temp;
//    }

	public boolean verifyValues(final RandomAccessibleInterval<T> values, final int begin,
	                            final int length, final boolean allowEmpty) throws MathIllegalArgumentException {

		if (values == null) {
			throw new NullArgumentException(LocalizedFormats.INPUT_ARRAY);
		}

		if (begin < 0) {
			throw new NotPositiveException(LocalizedFormats.START_POSITION, Integer.valueOf(begin));
		}

		if (length < 0) {
			throw new NotPositiveException(LocalizedFormats.LENGTH, Integer.valueOf(length));
		}

		if (begin + length > values.dimension(0)) {
			throw new NumberIsTooLargeException(LocalizedFormats.SUBARRAY_ENDS_AFTER_ARRAY_END,
					Integer.valueOf(begin + length), Integer.valueOf((int) values.dimension(0)), true);
		}

		if (length == 0 && !allowEmpty) {
			return false;
		}

		return true;

	}

    /**
     * Get pivots which is either cached or a newly created one
     *
     * @param values array containing the input numbers
     * @return cached pivots or a newly created one
     */
    private int[] getPivots(final RandomAccessibleInterval<T> values) {
        final int[] pivotsHeap;
        if (values == getDataRef()) {
            pivotsHeap = cachedPivots;
        } else {
            pivotsHeap = new int[PIVOTS_HEAP_LENGTH];
            Arrays.fill(pivotsHeap, -1);
        }
        return pivotsHeap;
    }

    /**
     * Get the estimation {@link EstimationType type} used for computation.
     *
     * @return the {@code estimationType} set
     */
    public EstimationType getEstimationType() {
        return estimationType;
    }

    public NaNStrategy getNaNStrategy() {
        return nanStrategy;
    }

    public MyKthSelector getKthSelector() {
        return kthSelector;
    }

    public MyMedianOf3PivotingStrategy getPivotingStrategy() {
        return kthSelector.getPivotingStrategy();
    }

//    /**
//     * An enum for various estimation strategies of a percentile referred in
//     * <a href="http://en.wikipedia.org/wiki/Quantile">wikipedia on quantile</a>
//     * with the names of enum matching those of types mentioned in
//     * wikipedia.
//     * <p>
//     * Each enum corresponding to the specific type of estimation in wikipedia
//     * implements  the respective formulae that specializes in the below aspects
//     * <ul>
//     * <li>An <b>index method</b> to calculate approximate index of the
//     * estimate</li>
//     * <li>An <b>estimate method</b> to estimate a value found at the earlier
//     * computed index</li>
//     * <li>A <b> minLimit</b> on the quantile for which first element of sorted
//     * input is returned as an estimate </li>
//     * <li>A <b> maxLimit</b> on the quantile for which last element of sorted
//     * input is returned as an estimate </li>
//     * </ul>
//     * <p>
//     * Users can now create {@link MyPercentile} by explicitly passing this enum;
//     * such as by invoking {@link MyPercentile#withEstimationType(EstimationType)}
//     * <p>
//     * References:
//     * <ol>
//     * <li>
//     * <a href="http://en.wikipedia.org/wiki/Quantile">Wikipedia on quantile</a>
//     * </li>
//     * <li>
//     * <a href="https://www.amherst.edu/media/view/129116/.../Sample+Quantiles.pdf">
//     * Hyndman, R. J. and Fan, Y. (1996) Sample quantiles in statistical
//     * packages, American Statistician 50, 361–365</a> </li>
//     * <li>
//     * <a href="http://stat.ethz.ch/R-manual/R-devel/library/stats/html/quantile.html">
//     * R-Manual </a></li>
//     * </ol>
//     *
//     */
    private abstract class EstimationType {

        /** Simple name such as R-1, R-2 corresponding to those in wikipedia. */
        private final String name;

        /**
         * Constructor
         *
         * @param type name of estimation type as per wikipedia
         */
        EstimationType(final String type) {
            this.name = type;
        }

        protected abstract double index(final double p, final int length);

        protected T estimate(final RandomAccessibleInterval<T> work, final int[] pivotsHeap,
                                  final double pos, final int length,
                                  final MyKthSelector selector) {

            final double fpos = FastMath.floor(pos);
            final int intPos = (int) fpos;
            final double dif = pos - fpos;

            if (pos < 1) {
                return (T) selector.select(work, pivotsHeap, 0);
            }
            if (pos >= length) {
                return (T) selector.select(work, pivotsHeap, length - 1);
            }

            final T lower = (T) selector.select(work, pivotsHeap, intPos - 1);
            final T upper = (T) selector.select(work, pivotsHeap, intPos);
            final T res = upper.copy();
            res.sub(lower);
            res.mul(dif);
            res.add(lower);
            return res;
        }

        protected T evaluate(final RandomAccessibleInterval<T> work, final int[] pivotsHeap, final double p,
                             final MyKthSelector selector) {
            MathUtils.checkNotNull(work);
            if (p > 100 || p <= 0) {
                throw new OutOfRangeException(LocalizedFormats.OUT_OF_BOUNDS_QUANTILE_VALUE,
                                              p, 0, 100);
            }
            return estimate(work, pivotsHeap, index(p/100d, (int)work.dimension(0)), (int) work.dimension(0), selector);
        }

        public T evaluate(final RandomAccessibleInterval<T> work, final double p, final MyKthSelector selector) {
            return this.evaluate(work, null, p, selector);
        }

        /**
         * Gets the name of the enum
         *
         * @return the name
         */
        String getName() {
            return name;
        }
    }

    private class EstimationLegacy extends EstimationType {

        /**
     * This is the default type used in the {@link MyPercentile}.This method
     * has the following formulae for index and estimates<br>
     * \( \begin{align}
     * &amp;index    = (N+1)p\ \\
     * &amp;estimate = x_{\lceil h\,-\,1/2 \rceil} \\
     * &amp;minLimit = 0 \\
     * &amp;maxLimit = 1 \\
     * \end{align}\)
     */
        EstimationLegacy() {
            super("Legacy Apache Commons Math");
        }
        /**
         * {@inheritDoc}.This method in particular makes use of existing
         * Apache Commons Math style of picking up the index.
         */
        @Override
        protected double index(final double p, final int length) {
            final double minLimit = 0d;
            final double maxLimit = 1d;
            return Double.compare(p, minLimit) == 0 ? 0 :
                    Double.compare(p, maxLimit) == 0 ?
                            length : p * (length + 1);
        }
    }

//    private class EstimationR_1 extends EstimationType {
//    /**
//     * The method R_1 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index= Np + 1/2\,  \\
//     * &amp;estimate= x_{\lceil h\,-\,1/2 \rceil} \\
//     * &amp;minLimit = 0 \\
//     * \end{align}\)
//     */
//
//        EstimationR_1() {
//            super("R-1");
//        }
//
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 0d;
//            return Double.compare(p, minLimit) == 0 ? 0 : length * p + 0.5;
//        }
//
//        /**
//         * {@inheritDoc}This method in particular for R_1 uses ceil(pos-0.5)
//         */
//        @Override
//        protected T estimate(final RandomAccessibleInterval<T> values,
//        final int[] pivotsHeap, final double pos,
//        final int length, final KthSelector selector) {
//            return super.estimate(values, pivotsHeap, FastMath.ceil(pos - 0.5), length, selector);
//        }
//
//    }
//    /**
//     * The method R_2 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index= Np + 1/2\, \\
//     * &amp;estimate=\frac{x_{\lceil h\,-\,1/2 \rceil} +
//     * x_{\lfloor h\,+\,1/2 \rfloor}}{2} \\
//     * &amp;minLimit = 0 \\
//     * &amp;maxLimit = 1 \\
//     * \end{align}\)
//     */
//    R_2("R-2") {
//
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 0d;
//            final double maxLimit = 1d;
//            return Double.compare(p, maxLimit) == 0 ? length :
//                    Double.compare(p, minLimit) == 0 ? 0 : length * p + 0.5;
//        }
//
//        /**
//         * {@inheritDoc}This method in particular for R_2 averages the
//         * values at ceil(p+0.5) and floor(p-0.5).
//         */
//        @Override
//        protected double estimate(final RandomAccessibleInterval<T> values,
//        final int[] pivotsHeap, final double pos,
//        final int length, final KthSelector selector) {
//            final double low =
//                    super.estimate(values, pivotsHeap, FastMath.ceil(pos - 0.5), length, selector);
//            final double high =
//                    super.estimate(values, pivotsHeap,FastMath.floor(pos + 0.5), length, selector);
//            return (low + high) / 2;
//        }
//
//    },
//    /**
//     * The method R_3 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index= Np \\
//     * &amp;estimate= x_{\lfloor h \rceil}\, \\
//     * &amp;minLimit = 0.5/N \\
//     * \end{align}\)
//     */
//    R_3("R-3") {
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 1d/2 / length;
//            return Double.compare(p, minLimit) <= 0 ?
//                    0 : FastMath.rint(length * p);
//        }
//
//    },
//    /**
//     * The method R_4 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index= Np\, \\
//     * &amp;estimate= x_{\lfloor h \rfloor} + (h -
//     * \lfloor h \rfloor) (x_{\lfloor h \rfloor + 1} - x_{\lfloor h
//     * \rfloor}) \\
//     * &amp;minLimit = 1/N \\
//     * &amp;maxLimit = 1 \\
//     * \end{align}\)
//     */
//    R_4("R-4") {
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 1d / length;
//            final double maxLimit = 1d;
//            return Double.compare(p, minLimit) < 0 ? 0 :
//                    Double.compare(p, maxLimit) == 0 ? length : length * p;
//        }
//
//    },
//    /**
//     * The method R_5 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index= Np + 1/2\\
//     * &amp;estimate= x_{\lfloor h \rfloor} + (h -
//     * \lfloor h \rfloor) (x_{\lfloor h \rfloor + 1} - x_{\lfloor h
//     * \rfloor}) \\
//     * &amp;minLimit = 0.5/N \\
//     * &amp;maxLimit = (N-0.5)/N
//     * \end{align}\)
//     */
//    R_5("R-5"){
//
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 1d/2 / length;
//            final double maxLimit = (length - 0.5) / length;
//            return Double.compare(p, minLimit) < 0 ? 0 :
//                    Double.compare(p, maxLimit) >= 0 ?
//                            length : length * p + 0.5;
//        }
//    },
//    /**
//     * The method R_6 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index= (N + 1)p \\
//     * &amp;estimate= x_{\lfloor h \rfloor} + (h -
//     * \lfloor h \rfloor) (x_{\lfloor h \rfloor + 1} - x_{\lfloor h
//     * \rfloor}) \\
//     * &amp;minLimit = 1/(N+1) \\
//     * &amp;maxLimit = N/(N+1) \\
//     * \end{align}\)
//     * <p>
//     * <b>Note:</b> This method computes the index in a manner very close to
//     * the default Commons Math MyPercentile existing implementation. However
//     * the difference to be noted is in picking up the limits with which
//     * first element (p&lt;1(N+1)) and last elements (p&gt;N/(N+1))are done.
//     * While in default case; these are done with p=0 and p=1 respectively.
//     */
//    R_6("R-6"){
//
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 1d / (length + 1);
//            final double maxLimit = 1d * length / (length + 1);
//            return Double.compare(p, minLimit) < 0 ? 0 :
//                    Double.compare(p, maxLimit) >= 0 ?
//                            length : (length + 1) * p;
//        }
//    },
//
//    /**
//     * The method R_7 implements Microsoft Excel style computation has the
//     * following formulae for index and estimates.<br>
//     * \( \begin{align}
//     * &amp;index = (N-1)p + 1 \\
//     * &amp;estimate = x_{\lfloor h \rfloor} + (h -
//     * \lfloor h \rfloor) (x_{\lfloor h \rfloor + 1} - x_{\lfloor h
//     * \rfloor}) \\
//     * &amp;minLimit = 0 \\
//     * &amp;maxLimit = 1 \\
//     * \end{align}\)
//     */
//    R_7("R-7") {
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 0d;
//            final double maxLimit = 1d;
//            return Double.compare(p, minLimit) == 0 ? 0 :
//                    Double.compare(p, maxLimit) == 0 ?
//                            length : 1 + (length - 1) * p;
//        }
//
//    },
//
//    /**
//     * The method R_8 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index = (N + 1/3)p + 1/3  \\
//     * &amp;estimate = x_{\lfloor h \rfloor} + (h -
//     \lfloor h \rfloor) (x_{\lfloor h \rfloor + 1} - x_{\lfloor h
//     * \rfloor}) \\
//     * &amp;minLimit = (2/3)/(N+1/3) \\
//     * &amp;maxLimit = (N-1/3)/(N+1/3) \\
//     * \end{align}\)
//     * <p>
//     * As per Ref [2,3] this approach is most recommended as it provides
//     * an approximate median-unbiased estimate regardless of distribution.
//     */
//    R_8("R-8") {
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 2 * (1d / 3) / (length + 1d / 3);
//            final double maxLimit =
//                    (length - 1d / 3) / (length + 1d / 3);
//            return Double.compare(p, minLimit) < 0 ? 0 :
//                    Double.compare(p, maxLimit) >= 0 ? length :
//                            (length + 1d / 3) * p + 1d / 3;
//        }
//    },
//
//    /**
//     * The method R_9 has the following formulae for index and estimates<br>
//     * \( \begin{align}
//     * &amp;index = (N + 1/4)p + 3/8\\
//     * &amp;estimate = x_{\lfloor h \rfloor} + (h -
//     \lfloor h \rfloor) (x_{\lfloor h \rfloor + 1} - x_{\lfloor h
//     * \rfloor}) \\
//     * &amp;minLimit = (5/8)/(N+1/4) \\
//     * &amp;maxLimit = (N-3/8)/(N+1/4) \\
//     * \end{align}\)
//     */
//    R_9("R-9") {
//        @Override
//        protected double index(final double p, final int length) {
//            final double minLimit = 5d/8 / (length + 0.25);
//            final double maxLimit = (length - 3d/8) / (length + 0.25);
//            return Double.compare(p, minLimit) < 0 ? 0 :
//                    Double.compare(p, maxLimit) >= 0 ? length :
//                            (length + 0.25) * p + 3d/8;
//        }
//
//    },
}
