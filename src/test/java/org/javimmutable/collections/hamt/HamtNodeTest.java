package org.javimmutable.collections.hamt;

import junit.framework.TestCase;
import org.javimmutable.collections.MapEntry;
import org.javimmutable.collections.array.trie32.Transforms;
import org.javimmutable.collections.common.MutableDelta;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;

public class HamtNodeTest
    extends TestCase
{
    public void testVarious()
    {
        final Transforms<MapEntry<Integer, String>, Integer, String> transforms = new SingleKeyTransforms<>();
        HamtNode<MapEntry<Integer, String>> empty = HamtNode.of();
        assertEquals(null, empty.getValueOr(transforms, 1, 1, null));
        verifyContents(transforms, empty);

        MutableDelta delta = new MutableDelta();
        HamtNode<MapEntry<Integer, String>> node = empty.assign(transforms, 1, 1, "able", delta);
        assertEquals(1, delta.getValue());
        assertEquals("able", node.getValueOr(transforms, 1, 1, null));
        verifyContents(transforms, node, "able");

        assertSame(node, node.assign(transforms, 1, 1, "able", delta));
        assertEquals(1, delta.getValue());

        node = node.assign(transforms, 1, 1, "baker", delta);
        assertEquals(1, delta.getValue());
        assertEquals("baker", node.getValueOr(transforms, 1, 1, null));
        verifyContents(transforms, node, "baker");

        node = node.assign(transforms, -1, -1, "charlie", delta);
        assertEquals(2, delta.getValue());
        assertEquals("charlie", node.getValueOr(transforms, -1, -1, null));
        verifyContents(transforms, node, "baker", "charlie");

        assertSame(node, node.assign(transforms, -1, -1, "charlie", delta));
        assertEquals(2, delta.getValue());

        node = node.assign(transforms, 7, 7, "delta", delta);
        assertEquals(3, delta.getValue());
        assertEquals("delta", node.getValueOr(transforms, 7, 7, null));
        verifyContents(transforms, node, "baker", "charlie", "delta");

        node = node.assign(transforms, 4725297, 4725297, "echo", delta);
        assertEquals(4, delta.getValue());
        assertEquals("echo", node.getValueOr(transforms, 4725297, 4725297, null));
        verifyContents(transforms, node, "baker", "charlie", "delta", "echo");

        assertSame(node, node.delete(transforms, -2, -2, delta));
        assertEquals(4, delta.getValue());
        verifyContents(transforms, node, "baker", "charlie", "delta", "echo");

        node = node.assign(transforms, 33, 33, "foxtrot", delta);
        assertEquals(5, delta.getValue());
        assertEquals("foxtrot", node.getValueOr(transforms, 33, 33, null));
        verifyContents(transforms, node, "baker", "charlie", "delta", "echo", "foxtrot");

        node = node.delete(transforms, 1, 1, delta);
        assertEquals(4, delta.getValue());
        assertEquals(null, node.getValueOr(transforms, 1, 1, null));
        verifyContents(transforms, node, "charlie", "delta", "echo", "foxtrot");

        assertSame(node, node.delete(transforms, -2, -2, delta));
        assertEquals(4, delta.getValue());

        node = node.delete(transforms, 4725297, 4725297, delta);
        assertEquals(3, delta.getValue());
        assertEquals(null, node.getValueOr(transforms, 4725297, 4725297, null));
        verifyContents(transforms, node, "charlie", "delta", "foxtrot");

        node = node.delete(transforms, -1, -1, delta);
        assertEquals(2, delta.getValue());
        assertEquals(null, node.getValueOr(transforms, -1, -1, null));
        verifyContents(transforms, node, "delta", "foxtrot");

        node = node.delete(transforms, 7, 7, delta);
        assertEquals(1, delta.getValue());
        assertEquals(null, node.getValueOr(transforms, 7, 7, null));
        verifyContents(transforms, node, "foxtrot");

        node = node.delete(transforms, 33, 33, delta);
        assertEquals(0, delta.getValue());
        assertSame(HamtNode.of(), node);
    }

    public void testRandom()
    {
        final Random r = new Random();
        final List<Integer> domain = IntStream.range(1, 1200)
            .boxed()
            .map(i -> r.nextInt())
            .collect(Collectors.toList());

        final Transforms<MapEntry<Integer, Integer>, Integer, Integer> transforms = new SingleKeyTransforms<>();
        final MutableDelta size = new MutableDelta();
        HamtNode<MapEntry<Integer, Integer>> node = HamtNode.of();
        for (Integer key : domain) {
            node = node.assign(transforms, key, key, key, size);
        }
        verifyIntContents(transforms, node, domain);

        final MutableDelta zero = new MutableDelta();
        Collections.shuffle(domain);
        for (Integer key : domain) {
            node = node.delete(transforms, key, key, size);
            assertSame(node, node.delete(transforms, key, key, zero));
        }
        assertSame(HamtNode.of(), node);
        assertEquals(0, size.getValue());
        assertEquals(0, zero.getValue());
    }

    private void verifyContents(Transforms<MapEntry<Integer, String>, Integer, String> transforms,
                                HamtNode<MapEntry<Integer, String>> node,
                                String... values)
    {
        Set<String> expected = new HashSet<>();
        expected.addAll(asList(values));
        Set<String> actual = node.values(transforms).stream().collect(Collectors.toSet());
        assertEquals(expected, actual);
    }

    private void verifyIntContents(Transforms<MapEntry<Integer, Integer>, Integer, Integer> transforms,
                                   HamtNode<MapEntry<Integer, Integer>> node,
                                   List<Integer> values)
    {
        Set<Integer> expected = new HashSet<>();
        expected.addAll(values);
        Set<Integer> actual = node.keys(transforms).stream().collect(Collectors.toSet());
        assertEquals(expected, actual);
    }
}