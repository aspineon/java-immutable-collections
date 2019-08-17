package org.javimmutable.collections.list;

import junit.framework.TestCase;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.*;
import static org.javimmutable.collections.list.EmptyNodeTest.verifyOutOfBounds;
import static org.javimmutable.collections.list.LeafNode.*;
import static org.javimmutable.collections.list.LeafNodeTest.leaf;

public class BranchNodeTest
    extends TestCase
{
    public void testRotateLeft()
    {
        AbstractNode<Integer> expected = branch(branch(leaf(0, MAX_SIZE),
                                                       leaf(MAX_SIZE, 2 * MAX_SIZE)),
                                                branch(leaf(2 * MAX_SIZE, 2 * MAX_SIZE + SPLIT_SIZE),
                                                       leaf(2 * MAX_SIZE + SPLIT_SIZE, 3 * MAX_SIZE + 1)));
        AbstractNode<Integer> actual = branch(leaf(0, MAX_SIZE),
                                              branch(leaf(MAX_SIZE, 2 * MAX_SIZE),
                                                     leaf(2 * MAX_SIZE, 3 * MAX_SIZE)));
        assertThat(actual.append(3 * MAX_SIZE)).isEqualTo(expected);
        assertThat(actual.insert(3 * MAX_SIZE, 3 * MAX_SIZE)).isEqualTo(expected);
    }

    public void testRotateRight()
    {
        AbstractNode<Integer> expected = branch(branch(leaf(-1, SPLIT_SIZE),
                                                       leaf(SPLIT_SIZE, MAX_SIZE)),
                                                branch(leaf(MAX_SIZE, 2 * MAX_SIZE),
                                                       leaf(2 * MAX_SIZE, 3 * MAX_SIZE)));
        AbstractNode<Integer> actual = branch(branch(leaf(0, MAX_SIZE),
                                                     leaf(MAX_SIZE, 2 * MAX_SIZE)),
                                              leaf(2 * MAX_SIZE, 3 * MAX_SIZE));
        assertThat(actual.prepend(-1)).isEqualTo(expected);
        assertThat(actual.insert(0, -1)).isEqualTo(expected);
    }

    public void testDelete()
    {
        AbstractNode<Integer> node = branch(leaf(0, MAX_SIZE), leaf(MAX_SIZE, MAX_SIZE + 2));
        node = node.delete(0);
        assertThat(node).isEqualTo(branch(leaf(1, MAX_SIZE), leaf(MAX_SIZE, MAX_SIZE + 2)));
        node = node.delete(MAX_SIZE);
        assertThat(node).isEqualTo(leaf(1, MAX_SIZE + 1));

        node = branch(leaf(0, 3), leaf(3, MAX_SIZE + 2));
        node = node.delete(2);
        assertThat(node).isEqualTo(branch(leaf(0, 2), leaf(3, MAX_SIZE + 2)));
        node = node.delete(0).delete(0);
        assertThat(node).isEqualTo(leaf(3, MAX_SIZE + 2));

        node = branch(leaf(0, 5), leaf(3, MAX_SIZE));
        node = node.delete(6).delete(5);
        assertThat(node).isEqualTo(leaf(0, MAX_SIZE));

        node = branch(leaf(0, 2), leaf(3, MAX_SIZE + 3));
        node = node.deleteFirst().deleteFirst();
        assertThat(node).isEqualTo(leaf(3, MAX_SIZE + 3));
    }

    public void testPrefix()
    {
        final AbstractNode<Integer> node = branch(branch(leaf(-1, SPLIT_SIZE),
                                                         leaf(SPLIT_SIZE, MAX_SIZE)),
                                                  branch(leaf(MAX_SIZE, 2 * MAX_SIZE),
                                                         leaf(2 * MAX_SIZE, 3 * MAX_SIZE)));
        final int size = 3 * MAX_SIZE + 1;
        assertThat(node.size()).isEqualTo(size);
        assertThat(node.prefix(size)).isSameAs(node);
        assertThat(node.prefix(0)).isSameAs(EmptyNode.instance());
        assertThat(node.prefix(SPLIT_SIZE + 1)).isSameAs(node.left().left());
        assertThat(node.prefix(MAX_SIZE)).isEqualTo(leaf(-1, MAX_SIZE - 1));
        assertThat(node.prefix(2 * MAX_SIZE)).isEqualTo(branch(branch(leaf(-1, SPLIT_SIZE),
                                                                      leaf(SPLIT_SIZE, MAX_SIZE)),
                                                               leaf(MAX_SIZE, 2 * MAX_SIZE - 1)));
    }

    public void testSuffix()
    {
        final AbstractNode<Integer> node = branch(branch(leaf(-1, SPLIT_SIZE),
                                                         leaf(SPLIT_SIZE, MAX_SIZE)),
                                                  branch(leaf(MAX_SIZE, 2 * MAX_SIZE),
                                                         leaf(2 * MAX_SIZE, 3 * MAX_SIZE)));
        final int size = 3 * MAX_SIZE + 1;
        assertThat(node.size()).isEqualTo(size);
        assertThat(node.suffix(size)).isSameAs(EmptyNode.instance());
        assertThat(node.suffix(0)).isSameAs(node);
        assertThat(node.suffix(2 * MAX_SIZE + 1)).isSameAs(node.right().right());
        assertThat(node.suffix(2 * MAX_SIZE + 5)).isEqualTo(leaf(2 * MAX_SIZE + 4, 3 * MAX_SIZE));
        assertThat(node.suffix(SPLIT_SIZE)).isEqualTo(branch(leaf(SPLIT_SIZE - 1, MAX_SIZE),
                                                             branch(leaf(MAX_SIZE, 2 * MAX_SIZE),
                                                                    leaf(2 * MAX_SIZE, 3 * MAX_SIZE))));
    }

    public void testIndexOutOfBounds()
    {
        final AbstractNode<Integer> node = branch(leaf(0, MAX_SIZE), leaf(MAX_SIZE, 2 * MAX_SIZE));
        final int size = 2 * MAX_SIZE;
        assertThat(node.size()).isEqualTo(size);

        verifyOutOfBounds(() -> node.get(-1));
        verifyOutOfBounds(() -> node.get(size));

        verifyOutOfBounds(() -> node.assign(-1, 100));
        verifyOutOfBounds(() -> node.assign(size, 100));

        verifyOutOfBounds(() -> node.insert(-1, 100));
        verifyOutOfBounds(() -> node.insert(size + 1, 100));

        verifyOutOfBounds(() -> node.delete(-1));
        verifyOutOfBounds(() -> node.delete(size));

        verifyOutOfBounds(() -> node.prefix(-1));
        verifyOutOfBounds(() -> node.prefix(size + 1));

        verifyOutOfBounds(() -> node.suffix(-1));
        verifyOutOfBounds(() -> node.suffix(size + 1));
    }

    @Nonnull
    static AbstractNode<Integer> branch(@Nonnull AbstractNode<Integer> left,
                                        @Nonnull AbstractNode<Integer> right)
    {
        return new BranchNode<>(left, right);
    }
}
