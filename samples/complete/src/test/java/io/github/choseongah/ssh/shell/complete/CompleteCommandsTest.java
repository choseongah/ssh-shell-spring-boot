package io.github.choseongah.ssh.shell.complete;

import io.github.choseongah.ssh.shell.SshShellHelper;
import io.github.choseongah.ssh.shell.interactive.Interactive;
import org.jline.terminal.Size;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompleteCommandsTest {

    @Test
    void interactiveBuildsProgressLines() {
        SshShellHelper helper = mock(SshShellHelper.class);
        when(helper.progress(anyInt())).thenReturn("progress");

        CompleteCommands commands = new CompleteCommands(helper);
        commands.interactive(false, 1234);

        ArgumentCaptor<Interactive> interactiveCaptor = ArgumentCaptor.forClass(Interactive.class);
        verify(helper).interactive(interactiveCaptor.capture());

        Interactive interactive = interactiveCaptor.getValue();
        assertFalse(interactive.isFullScreen());
        assertEquals(1234, interactive.getRefreshDelay());

        List<AttributedString> lines = interactive.getInput().getLines(new Size(120, 40), 1234);
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).toString().contains("Current time"));
        assertTrue(lines.get(1).toString().contains("progress"));
        assertTrue(lines.get(2).toString().contains(SshShellHelper.INTERACTIVE_LONG_MESSAGE));
    }
}
