/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Extend functionality of ArrayList
 */
@SuppressWarnings("ALL")
public class ArrayListEx<E> extends ArrayList<E> {

    @SuppressWarnings("unused")
    public ArrayListEx(int initialCapacity) {
        super(initialCapacity);
    }

    public ArrayListEx(Collection<? extends E> collection) {
        super(collection);
    }


    public ArrayListEx(E singleItem) {
        super(Arrays.asList(singleItem));
    }

    public ArrayListEx() {
        super();
    }

    public static <E> E remove(ArrayListEx<E> list, int idx) {
        return (list == null || idx >= list.size()) ? null : list.remove(idx);
    }

    @NonNull
    public static <E> E get(ArrayList<E> list, int idx, E defValue) {
        return (list != null && idx >= 0 && idx < list.size() && list.get(idx) != null)
                ? list.get(idx) : defValue;
    }

    public static <E> E first(ArrayListEx<E> list, E def) {
        return list == null ? def : list.first(def);
    }

    public static <E> E last(ArrayListEx<E> list, E def) {
        return (list == null) ? def : list.last(def);
    }

    public static <E> int size(ArrayListEx<E> list, int def) {
        return (list == null) ? def : list.size();
    }

    // ---------------------------------------------------------------------------------------------
    // Static

    @SuppressWarnings("SimplifiableIfStatement")
    public static <E> boolean equals(List<E> list1, List<E> list2) {
        if (list1 == list2) {
            return true;
        }
        if (list1 == null || list2 == null) {
            return false;
        }
        return list1.equals(list2);
    }

    /**
     * Merge two lists avoid duplicates. List2 add after list1.
     */
    @Nullable
    public static <F extends ArrayList<E>, E> ArrayListEx<E> merge(@Nullable F list1, @Nullable F list2) {
        if (list1 == null) {
            return list2 == null ? null : new ArrayListEx<E>(list2);
        } else if (list2 == null) {
            return new ArrayListEx<E>(list1);
        }

        ArrayListEx<E> outList = new ArrayListEx<>(list1.size() + list2.size());
        outList.addAll(list1);
        for (E item : list2) {
            if (!outList.contains(item)) {
                outList.add(item);
            }
        }

        return outList;
    }

    public E get(int idx, E defValue) {
        return (idx >= 0 && idx < size()) ? get(idx) : defValue;
    }

    public E first(E def) {
        return isEmpty() ? def : get(0);
    }

    public E last(E def) {
        return isEmpty() ? def : get(size() - 1);
    }

    @SafeVarargs
    public final void addAll(E... values) {
        ensureCapacity(size() + values.length);
        for (E value : values) {
            add(value);
        }
    }
}
