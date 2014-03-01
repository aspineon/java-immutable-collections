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

package org.javimmutable.collections.hash;

import org.javimmutable.collections.Cursor;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.array.trie32.Trie32HashTable;
import org.javimmutable.collections.common.AbstractJImmutableMap;
import org.javimmutable.collections.common.MutableDelta;

public class JImmutableHashMap<K, V>
        extends AbstractJImmutableMap<K, V>
{
    // we only new one instance of the transformations object
    private static final TransformsImpl TRANSFORMS = new TransformsImpl();

    // this is safe since the transformations object works for any possible K and V
    @SuppressWarnings("unchecked")
    private static final JImmutableHashMap EMPTY = new JImmutableHashMap(Trie32HashTable.of(TRANSFORMS));

    private final Trie32HashTable<K, V> values;

    private JImmutableHashMap(Trie32HashTable<K, V> values)
    {
        this.values = values;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> JImmutableHashMap<K, V> of()
    {
        return (JImmutableHashMap<K, V>)EMPTY;
    }

    @Override
    public V getValueOr(K key,
                        V defaultValue)
    {
        return values.getValueOr(key.hashCode(), key, defaultValue);
    }

    @Override
    public Holder<V> find(K key)
    {
        return values.findValue(key.hashCode(), key);
    }

    @Override
    public Holder<Entry<K, V>> findEntry(K key)
    {
        return values.findEntry(key.hashCode(), key);
    }

    @Override
    public JImmutableHashMap<K, V> assign(K key,
                                          V value)
    {
        final Trie32HashTable<K, V> newValues = values.assign(key.hashCode(), key, value);
        return (newValues == values) ? this : new JImmutableHashMap<K, V>(newValues);
    }

    @Override
    public JImmutableHashMap<K, V> delete(K key)
    {
        final Trie32HashTable<K, V> newValues = values.delete(key.hashCode(), key);
        return (newValues == values) ? this : ((newValues.size() == 0) ? JImmutableHashMap.<K, V>of() : new JImmutableHashMap<K, V>(newValues));
    }

    @Override
    public int size()
    {
        return values.size();
    }

    @Override
    public JImmutableHashMap<K, V> deleteAll()
    {
        return of();
    }

    @Override
    public Cursor<Entry<K, V>> cursor()
    {
        return values.cursor();
    }

    private static class TransformsImpl<K, V>
            implements Trie32HashTable.Transforms<K, V>
    {
        @Override
        public Object update(Holder<Object> oldLeaf,
                             K key,
                             V value,
                             MutableDelta delta)
        {
            if (oldLeaf.isEmpty()) {
                delta.add(1);
                return new SingleValueLeafNode<K, V>(key, value);
            } else {
                @SuppressWarnings("unchecked") final LeafNode<K, V> oldNode = (LeafNode<K, V>)oldLeaf.getValue();
                return oldNode.setValueForKey(key, value, delta);
            }
        }

        @Override
        public Holder<Object> delete(Object oldLeaf,
                                     K key,
                                     MutableDelta delta)
        {
            @SuppressWarnings("unchecked") final LeafNode<K, V> oldNode = (LeafNode<K, V>)oldLeaf;
            final LeafNode<K, V> newNode = oldNode.deleteValueForKey(key, delta);
            return (newNode == null || newNode.size() == 0) ? Holders.of() : Holders.<Object>of(newNode);
        }

        @Override
        public Holder<V> findValue(Object oldLeaf,
                                   K key)
        {
            @SuppressWarnings("unchecked") final LeafNode<K, V> oldNode = (LeafNode<K, V>)oldLeaf;
            return oldNode.getValueForKey(key);
        }

        @Override
        public Holder<Entry<K, V>> findEntry(Object oldLeaf,
                                             K key)
        {
            @SuppressWarnings("unchecked") final LeafNode<K, V> oldNode = (LeafNode<K, V>)oldLeaf;
            return Holders.fromNullable(oldNode.getEntryForKey(key));
        }

        @SuppressWarnings("unchecked")
        @Override
        public Cursor<Entry<K, V>> cursor(Object leaf)
        {
            return ((LeafNode<K, V>)leaf).cursor();
        }
    }
}
