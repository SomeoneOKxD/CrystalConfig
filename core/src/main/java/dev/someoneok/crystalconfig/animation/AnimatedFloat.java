package dev.someoneok.crystalconfig.animation;

public final class AnimatedFloat {
    private float value;
    private float target;
    private float speed = 18.0f;

    public AnimatedFloat(float value) {
        this.value = value;
        this.target = value;
    }

    public float value() { return value; }
    public float target() { return target; }

    public AnimatedFloat speed(float speed) {
        this.speed = Math.max(0.01f, speed);
        return this;
    }

    public void snap(float value) {
        this.value = value;
        this.target = value;
    }

    public void target(float target) {
        this.target = target;
    }

    public void update(float deltaSeconds) {
        float alpha = 1.0f - (float)Math.exp(-speed * Math.max(0.0f, deltaSeconds));
        value += (target - value) * alpha;
        if (Math.abs(target - value) < 0.00005f) {
            value = target;
        }
    }
}
