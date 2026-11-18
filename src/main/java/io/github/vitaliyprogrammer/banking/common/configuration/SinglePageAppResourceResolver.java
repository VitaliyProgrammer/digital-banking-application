package io.github.vitaliyprogrammer.banking.common.configuration;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the web client's files as they are, and its index.html for any other path without a file extension.
 * The client routes in the browser, so a reload on /app/cards or a link to it must still open the application.
 * A missing file with an extension (a script or an image) stays a 404 instead of quietly turning into a page.
 */
class SinglePageAppResourceResolver extends PathResourceResolver {

    private static final String INDEX_PAGE = "index.html";

    @Override
    protected Resource getResource(String resourcePath, Resource location) throws IOException {

        if (!resourcePath.isEmpty()) {
            Resource requested = location.createRelative(resourcePath);

            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
        }

        return isClientRoute(resourcePath) ? location.createRelative(INDEX_PAGE) : null;
    }

    private static boolean isClientRoute(String resourcePath) {

        String lastSegment = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        return !lastSegment.contains(".");
    }
}
