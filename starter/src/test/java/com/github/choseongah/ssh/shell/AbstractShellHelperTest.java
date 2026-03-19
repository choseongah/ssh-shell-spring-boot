/*
 * Copyright (c) 2020 François Onimus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.choseongah.ssh.shell;

import org.jline.reader.LineReader;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.mockito.Mockito.*;

public abstract class AbstractShellHelperTest {

    protected static SshShellHelper h;

    protected static LineReader lr;

    protected static Terminal ter;

    protected static PrintWriter writer;

    protected TestNonBlockingReader reader;

    @BeforeEach
    protected void each() {
        lr = mock(LineReader.class);
        ter = mock(Terminal.class);
        writer = spy(new PrintWriter(new StringWriter(), true));
        when(ter.writer()).thenReturn(writer);
        reader = new TestNonBlockingReader();
        when(ter.reader()).thenReturn(reader);
        when(lr.getTerminal()).thenReturn(ter);
        when(lr.getHistory()).thenReturn(new DefaultHistory());

        h = new SshShellHelper(null);
        h.setDefaultTerminal(ter);
        h.setDefaultLineReader(lr);
        when(ter.getType()).thenReturn("osx");
        when(ter.getSize()).thenReturn(new Size(123, 40));
    }

    @AfterEach
    protected void cleanUp() {
        SshShellCommandFactory.SSH_THREAD_CONTEXT.remove();
    }

    protected void setReaderResponses(int... responses) {
        reader.setResponses(responses);
    }

    protected static class TestNonBlockingReader extends NonBlockingReader {

        private final Queue<Integer> responses = new ArrayDeque<>();

        void setResponses(int... values) {
            responses.clear();
            for (int value : values) {
                responses.add(value);
            }
        }

        @Override
        public int readBuffered(char[] b, int off, int len, long timeout) {
            int value = read(timeout, false);
            if (value < 0) {
                return value;
            }
            b[off] = (char) value;
            return 1;
        }

        @Override
        protected int read(long timeout, boolean isPeek) {
            Integer value = responses.peek();
            if (value == null) {
                return READ_EXPIRED;
            }
            if (!isPeek) {
                responses.poll();
            }
            return value;
        }

        @Override
        public void close() {
            responses.clear();
        }
    }
}
