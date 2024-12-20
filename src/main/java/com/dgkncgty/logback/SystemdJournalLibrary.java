/*
 * This file is part of the logback-journal project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dgkncgty.logback;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * Binding to the native journald library.
 *
 * @author Lucas Satabin
 */
public interface SystemdJournalLibrary extends Library {

    SystemdJournalLibrary INSTANCE = Native
            .load(System.getProperty("systemd.library", "systemd"), SystemdJournalLibrary.class);

    int sd_journal_print(int priority, String format, Object... args);

    int sd_journal_send(String format, Object... args);

    int sd_journal_perror(String message);

}
