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

import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * A Simple K<sup>th</sup> selector implementation to pick up the
 * K<sup>th</sup> ordered element from a work array containing the input
 * numbers.
 * @since 3.4
 */
public class MyKthSelector< T extends RealType< T >>  {

    /** Minimum selection size for insertion sort rather than selection. */
    private static final int MIN_SELECT_SIZE = 15;

    /** A {@link PivotingStrategyInterface} used for pivoting  */
    private final MyMedianOf3PivotingStrategy pivotingStrategy;

    /**
     * Constructor with default {@link MedianOf3PivotingStrategy median of 3} pivoting strategy
     */
    public MyKthSelector() {
        this.pivotingStrategy = new MyMedianOf3PivotingStrategy();
    }

    /**
     * Constructor with specified pivoting strategy
     *
     * @param pivotingStrategy pivoting strategy to use
     * @throws NullArgumentException when pivotingStrategy is null
     * @see MedianOf3PivotingStrategy
     * @see RandomPivotingStrategy
     * @see CentralPivotingStrategy
     */
    public MyKthSelector(final MyMedianOf3PivotingStrategy pivotingStrategy)
        throws NullArgumentException {
        MathUtils.checkNotNull(pivotingStrategy);
        this.pivotingStrategy = pivotingStrategy;
    }

    /** Get the pivotin strategy.
     * @return pivoting strategy
     */
    public MyMedianOf3PivotingStrategy getPivotingStrategy() {
        return pivotingStrategy;
    }

    /**
     * Select K<sup>th</sup> value in the array.
     *
     * @param work work array to use to find out the K<sup>th</sup> value
     * @param pivotsHeap cached pivots heap that can be used for efficient estimation
     * @param k the index whose value in the array is of interest
     * @return K<sup>th</sup> value
     */
    public T select(final RandomAccessibleInterval<T> work, final int[] pivotsHeap, final int k) {
        int begin = 0;
        int end = (int)work.dimension(0);
        int node = 0;
        final boolean usePivotsHeap = pivotsHeap != null;
        while (end - begin > MIN_SELECT_SIZE) {
            final int pivot;

            if (usePivotsHeap && node < pivotsHeap.length &&
                    pivotsHeap[node] >= 0) {
                // the pivot has already been found in a previous call
                // and the array has already been partitioned around it
                pivot = pivotsHeap[node];
            } else {
                // select a pivot and partition work array around it
                pivot = partition(work, begin, end, pivotingStrategy.pivotIndex(work, begin, end));
                if (usePivotsHeap && node < pivotsHeap.length) {
                    pivotsHeap[node] = pivot;
                }
            }

            if (k == pivot) {
                // the pivot was exactly the element we wanted
                final RandomAccess<T> cursor = work.randomAccess();
                cursor.setPosition(k, 0);
                return cursor.get().copy();
            } else if (k < pivot) {
                // the element is in the left partition
                end  = pivot;
                node = FastMath.min(2 * node + 1, usePivotsHeap ? pivotsHeap.length : end);
            } else {
                // the element is in the right partition
                begin = pivot + 1;
                node  = FastMath.min(2 * node + 2, usePivotsHeap ? pivotsHeap.length : end);
            }
        }

        List<T> part = new ArrayList<T>();

        final RandomAccess<T> cursor = work.randomAccess();
        for(int i = begin; i < end; i++) {
            cursor.setPosition(i, 0);
            part.add(cursor.get().copy());
        }

        part.sort(Comparator.naturalOrder());

//        Arrays.sort(work, begin, end);

        for(int i = begin; i < end; i++) {
            cursor.setPosition(i, 0);
            cursor.get().set(part.get(i-begin));
        }

        cursor.setPosition(k, 0);
        return cursor.get().copy();
    }

    /**
     * Partition an array slice around a pivot.Partitioning exchanges array
     * elements such that all elements smaller than pivot are before it and
     * all elements larger than pivot are after it.
     *
     * @param work work array
     * @param begin index of the first element of the slice of work array
     * @param end index after the last element of the slice of work array
     * @param pivot initial index of the pivot
     * @return index of the pivot after partition
     */
    private int partition(final RandomAccessibleInterval<T> work, final int begin, final int end, final int pivot) {

        RandomAccess<T> beginRA = work.randomAccess();
        RandomAccess<T> endRA = beginRA.copyRandomAccess();
        RandomAccess<T> pivotRA = beginRA.copyRandomAccess();

        beginRA.setPosition(begin, 0);

        pivotRA.setPosition(pivot, 0);
        final T value = pivotRA.get().copy();
        pivotRA.get().set(beginRA.get().copy());

        int i = begin + 1;
        int j = end - 1;

        pivotRA.setPosition(i, 0);
        endRA.setPosition(j, 0);

        while (i < j) {
            while (i < j && endRA.get().compareTo(value) > 0) {
                endRA.setPosition(--j, 0);
            }
            while (i < j && pivotRA.get().compareTo(value) < 0) {
                pivotRA.setPosition(++i, 0);
            }

            if (i < j) {
                final T tmp = pivotRA.get().copy();
                pivotRA.get().set(endRA.get().copy());
                endRA.get().set(tmp);
                pivotRA.setPosition(++i, 0);
                endRA.setPosition(--j, 0);
            }
        }

        if (i >= end || pivotRA.get().compareTo(value) > 0) {
            pivotRA.setPosition(--i, 0);
        }
        beginRA.get().set(pivotRA.get().copy());
        pivotRA.get().set(value);
        return i;
    }
}
