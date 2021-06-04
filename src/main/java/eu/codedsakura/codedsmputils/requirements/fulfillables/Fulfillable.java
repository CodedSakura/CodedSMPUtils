package eu.codedsakura.codedsmputils.requirements.fulfillables;

import java.util.HashMap;

public abstract class Fulfillable {
    public boolean checked = false, fulfilled = false;

    public abstract HashMap<String, ?> asArguments();
    public abstract String getMissingLocale();
}

