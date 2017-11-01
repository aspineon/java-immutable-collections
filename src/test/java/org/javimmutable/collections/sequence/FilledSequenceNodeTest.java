///###////////////////////////////////////////////////////////////////////////
//
// Burton Computer Corporation
// http://www.burton-computer.com
//
// Copyright (c) 2017, Burton Computer Corporation
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

package org.javimmutable.collections.sequence;

import junit.framework.TestCase;
import org.javimmutable.collections.InsertableSequence;
import org.javimmutable.collections.Sequence;

import static java.util.Arrays.asList;

public class FilledSequenceNodeTest
    extends TestCase
{
    public void test()
    {
        InsertableSequence<String> seq = FilledSequenceNode.of("z");
        verifyContents(asList("z"), seq);
        verifyContents(asList("y", "z"), seq.insert("y"));
        verifyContents(asList("x", "z"), seq.insert("x"));
    }

    private void verifyContents(Iterable<String> expected,
                                Sequence<String> seq)
    {
        for (String value : expected) {
            assertEquals(true, seq instanceof FilledSequenceNode);
            assertEquals(false, seq.isEmpty());
            assertEquals(value, seq.getHead());
            seq = seq.getTail();
        }
        assertEquals(true, seq instanceof EmptySequenceNode);
        assertEquals(true, seq.isEmpty());
        try {
            seq.getTail();
            fail();
        } catch (UnsupportedOperationException ignored) {
        }
    }
}
