package eu.codedsakura.common;

public class Pair<A, B> {
    private A a;
    private B b;

    public Pair(A a, B b) {
        this.a = a;
        this.b = b;
    }

    public A getLeft() {
        return a;
    }
    public B getRight() {
        return b;
    }

    public void setLeft(A a) {
        this.a = a;
    }
    public void setRight(B b) {
        this.b = b;
    }
}
