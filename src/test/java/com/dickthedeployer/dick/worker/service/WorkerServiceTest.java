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
import static com.dickthedeployer.dick.worker.ContextTestBase.isWindows;
import com.dickthedeployer.dick.worker.facade.DickWebFacade;
import com.dickthedeployer.dick.worker.facade.model.BuildForm;
import com.dickthedeployer.dick.worker.facade.model.BuildStatus;
import static com.watchrabbit.commons.sleep.Sleep.sleep;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import org.mockito.Mockito;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mariusz
 */
public class WorkerServiceTest extends ContextTestBase {

    @Autowired
    WorkerService workerService;

    @Autowired
    DickWebFacade dickWebFacade;

    @Before
    public void prepare() {
        reset(dickWebFacade);
    }

    @Test
    public void shouldBuildEvenIfDickWebCheckStatusFails() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenThrow(new RuntimeException());

        workerService.performBuild("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(7, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldBuildEvenIfDickWebReportProgressFails() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus());
        Mockito.doThrow(new RuntimeException()).when(dickWebFacade).reportProgress(eq("someId"), any());

        workerService.performBuild("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(7, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldBuildEvenIfDickWebReportSuccessFails() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus());
        Mockito.doThrow(new RuntimeException()).when(dickWebFacade).reportSuccess(eq("someId"), any());

        workerService.performBuild("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(7, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldBuildEvenIfDickWebReportFailureFails() {
        Mockito.doThrow(new RuntimeException()).when(dickWebFacade).reportFailure(eq("someId"), any());
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus());

        workerService.performBuild("someId",
                produceErrorCommands(),
                emptyMap());

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), any());
        verify(dickWebFacade, times(1)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldBuildSucessfullyCheckingIfShouldStop() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus());

        workerService.performBuild("someId",
                produceCommands(),
                singletonMap("FOO", "foo"));

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(2)).reportProgress(eq("someId"), any());
        verify(dickWebFacade, times(1)).reportSuccess(eq("someId"), any());
        verify(dickWebFacade, times(2)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldReportErrorCheckingIfShouldStop() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus());
        workerService.performBuild("someId",
                produceErrorCommands(),
                emptyMap());

        sleep(10, TimeUnit.SECONDS);

        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), any());
        verify(dickWebFacade, times(1)).checkStatus(eq("someId"));
    }

    @Test
    public void shouldStopBuildOnSignalFromWeb() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus(true));
        workerService.performBuild("someId",
                produceCommands(),
                emptyMap());

        sleep(10, TimeUnit.SECONDS);

        ArgumentCaptor<BuildForm> captor = ArgumentCaptor.forClass(BuildForm.class);
        verify(dickWebFacade, times(1)).reportProgress(any(), any());
        verify(dickWebFacade, times(0)).reportSuccess(any(), any());
        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), captor.capture());
        verify(dickWebFacade, times(1)).checkStatus(eq("someId"));
        if (isWindows()) {
            assertThat(captor.getValue().getLog()).isEqualTo(
                    "Executing command: [cmd.exe, /c, echo, %FOO%]\n"
                    + "%FOO%\n"
                    + "Executing command: [cmd.exe, /c, ping, 127.0.0.1, -n, 4, >, nul]\n"
            );
        }
    }

    @Test
    public void shouldStopBuildOnTimeout() {
        when(dickWebFacade.checkStatus(eq("someId"))).thenReturn(new BuildStatus());
        workerService.performBuild("someId",
                produceCommandsWithTimeout(100),
                emptyMap());

        sleep(15, TimeUnit.SECONDS);

        ArgumentCaptor<BuildForm> captor = ArgumentCaptor.forClass(BuildForm.class);
        verify(dickWebFacade, times(1)).reportProgress(any(), any());
        verify(dickWebFacade, times(0)).reportSuccess(any(), any());
        verify(dickWebFacade, times(1)).reportFailure(eq("someId"), captor.capture());
        verify(dickWebFacade, times(3)).checkStatus(eq("someId"));
    }

}