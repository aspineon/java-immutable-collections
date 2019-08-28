///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2018, Burton Computer Corporation
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
// HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECINDIRECINCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACSTRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package org.javimmutable.collections.hash.hamt;

import org.javimmutable.collections.Func1;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.InvariantCheckable;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.SplitableIterable;
import org.javimmutable.collections.SplitableIterator;
import org.javimmutable.collections.common.CollisionMap;
import org.javimmutable.collections.iterators.GenericIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HamtNode<K, V>
    extends GenericIterator.Iterable<JImmutableMap.Entry<K, V>>,
            SplitableIterable<JImmutableMap.Entry<K, V>>,
            InvariantCheckable

{
    Holder<V> find(int hashCode,
                   @Nonnull K hashKey);

    V getValueOr(int hashCode,
                 @Nonnull K hashKey,
                 V defaultValue);

    @Nonnull
    HamtNode<K, V> assign(@Nonnull CollisionMap<K, V> emptyMap,
                          int hashCode,
                          @Nonnull K hashKey,
                          @Nullable V value);

    @Nonnull
    HamtNode<K, V> update(@Nonnull CollisionMap<K, V> emptyMap,
                          int hashCode,
                          @Nonnull K hashKey,
                          @Nonnull Func1<Holder<V>, V> generator);

    @Nonnull
    HamtNode<K, V> delete(@Nonnull CollisionMap<K, V> emptyMap,
                          int hashCode,
                          @Nonnull K hashKey);

    boolean isEmpty();

    int size();

    @Override
    default void checkInvariants()
    {
    }

    @Override
    default SplitableIterator<JImmutableMap.Entry<K, V>> iterator()
    {
        return new GenericIterator<>(this, 0, size());
    }
}
