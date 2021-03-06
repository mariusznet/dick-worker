/*
 * Copyright dick the deployer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dickthedeployer.dick.worker.service;

import com.dickthedeployer.dick.worker.ContextTestBase;
import com.dickthedeployer.dick.worker.command.CommandChainFactory;
import com.dickthedeployer.dick.worker.facade.DickWebFacade;
import com.dickthedeployer.dick.worker.facade.model.BuildForm;
import com.dickthedeployer.dick.worker.facade.model.BuildOrder;
import com.dickthedeployer.dick.worker.facade.model.EnvironmentVariable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

import static com.watchrabbit.commons.sleep.Sleep.sleep;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author mariusz
 */
public class BuildServiceTest extends ContextTestBase {

    @Autowired
    BuildService buildService;

    @Autowired
    DickWebFacade dickWebFacade;

    @Autowired
    CommandChainFactory commandChainFactory;

    @Before
    public void prepare() {
        reset(dickWebFacade);
    }

    @Test
    public void shouldBuildSucessfully() {
        buildService.build(123L, commandChainFactory.produceCommands(BuildOrder.builder()
                .environment(singletonList(
                        EnvironmentVariable.builder()
                                .name("FOO")
                                .value("foo")
                                .build()
                ))
                .commands(produceCommands())
                .build()));

        sleep(6, TimeUnit.SECONDS);

        ArgumentCaptor<BuildForm> captor = ArgumentCaptor.forClass(BuildForm.class);
        verify(dickWebFacade, times(2)).reportProgress(eq(123L), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq(123L), captor.capture());

        assertThat(captor.getValue()).isNotNull();
        if (isWindows()) {
            assertThat(captor.getValue().getLog()).isEqualTo(
                    "Executing command: [cmd.exe, /c, echo, %FOO%]\n"
                            + "Setting environment variable: FOO=foo\n"
                            + "foo\n"
                            + "Executing command: [cmd.exe, /c, ping, 127.0.0.1, -n, 4, >, nul]\n"
                            + "Setting environment variable: FOO=foo\n"
                            + "Executing command: [cmd.exe, /c, echo, bar]\n"
                            + "Setting environment variable: FOO=foo\n"
                            + "bar\n"
            );
        }
    }

    @Test
    public void shouldReportError() {
        buildService.build(123L, commandChainFactory.produceCommands(BuildOrder.builder()
                .environment(emptyList())
                .commands(produceErrorCommands())
                .build()));

        sleep(2, TimeUnit.SECONDS);

        ArgumentCaptor<BuildForm> captor = ArgumentCaptor.forClass(BuildForm.class);
        verify(dickWebFacade, times(1)).reportFailure(eq(123L), captor.capture());

        assertThat(captor.getValue()).isNotNull();
        if (isWindows()) {
            assertThat(captor.getValue().getLog()).isEqualTo(
                    "Executing command: [cmd.exe, /c, return, 1]\n"
                            + "'return' is not recognized as an internal or external command,\n"
                            + "operable program or batch file.\n"
                            + "\n"
                            + "Command exited with non-zero: 1\n"
                            + "\n"
                            + ""
            );
        }
    }

}
