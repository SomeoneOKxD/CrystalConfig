package dev.someoneok.crystalconfig.render;

public record Rect(float x, float y, float w, float h) {
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    public float right() { return x + w; }
    public float bottom() { return y + h; }
    public float centerX() { return x + w * 0.5f; }
    public float centerY() { return y + h * 0.5f; }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px <= right() && py <= bottom();
    }

    public boolean intersects(Rect other) {
        return right() > other.x && other.right() > x && bottom() > other.y && other.bottom() > y;
    }

    public Rect intersect(Rect other) {
        float nx = Math.max(x, other.x);
        float ny = Math.max(y, other.y);
        float nr = Math.min(right(), other.right());
        float nb = Math.min(bottom(), other.bottom());
        return new Rect(nx, ny, Math.max(0, nr - nx), Math.max(0, nb - ny));
    }

    public Rect inset(float amount) {
        return new Rect(x + amount, y + amount, Math.max(0, w - amount * 2), Math.max(0, h - amount * 2));
    }

    public Rect inset(float left, float top, float right, float bottom) {
        return new Rect(x + left, y + top, Math.max(0, w - left - right), Math.max(0, h - top - bottom));
    }

    public Rect move(float dx, float dy) {
        return new Rect(x + dx, y + dy, w, h);
    }

    public Rect withHeight(float height) {
        return new Rect(x, y, w, height);
    }

    public Rect withWidth(float width) {
        return new Rect(x, y, width, h);
    }

    public boolean isEmpty() {
        return w <= 0 || h <= 0;
    }
}
