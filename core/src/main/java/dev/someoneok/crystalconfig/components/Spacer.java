package dev.someoneok.crystalconfig.components;

import dev.someoneok.crystalconfig.layout.Constraints;
import dev.someoneok.crystalconfig.layout.LayoutContext;
import dev.someoneok.crystalconfig.layout.Size;
import dev.someoneok.crystalconfig.ui.Component;

public class Spacer extends Component {
    public Spacer() { }

    @Override
    protected Size measureSelf(LayoutContext context, Constraints constraints) {
        return new Size(Math.max(0, preferredWidth >= 0 ? preferredWidth : minWidth), Math.max(0, preferredHeight >= 0 ? preferredHeight : minHeight));
    }
}
