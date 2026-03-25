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

package io.github.choseongah.ssh.shell.providers;

import io.github.choseongah.ssh.shell.completion.ExtendedFileCompletionProvider;
import org.junit.jupiter.api.Test;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExtendedFileValueProviderTest {

    @Test
    void complete() {
        ExtendedFileCompletionProvider provider = new ExtendedFileCompletionProvider();
        CompletionContext completionContext = mock(CompletionContext.class);
        when(completionContext.currentWordUpToCursor()).thenReturn("src");
        List<CompletionProposal> result = provider.apply(completionContext);
        assertNotEquals(0, result.size());
        when(completionContext.currentWordUpToCursor()).thenReturn("xxx");
        result = provider.apply(completionContext);
        assertEquals(0, result.size());
    }

}
