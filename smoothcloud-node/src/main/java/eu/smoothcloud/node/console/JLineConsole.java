/*
 * Copyright (c) 2024 SmoothCloud team & contributors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.smoothcloud.node.console;

import eu.smoothcloud.node.SmoothCloudNode;
import eu.smoothcloud.node.console.modes.DefaultMode;
import eu.smoothcloud.node.console.modes.Mode;
import eu.smoothcloud.node.console.modes.SetupMode;
import eu.smoothcloud.util.console.ConsoleColor;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class JLineConsole {
    private final SmoothCloudNode node;
    private final Terminal terminal;
    private final LineReaderImpl reader;

    private boolean isRunning;
    private boolean isPaused;
    private Mode currentMode;

    public JLineConsole(SmoothCloudNode node) {
        this.node = node;
        this.sendWelcomeMessage();
        try (Terminal terminal = TerminalBuilder.builder()
                        .system(true)
                        .encoding(StandardCharsets.UTF_8)
                        .dumb(true)
                        .jansi(true)
                        .build()) {
            terminal.enterRawMode();
            this.reader = new LineReaderImpl(terminal);
            AttributedString coloredPrefix = new AttributedString(this.prefix());
            this.reader.setPrompt(coloredPrefix.toAnsi());
            this.terminal = terminal;
            this.isRunning = true;
            this.isPaused = false;
            this.currentMode = new DefaultMode(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        while (this.isRunning) {
            AttributedString coloredPrefix = new AttributedString(this.prefix());
            String input = this.reader.readLine(coloredPrefix.toAnsi()).trim();

            if (this.isPaused) {
                if (input.equalsIgnoreCase("resume")) {
                    this.isPaused = false;
                    this.print("Resumed console.");
                }
                continue;
            }

            if (input.equalsIgnoreCase("")) {
                this.print("[FF3333]The input field can not be empty.");
                continue;
            }
            String[] inputParts = input.split(" ");
            String command = inputParts[0];
            String[] args = Arrays.copyOfRange(inputParts, 1, inputParts.length);

            switch (command.toLowerCase()) {
                case "exit" -> {
                    this.isRunning = false;
                    this.print("Exiting console...");
                }
                case "pause" -> {
                    this.isPaused = true;
                    this.print("Paused console.");
                }
                default -> {
                    this.currentMode.handleCommand(command, args);
                }
            }
        }
        this.print("Exited console.");
        System.exit(0);
    }

    public void switchMode(String modeName) {
        switch (modeName.toLowerCase()) {
            case "setup" -> this.currentMode = new SetupMode(this.node, this);
            case "default" -> this.currentMode = new DefaultMode(this);
            default -> this.print("Unknown mode: " + modeName);
        }
    }

    public String prefix() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String prefix = this.currentMode != null
                    ? this.currentMode.getPrefix().replace("%hostname", hostname)
                    : "NoMode";
            return ConsoleColor.apply("\r" + prefix);

        } catch (UnknownHostException e) {
            return ConsoleColor.apply("\r" + this.currentMode.getPrefix().replace("%hostname", "unknown"));
        }
    }

    public void print(String message) {
        if (this.currentMode == null) {
            throw new IllegalStateException("Current mode is not initialized");
        }
        this.print(message, true);
    }

    public void print(String message, boolean newLine) {
        String coloredMessage = ConsoleColor.apply(this.prefix() + message);
        if (newLine) {
            System.out.println(coloredMessage);
            return;
        }
        System.out.print(coloredMessage);
    }

    public void sendWelcomeMessage() {
        System.out.print("\n");
        System.out.print("\n");
        System.out.println(ConsoleColor.apply("       [00FFFF-00BFFF]SmoothCloud &7- &b1.0.0&7@&bdevelopment"));
        System.out.println(ConsoleColor.apply("       &fby &bezTxmMC&7, &bTntTastisch&7, &bSyntaxJason &fand contributors."));
        System.out.print("\n");
        System.out.println(ConsoleColor.apply("       &fType &bhelp &f to list all commands."));
        System.out.print("\n");
        System.out.print("\n");
    }

    public void clear() {
        this.terminal.puts(InfoCmp.Capability.clear_screen);
        this.terminal.flush();
    }

    public Mode getCurrentMode() {
        return this.currentMode;
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isPaused() {
        return this.isPaused;
    }

    public void setRunning(boolean running) {
        this.isRunning = running;
    }

    public boolean isRunning() {
        return this.isRunning;
    }
}
