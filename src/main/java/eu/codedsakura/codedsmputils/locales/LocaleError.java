package eu.codedsakura.codedsmputils.locales;

public class LocaleError extends RuntimeException {
    public boolean fallback = false;

    public LocaleError(String s) {
        super(s + " (fallback=false)");
    }

    public LocaleError(String s, boolean fallback) {
        super(s + " (fallback=" + fallback + ")");
        this.fallback = fallback;
    }
}
