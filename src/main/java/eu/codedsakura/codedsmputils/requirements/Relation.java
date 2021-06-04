package eu.codedsakura.codedsmputils.requirements;

public enum Relation {
    OR, AND;

    public boolean update(boolean value, boolean newValue) {
        if (this == OR ) return value || newValue;
        if (this == AND) return value && newValue;
        return value;
    }
}
