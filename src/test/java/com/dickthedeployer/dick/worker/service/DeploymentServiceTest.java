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
import com.watchrabbit.commons.marker.Todo;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author mariusz
 */
public class DeploymentServiceTest extends ContextTestBase {

    @Autowired
    DeploymentService deploymentService;

    @Test
    @Todo("Synhronous invocaion")
    public void shouldStartContext() {

        deploymentService.deploy("someId",
                asList("cmd.exe /c echo %FOO%",
                        "cmd.exe /c echo %FOO%",
                        "cmd.exe /c ping 127.0.0.1 -n 2 > nul",
                        "cmd.exe /c echo bar"),
                singletonMap("FOO", "foo"));

        System.err.println("");
    }
}
