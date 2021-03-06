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

package org.javimmutable.collections.hash.hamt;

import org.javimmutable.collections.Func1;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.Indexed;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.Proc2;
import org.javimmutable.collections.Proc2Throws;
import org.javimmutable.collections.Sum2;
import org.javimmutable.collections.Sum2Throws;
import org.javimmutable.collections.common.ArrayHelper;
import org.javimmutable.collections.common.CollisionMap;
import org.javimmutable.collections.iterators.GenericIterator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@Immutable
public class HamtBranchNode<K, V>
    implements ArrayHelper.Allocator<HamtNode<K, V>>,
               HamtNode<K, V>
{
    private static final HamtBranchNode[] EMPTY_NODES = new HamtBranchNode[0];

    static final int SHIFT = 5;
    static final int MASK = 0x1f;

    private final int bitmask;
    @Nonnull
    private final CollisionMap.Node value;
    @Nonnull
    private final HamtNode<K, V>[] children;
    private final int size;

    HamtBranchNode(int bitmask,
                   @Nonnull CollisionMap.Node value,
                   @Nonnull HamtNode<K, V>[] children,
                   int size)
    {
        this.bitmask = bitmask;
        this.value = value;
        this.children = children;
        this.size = size;
    }

    @SuppressWarnings("unchecked")
    static <K, V> HamtNode<K, V> forLeafExpansion(@Nonnull CollisionMap<K, V> collisionMap,
                                                  int hashCode,
                                                  @Nonnull CollisionMap.Node value)
    {
        if (hashCode == 0) {
            return new HamtBranchNode<>(0, value, EMPTY_NODES, collisionMap.size(value));
        } else {
            final int index = hashCode & MASK;
            final int remainder = hashCode >>> SHIFT;
            final int bit = 1 << index;
            final HamtNode<K, V>[] children = new HamtNode[1];
            children[0] = new HamtLeafNode<>(remainder, value);
            return new HamtBranchNode<>(bit, collisionMap.emptyNode(), children, collisionMap.size(value));
        }
    }

    @Override
    public int size(@Nonnull CollisionMap<K, V> collisionMap)
    {
        return size;
    }

    @Override
    public Holder<V> find(@Nonnull CollisionMap<K, V> collisionMap,
                          int hashCode,
                          @Nonnull K hashKey)
    {
        if (hashCode == 0) {
            return collisionMap.findValue(value, hashKey);
        }
        final int index = hashCode & MASK;
        final int remainder = hashCode >>> SHIFT;
        final int bit = 1 << index;
        final int bitmask = this.bitmask;
        if ((bitmask & bit) == 0) {
            return Holders.of();
        } else {
            final int childIndex = realIndex(bitmask, bit);
            return children[childIndex].find(collisionMap, remainder, hashKey);
        }
    }

    @Override
    public V getValueOr(@Nonnull CollisionMap<K, V> collisionMap,
                        int hashCode,
                        @Nonnull K hashKey,
                        V defaultValue)
    {
        if (hashCode == 0) {
            return collisionMap.getValueOr(value, hashKey, defaultValue);
        }
        final int index = hashCode & MASK;
        final int remainder = hashCode >>> SHIFT;
        final int bit = 1 << index;
        final int bitmask = this.bitmask;
        if ((bitmask & bit) == 0) {
            return defaultValue;
        } else {
            final int childIndex = realIndex(bitmask, bit);
            return children[childIndex].getValueOr(collisionMap, remainder, hashKey, defaultValue);
        }
    }

    @Override
    @Nonnull
    public HamtNode<K, V> assign(@Nonnull CollisionMap<K, V> collisionMap,
                                 int hashCode,
                                 @Nonnull K hashKey,
                                 @Nullable V value)
    {
        final HamtNode<K, V>[] children = this.children;
        final int bitmask = this.bitmask;
        final CollisionMap.Node thisValue = this.value;
        if (hashCode == 0) {
            final CollisionMap.Node newValue = collisionMap.update(thisValue, hashKey, value);
            if (thisValue == newValue) {
                return this;
            } else {
                return new HamtBranchNode<>(bitmask, newValue, children, size - collisionMap.size(thisValue) + collisionMap.size(newValue));
            }
        }
        final int index = hashCode & MASK;
        final int remainder = hashCode >>> SHIFT;
        final int bit = 1 << index;
        final int childIndex = realIndex(bitmask, bit);
        if ((bitmask & bit) == 0) {
            final HamtNode<K, V> newChild = new HamtLeafNode<>(remainder, collisionMap.update(collisionMap.emptyNode(), hashKey, value));
            final HamtNode<K, V>[] newChildren = ArrayHelper.insert(this, children, childIndex, newChild);
            return new HamtBranchNode<>(bitmask | bit, thisValue, newChildren, size + 1);
        } else {
            final HamtNode<K, V> child = children[childIndex];
            final HamtNode<K, V> newChild = child.assign(collisionMap, remainder, hashKey, value);
            if (newChild == child) {
                return this;
            } else {
                final HamtNode<K, V>[] newChildren = ArrayHelper.assign(children, childIndex, newChild);
                return new HamtBranchNode<>(bitmask, thisValue, newChildren, size - child.size(collisionMap) + newChild.size(collisionMap));
            }
        }
    }

    @Nonnull
    @Override
    public HamtNode<K, V> update(@Nonnull CollisionMap<K, V> collisionMap,
                                 int hashCode,
                                 @Nonnull K hashKey,
                                 @Nonnull Func1<Holder<V>, V> generator)
    {
        final HamtNode<K, V>[] children = this.children;
        final int bitmask = this.bitmask;
        final CollisionMap.Node thisValue = this.value;
        if (hashCode == 0) {
            final CollisionMap.Node newValue = collisionMap.update(thisValue, hashKey, generator);
            if (thisValue == newValue) {
                return this;
            } else {
                return new HamtBranchNode<>(bitmask, newValue, children, size - collisionMap.size(thisValue) + collisionMap.size(newValue));
            }
        }
        final int index = hashCode & MASK;
        final int remainder = hashCode >>> SHIFT;
        final int bit = 1 << index;
        final int childIndex = realIndex(bitmask, bit);
        if ((bitmask & bit) == 0) {
            final HamtNode<K, V> newChild = new HamtLeafNode<>(remainder, collisionMap.update(collisionMap.emptyNode(), hashKey, generator));
            final HamtNode<K, V>[] newChildren = ArrayHelper.insert(this, children, childIndex, newChild);
            return new HamtBranchNode<>(bitmask | bit, thisValue, newChildren, size + 1);
        } else {
            final HamtNode<K, V> child = children[childIndex];
            final HamtNode<K, V> newChild = child.update(collisionMap, remainder, hashKey, generator);
            if (newChild == child) {
                return this;
            } else {
                final HamtNode<K, V>[] newChildren = ArrayHelper.assign(children, childIndex, newChild);
                return new HamtBranchNode<>(bitmask, thisValue, newChildren, size - child.size(collisionMap) + newChild.size(collisionMap));
            }
        }
    }

    @Override
    @Nonnull
    public HamtNode<K, V> delete(@Nonnull CollisionMap<K, V> collisionMap,
                                 int hashCode,
                                 @Nonnull K hashKey)
    {
        final int bitmask = this.bitmask;
        final HamtNode<K, V>[] children = this.children;
        final CollisionMap.Node value = this.value;
        if (hashCode == 0) {
            final CollisionMap.Node newValue = collisionMap.delete(value, hashKey);
            final int newSize = this.size - collisionMap.size(value) + collisionMap.size(newValue);
            if (newValue == value) {
                return this;
            } else if (collisionMap.size(newValue) == 0) {
                if (bitmask == 0) {
                    return HamtEmptyNode.of();
                } else {
                    return createForDelete(collisionMap, bitmask, newValue, children, newSize);
                }
            } else {
                return new HamtBranchNode<>(bitmask, newValue, children, newSize);
            }
        }
        final int index = hashCode & MASK;
        final int remainder = hashCode >>> SHIFT;
        final int bit = 1 << index;
        final int childIndex = realIndex(bitmask, bit);
        if ((bitmask & bit) == 0) {
            return this;
        } else {
            final HamtNode<K, V> child = children[childIndex];
            final HamtNode<K, V> newChild = child.delete(collisionMap, remainder, hashKey);
            final int newSize = size - child.size(collisionMap) + newChild.size(collisionMap);
            if (newChild == child) {
                return this;
            } else if (newChild.isEmpty(collisionMap)) {
                if (children.length == 1) {
                    if (collisionMap.size(value) == 0) {
                        return HamtEmptyNode.of();
                    } else {
                        return new HamtLeafNode<>(0, value);
                    }
                } else {
                    final HamtNode<K, V>[] newChildren = ArrayHelper.delete(this, children, childIndex);
                    return createForDelete(collisionMap, bitmask & ~bit, value, newChildren, newSize);
                }
            } else {
                final HamtNode<K, V>[] newChildren = ArrayHelper.assign(children, childIndex, newChild);
                return createForDelete(collisionMap, bitmask, value, newChildren, newSize);
            }
        }
    }

    private HamtNode<K, V> createForDelete(@Nonnull CollisionMap<K, V> collisionMap,
                                           int bitmask,
                                           CollisionMap.Node value,
                                           @Nonnull HamtNode<K, V>[] children,
                                           int newSize)
    {
        if (collisionMap.size(value) == 0 && children.length == 1) {
            final HamtNode<K, V> child = children[0];
            if (child instanceof HamtLeafNode) {
                final HamtLeafNode<K, V> leaf = (HamtLeafNode<K, V>)child;
                assert newSize == leaf.size(collisionMap);
                return leaf.liftNode(Integer.numberOfTrailingZeros(bitmask));
            }
            if (child instanceof HamtBranchNode) {
                final HamtBranchNode<K, V> branch = (HamtBranchNode<K, V>)child;
                if (collisionMap.size(branch.value) > 0 && branch.children.length == 0) {
                    assert newSize == collisionMap.size(branch.value);
                    return new HamtLeafNode<>(Integer.numberOfTrailingZeros(bitmask), branch.value);
                }
            }
        }
        return new HamtBranchNode<>(bitmask, value, children, newSize);
    }

    @Override
    public boolean isEmpty(@Nonnull CollisionMap<K, V> collisionMap)
    {
        return bitmask == 0 && collisionMap.size(value) == 0;
    }

    private static int realIndex(int bitmask,
                                 int bit)
    {
        return Integer.bitCount(bitmask & (bit - 1));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    @Override
    public HamtNode<K, V>[] allocate(int size)
    {
        return new HamtNode[size];
    }

    @Nullable
    @Override
    public GenericIterator.State<JImmutableMap.Entry<K, V>> iterateOverRange(@Nonnull CollisionMap<K, V> collisionMap,
                                                                             @Nullable GenericIterator.State<JImmutableMap.Entry<K, V>> parent,
                                                                             int offset,
                                                                             int limit)
    {
        assert offset >= 0 && offset <= limit && limit <= size;
        return GenericIterator.indexedState(parent, indexedForIterator(collisionMap), offset, limit);
    }


    @Override
    public void forEach(@Nonnull CollisionMap<K, V> collisionMap,
                        @Nonnull Proc2<K, V> proc)
    {
        collisionMap.forEach(value, proc);
        for (HamtNode<K, V> child : children) {
            child.forEach(collisionMap, proc);
        }
    }

    @Override
    public <E extends Exception> void forEachThrows(@Nonnull CollisionMap<K, V> collisionMap,
                                                    @Nonnull Proc2Throws<K, V, E> proc)
        throws E
    {
        collisionMap.forEachThrows(value, proc);
        for (HamtNode<K, V> child : children) {
            child.forEachThrows(collisionMap, proc);
        }
    }

    @Override
    public <R> R reduce(@Nonnull CollisionMap<K, V> collisionMap,
                        R sum,
                        @Nonnull Sum2<K, V, R> proc)
    {
        sum = collisionMap.reduce(value, sum, proc);
        for (HamtNode<K, V> child : children) {
            sum = child.reduce(collisionMap, sum, proc);
        }
        return sum;
    }

    @Override
    public <R, E extends Exception> R reduceThrows(@Nonnull CollisionMap<K, V> collisionMap,
                                                   R sum,
                                                   @Nonnull Sum2Throws<K, V, R, E> proc)
        throws E
    {
        sum = collisionMap.reduceThrows(value, sum, proc);
        for (HamtNode<K, V> child : children) {
            sum = child.reduceThrows(collisionMap, sum, proc);
        }
        return sum;
    }

    @Override
    public String toString()
    {
        return "(" + value + ",0x" + Integer.toHexString(bitmask) + "," + children.length + ")";
    }

    private int computeSize(@Nonnull CollisionMap<K, V> collisionMap)
    {
        int answer = collisionMap.size(value);
        for (HamtNode<K, V> child : children) {
            answer += child.size(collisionMap);
        }
        return answer;
    }

    @Override
    public void checkInvariants(@Nonnull CollisionMap<K, V> collisionMap)
    {
        if (size != computeSize(collisionMap)) {
            throw new IllegalStateException(String.format("incorrect size: expected=%d actual=%d", computeSize(collisionMap), size));
        }
        if (collisionMap.size(value) == 0 && children.length == 1) {
            if (children[0] instanceof HamtLeafNode) {
                // we should have replaced ourselves with a leaf
                throw new IllegalStateException();
            }
        }
        for (HamtNode<K, V> child : children) {
            child.checkInvariants(collisionMap);
        }
    }

    @Nonnull
    private Indexed<GenericIterator.Iterable<JImmutableMap.Entry<K, V>>> indexedForIterator(@Nonnull CollisionMap<K, V> collisionMap)
    {
        return new Indexed<GenericIterator.Iterable<JImmutableMap.Entry<K, V>>>()
        {
            @Override
            public GenericIterator.Iterable<JImmutableMap.Entry<K, V>> get(int index)
            {
                if (index == 0) {
                    return collisionMap.genericIterable(value);
                } else {
                    return children[index - 1].genericIterable(collisionMap);
                }
            }

            @Override
            public int size()
            {
                return 1 + children.length;
            }
        };
    }
}
