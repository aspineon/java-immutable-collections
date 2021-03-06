///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2019, Burton Computer Corporation
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

package org.javimmutable.collections.list;

import org.javimmutable.collections.Func2;
import org.javimmutable.collections.InvariantCheckable;
import org.javimmutable.collections.Proc1Throws;
import org.javimmutable.collections.SplitableIterable;
import org.javimmutable.collections.Sum1Throws;
import org.javimmutable.collections.common.CollisionMap;
import org.javimmutable.collections.iterators.GenericIterator;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

abstract class AbstractNode<T>
    implements SplitableIterable<T>,
               InvariantCheckable,
               GenericIterator.Iterable<T>,
               CollisionMap.Node
{
    abstract boolean isEmpty();

    abstract int size();

    abstract int depth();

    abstract T get(int index);

    @Nonnull
    abstract AbstractNode<T> append(T value);

    @Nonnull
    abstract AbstractNode<T> append(@Nonnull AbstractNode<T> node);

    @Nonnull
    abstract AbstractNode<T> prepend(T value);

    @Nonnull
    abstract AbstractNode<T> prepend(@Nonnull AbstractNode<T> node);

    @Nonnull
    abstract AbstractNode<T> assign(int index,
                                    T value);

    @Nonnull
    abstract AbstractNode<T> insert(int index,
                                    T value);

    @Nonnull
    abstract AbstractNode<T> deleteFirst();

    @Nonnull
    abstract AbstractNode<T> deleteLast();

    @Nonnull
    abstract AbstractNode<T> delete(int index);

    @Nonnull
    abstract AbstractNode<T> prefix(int limit);

    @Nonnull
    abstract AbstractNode<T> suffix(int offset);

    @Override
    public abstract void checkInvariants();

    abstract void copyTo(T[] array,
                         int offset);

    @Nonnull
    AbstractNode<T> left()
    {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    AbstractNode<T> right()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int iterableSize()
    {
        return size();
    }

    @Override
    public abstract void forEach(Consumer<? super T> action);

    @Override
    public abstract <E extends Exception> void forEachThrows(@Nonnull Proc1Throws<T, E> proc)
        throws E;

    @Override
    public abstract <V> V reduce(V initialValue,
                                 Func2<V, T, V> accumulator);

    @Override
    public abstract <V, E extends Exception> V reduceThrows(V initialValue,
                                                            Sum1Throws<T, V, E> accumulator)
        throws E;
}
