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

package io.github.choseongah.ssh.shell;

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.channel.PtyMode;
import org.jline.terminal.Attributes;

import java.util.Map;

/**
 * Utility tools
 */
@Slf4j
public final class SshShellUtils {

    private SshShellUtils() {
        // private constructor
    }

    /**
     * Fill attributes with given modes
     *
     * @param attr     attributes
     * @param ptyModes pty modes
     */
    public static void fill(Attributes attr, Map<PtyMode, Integer> ptyModes) {
        for (Map.Entry<PtyMode, Integer> e : ptyModes.entrySet()) {
            switch (e.getKey()) {
                case VINTR ->
                        attr.setControlChar(Attributes.ControlChar.VINTR, e.getValue());
                case VQUIT ->
                        attr.setControlChar(Attributes.ControlChar.VQUIT, e.getValue());
                case VERASE ->
                        attr.setControlChar(Attributes.ControlChar.VERASE, e.getValue());
                case VKILL ->
                        attr.setControlChar(Attributes.ControlChar.VKILL, e.getValue());
                case VEOF ->
                        attr.setControlChar(Attributes.ControlChar.VEOF, e.getValue());
                case VEOL ->
                        attr.setControlChar(Attributes.ControlChar.VEOL, e.getValue());
                case VEOL2 ->
                        attr.setControlChar(Attributes.ControlChar.VEOL2, e.getValue());
                case VSTART ->
                        attr.setControlChar(Attributes.ControlChar.VSTART, e.getValue());
                case VSTOP ->
                        attr.setControlChar(Attributes.ControlChar.VSTOP, e.getValue());
                case VSUSP ->
                        attr.setControlChar(Attributes.ControlChar.VSUSP, e.getValue());
                case VDSUSP ->
                        attr.setControlChar(Attributes.ControlChar.VDSUSP, e.getValue());
                case VREPRINT ->
                        attr.setControlChar(Attributes.ControlChar.VREPRINT, e.getValue());
                case VWERASE ->
                        attr.setControlChar(Attributes.ControlChar.VWERASE, e.getValue());
                case VLNEXT ->
                        attr.setControlChar(Attributes.ControlChar.VLNEXT, e.getValue());
                case VSTATUS ->
                        attr.setControlChar(Attributes.ControlChar.VSTATUS, e.getValue());
                case VDISCARD ->
                        attr.setControlChar(Attributes.ControlChar.VDISCARD, e.getValue());
                case CS7 ->
                        attr.setControlFlag(Attributes.ControlFlag.CS7, e.getValue() != 0);
                case CS8 ->
                        attr.setControlFlag(Attributes.ControlFlag.CS8, e.getValue() != 0);
                case PARENB ->
                        attr.setControlFlag(Attributes.ControlFlag.PARENB, e.getValue() != 0);
                case PARODD ->
                        attr.setControlFlag(Attributes.ControlFlag.PARODD, e.getValue() != 0);
                case ECHO ->
                        attr.setLocalFlag(Attributes.LocalFlag.ECHO, e.getValue() != 0);
                case ECHOE ->
                        attr.setLocalFlag(Attributes.LocalFlag.ECHOE, e.getValue() != 0);
                case ECHOK ->
                        attr.setLocalFlag(Attributes.LocalFlag.ECHOK, e.getValue() != 0);
                case ECHONL ->
                        attr.setLocalFlag(Attributes.LocalFlag.ECHONL, e.getValue() != 0);
                case ICANON ->
                        attr.setLocalFlag(Attributes.LocalFlag.ICANON, e.getValue() != 0);
                case ISIG ->
                        attr.setLocalFlag(Attributes.LocalFlag.ISIG, e.getValue() != 0);
                case NOFLSH ->
                        attr.setLocalFlag(Attributes.LocalFlag.NOFLSH, e.getValue() != 0);
                case TOSTOP ->
                        attr.setLocalFlag(Attributes.LocalFlag.TOSTOP, e.getValue() != 0);
                case IEXTEN ->
                        attr.setLocalFlag(Attributes.LocalFlag.IEXTEN, e.getValue() != 0);
                case ECHOCTL ->
                        attr.setLocalFlag(Attributes.LocalFlag.ECHOCTL, e.getValue() != 0);
                case ECHOKE ->
                        attr.setLocalFlag(Attributes.LocalFlag.ECHOKE, e.getValue() != 0);
                case PENDIN ->
                        attr.setLocalFlag(Attributes.LocalFlag.PENDIN, e.getValue() != 0);
                case ICRNL ->
                        attr.setInputFlag(Attributes.InputFlag.ICRNL, e.getValue() != 0);
                case IGNPAR ->
                        attr.setInputFlag(Attributes.InputFlag.IGNPAR, e.getValue() != 0);
                case PARMRK ->
                        attr.setInputFlag(Attributes.InputFlag.PARMRK, e.getValue() != 0);
                case INPCK ->
                        attr.setInputFlag(Attributes.InputFlag.INPCK, e.getValue() != 0);
                case ISTRIP ->
                        attr.setInputFlag(Attributes.InputFlag.ISTRIP, e.getValue() != 0);
                case INLCR ->
                        attr.setInputFlag(Attributes.InputFlag.INLCR, e.getValue() != 0);
                case IGNCR ->
                        attr.setInputFlag(Attributes.InputFlag.IGNCR, e.getValue() != 0);
                case IXON ->
                        attr.setInputFlag(Attributes.InputFlag.IXON, e.getValue() != 0);
                case IXANY ->
                        attr.setInputFlag(Attributes.InputFlag.IXANY, e.getValue() != 0);
                case IXOFF ->
                        attr.setInputFlag(Attributes.InputFlag.IXOFF, e.getValue() != 0);
                case IMAXBEL ->
                        attr.setInputFlag(Attributes.InputFlag.IMAXBEL, e.getValue() != 0);
                case IUTF8 ->
                        attr.setInputFlag(Attributes.InputFlag.IUTF8, e.getValue() != 0);
                case OCRNL ->
                        attr.setOutputFlag(Attributes.OutputFlag.OCRNL, e.getValue() != 0);
                case ONLCR ->
                        attr.setOutputFlag(Attributes.OutputFlag.ONLCR, e.getValue() != 0);
                case ONLRET ->
                        attr.setOutputFlag(Attributes.OutputFlag.ONLRET, e.getValue() != 0);
                case OPOST ->
                        attr.setOutputFlag(Attributes.OutputFlag.OPOST, e.getValue() != 0);
                case ONOCR ->
                        attr.setOutputFlag(Attributes.OutputFlag.ONOCR, e.getValue() != 0);

                case VFLUSH, VSWTCH, IUCLC, XCASE, OLCUC, TTY_OP_ISPEED, TTY_OP_OSPEED -> {
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Unsupported PTY mode [{}] with value [{}] will be ignored", e.getKey(), e.getValue());
                        }
                }
            }
        }
    }

}
