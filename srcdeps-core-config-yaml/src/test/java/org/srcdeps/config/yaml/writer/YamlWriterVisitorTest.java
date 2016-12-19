/**
 * Copyright 2015-2016 Maven Source Dependencies
 * Plugin contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.srcdeps.config.yaml.writer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import org.junit.Assert;
import org.junit.Test;
import org.srcdeps.config.yaml.YamlConfigurationIo;
import org.srcdeps.core.config.Configuration;
import org.srcdeps.core.config.ConfigurationException;

public class YamlWriterVisitorTest {

    @Test
    public void writeFull() throws ConfigurationException, UnsupportedEncodingException, IOException {
        final StringWriter out = new StringWriter();
        final Configuration configFromFile;
        try (Reader in = new InputStreamReader(getClass().getResourceAsStream("/srcdeps-full.yaml"), "utf-8");
                YamlWriterVisitor writerVisitor = new YamlWriterVisitor(out,
                        YamlWriterConfiguration.builder().build());) {
            configFromFile = new YamlConfigurationIo() //
                    .read(in) //
                    .accept(writerVisitor) //
                    .build();
        }

        /*
         * now read the serialized output we have written to the out StringWriter back into a new Configuration instance
         */
        try (Reader in = new StringReader(out.toString())) {
            Configuration configFromOut = new YamlConfigurationIo() //
                    .read(in) //
                    .build();

            Assert.assertEquals(configFromFile, configFromOut);
        }

    }
}
