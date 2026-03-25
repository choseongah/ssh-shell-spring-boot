/*
 * Copyright (c) 2026 OpenAI Codex with Cho Seong-ah (choseongah)
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

package io.github.choseongah.ssh.shell;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.springframework.shell.core.command.CommandRegistry;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.jline.CommandCompleter;

import java.util.List;

/**
 * Command completer handling post processors and file redirection.
 */
public class SshCommandCompleter implements Completer {

    private final CommandCompleter delegate;

    private final SshPostProcessorService postProcessorService;

    private final io.github.choseongah.ssh.shell.completion.ExtendedFileCompletionProvider fileCompletionProvider =
            new io.github.choseongah.ssh.shell.completion.ExtendedFileCompletionProvider();

    public SshCommandCompleter(CommandRegistry commandRegistry, SshPostProcessorService postProcessorService) {
        this.delegate = new CommandCompleter(commandRegistry);
        this.postProcessorService = postProcessorService;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String beforeCursor = line.line().substring(0, line.cursor());
        int lastPipe = beforeCursor.lastIndexOf('|');
        int lastArrow = beforeCursor.lastIndexOf('>');
        if (lastPipe > lastArrow) {
            for (String postProcessorName : postProcessorService.getPostProcessorNames()) {
                candidates.add(new Candidate(postProcessorName));
            }
            return;
        }
        if (lastArrow > lastPipe) {
            String currentWord = line.word() == null ? "" : line.word().substring(0, line.wordCursor());
            CompletionContext completionContext = new CompletionContext(List.of(currentWord), 0,
                    currentWord.length(), null, null);
            for (CompletionProposal proposal : fileCompletionProvider.apply(completionContext)) {
                candidates.add(new Candidate(proposal.value(), proposal.displayText(), proposal.category(),
                        proposal.description(), null, null, proposal.complete(), 0));
            }
            return;
        }
        delegate.complete(reader, line, candidates);
    }
}
