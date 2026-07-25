package ru.npepub.dto;

/**
 * Input parameters for preparing project context.
 */
public record PrepareRequest(
        String sourcePath,
        String outputPath,
        String limitText
) {}