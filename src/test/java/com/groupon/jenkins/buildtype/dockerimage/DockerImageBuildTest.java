/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014, Groupon, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.groupon.jenkins.buildtype.dockerimage;

import com.groupon.jenkins.buildtype.InvalidBuildConfigurationException;
import com.groupon.jenkins.dynamic.build.DynamicBuild;
import com.groupon.jenkins.dynamic.build.execution.BuildExecutionContext;
import com.groupon.jenkins.testhelpers.DynamicBuildFactory;
import hudson.Launcher;
import hudson.model.BuildListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class DockerImageBuildTest {

    @Test(expected = InvalidBuildConfigurationException.class)
    public void should_throw_exception_when_dotciyml_is_not_specified() throws IOException, InterruptedException {
        DockerImageBuild dockerImageBuildtype = new DockerImageBuild();
        DynamicBuild dynamicBuild = DynamicBuildFactory.newBuild().get();
        when(dynamicBuild.getGithubRepositoryService().getGHFile(".ci.yml",dynamicBuild.getSha())).thenThrow(FileNotFoundException.class);
        dockerImageBuildtype.runBuild(dynamicBuild, mock(BuildExecutionContext.class), mock(Launcher.class), mock(BuildListener.class));
    }
}