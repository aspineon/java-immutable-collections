///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2014, Burton Computer Corporation
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//     Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//
//     Redistributions in binary form must reproduce the above copyright
//     notice, this list of conditions and the following disclaimer in
//     the documentation and/or other materials provided with the
//     distribution.
//
//     Neither the name of the Burton Computer Corporation nor the names
//     of its contributors may be used to endorse or promote products
//     derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.javimmutable.collections.array.trie32;

import org.javimmutable.collections.Cursor;
import org.javimmutable.collections.Cursorable;
import org.javimmutable.collections.Func1;
import org.javimmutable.collections.Func2;
import org.javimmutable.collections.Func3;
import org.javimmutable.collections.Func4;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.array.bit32.Bit32Array;
import org.javimmutable.collections.common.MutableDelta;
import org.javimmutable.collections.cursors.MultiTransformCursor;

/**
 * Similar to Trie32Array but uses caller provided function objects to manage the leaf values.
 * Abstracting the leaf operations into the Transforms object allows different hash implementations
 * to use different types of leaf classes.  Also it moves the added complexity of hash collision
 * detection and management out of this class and into the class that defines the transforms.
 * <p/>
 * The root is a Bit32Array 6 levels deep.  At all levels except the bottom level the values
 * stored in the arrays are other Bit32Arrays.  At the bottom level (leaf level) the values
 * are objects managed by the transform functions.  These functions provide the ability to
 * create and replace these objects, pull cursors and values from them, etc.
 */
