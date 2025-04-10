package com.udacity.catpoint.security.service;

import java.awt.*;

public final class StyleService {
    public static final Font HEADING_FONT = new Font("Sans Serif", Font.BOLD, 24);
    private StyleService() {
        throw new AssertionError("utility class cannot be instantiated");
    }
}