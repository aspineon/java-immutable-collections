package org.javimmutable.collections.array.int_trie;

import org.javimmutable.collections.Cursor;
import org.javimmutable.collections.Holder;
import org.javimmutable.collections.Holders;
import org.javimmutable.collections.JImmutableMap;
import org.javimmutable.collections.common.MutableDelta;
import org.javimmutable.collections.cursors.StandardCursor;

public class EmptyTrieNode<T>
        extends TrieNode<T>
{
    private final int shift;

    EmptyTrieNode(int shift)
    {
        assert shift >= 0;
        this.shift = shift;
    }

    static <T> EmptyTrieNode<T> of(int shift)
    {
        return new EmptyTrieNode<T>(shift);
    }

    @Override
    public boolean isEmpty()
    {
        return true;
    }

    @Override
    public T getValueOr(int shift,
                        int index,
                        T defaultValue)
    {
        assert this.shift == shift;
        return defaultValue;
    }

    @Override
    public Holder<T> find(int shift,
                          int index)
    {
        assert this.shift == shift;
        return Holders.of();
    }

    @Override
    public TrieNode<T> assign(int shift,
                              int index,
                              T value,
                              MutableDelta sizeDelta)
    {
        assert this.shift == shift;
        sizeDelta.add(1);
        return new LeafTrieNode<T>(shift, index, value);
    }

    @Override
    public TrieNode<T> delete(int shift,
                              int index,
                              MutableDelta sizeDelta)
    {
        assert this.shift == shift;
        return this;
    }

    @Override
    public Cursor<JImmutableMap.Entry<Integer, T>> anyOrderEntryCursor()
    {
        return StandardCursor.of();
    }

    @Override
    public Cursor<T> anyOrderValueCursor()
    {
        return StandardCursor.of();
    }
}