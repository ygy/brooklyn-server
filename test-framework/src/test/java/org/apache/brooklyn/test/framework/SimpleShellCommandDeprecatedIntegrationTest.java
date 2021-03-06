/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.test.framework;

import static org.apache.brooklyn.core.entity.trait.Startable.SERVICE_UP;
import static org.apache.brooklyn.test.framework.SimpleShellCommandTest.ASSERT_OUT;
import static org.apache.brooklyn.test.framework.SimpleShellCommandTest.ASSERT_STATUS;
import static org.apache.brooklyn.test.framework.SimpleShellCommandTest.COMMAND;
import static org.apache.brooklyn.test.framework.SimpleShellCommandTest.DOWNLOAD_URL;
import static org.apache.brooklyn.test.framework.SimpleShellCommandTest.RUN_DIR;
import static org.apache.brooklyn.test.framework.TargetableTestComponent.TARGET_ENTITY;
import static org.apache.brooklyn.test.framework.TestFrameworkAssertions.CONTAINS;
import static org.apache.brooklyn.test.framework.TestFrameworkAssertions.EQUALS;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.core.entity.lifecycle.Lifecycle;
import org.apache.brooklyn.core.entity.lifecycle.ServiceStateLogic;
import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.core.test.entity.TestEntity;
import org.apache.brooklyn.test.Asserts;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.exceptions.Exceptions;
import org.apache.brooklyn.util.text.Identifiers;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * @deprecated since 0.10.0
 */
@Deprecated
public class SimpleShellCommandDeprecatedIntegrationTest extends BrooklynAppUnitTestSupport {

    private static final String UP = "up";

    @DataProvider(name = "shouldInsistOnJustOneOfCommandAndScript")
    public Object[][] createData1() {

        return new Object[][] {
            { "pwd", "pwd.sh", Boolean.FALSE },
            { null, null, Boolean.FALSE },
            { "pwd", null, Boolean.TRUE },
            { null, "pwd.sh", Boolean.TRUE }
        };
    }

    @Test(groups= "Integration", dataProvider = "shouldInsistOnJustOneOfCommandAndScript")
    public void shouldInsistOnJustOneOfCommandAndScript(String command, String script, boolean valid) throws Exception {
        Path scriptPath = null;
        String scriptUrl = null;
        if (null != script) {
            scriptPath = createTempScript("pwd", "pwd");
            scriptUrl = "file:" + scriptPath;
        }
        TestEntity testEntity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(TestApplication.LOCALHOST_MACHINE_SPEC));

