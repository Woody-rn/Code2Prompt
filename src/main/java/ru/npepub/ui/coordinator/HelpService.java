package ru.npepub.ui.coordinator;

import ru.npepub.di.api.C2PComponent;

import java.util.ResourceBundle;

/**
 * Builds help content for the help dialog.
 */
@C2PComponent
public class HelpService {

    /** Builds full help text from resource bundle. */
    public String buildHelpText(ResourceBundle messages) {
        return messages.getString("help.what") + "\n" +
                messages.getString("help.what.text") + "\n\n" +
                messages.getString("help.how") + "\n" +
                messages.getString("help.how.text") + "\n\n" +
                messages.getString("help.cert") + "\n" +
                messages.getString("help.cert.text") + "\n\n" +
                messages.getString("help.extension") + "\n" +
                messages.getString("help.extension.text") + "\n\n" +
                messages.getString("help.exclude") + "\n" +
                messages.getString("help.exclude.text") + "\n\n" +
                messages.getString("help.trouble") + "\n" +
                messages.getString("help.trouble.text");
    }
}