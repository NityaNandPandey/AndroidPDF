package com.pdftron.pdf.model;

import android.support.annotation.NonNull;

/**
 * Annotation Property class
 */
public class AnnotationProperty {

    /**
     * Annotation property
     */
    public enum Property {
        /**
         * Opacity
         */
        OPACITY,
        /**
         * Thickness
         */
        THICKNESS,
        /**
         * Color
         */
        COLOR,
        /**
         * Fill color
         */
        FILL_COLOR,
        /**
         * Text size
         */
        TEXT_SIZE,
        /**
         * Text font
         */
        TEXT_FONT,
        /**
         * Text content
         */
        TEXT_CONTENT,
        /**
         * Note content
         */
        NOTE_CONTENT,
        /**
         * Form text content
         */
        FORM_TEXT_CONTENT,
        /**
         * Sticky note icon
         */
        STICKY_NOTE_ICON,
        /**
         * Text markup type
         */
        TEXT_MARKUP_TYPE,
        /**
         * Other
         */
        OTHER
    }

    /**
     * Converts method name from ToolManager callbacks such as
     * {@link com.pdftron.pdf.tools.ToolManager.AnnotationModificationListener}
     * to the String representation of the {@link Property} enum,
     * or the original value if no match found.
     *
     * @param methodName the method name from Bundle
     * @return String representation of {@link Property} enum, or returns original value if no match found
     */
    public static String getPropertyString(@NonNull String methodName) {
        if (methodName.equals("editOpacity")) {
            return Property.OPACITY.toString();
        } else if (methodName.equals("editThickness")) {
            return Property.THICKNESS.toString();
        } else if (methodName.equals("editTextSize")) {
            return Property.TEXT_SIZE.toString();
        } else if (methodName.equals("editFillColor")) {
            return Property.FILL_COLOR.toString();
        } else if (methodName.equals("editFont")) {
            return Property.TEXT_FONT.toString();
        } else if (methodName.equals("editIcon")) {
            return Property.STICKY_NOTE_ICON.toString();
        } else if (methodName.equals("editColor")) {
            return Property.COLOR.toString();
        } else if (methodName.equals("updateFreeText")) {
            return Property.TEXT_CONTENT.toString();
        } else if (methodName.equals("prepareDialogStickyNoteDismiss")) {
            return Property.NOTE_CONTENT.toString();
        } else if (methodName.equals("prepareDialogAnnotNoteDismiss")) {
            return Property.NOTE_CONTENT.toString();
        } else if (methodName.equals("changeAnnotType")) {
            return Property.TEXT_MARKUP_TYPE.toString();
        } else if (methodName.equals("applyFormFieldEditBoxAndQuit")) {
            return Property.FORM_TEXT_CONTENT.toString();
        }
        return methodName;
    }

    /**
     * Converts the result of {@link AnnotationProperty#getPropertyString(String)}
     * to {@link Property} enum.
     *
     * @param methodName the method name from Bundle
     * @return the {@link Property} enum
     */
    public static Property getProperty(@NonNull String methodName) {
        try {
            return Property.valueOf(getPropertyString(methodName));
        } catch (Exception e) {
            return Property.OTHER;
        }
    }
}