        app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
            .configure(TARGET_ENTITY, testEntity)
            .configure(COMMAND, command)
            .configure(DOWNLOAD_URL, scriptUrl));

        try {
            app.start(ImmutableList.<Location>of());
            if (!valid) {
                Asserts.shouldHaveFailedPreviously();
            }

        } catch (Exception e) {
            Asserts.expectedFailureContains(e, "Must specify exactly one of download.url and command");

        } finally {
            if (null != scriptPath) {
                Files.delete(scriptPath);
            }
        }
    }

    private List<Map<String, ?>> makeAssertions(Map<String, ?> ...maps) {
        ArrayList<Map<String, ?>> assertions = new ArrayList<>();
        for (Map<String, ?> map : maps) {
            assertions.add(map);
        }
        return assertions;
    }

    @Test(groups = "Integration")
    public void shouldSucceedUsingSuccessfulExitAsDefaultCondition() {
        TestEntity testEntity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(TestApplication.LOCALHOST_MACHINE_SPEC));

        SimpleShellCommandTest uptime = app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
            .configure(TARGET_ENTITY, testEntity)
            .configure(COMMAND, "uptime"));

        app.start(ImmutableList.<Location>of());

        assertThat(uptime.sensors().get(SERVICE_UP)).isTrue()
            .withFailMessage("Service should be up");
        assertThat(ServiceStateLogic.getExpectedState(uptime)).isEqualTo(Lifecycle.RUNNING)
            .withFailMessage("Service should be marked running");
    }


    @Test(groups = "Integration")
    public void shouldFailUsingSuccessfulExitAsDefaultCondition() {
        TestEntity testEntity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(TestApplication.LOCALHOST_MACHINE_SPEC));

        SimpleShellCommandTest uptime = app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
            .configure(TARGET_ENTITY, testEntity)
            .configure(COMMAND, "ls /tmp/bogus-" + Identifiers.randomLong()));

        try {
            app.start(ImmutableList.<Location>of());
        } catch (Throwable t) {
            Asserts.expectedFailureContains(t, "exit code expected equals 0 but found 1");
        }

        assertThat(uptime.sensors().get(SERVICE_UP)).isFalse()
            .withFailMessage("Service should be down");
        assertThat(ServiceStateLogic.getExpectedState(uptime)).isEqualTo(Lifecycle.ON_FIRE)
            .withFailMessage("Service should be marked on fire");
    }



    @Test(groups = "Integration")
    public void shouldInvokeCommand() {
        TestEntity testEntity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(TestApplication.LOCALHOST_MACHINE_SPEC));

        Map<String, Object> equalsZero = MutableMap.of();
        equalsZero.put(EQUALS, 0);

        Map<String, Object> containsUp = MutableMap.of();
        containsUp.put(CONTAINS, UP);

        SimpleShellCommandTest uptime = app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
            .configure(TARGET_ENTITY, testEntity)
            .configure(COMMAND, "uptime")
            .configure(ASSERT_STATUS, makeAssertions(equalsZero))
            .configure(ASSERT_OUT, makeAssertions(containsUp)));

        app.start(ImmutableList.<Location>of());

        assertThat(uptime.sensors().get(SERVICE_UP)).isTrue()
            .withFailMessage("Service should be up");
        assertThat(ServiceStateLogic.getExpectedState(uptime)).isEqualTo(Lifecycle.RUNNING)
            .withFailMessage("Service should be marked running");

    }

    @Test(groups = "Integration")
    public void shouldNotBeUpIfAssertionsFail() {
        TestEntity testEntity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(TestApplication.LOCALHOST_MACHINE_SPEC));

        Map<String, Object> equalsOne = MutableMap.of();
        equalsOne.put(EQUALS, 1);

        Map<String, Object> equals255 = MutableMap.of();
        equals255.put(EQUALS, 255);

        SimpleShellCommandTest uptime = app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
            .configure(TARGET_ENTITY, testEntity)
            .configure(COMMAND, "uptime")
            .configure(ASSERT_STATUS, makeAssertions(equalsOne, equals255)));

        try {
            app.start(ImmutableList.<Location>of());
            Asserts.shouldHaveFailedPreviously();
        } catch (Exception e) {
            Asserts.expectedFailureContains(e, "exit code expected equals 1", "exit code expected equals 255");
        }

        assertThat(ServiceStateLogic.getExpectedState(uptime)).isEqualTo(Lifecycle.ON_FIRE)
            .withFailMessage("Service should be marked on fire");

    }

    @Test(groups = "Integration")
    public void shouldInvokeScript() throws Exception {
        TestEntity testEntity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(TestApplication.LOCALHOST_MACHINE_SPEC));

        String text = "hello world";
        Path testScript = createTempScript("script", "echo " + text);

        try {

            Map<String, Object> equalsZero = MutableMap.of();
            equalsZero.put(EQUALS, 0);

            Map<String, Object> containsText = MutableMap.of();
            containsText.put(CONTAINS, text);

            SimpleShellCommandTest uptime = app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
                .configure(TARGET_ENTITY, testEntity)
                .configure(DOWNLOAD_URL, "file:" + testScript)
                .configure(ASSERT_STATUS, makeAssertions(equalsZero))
                .configure(ASSERT_OUT, makeAssertions(containsText)));

            app.start(ImmutableList.<Location>of());

            assertThat(uptime.sensors().get(SERVICE_UP)).isTrue()
                .withFailMessage("Service should be up");
            assertThat(ServiceStateLogic.getExpectedState(uptime)).isEqualTo(Lifecycle.RUNNING)
                .withFailMessage("Service should be marked running");

        } finally {
            Files.delete(testScript);
        }
    }

    @Test(groups = "Integration")
    public void shouldExecuteInTheRunDir() throws Exception {
        TestEntity testEntity = app.createAndManageChild(EntitySpec.create(TestEntity.class).location(TestApplication.LOCALHOST_MACHINE_SPEC));

        Path pwdPath = createTempScript("pwd", "pwd");

        try {

            Map<String, Object> equalsZero = MutableMap.of();
            equalsZero.put(EQUALS, 0);

            Map<String, Object> containsTmp = MutableMap.of();
            containsTmp.put(CONTAINS, "/tmp");

            SimpleShellCommandTest pwd = app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
                .configure(TARGET_ENTITY, testEntity)
                .configure(DOWNLOAD_URL, "file:" + pwdPath)
                .configure(RUN_DIR, "/tmp")
                .configure(ASSERT_STATUS, makeAssertions(equalsZero))
                .configure(ASSERT_OUT, makeAssertions(containsTmp)));


            SimpleShellCommandTest alsoPwd = app.createAndManageChild(EntitySpec.create(SimpleShellCommandTest.class)
                .configure(TARGET_ENTITY, testEntity)
                .configure(COMMAND, "pwd")
                .configure(RUN_DIR, "/tmp")
                .configure(ASSERT_STATUS, makeAssertions(equalsZero))
                .configure(ASSERT_OUT, makeAssertions(containsTmp)));

            app.start(ImmutableList.<Location>of());

            assertThat(pwd.sensors().get(SERVICE_UP)).isTrue().withFailMessage("Service should be up");
            assertThat(ServiceStateLogic.getExpectedState(pwd)).isEqualTo(Lifecycle.RUNNING)
                .withFailMessage("Service should be marked running");

            assertThat(alsoPwd.sensors().get(SERVICE_UP)).isTrue().withFailMessage("Service should be up");
            assertThat(ServiceStateLogic.getExpectedState(alsoPwd)).isEqualTo(Lifecycle.RUNNING)
                .withFailMessage("Service should be marked running");

        } finally {
            Files.delete(pwdPath);
        }
    }

    private Path createTempScript(String filename, String contents) {
        try {
            Path tempFile = Files.createTempFile("SimpleShellCommandIntegrationTest-" + filename, ".sh");
            Files.write(tempFile, contents.getBytes());
            return tempFile;
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
    }

}
