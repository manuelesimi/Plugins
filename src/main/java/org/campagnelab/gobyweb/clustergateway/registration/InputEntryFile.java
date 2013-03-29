package org.campagnelab.gobyweb.clustergateway.registration;

import java.io.File;
import java.net.URI;

/**
 * A file registered as part of an {@link InputEntry}
 */
class InputEntryFile extends File {

    /**
     * state if the file has been already included in a fileset instance
     */
    private boolean consumed = false;

    protected InputEntryFile(String s) {
        super(s);
    }

    protected InputEntryFile(File f) {
        super(f.getAbsolutePath());
    }

    protected InputEntryFile(String s, String s2) {
        super(s, s2);
    }

    protected InputEntryFile(File file, String s) {
        super(file, s);
    }

    protected InputEntryFile(URI uri) {
        super(uri);
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

}
