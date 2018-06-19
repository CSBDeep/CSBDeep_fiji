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

import java.io.Serializable;

import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.util.LocalizedFormats;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.PivotingStrategyInterface;


/**
 * Classic median of 3 strategy given begin and end indices.
 * @since 3.4
 */
public class MyMedianOf3PivotingStrategy< T extends RealType< T >> {

    /**{@inheritDoc}
     * This in specific makes use of median of 3 pivoting.
     * @return The index corresponding to a pivot chosen between the
     * first, middle and the last indices of the array slice
     * @throws MathIllegalArgumentException when indices exceeds range
     */
    public int pivotIndex(final RandomAccessibleInterval<T> work, final int begin, final int end)
        throws MathIllegalArgumentException {
        verifyValues(work, begin, end-begin, false);
        final int inclusiveEnd = end - 1;
        final int middle = begin + (inclusiveEnd - begin) / 2;
        RandomAccess<T> cursor = work.randomAccess();
        cursor.setPosition(begin, 0);
        final T wBegin = cursor.get().copy();
        cursor.setPosition(middle, 0);
        final T wMiddle = cursor.get().copy();
        cursor.setPosition(inclusiveEnd, 0);
        final T wEnd = cursor.get().copy();

        if (wBegin.compareTo(wMiddle) < 0) {
            if (wMiddle.compareTo(wEnd) < 0) {
                return middle;
            } else {
                return wBegin.compareTo(wEnd) < 0 ? inclusiveEnd : begin;
            }
        } else {
            if (wBegin.compareTo(wEnd) < 0) {
                return begin;
            } else {
                return wMiddle.compareTo(wEnd) < 0 ? inclusiveEnd : middle;
            }
        }
    }

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

}
