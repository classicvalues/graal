/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.graalvm.compiler.truffle.test.jdk11;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.graalvm.compiler.serviceprovider.JavaVersionUtil;
import org.graalvm.compiler.truffle.test.PartialEvaluationTest;
import org.graalvm.compiler.truffle.test.nodes.AbstractTestNode;
import org.graalvm.compiler.truffle.test.nodes.RootTestNode;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class VarHandlePartialEvaluationTest extends PartialEvaluationTest {

    static final VarHandle byteArrayHandle = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.nativeOrder());
    static final VarHandle byteBufferHandle = MethodHandles.byteBufferViewVarHandle(int[].class, ByteOrder.nativeOrder());

    /**
     * Tests partial evaluation of a byte array view {@link VarHandle#get}.
     */
    @Test
    public void byteArrayHandle() {
        AbstractTestNode result = new VarHandleTestNode(true);
        testCommon(result, "byteArrayHandle", ByteBuffer.allocate(42).order(ByteOrder.nativeOrder()).putInt(0, 42).array(), 0);
    }

    /**
     * Tests partial evaluation of a byte buffer view {@link VarHandle#get}.
     */
    @Test
    public void byteBufferHandle() {
        Assume.assumeTrue("GR-23778", JavaVersionUtil.JAVA_SPEC <= 11);
        AbstractTestNode result = new VarHandleTestNode(false);
        testCommon(result, "byteBufferHandle", ByteBuffer.allocate(42).order(ByteOrder.nativeOrder()).putInt(0, 42), 0);
    }

    private void testCommon(AbstractTestNode testNode, String testName, Object... args) {
        FrameDescriptor fd = new FrameDescriptor();
        RootNode rootNode = new RootTestNode(fd, testName, testNode);
        RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(rootNode);
        Assert.assertEquals(42, callTarget.call(args));
        assertPartialEvalNoInvokes(callTarget, args);
    }

    static final class VarHandleTestNode extends AbstractTestNode {
        private final boolean isArray;

        VarHandleTestNode(boolean isArray) {
            this.isArray = isArray;
        }

        @Override
        public int execute(VirtualFrame frame) {
            Object buf = frame.getArguments()[0];
            int idx = (int) frame.getArguments()[1];
            if (isArray) {
                return (int) byteArrayHandle.get((byte[]) buf, idx);
            } else {
                return (int) byteBufferHandle.get((ByteBuffer) buf, idx);
            }
        }
    }
}