public class Trie32HashTable<K, V>
        implements Cursorable<JImmutableMap.Entry<K, V>>
{
    private static final Bit32Array<Object> EMPTY_ARRAY = Bit32Array.of();

    private final Transforms<K, V> transforms;
    private final Bit32Array<Object> root;
    private final int size;

    public static class Transforms<K, V>
    {
        /**
         * Function to take the current leaf object (if there is one) and produce a new one
         * (possibly the same) with the specified key and value.  If there is not currently
         * a leaf for this key in the array the Holder will be empty.  The result must be
         * a non-null leaf object with the specified value associated with the specified key.
         * If this key was not previously present the function must add 1 to the delta
         * so that the size of the array can be properly maintained.
         */
        public final Func4<Holder<Object>, K, V, MutableDelta, Object> updater;

        /**
         * Function to take the current leaf object and produce a new one (possibly the same)
         * with the specified key removed.  If the key was previously present in the leaf
         * the function must subtract 1 from the delta so that the size of the array can be
         * properly maintained.
         */
        public final Func3<Object, K, MutableDelta, Holder<Object>> deleter;

        /**
         * Function to look for the specified key in the leaf object and return a Holder
         * that is empty if the key is not in the leaf or else contains the value associated
         * with the key.
         */
        public final Func2<Object, K, Holder<V>> valueGetter;

        /**
         * Function to look for the specified key in the leaf object and return a Holder
         * that is empty if the key is not in the leaf or else contains a JImmutableMap.Entry
         * associated with the key and value.
         */
        public final Func2<Object, K, Holder<JImmutableMap.Entry<K, V>>> entryGetter;

        /**
         * Function to return a (possibly empty) Cursor over all of the JImmutableMap.Entries
         * in the specified leaf object.
         */
        public final Func1<Object, Cursor<JImmutableMap.Entry<K, V>>> cursorGetter;

        public Transforms(Func4<Holder<Object>, K, V, MutableDelta, Object> updater,
                          Func3<Object, K, MutableDelta, Holder<Object>> deleter,
                          Func2<Object, K, Holder<V>> valueGetter,
                          Func2<Object, K, Holder<JImmutableMap.Entry<K, V>>> entryGetter,
                          Func1<Object, Cursor<JImmutableMap.Entry<K, V>>> cursorGetter)
        {
            this.updater = updater;
            this.deleter = deleter;
            this.valueGetter = valueGetter;
            this.entryGetter = entryGetter;
            this.cursorGetter = cursorGetter;
        }
    }

    private Trie32HashTable(Transforms<K, V> transforms,
                            Bit32Array<Object> root,
                            int size)
    {
        this.transforms = transforms;
        this.root = root;
        this.size = size;
    }

    public static <K, V> Trie32HashTable<K, V> of(Transforms<K, V> transforms)
    {
        return new Trie32HashTable<K, V>(transforms, Bit32Array.of(), 0);
    }

    public Trie32HashTable<K, V> assign(int index,
                                        K key,
                                        V value)
    {
        final MutableDelta delta = new MutableDelta();
        final Bit32Array<Object> newRoot = assign(root, index, 30, key, value, delta);
        return (newRoot == root) ? this : new Trie32HashTable<K, V>(transforms, newRoot, size + delta.getValue());
    }

    public Trie32HashTable<K, V> delete(int index,
                                        K key)
    {
        final MutableDelta delta = new MutableDelta();
        final Bit32Array<Object> newRoot = delete(root, index, 30, key, delta);
        return (newRoot == root) ? this : new Trie32HashTable<K, V>(transforms, newRoot, size + delta.getValue());
    }

    public Holder<V> findValue(int index,
                               K key)
    {
        final Holder<Object> entry = find(root, index, 30);
        if (entry.isEmpty()) {
            return Holders.of();
        } else {
            return transforms.valueGetter.apply(entry.getValue(), key);
        }
    }

    public Holder<JImmutableMap.Entry<K, V>> findEntry(int index,
                                                       K key)
    {
        final Holder<Object> entry = find(root, index, 30);
        if (entry.isEmpty()) {
            return Holders.of();
        } else {
            return transforms.entryGetter.apply(entry.getValue(), key);
        }
    }

    public int size()
    {
        return this.size;
    }

    public Cursor<JImmutableMap.Entry<K, V>> cursor()
    {
        return MultiTransformCursor.of(root.valuesCursor(), new CursorTransforminator(30));
    }

    @SuppressWarnings("unchecked")
    private Holder<Object> find(Bit32Array<Object> array,
                                int index,
                                int shift)
    {
        if (shift == 0) {
            // child contains key/value pairs
            final int childIndex = index & 0x1f;
            return array.find(childIndex);
        } else {
            // child contains next level of arrays
            final int childIndex = (index >>> shift) & 0x1f;
            final Bit32Array<Object> childArray = (Bit32Array<Object>)array.find(childIndex).getValueOr(EMPTY_ARRAY);
            return find(childArray, index, shift - 5);
        }
    }

    @SuppressWarnings("unchecked")
    private Bit32Array<Object> assign(Bit32Array<Object> array,
                                      int index,
                                      int shift,
                                      K key,
                                      V value,
                                      MutableDelta delta)
    {
        if (shift == 0) {
            // child contains key/value pairs
            final int childIndex = index & 0x1f;
            final Bit32Array<Object> newArray = array.assign(childIndex, transforms.updater.apply(array.find(childIndex), key, value, delta));
            return (newArray == array) ? array : newArray;
        } else {
            // child contains next level of arrays
            final int childIndex = (index >>> shift) & 0x1f;
            final Bit32Array<Object> oldChildArray = (Bit32Array<Object>)array.find(childIndex).getValueOr(EMPTY_ARRAY);
            final Bit32Array<Object> newChildArray = assign(oldChildArray, index, shift - 5, key, value, delta);
            return (oldChildArray == newChildArray) ? array : array.assign(childIndex, newChildArray);
        }
    }

    @SuppressWarnings("unchecked")
    private Bit32Array<Object> delete(Bit32Array<Object> array,
                                      int index,
                                      int shift,
                                      K key,
                                      MutableDelta delta)
    {
        if (shift == 0) {
            // child contains key/value pairs
            final int childIndex = index & 0x1f;
            final Holder<Object> oldLeaf = array.find(childIndex);
            if (oldLeaf.isEmpty()) {
                return array;
            } else {
                final Holder<Object> newLeaf = transforms.deleter.apply(oldLeaf.getValue(), key, delta);
                final Bit32Array<Object> newArray = newLeaf.isEmpty() ? array.delete(childIndex) : array.assign(childIndex, newLeaf.getValue());
                return (newArray == array) ? array : newArray;
            }
        } else {
            // child contains next level of arrays
            final int childIndex = (index >>> shift) & 0x1f;
            final Bit32Array<Object> oldChildArray = (Bit32Array<Object>)array.find(childIndex).getValueOr(null);
            if (oldChildArray == null) {
                return array;
            } else {
                final Bit32Array<Object> newChildArray = delete(oldChildArray, index, shift - 5, key, delta);
                if (oldChildArray == newChildArray) {
                    return array;
                } else if (newChildArray.size() == 0) {
                    return array.delete(childIndex);
                } else {
                    return array.assign(childIndex, newChildArray);
                }
            }
        }
    }

    /**
     * Transforminator (BEHOLD!!) that takes a Cursor of array (if shift > 0) or leaf (if shift == 0)
     * objects and returns a Cursor of the JImmutableMap.Entries stored in the children (if shift > 0)
     * or in the leaves (if shift == 0).
     */
    private class CursorTransforminator
            implements Func1<Object, Cursor<JImmutableMap.Entry<K, V>>>
    {
        private final int shift;

        private CursorTransforminator(int shift)
        {
            this.shift = shift;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Cursor<JImmutableMap.Entry<K, V>> apply(Object arrayValue)
        {
            if (shift > 0) {
                // the internal arrays contain other arrays as values
                Bit32Array<Object> array = (Bit32Array<Object>)arrayValue;
                return MultiTransformCursor.of(array.valuesCursor(), new CursorTransforminator(shift - 5));
            } else {
                // the leaf arrays contain value objects as values
                return transforms.cursorGetter.apply(arrayValue);
            }
        }
    }
}
