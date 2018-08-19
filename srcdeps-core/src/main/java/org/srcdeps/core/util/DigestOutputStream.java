/**
 * Copyright 2015-2018 Maven Source Dependencies
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
package org.srcdeps.core.util;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link OutputStream} doing nothing else than computing digest out of the input bytes.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class DigestOutputStream extends OutputStream {
    private static final Logger log = LoggerFactory.getLogger(DigestOutputStream.class);
    private final MessageDigest digest;

    public DigestOutputStream(MessageDigest digest) {
        super();
        this.digest = digest;
    }

    /**
     * @return the digest
     */
    public byte[] digest() {
        return digest.digest();
    }

    @Override
    public void write(byte[] b) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("srcdeps: Updating digest with "+ SrcdepsCoreUtils.bytesToHexString(b));
        }
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] input, int offset, int len) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("srcdeps: Updating digest with "+ SrcdepsCoreUtils.bytesToHexString(input, offset, len));
        }
        digest.update(input, offset, len);
    }

    @Override
    public void write(int b) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("srcdeps: Updating digest with "+ SrcdepsCoreUtils.bytesToHexString(new byte[] {(byte) b}));
        }
        digest.update((byte) b);
    }

}
